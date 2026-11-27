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
 * Operaciones sobre la tabla. Actividad 4: TODAS las consultas reciben el uid
 * del usuario autenticado, así una cuenta nunca lee registros de otra aunque
 * compartan el mismo teléfono.
 */
@Dao
interface RecordatorioDao {

    /* ---------- Consultas observables (para la interfaz) ---------- */

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync != 'PENDIENTE_ELIMINAR'")
    fun observarTodos(uid: String): Flow<List<RecordatorioEntity>>

    /** Filtro por estado: se usa cuando la preferencia "mostrar completados" está apagada. */
    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync != 'PENDIENTE_ELIMINAR' AND estado != 'COMPLETADO'")
    fun observarNoCompletados(uid: String): Flow<List<RecordatorioEntity>>

    /** Filtro por prioridad. */
    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync != 'PENDIENTE_ELIMINAR' AND prioridad = :prioridad")
    fun observarPorPrioridad(uid: String, prioridad: String): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND id = :id AND estadoSync != 'PENDIENTE_ELIMINAR'")
    fun observarPorId(uid: String, id: String): Flow<RecordatorioEntity?>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE uid = :uid AND estadoSync != 'SINCRONIZADO'")
    fun observarCantidadSinSincronizar(uid: String): Flow<Int>

    /* ---------- Escritura ---------- */

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(recordatorio: RecordatorioEntity)

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    /** Inserta o reemplaza (lo usa la sincronización al bajar datos de la nube). */
    @Upsert
    suspend fun upsert(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun eliminar(recordatorio: RecordatorioEntity)

    /* ---------- Para la sincronización ---------- */

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND id = :id")
    suspend fun obtenerPorId(uid: String, id: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE uid = :uid AND estadoSync IN ('PENDIENTE', 'PENDIENTE_ELIMINAR') ORDER BY actualizadoEn")
    suspend fun obtenerPendientes(uid: String): List<RecordatorioEntity>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE uid = :uid AND estadoSync != 'SINCRONIZADO'")
    suspend fun contarSinSincronizar(uid: String): Int

    /**
     * Marca como sincronizado SOLO si el registro no cambió mientras se enviaba
     * (misma versión). Si el usuario lo editó en el medio, queda pendiente.
     */
    @Query(
        "UPDATE recordatorios SET estadoSync = 'SINCRONIZADO', existeEnServidor = 1, " +
            "intentosSync = 0, ultimoErrorSync = NULL WHERE uid = :uid AND id = :id AND actualizadoEn = :version"
    )
    suspend fun marcarSincronizado(uid: String, id: String, version: Long): Int

    /** Error temporal (sin red, timeout, servicio no disponible): sigue pendiente. */
    @Query("UPDATE recordatorios SET intentosSync = intentosSync + 1, ultimoErrorSync = :mensaje WHERE uid = :uid AND id = :id")
    suspend fun registrarFalloTemporal(uid: String, id: String, mensaje: String)

    /** Error permanente (datos rechazados / permiso denegado): queda en ERROR hasta reintentar. */
    @Query(
        "UPDATE recordatorios SET estadoSync = 'ERROR', intentosSync = intentosSync + 1, " +
            "ultimoErrorSync = :mensaje WHERE uid = :uid AND id = :id AND actualizadoEn = :version"
    )
    suspend fun marcarError(uid: String, id: String, version: Long, mensaje: String): Int

    @Query("UPDATE recordatorios SET estadoSync = 'PENDIENTE', ultimoErrorSync = NULL WHERE uid = :uid AND id = :id AND estadoSync = 'ERROR'")
    suspend fun volverAPendiente(uid: String, id: String): Int

    @Query("DELETE FROM recordatorios WHERE uid = :uid AND id = :id")
    suspend fun eliminarPorId(uid: String, id: String)

    /** Borra los que ya estaban sincronizados y la nube ya no tiene (se borraron en otro dispositivo). */
    @Query("DELETE FROM recordatorios WHERE uid = :uid AND estadoSync = 'SINCRONIZADO' AND id NOT IN (:idsRemotos)")
    suspend fun eliminarSincronizadosAusentes(uid: String, idsRemotos: List<String>)

    /* ---------- Cierre de sesión ---------- */

    /** Al cerrar sesión: lo que ya está en la nube se borra del teléfono (se recupera al volver a entrar). */
    @Query("DELETE FROM recordatorios WHERE uid = :uid AND estadoSync = 'SINCRONIZADO'")
    suspend fun eliminarSincronizadosDe(uid: String): Int

    /** Registros sin dueño (de versiones anteriores a la Actividad 4): no se muestran a nadie. */
    @Query("DELETE FROM recordatorios WHERE uid = ''")
    suspend fun eliminarSinDuenio(): Int
}
