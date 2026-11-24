package com.example.recordatorios.fakes

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO falso en memoria para probar la sincronización sin Room ni emulador. */
class FakeRecordatorioDao : RecordatorioDao {

    val tabla = MutableStateFlow<Map<String, RecordatorioEntity>>(emptyMap())

    private fun visibles() = tabla.value.values.filter { it.estadoSync != EstadoSyncEntity.PENDIENTE_ELIMINAR }

    override fun observarTodos(): Flow<List<RecordatorioEntity>> =
        tabla.map { m -> m.values.filter { it.estadoSync != EstadoSyncEntity.PENDIENTE_ELIMINAR } }

    override fun observarNoCompletados(): Flow<List<RecordatorioEntity>> =
        tabla.map { m -> m.values.filter { it.estadoSync != EstadoSyncEntity.PENDIENTE_ELIMINAR && it.estado != "COMPLETADO" } }

    override fun observarPorPrioridad(prioridad: String): Flow<List<RecordatorioEntity>> =
        tabla.map { m -> m.values.filter { it.estadoSync != EstadoSyncEntity.PENDIENTE_ELIMINAR && it.prioridad == prioridad } }

    override fun observarPorId(id: String): Flow<RecordatorioEntity?> =
        tabla.map { m -> m[id]?.takeIf { it.estadoSync != EstadoSyncEntity.PENDIENTE_ELIMINAR } }

    override fun observarCantidadSinSincronizar(): Flow<Int> =
        tabla.map { m -> m.values.count { it.estadoSync != EstadoSyncEntity.SINCRONIZADO } }

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

    override suspend fun eliminar(recordatorio: RecordatorioEntity) = eliminarPorId(recordatorio.id)

    override suspend fun obtenerPorId(id: String): RecordatorioEntity? = tabla.value[id]

    override suspend fun obtenerPendientes(): List<RecordatorioEntity> = tabla.value.values
        .filter { it.estadoSync == EstadoSyncEntity.PENDIENTE || it.estadoSync == EstadoSyncEntity.PENDIENTE_ELIMINAR }
        .sortedBy { it.actualizadoEn }

    override suspend fun marcarSincronizado(id: String, version: Long): Int =
        cambiarSi(id, { it.actualizadoEn == version }) {
            it.copy(estadoSync = EstadoSyncEntity.SINCRONIZADO, existeEnServidor = true, intentosSync = 0, ultimoErrorSync = null)
        }

    override suspend fun registrarFalloTemporal(id: String, mensaje: String) {
        cambiarSi(id, { true }) { it.copy(intentosSync = it.intentosSync + 1, ultimoErrorSync = mensaje) }
    }

    override suspend fun marcarError(id: String, version: Long, mensaje: String): Int =
        cambiarSi(id, { it.actualizadoEn == version }) {
            it.copy(estadoSync = EstadoSyncEntity.ERROR, intentosSync = it.intentosSync + 1, ultimoErrorSync = mensaje)
        }

    override suspend fun volverAPendiente(id: String): Int =
        cambiarSi(id, { it.estadoSync == EstadoSyncEntity.ERROR }) {
            it.copy(estadoSync = EstadoSyncEntity.PENDIENTE, ultimoErrorSync = null)
        }

    override suspend fun eliminarPorId(id: String) {
        tabla.value = tabla.value - id
    }

    override suspend fun eliminarSincronizadosAusentes(idsRemotos: List<String>) {
        tabla.value = tabla.value.filterValues { it.estadoSync != EstadoSyncEntity.SINCRONIZADO || it.id in idsRemotos }
    }

    private fun cambiarSi(
        id: String,
        condicion: (RecordatorioEntity) -> Boolean,
        cambio: (RecordatorioEntity) -> RecordatorioEntity
    ): Int {
        val actual = tabla.value[id] ?: return 0
        if (!condicion(actual)) return 0
        tabla.value = tabla.value + (id to cambio(actual))
        return 1
    }

    fun cantidadVisibles() = visibles().size
}
