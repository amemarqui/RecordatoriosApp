package com.example.recordatorios.ui.detalle

import com.example.recordatorios.model.Recordatorio

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Encontrado(
        val recordatorio: Recordatorio,
        val procesando: Boolean = false,      // mientras se guarda un cambio
        val mensaje: String? = null           // error de una acción (para el Snackbar)
    ) : DetalleUiState
    data class NoEncontrado(val id: String) : DetalleUiState
    /** Se acaba de eliminar: la pantalla vuelve atrás. */
    data object Eliminado : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
