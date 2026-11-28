package com.example.recordatorios.ui.formulario

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.ErroresFormulario
import com.example.recordatorios.model.Prioridad
import java.time.LocalDate

/** Estado del guardado. Uno solo a la vez. */
sealed interface EstadoEnvio {
    data object Inactivo : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Exitoso(val idGuardado: String) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

/**
 * Estado del formulario (crear o editar). Inmutable: se reemplaza con copy().
 */
data class FormularioUiState(
    val modoEdicion: Boolean = false,
    val cargando: Boolean = false,               // cargando el recordatorio a editar
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val fechaLimite: LocalDate? = null,
    val categoriasDisponibles: List<Categoria> = emptyList(),
    val errores: ErroresFormulario = ErroresFormulario(),  // los que se muestran
    val formularioValido: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Inactivo
) {
    val enviando: Boolean get() = envio is EstadoEnvio.Enviando

    /** Guardar se deshabilita si el formulario es inválido, si está cargando o si hay un envío en curso. */
    val puedeGuardar: Boolean
        get() = formularioValido && !cargando &&
            envio !is EstadoEnvio.Enviando && envio !is EstadoEnvio.Exitoso
}
