package com.example.recordatorios.ui.crear

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.ErroresFormulario
import com.example.recordatorios.model.Prioridad

/** Estado del envío del formulario. Uno solo a la vez. */
sealed interface EstadoEnvio {
    data object Inactivo : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Exitoso(val idGuardado: Int) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

/**
 * Estado de la pantalla de creación (inmutable, se reemplaza con copy()).
 * Tiene: valores de los campos, mensajes de validación, estado de envío,
 * resultado (Exitoso) y error general de guardado (EstadoEnvio.Error).
 */
data class CrearUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val categoriasDisponibles: List<Categoria> = emptyList(),
    val errores: ErroresFormulario = ErroresFormulario(),  // los que se muestran
    val formularioValido: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Inactivo
) {
    val enviando: Boolean get() = envio is EstadoEnvio.Enviando

    /** El botón Guardar se deshabilita si el formulario es inválido o hay un envío en curso. */
    val puedeGuardar: Boolean
        get() = formularioValido && envio !is EstadoEnvio.Enviando && envio !is EstadoEnvio.Exitoso
}
