package com.example.recordatorios.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.data.AppContainer
import com.example.recordatorios.data.Escenario
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.data.SimuladorEscenarios
import com.example.recordatorios.data.filtrarPorEstado
import com.example.recordatorios.data.filtrarPorPrioridad
import com.example.recordatorios.data.ordenados
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * ViewModel del listado. Depende de las INTERFACES RecordatoriosRepository
 * y SimuladorEscenarios, no de RepositorioEnMemoria.
 * No guarda referencias a Activity, View ni Context.
 */
class ListadoViewModel(
    private val repositorio: RecordatoriosRepository,
    private val simulador: SimuladorEscenarios
) : ViewModel() {

    // Estado modificable solo desde acá adentro...
    private val _uiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Cargando)
    // ...y hacia la UI se expone de solo lectura
    val uiState: StateFlow<ListadoUiState> = _uiState.asStateFlow()

    private var ultimaLista: List<Recordatorio> = emptyList()
    private var filtroEstado: KClass<out EstadoRecordatorio>? = null
    private var filtroPrioridad: Prioridad? = null
    private var trabajoCarga: Job? = null

    init {
        cargarRecordatorios()
    }

    /** Carga inicial (y reintento). Corre en viewModelScope, así la UI no se bloquea. */
    fun cargarRecordatorios() {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _uiState.update { ListadoUiState.Cargando }
            repositorio.observarRecordatorios()
                .catch { e ->
                    _uiState.update {
                        ListadoUiState.Error(e.message ?: "Ocurrió un error inesperado.")
                    }
                }
                .collect { lista ->
                    ultimaLista = lista
                    _uiState.update { armarEstado() }
                }
        }
    }

    fun onReintentar() {
        cargarRecordatorios()
    }

    fun onFiltroEstadoChange(filtro: KClass<out EstadoRecordatorio>?) {
        filtroEstado = filtro
        actualizarSiHayDatos()
    }

    fun onFiltroPrioridadChange(filtro: Prioridad?) {
        filtroPrioridad = filtro
        actualizarSiHayDatos()
    }

    /** Menú de simulación: cambia el escenario y vuelve a cargar para ver el resultado. */
    fun onSimularEscenario(escenario: Escenario) {
        simulador.aplicarEscenario(escenario)
        // El error al guardar se ve en el formulario, no hace falta recargar
        if (escenario != Escenario.ERROR_GUARDADO) {
            filtroEstado = null
            filtroPrioridad = null
            cargarRecordatorios()
        }
    }

    private fun actualizarSiHayDatos() {
        if (_uiState.value is ListadoUiState.ConDatos) {
            _uiState.update { armarEstado() }
        }
    }

    /** Siempre crea una instancia NUEVA del estado (no se modifica la anterior). */
    private fun armarEstado(): ListadoUiState =
        if (ultimaLista.isEmpty()) {
            ListadoUiState.Vacio
        } else {
            ListadoUiState.ConDatos(
                recordatorios = ultimaLista,
                visibles = ultimaLista
                    .filtrarPorEstado(filtroEstado)
                    .filtrarPorPrioridad(filtroPrioridad)
                    .ordenados(),
                filtroEstado = filtroEstado,
                filtroPrioridad = filtroPrioridad
            )
        }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ListadoViewModel(AppContainer.repositorio, AppContainer.simulador)
            }
        }
    }
}
