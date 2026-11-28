package com.example.recordatorios.model

import java.time.LocalDate
import java.time.LocalDateTime

/** Resultado de validar los datos de un recordatorio antes de crearlo. */
sealed class ResultadoValidacion {
    data class Valido(val recordatorio: Recordatorio) : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

/**
 * Errores del formulario separados por campo (Actividad 2).
 * Así cada campo puede mostrar su propio mensaje con isError + supportingText.
 * null = el campo está bien.
 */
data class ErroresFormulario(
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val prioridad: String? = null,
    val fechaLimite: String? = null
) {
    val hayErrores: Boolean
        get() = titulo != null || descripcion != null || categoria != null ||
            prioridad != null || fechaLimite != null
}

/**
 * Reglas de validación del producto. A diferencia del init de Recordatorio
 * (que lanza una excepción), este validador junta los errores y los
 * devuelve sin romper la app.
 */
object ValidadorRecordatorio {

    /**
     * Reglas del formulario de creación (Actividad 2, consigna 2):
     * - título: al menos 4 caracteres después del trim()
     * - descripción: máximo 200 caracteres
     * - categoría: tiene que ser una de las opciones definidas
     * - prioridad: obligatoria
     */
    fun validarFormulario(
        titulo: String,
        descripcion: String,
        categoria: Categoria?,
        prioridad: Prioridad?,
        categoriasValidas: Set<Categoria>,
        fechaLimite: LocalDate? = null,      // Actividad 3: fecha opcional
        hoy: LocalDate = LocalDate.now()
    ): ErroresFormulario {
        val tituloLimpio = titulo.trim()
        return ErroresFormulario(
            titulo = when {
                tituloLimpio.length < Recordatorio.MIN_TITULO ->
                    "El título debe tener al menos ${Recordatorio.MIN_TITULO} caracteres"
                tituloLimpio.length > Recordatorio.MAX_TITULO ->
                    "El título no puede superar ${Recordatorio.MAX_TITULO} caracteres"
                else -> null
            },
            descripcion = if (descripcion.length > Recordatorio.MAX_DESCRIPCION) {
                "La descripción no puede superar ${Recordatorio.MAX_DESCRIPCION} caracteres"
            } else null,
            categoria = when {
                categoria == null -> "Seleccioná una categoría"
                categoria !in categoriasValidas -> "La categoría no es una opción válida"
                else -> null
            },
            prioridad = if (prioridad == null) "Seleccioná una prioridad" else null,
            fechaLimite = if (fechaLimite != null && fechaLimite.isBefore(hoy)) {
                "La fecha límite no puede ser anterior a hoy"
            } else null
        )
    }

    /** Validación completa de la Actividad 1 (se mantiene, ahora con las reglas nuevas del título y la descripción). */
    fun validar(
        id: String,
        titulo: String,
        categoria: Categoria?,
        prioridad: Prioridad?,
        estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
        descripcion: String? = null,
        fechaLimite: LocalDateTime? = null,
        ubicacion: Ubicacion? = null,
        minutosAvisoPrevio: Int? = null,
        ahora: LocalDateTime = LocalDateTime.now()
    ): ResultadoValidacion {
        val errores = mutableListOf<String>()

        if (id.isBlank()) errores += "El id es obligatorio"
        if (titulo.trim().length < Recordatorio.MIN_TITULO) {
            errores += "El título debe tener al menos ${Recordatorio.MIN_TITULO} caracteres"
        }
        if (titulo.trim().length > Recordatorio.MAX_TITULO) {
            errores += "El título no puede superar ${Recordatorio.MAX_TITULO} caracteres"
        }
        if ((descripcion?.length ?: 0) > Recordatorio.MAX_DESCRIPCION) {
            errores += "La descripción no puede superar ${Recordatorio.MAX_DESCRIPCION} caracteres"
        }
        if (categoria == null) errores += "Debe seleccionar una categoría"
        if (prioridad == null) errores += "Debe seleccionar una prioridad"
        if (estado is EstadoRecordatorio.Pendiente && fechaLimite != null && fechaLimite.isBefore(ahora)) {
            errores += "Un recordatorio pendiente no puede tener la fecha límite en el pasado"
        }
        if (estado is EstadoRecordatorio.Completado && estado.fechaCompletado.isAfter(ahora)) {
            errores += "La fecha de completado no puede ser futura"
        }
        if (estado is EstadoRecordatorio.Pospuesto && !estado.hasta.isAfter(ahora)) {
            errores += "Un recordatorio pospuesto debe posponerse a una fecha futura"
        }
        if (minutosAvisoPrevio != null && minutosAvisoPrevio <= 0) {
            errores += "El aviso previo debe ser mayor a 0 minutos"
        }
        if (minutosAvisoPrevio != null && fechaLimite == null) {
            errores += "Para configurar un aviso hace falta una fecha límite"
        }

        if (errores.isNotEmpty() || categoria == null || prioridad == null) {
            return ResultadoValidacion.Invalido(errores)
        }

        return ResultadoValidacion.Valido(
            Recordatorio(
                id = id,
                titulo = titulo.trim(),
                categoria = categoria,
                prioridad = prioridad,
                estado = estado,
                descripcion = descripcion?.trim()?.takeIf { it.isNotEmpty() },
                fechaLimite = fechaLimite,
                ubicacion = ubicacion,
                minutosAvisoPrevio = minutosAvisoPrevio
            )
        )
    }
}
