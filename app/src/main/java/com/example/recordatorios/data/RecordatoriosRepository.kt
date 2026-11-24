package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.flow.Flow

/**
 * Datos que carga el usuario en el formulario. No lleva id porque
 * el id lo asigna la fuente de datos al guardar.
 */
data class NuevoRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad
)

/**
 * Contrato de la fuente de datos (consigna 4).
 * Los ViewModel dependen de esta interfaz y no de una lista concreta,
 * así más adelante se puede cambiar por Room o por una API sin tocar las pantallas.
 */
interface RecordatoriosRepository {

    /** Flujo con la lista actual; vuelve a emitir cada vez que se guarda algo. */
    fun observarRecordatorios(): Flow<List<Recordatorio>>

    /** Devuelve el recordatorio con ese id o null si no existe. */
    suspend fun obtenerPorId(id: Int): Recordatorio?

    /** Guarda un recordatorio nuevo y lo devuelve con su id asignado. */
    suspend fun guardar(nuevo: NuevoRecordatorio): Recordatorio

    /** Opciones válidas para el selector de categoría del formulario. */
    fun categoriasDisponibles(): Set<Categoria>
}
