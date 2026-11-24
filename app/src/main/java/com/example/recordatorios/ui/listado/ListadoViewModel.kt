package com.example.recordatorios.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.InfoSincronizacion
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.data.filtrarPorEstado
import com.example.recordatorios.data.filtrarPorPrioridad
import com.example.recordatorios.data.sync.mensaje
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Actividad 3: la lista sale de Room (vía repositorio) como Flow, así cualquier cambio
 * en la base (crear, editar, borrar, sincronizar) aparece solo en la pantalla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListadoViewModel(
    private val repositorio: RecordatoriosRepository
) : ViewModel() {

    private val filtroEstado = MutableStateFlow<KClass<out EstadoRecordatorio>?>(null)
    private val filtroPrioridad = MutableStateFlow<Prioridad?>(null)
    private val recargas = MutableStateFlow(0)

    val uiState: StateFlow<ListadoUiState> = recargas
        .flatMapLatest {
            combine(repositorio.observarRecordatorios(), filtroEstado, filtroPrioridad) { lista, estado, prioridad ->
                armarEstado(lista, estado, prioridad)
            }
                .onStart { emit(ListadoUiState.Cargando) }
                .catch { e ->
                    emit(ListadoUiState.Error("No se pudieron leer los recordatorios guardados. ${e.message ?: ""}".trim()))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState.Cargando)

    val sincronizacion: StateFlow<InfoSincronizacion> = repositorio.observarSincronizacion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InfoSincronizacion())

    /** Mensaje corto para el Snackbar (resultado de "Sincronizar ahora"). */
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun onReintentar() {
        recargas.update { it + 1 }
    }

    fun onFiltroEstadoChange(filtro: KClass<out EstadoRecordatorio>?) {
        filtroEstado.value = filtro
    }

    fun onFiltroPrioridadChange(filtro: Prioridad?) {
        filtroPrioridad.value = filtro
    }

    fun onSincronizarAhora() {
        viewModelScope.launch {
            val resultado = repositorio.sincronizarAhora()
            _mensaje.value = resultado.mensaje()
        }
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    private fun armarEstado(
        lista: List<Recordatorio>,
        estado: KClass<out EstadoRecordatorio>?,
        prioridad: Prioridad?
    ): ListadoUiState =
        if (lista.isEmpty()) {
            ListadoUiState.Vacio
        } else {
            ListadoUiState.ConDatos(
                recordatorios = lista,
                // el orden ya viene del repositorio (preferencia de DataStore)
                visibles = lista.filtrarPorEstado(estado).filtrarPorPrioridad(prioridad),
                filtroEstado = estado,
                filtroPrioridad = prioridad
            )
        }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ListadoViewModel(contenedor().repositorio) }
        }
    }
}
