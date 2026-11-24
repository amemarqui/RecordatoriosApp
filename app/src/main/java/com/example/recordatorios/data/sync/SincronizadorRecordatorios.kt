package com.example.recordatorios.data.sync

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.mapeo.aEntityONull
import com.example.recordatorios.data.remote.RecordatoriosApi
import com.example.recordatorios.data.remote.ResultadoRed
import com.example.recordatorios.data.remote.esTemporal
import com.example.recordatorios.data.remote.llamadaSegura
import com.example.recordatorios.data.remote.mensaje
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sincroniza Room con el servicio remoto.
 *
 * Cómo evita duplicados:
 * 1. Un Mutex: aunque varios eventos pidan sincronizar a la vez (vuelve la red, se guarda algo,
 *    el usuario toca "Sincronizar"), corre UNA sola sincronización. Si llega otro pedido mientras
 *    tanto, se anota y se hace una vuelta más al terminar.
 * 2. El id es un UUID generado en el teléfono y se envía con PUT /recordatorios/{id} (upsert):
 *    si un envío se repite (ej: hubo timeout pero el servidor sí lo guardó), queda un solo registro.
 * 3. Se marca como SINCRONIZADO solo si la versión (actualizadoEn) no cambió durante el envío.
 *    Nunca se crea un registro nuevo al sincronizar: se actualiza el estado del mismo.
 */
class SincronizadorRecordatorios(
    private val dao: RecordatorioDao,
    private val api: RecordatoriosApi,
    private val hayConexion: () -> Boolean,
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

    private suspend fun sincronizarUnaVez(): ResultadoSincronizacion {
        if (!hayConexion()) return ResultadoSincronizacion.SinConexion
        return try {
            val subida = enviarPendientes()
            if (subida !is ResultadoSincronizacion.Exito) return subida
            val bajada = descargarCambios()
            if (bajada !is ResultadoSincronizacion.Exito) return bajada
            subida.copy(recibidos = bajada.recibidos, descartados = bajada.descartados)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Error de Room (almacenamiento): no se pierde nada, se informa
            ResultadoSincronizacion.Error("Error del almacenamiento local: ${e.message}")
        }
    }

    /** 1) Sube lo pendiente: creaciones, modificaciones y eliminaciones hechas sin conexión. */
    private suspend fun enviarPendientes(): ResultadoSincronizacion {
        var enviados = 0
        var rechazados = 0
        for (pendiente in dao.obtenerPendientes()) {
            val resultado = enviar(pendiente)
            when {
                resultado is ResultadoRed.Exito -> {
                    if (pendiente.estadoSync == EstadoSyncEntity.PENDIENTE_ELIMINAR) {
                        dao.eliminarPorId(pendiente.id)
                    } else {
                        dao.marcarSincronizado(pendiente.id, pendiente.actualizadoEn)
                    }
                    enviados++
                }
                resultado.esTemporal -> {
                    // Sin red / timeout / 5xx: sigue PENDIENTE y se corta acá (los demás tampoco van a pasar)
                    dao.registrarFalloTemporal(pendiente.id, resultado.mensaje())
                    return resultado.aResultadoSincronizacion()
                }
                else -> {
                    // 4xx: el servidor rechaza ESTE registro. Queda en ERROR y se sigue con los demás.
                    dao.marcarError(pendiente.id, pendiente.actualizadoEn, resultado.mensaje())
                    rechazados++
                }
            }
        }
        return ResultadoSincronizacion.Exito(enviados = enviados, recibidos = 0, rechazados = rechazados)
    }

    private suspend fun enviar(entidad: RecordatorioEntity): ResultadoRed<*> =
        if (entidad.estadoSync == EstadoSyncEntity.PENDIENTE_ELIMINAR) {
            // 404 = ya no existía en el servidor: para nosotros es lo mismo que eliminado
            llamadaSegura(codigosAceptados = setOf(404)) { api.eliminar(entidad.id) }
        } else {
            llamadaSegura { api.guardar(entidad.id, entidad.aDto()) }
        }

    /** 2) Baja los datos del servidor y los guarda en Room (las pantallas se enteran por Flow). */
    private suspend fun descargarCambios(): ResultadoSincronizacion {
        val resultado = llamadaSegura { api.obtenerTodos() }
        if (resultado !is ResultadoRed.Exito) return resultado.aResultadoSincronizacion()

        val remotos = resultado.datos.orEmpty()
        val idsRemotos = remotos.mapNotNull { it.id }
        var recibidos = 0
        var descartados = 0

        for (dto in remotos) {
            val remoto = dto.aEntityONull()
            if (remoto == null) {
                descartados++          // dato remoto inválido: no entra a Room
                continue
            }
            val local = dao.obtenerPorId(remoto.id)
            val debeGuardarse = when {
                local == null -> true                                            // nuevo en el servidor
                local.estadoSync != EstadoSyncEntity.SINCRONIZADO -> false       // hay un cambio local sin enviar: gana el local
                else -> local.actualizadoEn != remoto.actualizadoEn              // cambió en el servidor
            }
            if (debeGuardarse) {
                dao.upsert(
                    remoto.copy(
                        // campos que el servicio no maneja todavía: se conservan los locales
                        nombreUbicacion = local?.nombreUbicacion,
                        latitud = local?.latitud,
                        longitud = local?.longitud,
                        minutosAviso = local?.minutosAviso
                    )
                )
                recibidos++
            }
        }
        // Los que estaban sincronizados y el servidor ya no tiene, se eliminaron en otro dispositivo
        dao.eliminarSincronizadosAusentes(idsRemotos)
        return ResultadoSincronizacion.Exito(enviados = 0, recibidos = recibidos, descartados = descartados)
    }

    private fun ResultadoRed<*>.aResultadoSincronizacion(): ResultadoSincronizacion = when (this) {
        ResultadoRed.SinConexion -> ResultadoSincronizacion.SinConexion
        ResultadoRed.Timeout -> ResultadoSincronizacion.Timeout
        is ResultadoRed.ErrorServidor -> ResultadoSincronizacion.ErrorServidor(codigo)
        else -> ResultadoSincronizacion.Error(mensaje())
    }
}
