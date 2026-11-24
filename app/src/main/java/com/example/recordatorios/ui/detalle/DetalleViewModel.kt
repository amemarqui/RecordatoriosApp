package com.example.recordatorios.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.data.ResultadoOperacion
import com.example.recordatorios.ui.navigation.Rutas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Recibe SOLO el id por navegación y observa ese registro en Room:
 * si se edita, se completa o se sincroniza, la pantalla se actualiza sola.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RecordatoriosRepository
) : ViewModel() {

    val idRecordatorio: String = checkNotNull(savedStateHandle.get<String>(Rutas.ARG_ID)) {
        "Falta el argumento ${Rutas.ARG_ID} en la navegación"
    }

    private val eliminado = MutableStateFlow(false)
    private val procesando = MutableStateFlow(false)
    private val mensaje = MutableStateFlow<String?>(null)
    private val recargas = MutableStateFlow(0)

    val uiState: StateFlow<DetalleUiState> = recargas
        .flatMapLatest {
            combine(
                repositorio.observarPorId(idRecordatorio),
                eliminado,
                procesando,
                mensaje
            ) { recordatorio, fueEliminado, estaProcesando, textoMensaje ->
                when {
                    fueEliminado -> DetalleUiState.Eliminado
                    recordatorio == null -> DetalleUiState.NoEncontrado(idRecordatorio)
                    else -> DetalleUiState.Encontrado(recordatorio, estaProcesando, textoMensaje)
                }
            }
                .onStart { emit(DetalleUiState.Cargando) }
                .catch { emit(DetalleUiState.Error("No se pudo leer el recordatorio.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    fun onReintentar() {
        recargas.update { it + 1 }
    }

    fun onCambiarCompletado(completado: Boolean) = ejecutar {
        repositorio.cambiarCompletado(idRecordatorio, completado)
    }

    fun onReintentarSincronizacion() = ejecutar {
        repositorio.reintentarSincronizacion(idRecordatorio)
    }

    fun onEliminar() = ejecutar {
        repositorio.eliminar(idRecordatorio).also {
            if (it is ResultadoOperacion.Exito) eliminado.value = true
        }
    }

    fun onMensajeMostrado() {
        mensaje.value = null
    }

    /** Ejecuta una acción del repositorio mostrando "procesando" y el error si falla. */
    private fun ejecutar(accion: suspend () -> ResultadoOperacion<*>) {
        if (procesando.value) return
        viewModelScope.launch {
            procesando.value = true
            val resultado = accion()
            if (resultado is ResultadoOperacion.Error) mensaje.value = resultado.mensaje
            procesando.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetalleViewModel(createSavedStateHandle(), contenedor().repositorio)
            }
        }
    }
}
