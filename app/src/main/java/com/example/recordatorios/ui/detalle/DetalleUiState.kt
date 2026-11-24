package com.example.recordatorios.ui.detalle

import com.example.recordatorios.model.Recordatorio

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Encontrado(val recordatorio: Recordatorio) : DetalleUiState
    data class NoEncontrado(val id: Int) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
