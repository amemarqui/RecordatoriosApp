package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.data.AppContainer
import com.example.recordatorios.data.NuevoRecordatorio
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.ErroresFormulario
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ValidadorRecordatorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrearRecordatorioViewModel(
    private val repositorio: RecordatoriosRepository
) : ViewModel() {

    private enum class Campo { TITULO, DESCRIPCION, CATEGORIA, PRIORIDAD }

    private val categoriasValidas: Set<Categoria> = repositorio.categoriasDisponibles()

    private val _uiState = MutableStateFlow(
        CrearUiState(categoriasDisponibles = categoriasValidas.toList())
    )
    val uiState: StateFlow<CrearUiState> = _uiState.asStateFlow()

    // Para no mostrar errores en rojo apenas se abre el formulario:
    // el error de un campo aparece recién cuando el usuario lo usó.
    private val camposTocados = mutableSetOf<Campo>()

    /* ----- Eventos que manda la interfaz ----- */

    fun onTituloChange(valor: String) = actualizarCampo(Campo.TITULO) { it.copy(titulo = valor) }

    fun onDescripcionChange(valor: String) = actualizarCampo(Campo.DESCRIPCION) { it.copy(descripcion = valor) }

    fun onCategoriaSeleccionada(categoria: Categoria) =
        actualizarCampo(Campo.CATEGORIA) { it.copy(categoria = categoria) }

    fun onPrioridadSeleccionada(prioridad: Prioridad) =
        actualizarCampo(Campo.PRIORIDAD) { it.copy(prioridad = prioridad) }

    fun onGuardar() {
        val actual = _uiState.value
        if (!actual.puedeGuardar) return   // doble control además del botón deshabilitado

        // Datos ya validados (puedeGuardar implica formularioValido)
        val categoria = actual.categoria ?: return
        val prioridad = actual.prioridad ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
            try {
                val guardado = repositorio.guardar(
                    NuevoRecordatorio(
                        titulo = actual.titulo.trim(),
                        descripcion = actual.descripcion.trim().ifEmpty { null },
                        categoria = categoria,
                        prioridad = prioridad
                    )
                )
                _uiState.update { it.copy(envio = EstadoEnvio.Exitoso(guardado.id)) }
            } catch (e: CancellationException) {
                throw e   // si se cancela la corrutina no es un error de guardado
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(envio = EstadoEnvio.Error(e.message ?: "No se pudo guardar el recordatorio."))
                }
            }
        }
    }

    /* ----- Lógica interna ----- */

    private fun actualizarCampo(campo: Campo, cambio: (CrearUiState) -> CrearUiState) {
        if (_uiState.value.enviando) return   // mientras se guarda no se puede editar
        camposTocados += campo
        _uiState.update { estado ->
            val editado = cambio(estado)
            // si había un error de guardado y el usuario corrige algo, se limpia el aviso
            val envio = if (editado.envio is EstadoEnvio.Error) EstadoEnvio.Inactivo else editado.envio
            validar(editado.copy(envio = envio))
        }
    }

    /** Aplica las reglas y devuelve una NUEVA instancia del estado con los errores actualizados. */
    private fun validar(estado: CrearUiState): CrearUiState {
        val errores = ValidadorRecordatorio.validarFormulario(
            titulo = estado.titulo,
            descripcion = estado.descripcion,
            categoria = estado.categoria,
            prioridad = estado.prioridad,
            categoriasValidas = categoriasValidas
        )
        val usoElFormulario = camposTocados.isNotEmpty()
        return estado.copy(
            errores = ErroresFormulario(
                titulo = errores.titulo?.takeIf { Campo.TITULO in camposTocados },
                descripcion = errores.descripcion?.takeIf { Campo.DESCRIPCION in camposTocados },
                // categoría y prioridad son obligatorias: se avisan apenas el usuario empieza a completar
                categoria = errores.categoria?.takeIf { usoElFormulario },
                prioridad = errores.prioridad?.takeIf { usoElFormulario }
            ),
            formularioValido = !errores.hayErrores
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CrearRecordatorioViewModel(AppContainer.repositorio) }
        }
    }
}
