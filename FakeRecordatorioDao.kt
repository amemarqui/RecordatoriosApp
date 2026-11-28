package com.example.recordatorios.fakes

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO falso en memoria (mismas reglas que las consultas SQL, filtrando por uid). */
class FakeRecordatorioDao : RecordatorioDao {

    val tabla = MutableStateFlow<Map<String, RecordatorioEntity>>(emptyMap())

    private fun visible(e: RecordatorioEntity, uid: String) =
        e.uid == uid && e.estadoSync != EstadoSyncEntity.PENDIENTE_ELIMINAR

    override fun observarTodos(uid: String): Flow<List<RecordatorioEntity>> =
        tabla.map { m -> m.values.filter { visible(it, uid) } }

    override fun observarNoCompletados(uid: String): Flow<List<RecordatorioEntity>> =
        tabla.map { m -> m.values.filter { visible(it, uid) && it.estado != "COMPLETADO" } }

    override fun observarPorPrioridad(uid: String, prioridad: String): Flow<List<RecordatorioEntity>> =
        tabla.map { m -> m.values.filter { visible(it, uid) && it.prioridad == prioridad } }

    override fun observarPorId(uid: String, id: String): Flow<RecordatorioEntity?> =
        tabla.map { m -> m[id]?.takeIf { visible(it, uid) } }

    override fun observarCantidadSinSincronizar(uid: String): Flow<Int> =
        tabla.map { m -> m.values.count { it.uid == uid && it.estadoSync != EstadoSyncEntity.SINCRONIZADO } }

    override suspend fun insertar(recordatorio: RecordatorioEntity) {
        check(recordatorio.id !in tabla.value) { "UNIQUE constraint failed" }
        tabla.value = tabla.value + (recordatorio.id to recordatorio)
    }

    override suspend fun actualizar(recordatorio: RecordatorioEntity) {
        if (recordatorio.id in tabla.value) tabla.value = tabla.value + (recordatorio.id to recordatorio)
    }

    override suspend fun upsert(recordatorio: RecordatorioEntity) {
        tabla.value = tabla.value + (recordatorio.id to recordatorio)
    }

    override suspend fun eliminar(recordatorio: RecordatorioEntity) = eliminarPorId(recordatorio.uid, recordatorio.id)

    override suspend fun obtenerPorId(uid: String, id: String): RecordatorioEntity? =
        tabla.value[id]?.takeIf { it.uid == uid }

    override suspend fun obtenerPendientes(uid: String): List<RecordatorioEntity> = tabla.value.values
        .filter { it.uid == uid && (it.estadoSync == EstadoSyncEntity.PENDIENTE || it.estadoSync == EstadoSyncEntity.PENDIENTE_ELIMINAR) }
        .sortedBy { it.actualizadoEn }

    override suspend fun contarSinSincronizar(uid: String): Int =
        tabla.value.values.count { it.uid == uid && it.estadoSync != EstadoSyncEntity.SINCRONIZADO }

    override suspend fun marcarSincronizado(uid: String, id: String, version: Long): Int =
        cambiarSi(uid, id, { it.actualizadoEn == version }) {
            it.copy(estadoSync = EstadoSyncEntity.SINCRONIZADO, existeEnServidor = true, intentosSync = 0, ultimoErrorSync = null)
        }

    override suspend fun registrarFalloTemporal(uid: String, id: String, mensaje: String) {
        cambiarSi(uid, id, { true }) { it.copy(intentosSync = it.intentosSync + 1, ultimoErrorSync = mensaje) }
    }

    override suspend fun marcarError(uid: String, id: String, version: Long, mensaje: String): Int =
        cambiarSi(uid, id, { it.actualizadoEn == version }) {
            it.copy(estadoSync = EstadoSyncEntity.ERROR, intentosSync = it.intentosSync + 1, ultimoErrorSync = mensaje)
        }

    override suspend fun volverAPendiente(uid: String, id: String): Int =
        cambiarSi(uid, id, { it.estadoSync == EstadoSyncEntity.ERROR }) {
            it.copy(estadoSync = EstadoSyncEntity.PENDIENTE, ultimoErrorSync = null)
        }

    override suspend fun eliminarPorId(uid: String, id: String) {
        if (tabla.value[id]?.uid == uid) tabla.value = tabla.value - id
    }

    override suspend fun eliminarSincronizadosAusentes(uid: String, idsRemotos: List<String>) {
        tabla.value = tabla.value.filterValues {
            it.uid != uid || it.estadoSync != EstadoSyncEntity.SINCRONIZADO || it.id in idsRemotos
        }
    }

    override suspend fun eliminarSincronizadosDe(uid: String): Int {
        val antes = tabla.value.size
        tabla.value = tabla.value.filterValues { !(it.uid == uid && it.estadoSync == EstadoSyncEntity.SINCRONIZADO) }
        return antes - tabla.value.size
    }

    override suspend fun eliminarSinDuenio(): Int {
        val antes = tabla.value.size
        tabla.value = tabla.value.filterValues { it.uid.isNotEmpty() }
        return antes - tabla.value.size
    }

    private fun cambiarSi(
        uid: String,
        id: String,
        condicion: (RecordatorioEntity) -> Boolean,
        cambio: (RecordatorioEntity) -> RecordatorioEntity
    ): Int {
        val actual = tabla.value[id]?.takeIf { it.uid == uid } ?: return 0
        if (!condicion(actual)) return 0
        tabla.value = tabla.value + (id to cambio(actual))
        return 1
    }
}
