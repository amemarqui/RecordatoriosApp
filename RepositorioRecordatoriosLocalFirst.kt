package com.example.recordatorios.data

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.mapeo.aDominio
import com.example.recordatorios.data.mapeo.aEpochMs
import com.example.recordatorios.data.mapeo.codigo
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.remote.ResultadoRed
import com.example.recordatorios.data.sync.ResultadoSincronizacion
import com.example.recordatorios.data.sync.SincronizadorRecordatorios
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RepositorioRecordatoriosLocalFirst(
    private val dao: RecordatorioDao,
    private val preferencias: PreferenciasRepository,
    private val sincronizador: SincronizadorRecordatorios,
    private val remota: FuenteRemotaRecordatorios,
    private val conectado: Flow<Boolean>,
    /** uid del usuario autenticado (null = sin sesión). */
    private val uidSesion: Flow<String?>,
    private val uidActual: () -> String?,
    private val alcanceApp: CoroutineScope,
    private val reloj: () -> Long = { System.currentTimeMillis() },
    private val generarId: () -> String = { UUID.randomUUID().toString() }
) : RecordatoriosRepository {

    /* ---------- Lectura: siempre desde Room y SOLO del usuario actual ---------- */

    override fun observarRecordatorios(): Flow<List<Recordatorio>> =
        combine(uidSesion, preferencias.preferencias) { uid, prefs ->
            Triple(uid, prefs.mostrarCompletados, prefs.orden)
        }
            .distinctUntilChanged()          // cambiar el tema no vuelve a consultar la base
            .flatMapLatest { (uid, mostrarCompletados, orden) ->
                if (uid == null) return@flatMapLatest flowOf(emptyList())
                val consulta = if (mostrarCompletados) dao.observarTodos(uid) else dao.observarNoCompletados(uid)
                consulta.map { entidades -> entidades.mapNotNull { it.aDominio() }.ordenarSegun(orden) }
            }

    override fun observarPorId(id: String): Flow<Recordatorio?> =
        uidSesion.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) flowOf(null) else dao.observarPorId(uid, id).map { it?.aDominio() }
        }

    override fun observarSincronizacion(): Flow<InfoSincronizacion> {
        val sinSincronizar = uidSesion.distinctUntilChanged().flatMapLatest { uid ->
            if (uid == null) flowOf(0) else dao.observarCantidadSinSincronizar(uid)
        }
        return combine(conectado, sincronizador.estado, sinSincronizar) { hayRed, estado, cantidad ->
            InfoSincronizacion(
                conectado = hayRed,
                sincronizando = estado.sincronizando,
                cambiosSinSincronizar = cantidad,
                ultimoResultado = estado.ultimoResultado,
                ultimaVezMs = estado.ultimaVezMs
            )
        }
    }

    /* ---------- Escritura: primero Room (queda PENDIENTE), después se sincroniza ---------- */

    override suspend fun crear(nuevo: NuevoRecordatorio): ResultadoOperacion<String> {
        val uid = uidActual() ?: return SIN_SESION
        return operacionLocal {
            val ahora = reloj()
            val entidad = RecordatorioEntity(
                id = generarId(),
                uid = uid,
                titulo = nuevo.titulo.trim(),
                descripcion = nuevo.descripcion?.trim()?.ifEmpty { null },
                categoriaId = nuevo.categoria.id,
                prioridad = nuevo.prioridad.codigo(),
                estado = "PENDIENTE",
                fechaEstado = null,
                fechaLimite = nuevo.fechaLimite?.aEpochMs(),
                fechaCreacion = ahora,
                actualizadoEn = ahora,
                estadoSync = EstadoSyncEntity.PENDIENTE,
                existeEnServidor = false
            )
            dao.insertar(entidad)
            solicitarSincronizacion()
            entidad.id
        }
    }

    override suspend fun actualizar(id: String, datos: NuevoRecordatorio): ResultadoOperacion<Unit> =
        modificar(id) { actual ->
            actual.copy(
                titulo = datos.titulo.trim(),
                descripcion = datos.descripcion?.trim()?.ifEmpty { null },
                categoriaId = datos.categoria.id,
                prioridad = datos.prioridad.codigo(),
                fechaLimite = datos.fechaLimite?.aEpochMs()
            )
        }

    override suspend fun cambiarCompletado(id: String, completado: Boolean): ResultadoOperacion<Unit> =
        modificar(id) { actual ->
            if (completado) actual.copy(estado = "COMPLETADO", fechaEstado = reloj())
            else actual.copy(estado = "PENDIENTE", fechaEstado = null)
        }

    /** No se borra enseguida: queda marcado y se oculta, así la nube también se entera. */
    override suspend fun eliminar(id: String): ResultadoOperacion<Unit> =
        modificar(id, nuevoEstadoSync = EstadoSyncEntity.PENDIENTE_ELIMINAR) { it }

    override suspend fun reintentarSincronizacion(id: String): ResultadoOperacion<Unit> {
        val uid = uidActual() ?: return SIN_SESION
        return operacionLocal {
            dao.volverAPendiente(uid, id)
            solicitarSincronizacion()
        }
    }

    override suspend fun sincronizarAhora(): ResultadoSincronizacion = sincronizador.sincronizar()

    override fun categoriasDisponibles(): Set<Categoria> = RepositorioSimulado.categorias

    /* ---------- Cierre de sesión ---------- */

    override suspend fun prepararCierreDeSesion(): ResultadoOperacion<Int> {
        val uid = uidActual() ?: return SIN_SESION
        sincronizador.sincronizar()                  // último intento de subir lo pendiente
        return operacionLocal {
            // Lo que ya está en la nube se borra del teléfono: la próxima cuenta no lo ve
            // y el dueño lo recupera al volver a entrar. Lo pendiente se conserva (solo su dueño lo ve).
            dao.eliminarSincronizadosDe(uid)
            sincronizador.reiniciarEstado()
            dao.contarSinSincronizar(uid)
        }
    }

    /* ---------- Prueba de seguridad ---------- */

    override suspend fun probarAccesoAOtraCuenta(uidAjeno: String): String {
        val propio = uidActual() ?: return "Iniciá sesión para hacer la prueba."
        val objetivo = uidAjeno.trim()
        if (objetivo.isEmpty()) return "Ingresá el uid de otra cuenta."
        if (objetivo == propio) return "Ese es tu propio uid: ingresá el de OTRA cuenta."

        val lectura = remota.obtenerTodos(objetivo)
        val escritura = remota.guardar(
            objetivo,
            RecordatorioDto(
                id = "prueba-seguridad", titulo = "Intento de escritura ajena", categoriaId = 1,
                prioridad = "BAJA", estado = "PENDIENTE", fechaCreacion = LocalDateTime.now().toString(),
                actualizadoEn = reloj()
            )
        )
        return "Lectura de users/$objetivo/reminders: ${describir(lectura)}\n" +
            "Escritura en users/$objetivo/reminders: ${describir(escritura)}"
    }

    private fun describir(resultado: ResultadoRed<*>): String = when (resultado) {
        is ResultadoRed.Exito -> "PERMITIDA ⚠ (las reglas de seguridad no están protegiendo los datos)"
        is ResultadoRed.SolicitudInvalida -> "DENEGADA ✔ (${resultado.codigo}: ${resultado.mensaje})"
        ResultadoRed.SinConexion -> "no se pudo probar: sin conexión"
        ResultadoRed.Timeout -> "no se pudo probar: tiempo agotado"
        is ResultadoRed.ErrorServidor -> "no se pudo probar: error del servicio (${resultado.codigo})"
        is ResultadoRed.Desconocido -> "no se pudo probar: ${resultado.mensaje}"
    }

    /* ---------- Auxiliares ---------- */

    private suspend fun modificar(
        id: String,
        nuevoEstadoSync: EstadoSyncEntity = EstadoSyncEntity.PENDIENTE,
        cambio: (RecordatorioEntity) -> RecordatorioEntity
    ): ResultadoOperacion<Unit> {
        val uid = uidActual() ?: return SIN_SESION
        val encontrado: RecordatorioEntity? = try {
            dao.obtenerPorId(uid, id)                // solo encuentra registros de ESTE usuario
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ResultadoOperacion.Error(MENSAJE_ERROR_LOCAL)
        }
        val actual = encontrado ?: return ResultadoOperacion.Error("El recordatorio ya no existe")

        return operacionLocal {
            dao.actualizar(
                cambio(actual).copy(
                    actualizadoEn = reloj(),
                    estadoSync = nuevoEstadoSync,
                    ultimoErrorSync = null
                )
            )
            solicitarSincronizacion()
        }
    }

    private suspend fun <T> operacionLocal(bloque: suspend () -> T): ResultadoOperacion<T> = try {
        ResultadoOperacion.Exito(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoOperacion.Error(MENSAJE_ERROR_LOCAL)
    }

    private fun solicitarSincronizacion() {
        alcanceApp.launch { sincronizador.sincronizar() }
    }

    private companion object {
        const val MENSAJE_ERROR_LOCAL = "No se pudo guardar en el almacenamiento del teléfono. Intentá de nuevo."
        val SIN_SESION = ResultadoOperacion.Error("No hay una sesión iniciada.")
    }
}

/** Ordena según la preferencia guardada en DataStore. */
fun List<Recordatorio>.ordenarSegun(orden: OrdenRecordatorios): List<Recordatorio> = when (orden) {
    OrdenRecordatorios.PRIORIDAD -> sortedWith(
        compareByDescending<Recordatorio> { it.prioridad.peso }
            .thenBy { it.fechaLimite ?: LocalDateTime.MAX }
    )
    OrdenRecordatorios.FECHA_LIMITE -> sortedBy { it.fechaLimite ?: LocalDateTime.MAX }
    OrdenRecordatorios.TITULO -> sortedBy { it.titulo.lowercase() }
    OrdenRecordatorios.RECIENTES -> sortedByDescending { it.fechaCreacion ?: LocalDateTime.MIN }
}
