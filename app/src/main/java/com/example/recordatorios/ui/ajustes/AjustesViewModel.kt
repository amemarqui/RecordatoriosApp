package com.example.recordatorios.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.data.remote.simulado.ControlServidorSimulado
import com.example.recordatorios.data.remote.simulado.ModoServidor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Preferencias (DataStore) y, solo para pruebas, el modo del servidor simulado.
 * Los cambios se guardan con funciones suspendidas dentro de viewModelScope.
 */
class AjustesViewModel(
    private val preferenciasRepo: PreferenciasRepository,
    private val controlServidor: ControlServidorSimulado?
) : ViewModel() {

    val preferencias: StateFlow<PreferenciasUsuario> = preferenciasRepo.preferencias
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferenciasUsuario())

    private val _modoServidor = MutableStateFlow(controlServidor?.modo)
    /** null = se usa un servidor real (no hay nada para simular). */
    val modoServidor: StateFlow<ModoServidor?> = _modoServidor.asStateFlow()

    fun onTemaChange(tema: TemaApp) {
        viewModelScope.launch { preferenciasRepo.cambiarTema(tema) }
    }

    fun onMostrarCompletadosChange(mostrar: Boolean) {
        viewModelScope.launch { preferenciasRepo.cambiarMostrarCompletados(mostrar) }
    }

    fun onOrdenChange(orden: OrdenRecordatorios) {
        viewModelScope.launch { preferenciasRepo.cambiarOrden(orden) }
    }

    fun onRestablecer() {
        viewModelScope.launch { preferenciasRepo.restablecer() }
    }

    fun onGuardarValorInvalido() {
        viewModelScope.launch { preferenciasRepo.guardarValorInvalidoDePrueba() }
    }

    fun onModoServidorChange(modo: ModoServidor) {
        controlServidor?.modo = modo
        _modoServidor.value = modo
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val contenedor = contenedor()
                AjustesViewModel(contenedor.preferencias, contenedor.controlServidor)
            }
        }
    }
}
