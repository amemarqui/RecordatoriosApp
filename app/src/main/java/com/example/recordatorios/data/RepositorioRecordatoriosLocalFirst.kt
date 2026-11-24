package com.example.recordatorios.data

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.mapeo.aDominio
import com.example.recordatorios.data.mapeo.aEpochMs
import com.example.recordatorios.data.mapeo.codigo
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

class RepositorioRecordatoriosLocalFirst(
    private val dao: RecordatorioDao,
    private val preferencias: PreferenciasRepository,
    private val sincronizador: SincronizadorRecordatorios,
    private val conectado: Flow<Boolean>,
    private val alcanceApp: CoroutineScope,          // para sincronizar en segundo plano
    private val reloj: () -> Long = { System.currentTimeMillis() },
    private val generarId: () -> String = { UUID.randomUUID().toString() }
) : RecordatoriosRepository {

    /* ---------- Lectura: siempre desde Room ---------- */

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarRecordatorios(): Flow<List<Recordatorio>> =
        preferencias.preferencias
            .map { it.mostrarCompletados to it.orden }
            .distinctUntilChanged()          // cambiar el tema no vuelve a consultar la base
            .flatMapLatest { (mostrarCompletados, orden) ->
                val consulta = if (mostrarCompletados) dao.observarTodos() else dao.observarNoCompletados()
                consulta.map { entidades ->
                    entidades.mapNotNull { it.aDominio() }.ordenarSegun(orden)
                }
            }

    override fun observarPorId(id: String): Flow<Recordatorio?> =
        dao.observarPorId(id).map { it?.aDominio() }

    override fun observarSincronizacion(): Flow<InfoSincronizacion> = combine(
        conectado,
        sincronizador.estado,
        dao.observarCantidadSinSincronizar()
    ) { hayRed, estado, sinSincronizar ->
        InfoSincronizacion(
            conectado = hayRed,
            sincronizando = estado.sincronizando,
            cambiosSinSincronizar = sinSincronizar,
            ultimoResultado = estado.ultimoResultado,
            ultimaVezMs = estado.ultimaVezMs
        )
    }

    /* ---------- Escritura: primero Room (queda PENDIENTE), después se intenta sincronizar ---------- */

    override suspend fun crear(nuevo: NuevoRecordatorio): ResultadoOperacion<String> = operacionLocal {
        val ahora = reloj()
        val entidad = RecordatorioEntity(
            id = generarId(),
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

    /** No se borra enseguida: queda marcado y se oculta, así el servidor también se entera. */
    override suspend fun eliminar(id: String): ResultadoOperacion<Unit> =
        modificar(id, nuevoEstadoSync = EstadoSyncEntity.PENDIENTE_ELIMINAR) { it }

    override suspend fun reintentarSincronizacion(id: String): ResultadoOperacion<Unit> = operacionLocal {
        dao.volverAPendiente(id)
        solicitarSincronizacion()
    }

    override suspend fun sincronizarAhora(): ResultadoSincronizacion = sincronizador.sincronizar()

    override fun categoriasDisponibles(): Set<Categoria> = RepositorioSimulado.categorias

    /* ---------- Auxiliares ---------- */

    private suspend fun modificar(
        id: String,
        nuevoEstadoSync: EstadoSyncEntity = EstadoSyncEntity.PENDIENTE,
        cambio: (RecordatorioEntity) -> RecordatorioEntity
    ): ResultadoOperacion<Unit> {
        val encontrado: RecordatorioEntity? = try {
            dao.obtenerPorId(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ResultadoOperacion.Error(MENSAJE_ERROR_LOCAL)
        }
        val actual = encontrado ?: return ResultadoOperacion.Error("El recordatorio ya no existe")

        return operacionLocal {
            dao.actualizar(
                cambio(actual).copy(
                    actualizadoEn = reloj(),        // nueva versión
                    estadoSync = nuevoEstadoSync,
                    ultimoErrorSync = null
                )
            )
            solicitarSincronizacion()
        }
    }

    /** Envuelve las operaciones de Room: si falla el almacenamiento, devuelve un error controlado. */
    private suspend fun <T> operacionLocal(bloque: suspend () -> T): ResultadoOperacion<T> = try {
        ResultadoOperacion.Exito(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoOperacion.Error(MENSAJE_ERROR_LOCAL)
    }

    /** Intenta sincronizar en segundo plano sin hacer esperar a la pantalla. */
    private fun solicitarSincronizacion() {
        alcanceApp.launch { sincronizador.sincronizar() }
    }

    private companion object {
        const val MENSAJE_ERROR_LOCAL = "No se pudo guardar en el almacenamiento del teléfono. Intentá de nuevo."
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
