package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Operaciones sobre la tabla. Las consultas que usa la interfaz devuelven Flow:
 * cuando cambia la tabla, Room vuelve a emitir y las pantallas se actualizan solas.
 * Los registros marcados para eliminar no se muestran.
 */
@Dao
interface RecordatorioDao {

    /* ---------- Consultas observables (para la interfaz) ---------- */

    @Query("SELECT * FROM recordatorios WHERE estadoSync != 'PENDIENTE_ELIMINAR'")
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    /** Filtro por estado: se usa cuando la preferencia "mostrar completados" está apagada. */
    @Query("SELECT * FROM recordatorios WHERE estadoSync != 'PENDIENTE_ELIMINAR' AND estado != 'COMPLETADO'")
    fun observarNoCompletados(): Flow<List<RecordatorioEntity>>

    /** Filtro por prioridad. */
    @Query("SELECT * FROM recordatorios WHERE estadoSync != 'PENDIENTE_ELIMINAR' AND prioridad = :prioridad")
    fun observarPorPrioridad(prioridad: String): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND estadoSync != 'PENDIENTE_ELIMINAR'")
    fun observarPorId(id: String): Flow<RecordatorioEntity?>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE estadoSync != 'SINCRONIZADO'")
    fun observarCantidadSinSincronizar(): Flow<Int>

    /* ---------- Escritura ---------- */

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(recordatorio: RecordatorioEntity)

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    /** Inserta o reemplaza (lo usa la sincronización al bajar datos del servidor). */
    @Upsert
    suspend fun upsert(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun eliminar(recordatorio: RecordatorioEntity)

    /* ---------- Para la sincronización ---------- */

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun obtenerPorId(id: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE estadoSync IN ('PENDIENTE', 'PENDIENTE_ELIMINAR') ORDER BY actualizadoEn")
    suspend fun obtenerPendientes(): List<RecordatorioEntity>

    /**
     * Marca como sincronizado SOLO si el registro no cambió mientras se enviaba
     * (misma versión). Si el usuario lo editó en el medio, queda pendiente y se envía de nuevo.
     * Devuelve la cantidad de filas actualizadas.
     */
    @Query(
        "UPDATE recordatorios SET estadoSync = 'SINCRONIZADO', existeEnServidor = 1, " +
            "intentosSync = 0, ultimoErrorSync = NULL WHERE id = :id AND actualizadoEn = :version"
    )
    suspend fun marcarSincronizado(id: String, version: Long): Int

    /** Error temporal (sin red, timeout, 5xx): sigue pendiente, se suma un intento. */
    @Query("UPDATE recordatorios SET intentosSync = intentosSync + 1, ultimoErrorSync = :mensaje WHERE id = :id")
    suspend fun registrarFalloTemporal(id: String, mensaje: String)

    /** Error permanente (400): queda en ERROR hasta que el usuario reintente. */
    @Query(
        "UPDATE recordatorios SET estadoSync = 'ERROR', intentosSync = intentosSync + 1, " +
            "ultimoErrorSync = :mensaje WHERE id = :id AND actualizadoEn = :version"
    )
    suspend fun marcarError(id: String, version: Long, mensaje: String): Int

    @Query("UPDATE recordatorios SET estadoSync = 'PENDIENTE', ultimoErrorSync = NULL WHERE id = :id AND estadoSync = 'ERROR'")
    suspend fun volverAPendiente(id: String): Int

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    /** Borra los que ya estaban sincronizados y el servidor ya no tiene (se eliminaron en otro lado). */
    @Query("DELETE FROM recordatorios WHERE estadoSync = 'SINCRONIZADO' AND id NOT IN (:idsRemotos)")
    suspend fun eliminarSincronizadosAusentes(idsRemotos: List<String>)
}
