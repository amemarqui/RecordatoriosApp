package com.example.recordatorios.data

import com.example.recordatorios.data.sync.ResultadoSincronizacion
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/** Datos que carga el usuario en el formulario (crear o editar). */
data class NuevoRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val fechaLimite: LocalDateTime? = null
)

/** Resultado de una operación local (Room). */
sealed interface ResultadoOperacion<out T> {
    data class Exito<T>(val valor: T) : ResultadoOperacion<T>
    data class Error(val mensaje: String) : ResultadoOperacion<Nothing>
}

/** Resumen del estado de sincronización para mostrar en la interfaz. */
data class InfoSincronizacion(
    val conectado: Boolean = true,
    val sincronizando: Boolean = false,
    val cambiosSinSincronizar: Int = 0,
    val ultimoResultado: ResultadoSincronizacion? = null,
    val ultimaVezMs: Long? = null
)

/**
 * Contrato de la capa de datos para los ViewModel (consigna 3).
 * Local-first: todo lo que se lee sale de Room, y todo lo que se escribe va primero a Room;
 * la nube (Firestore) se sincroniza aparte. Ningún DTO, entidad ni clase de Firebase sale de acá.
 * Actividad 4: todas las operaciones usan el uid del usuario autenticado.
 */
interface RecordatoriosRepository {
    /** Lista ya filtrada y ordenada según las preferencias de DataStore. */
    fun observarRecordatorios(): Flow<List<Recordatorio>>
    fun observarPorId(id: String): Flow<Recordatorio?>
    fun observarSincronizacion(): Flow<InfoSincronizacion>

    suspend fun crear(nuevo: NuevoRecordatorio): ResultadoOperacion<String>
    suspend fun actualizar(id: String, datos: NuevoRecordatorio): ResultadoOperacion<Unit>
    suspend fun cambiarCompletado(id: String, completado: Boolean): ResultadoOperacion<Unit>
    suspend fun eliminar(id: String): ResultadoOperacion<Unit>
    suspend fun reintentarSincronizacion(id: String): ResultadoOperacion<Unit>
    suspend fun sincronizarAhora(): ResultadoSincronizacion

    /**
     * Actividad 4: antes de cerrar sesión intenta sincronizar y borra del teléfono lo que
     * ya está en la nube. Devuelve cuántos cambios quedaron sin sincronizar.
     */
    suspend fun prepararCierreDeSesion(): ResultadoOperacion<Int>

    /** Actividad 4 (prueba de seguridad): intenta leer y escribir datos de OTRA cuenta. */
    suspend fun probarAccesoAOtraCuenta(uidAjeno: String): String

    fun categoriasDisponibles(): Set<Categoria>
}
