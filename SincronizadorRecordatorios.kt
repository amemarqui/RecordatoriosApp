package com.example.recordatorios.data.sync

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.mapeo.aEntityONull
import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.ResultadoRed
import com.example.recordatorios.data.remote.esTemporal
import com.example.recordatorios.data.remote.mensaje
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sincroniza Room con la nube (Firestore) para el usuario autenticado.
 *
 * Cómo evita duplicados:
 * 1. Un Mutex: aunque varios eventos pidan sincronizar a la vez (vuelve la red, se guarda algo,
 *    se inicia sesión, el usuario toca "Sincronizar"), corre UNA sola sincronización.
 * 2. El id es un UUID generado en el teléfono y es el id del documento en Firestore
 *    (users/{uid}/reminders/{id}): un reintento sobrescribe el mismo documento.
 * 3. Se marca SINCRONIZADO solo si la versión (actualizadoEn) no cambió durante el envío.
 */
class SincronizadorRecordatorios(
    private val dao: RecordatorioDao,
    private val remota: FuenteRemotaRecordatorios,
    private val hayConexion: () -> Boolean,
    private val uidActual: () -> String?,
    private val reloj: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()
    private val otroPedidoMientrasCorria = AtomicBoolean(false)

    private val _estado = MutableStateFlow(EstadoSincronizador())
    val estado: StateFlow<EstadoSincronizador> = _estado.asStateFlow()

    suspend fun sincronizar(): ResultadoSincronizacion {
        if (!mutex.tryLock()) {
            otroPedidoMientrasCorria.set(true)
            return ResultadoSincronizacion.YaEnCurso
        }
        try {
            _estado.update { it.copy(sincronizando = true) }
            var resultado: ResultadoSincronizacion
            var vueltas = 0
            do {
                otroPedidoMientrasCorria.set(false)
                resultado = sincronizarUnaVez()
                vueltas++
            } while (otroPedidoMientrasCorria.get() && resultado is ResultadoSincronizacion.Exito && vueltas < 3)

            _estado.update { EstadoSincronizador(false, resultado, reloj()) }
            return resultado
        } finally {
            _estado.update { it.copy(sincronizando = false) }
            mutex.unlock()
        }
    }

    /** Al cambiar de cuenta se borra el último resultado (no mostrar el de la sesión anterior). */
    fun reiniciarEstado() {
        _estado.value = EstadoSincronizador()
    }

    private suspend fun sincronizarUnaVez(): ResultadoSincronizacion {
        val uid = uidActual() ?: return ResultadoSincronizacion.SinSesion
        if (!hayConexion()) return ResultadoSincronizacion.SinConexion
        return try {
            val subida = enviarPendientes(uid)
            if (subida !is ResultadoSincronizacion.Exito) return subida
            val bajada = descargarCambios(uid)
            if (bajada !is ResultadoSincronizacion.Exito) return bajada
            subida.copy(recibidos = bajada.recibidos, descartados = bajada.descartados)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoSincronizacion.Error("Error del almacenamiento local: ${e.message}")
        }
    }

    /** 1) Sube lo pendiente: creaciones, modificaciones y eliminaciones hechas sin conexión. */
    private suspend fun enviarPendientes(uid: String): ResultadoSincronizacion {
        var enviados = 0
        var rechazados = 0
        for (pendiente in dao.obtenerPendientes(uid)) {
            val resultado = enviar(uid, pendiente)
            when {
                resultado is ResultadoRed.Exito -> {
                    if (pendiente.estadoSync == EstadoSyncEntity.PENDIENTE_ELIMINAR) {
                        dao.eliminarPorId(uid, pendiente.id)
                    } else {
                        dao.marcarSincronizado(uid, pendiente.id, pendiente.actualizadoEn)
                    }
                    enviados++
                }
                resultado.esTemporal -> {
                    // Sin red / timeout / servicio caído: sigue PENDIENTE y se corta acá
                    dao.registrarFalloTemporal(uid, pendiente.id, resultado.mensaje())
                    return resultado.aResultadoSincronizacion()
                }
                else -> {
                    // Rechazado (datos inválidos o permiso denegado): queda en ERROR y se sigue con los demás
                    dao.marcarError(uid, pendiente.id, pendiente.actualizadoEn, resultado.mensaje())
                    rechazados++
                }
            }
        }
        return ResultadoSincronizacion.Exito(enviados = enviados, recibidos = 0, rechazados = rechazados)
    }

    private suspend fun enviar(uid: String, entidad: RecordatorioEntity): ResultadoRed<*> =
        if (entidad.estadoSync == EstadoSyncEntity.PENDIENTE_ELIMINAR) {
            remota.eliminar(uid, entidad.id)
        } else {
            remota.guardar(uid, entidad.aDto())
        }

    /** 2) Baja los datos de la nube y los guarda en Room (las pantallas se enteran por Flow). */
    private suspend fun descargarCambios(uid: String): ResultadoSincronizacion {
        val resultado = remota.obtenerTodos(uid)
        if (resultado !is ResultadoRed.Exito) return resultado.aResultadoSincronizacion()

        val remotos = resultado.datos.orEmpty()
        val idsRemotos = remotos.mapNotNull { it.id }
        var recibidos = 0
        var descartados = 0

        for (dto in remotos) {
            val remoto = dto.aEntityONull(uid)
            if (remoto == null) {
                descartados++
                continue
            }
            val local = dao.obtenerPorId(uid, remoto.id)
            val debeGuardarse = when {
                local == null -> true                                            // nuevo (ej: creado en otro dispositivo)
                local.estadoSync != EstadoSyncEntity.SINCRONIZADO -> false       // hay un cambio local sin enviar: gana el local
                else -> local.actualizadoEn != remoto.actualizadoEn              // cambió en la nube
            }
            if (debeGuardarse) {
                dao.upsert(
                    remoto.copy(
                        nombreUbicacion = local?.nombreUbicacion,
                        latitud = local?.latitud,
                        longitud = local?.longitud,
                        minutosAviso = local?.minutosAviso
                    )
                )
                recibidos++
            }
        }
        dao.eliminarSincronizadosAusentes(uid, idsRemotos)
        return ResultadoSincronizacion.Exito(enviados = 0, recibidos = recibidos, descartados = descartados)
    }

    private fun ResultadoRed<*>.aResultadoSincronizacion(): ResultadoSincronizacion = when (this) {
        ResultadoRed.SinConexion -> ResultadoSincronizacion.SinConexion
        ResultadoRed.Timeout -> ResultadoSincronizacion.Timeout
        is ResultadoRed.ErrorServidor -> ResultadoSincronizacion.ErrorServidor(codigo)
        else -> ResultadoSincronizacion.Error(mensaje())
    }
}
