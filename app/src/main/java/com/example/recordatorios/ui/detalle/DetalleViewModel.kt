package com.example.recordatorios.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.data.AppContainer
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.ui.navigation.Rutas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Recibe SOLO el id del recordatorio (argumento de navegación) a través del
 * SavedStateHandle y busca el registro completo en el repositorio.
 */
class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RecordatoriosRepository
) : ViewModel() {

    private val idRecordatorio: Int = checkNotNull(savedStateHandle.get<Int>(Rutas.ARG_ID)) {
        "Falta el argumento ${Rutas.ARG_ID} en la navegación"
    }

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { DetalleUiState.Cargando }
            val nuevoEstado = try {
                val recordatorio = repositorio.obtenerPorId(idRecordatorio)
                if (recordatorio != null) DetalleUiState.Encontrado(recordatorio)
                else DetalleUiState.NoEncontrado(idRecordatorio)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetalleUiState.Error(e.message ?: "No se pudo cargar el recordatorio.")
            }
            _uiState.update { nuevoEstado }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetalleViewModel(createSavedStateHandle(), AppContainer.repositorio)
            }
        }
    }
}
