package com.example.recordatorios.model

import java.time.LocalDateTime

/** Resultado de validar los datos de un recordatorio antes de crearlo. */
sealed class ResultadoValidacion {
    data class Valido(val recordatorio: Recordatorio) : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

/**
 * Reglas de validación del producto. A diferencia del init de Recordatorio
 * (que lanza una excepción), este validador junta todos los errores y los
 * devuelve sin romper la app; es lo que usará el formulario en el módulo 2.
 */
object ValidadorRecordatorio {

    fun validar(
        id: Int,
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

        if (id <= 0) errores += "El id debe ser positivo"
        if (titulo.isBlank()) errores += "El título es obligatorio"
        if (titulo.length > Recordatorio.MAX_TITULO) {
            errores += "El título no puede superar ${Recordatorio.MAX_TITULO} caracteres"
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
                descripcion = descripcion?.takeIf { it.isNotBlank() },
                fechaLimite = fechaLimite,
                ubicacion = ubicacion,
                minutosAvisoPrevio = minutosAvisoPrevio
            )
        )
    }
}
