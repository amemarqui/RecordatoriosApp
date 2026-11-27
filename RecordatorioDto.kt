package com.example.recordatorios.data.remote

/**
 * Contrato del servicio remoto (JSON). Todos los campos son nullable porque
 * Gson puede dejar null cualquier campo que falte en la respuesta: el mapeo
 * valida los datos antes de guardarlos en Room.
 * Este modelo NO llega nunca a las pantallas.
 */
data class RecordatorioDto(
    val id: String? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoriaId: Int? = null,
    val prioridad: String? = null,         // "ALTA" | "MEDIA" | "BAJA"
    val estado: String? = null,            // "PENDIENTE" | "COMPLETADO" | "POSPUESTO" | "VENCIDO"
    val fechaEstado: String? = null,       // ISO-8601, ej. "2026-11-24T21:30:00"
    val fechaLimite: String? = null,
    val fechaCreacion: String? = null,
    val actualizadoEn: Long? = null
)

/** Cuerpo de error que devuelve el servidor (por ejemplo en un 400). */
data class ErrorDto(val mensaje: String? = null)
