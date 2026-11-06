package com.example.recordatorios.model

import java.time.LocalDateTime

/**
 * CONSIGNA 2 — Modelado de datos.
 *
 * Todas las clases de datos son inmutables (solo `val`). Para "modificar" un
 * recordatorio se usa copy(), que devuelve una instancia nueva.
 */

/** Categoría de un recordatorio. Al ser data class, dos categorías con el mismo
 *  id y nombre son iguales (equals/hashCode), lo que permite deduplicarlas en un Set. */
data class Categoria(
    val id: Int,
    val nombre: String
) {
    init {
        require(nombre.isNotBlank()) { "El nombre de la categoría no puede estar vacío" }
    }
}

/** Prioridad: conjunto cerrado de valores. Cada uno tiene un peso para ordenar. */
sealed class Prioridad(val etiqueta: String, val peso: Int) {
    data object Alta : Prioridad("Alta", 3)
    data object Media : Prioridad("Media", 2)
    data object Baja : Prioridad("Baja", 1)

    companion object {
        val todas: List<Prioridad> get() = listOf(Alta, Media, Baja)
    }
}

/** Estado de un recordatorio. Completado y Pospuesto llevan datos propios. */
sealed class EstadoRecordatorio(val etiqueta: String) {
    data object Pendiente : EstadoRecordatorio("Pendiente")
    data class Completado(val fechaCompletado: LocalDateTime) : EstadoRecordatorio("Completado")
    data class Pospuesto(val hasta: LocalDateTime) : EstadoRecordatorio("Pospuesto")
    data object Vencido : EstadoRecordatorio("Vencido")
}

/** Ubicación opcional asociada a un recordatorio (se usará en módulos siguientes). */
data class Ubicacion(
    val nombre: String,
    val latitud: Double,
    val longitud: Double
) {
    init {
        require(latitud in -90.0..90.0) { "Latitud fuera de rango: $latitud" }
        require(longitud in -180.0..180.0) { "Longitud fuera de rango: $longitud" }
    }
}

/**
 * Recordatorio. Los datos opcionales (descripción, fecha límite, ubicación,
 * aviso) son nullable con valor por defecto null.
 *
 * El bloque init garantiza que nunca pueda existir un Recordatorio inválido.
 */
data class Recordatorio(
    val id: Int,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
    val descripcion: String? = null,
    val fechaLimite: LocalDateTime? = null,
    val ubicacion: Ubicacion? = null,
    val minutosAvisoPrevio: Int? = null
) {
    init {
        require(id > 0) { "El id debe ser positivo" }
        require(titulo.isNotBlank()) { "El título no puede estar vacío" }
        require(titulo.length <= MAX_TITULO) { "El título supera los $MAX_TITULO caracteres" }
        require(minutosAvisoPrevio == null || minutosAvisoPrevio > 0) {
            "El aviso previo debe ser mayor a 0 minutos"
        }
        require(minutosAvisoPrevio == null || fechaLimite != null) {
            "No se puede configurar un aviso sin fecha límite"
        }
    }

    companion object {
        const val MAX_TITULO = 60
    }
}

/** Estado de la pantalla principal (UI state). */
sealed class EstadoUi {
    data object Cargando : EstadoUi()
    data class Exito(val recordatorios: List<Recordatorio>) : EstadoUi()
    data object Vacio : EstadoUi()
    data class Error(val mensaje: String) : EstadoUi()
}
