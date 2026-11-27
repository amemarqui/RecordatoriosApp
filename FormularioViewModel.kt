package com.example.recordatorios.ui.formulario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.NuevoRecordatorio
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.data.ResultadoOperacion
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.ErroresFormulario
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ValidadorRecordatorio
import com.example.recordatorios.ui.navigation.Rutas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Formulario para CREAR (ruta "crear") o EDITAR (ruta "editar/{id}").
 * Si llega un id por navegación, carga ese recordatorio desde Room.
 */
class FormularioViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RecordatoriosRepository
) : ViewModel() {

    private enum class Campo { TITULO, DESCRIPCION, CATEGORIA, PRIORIDAD, FECHA }

    private val idEditar: String? = savedStateHandle.get<String>(Rutas.ARG_ID)
    private val categoriasValidas: Set<Categoria> = repositorio.categoriasDisponibles()
    private val camposTocados = mutableSetOf<Campo>()

    private val _uiState = MutableStateFlow(
        FormularioUiState(
            modoEdicion = idEditar != null,
            cargando = idEditar != null,
            categoriasDisponibles = categoriasValidas.toList()
        )
    )
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()

    init {
        if (idEditar != null) cargarParaEditar(idEditar)
    }

    private fun cargarParaEditar(id: String) {
        viewModelScope.launch {
            val existente = repositorio.observarPorId(id).first()
            if (existente == null) {
                _uiState.update {
                    it.copy(cargando = false, envio = EstadoEnvio.Error("El recordatorio ya no existe."))
                }
                return@launch
            }
            // Al editar, todos los campos ya tienen datos: se muestran sus errores si los hay
            camposTocados += Campo.entries
            _uiState.update {
                validar(
                    it.copy(
                        cargando = false,
                        titulo = existente.titulo,
                        descripcion = existente.descripcion.orEmpty(),
                        categoria = existente.categoria,
                        prioridad = existente.prioridad,
                        fechaLimite = existente.fechaLimite?.toLocalDate()
                    )
                )
            }
        }
    }

    /* ----- Eventos de la interfaz ----- */

    fun onTituloChange(valor: String) = actualizarCampo(Campo.TITULO) { it.copy(titulo = valor) }

    fun onDescripcionChange(valor: String) = actualizarCampo(Campo.DESCRIPCION) { it.copy(descripcion = valor) }

    fun onCategoriaSeleccionada(categoria: Categoria) =
        actualizarCampo(Campo.CATEGORIA) { it.copy(categoria = categoria) }

    fun onPrioridadSeleccionada(prioridad: Prioridad) =
        actualizarCampo(Campo.PRIORIDAD) { it.copy(prioridad = prioridad) }

    fun onFechaLimiteChange(fecha: LocalDate?) = actualizarCampo(Campo.FECHA) { it.copy(fechaLimite = fecha) }

    fun onGuardar() {
        val actual = _uiState.value
        if (!actual.puedeGuardar) return
        val categoria = actual.categoria ?: return
        val prioridad = actual.prioridad ?: return

        val datos = NuevoRecordatorio(
            titulo = actual.titulo.trim(),
            descripcion = actual.descripcion.trim().ifEmpty { null },
            categoria = categoria,
            prioridad = prioridad,
            fechaLimite = actual.fechaLimite?.atTime(LocalTime.of(23, 59))
        )

        viewModelScope.launch {
            _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
            // Se guarda en Room (local-first): funciona con o sin conexión
            val resultado = if (idEditar == null) {
                repositorio.crear(datos)
            } else {
                when (val r = repositorio.actualizar(idEditar, datos)) {
                    is ResultadoOperacion.Exito -> ResultadoOperacion.Exito(idEditar)
                    is ResultadoOperacion.Error -> r
                }
            }
            _uiState.update {
                when (resultado) {
                    is ResultadoOperacion.Exito -> it.copy(envio = EstadoEnvio.Exitoso(resultado.valor))
                    is ResultadoOperacion.Error -> it.copy(envio = EstadoEnvio.Error(resultado.mensaje))
                }
            }
        }
    }

    /* ----- Lógica interna ----- */

    private fun actualizarCampo(campo: Campo, cambio: (FormularioUiState) -> FormularioUiState) {
        if (_uiState.value.enviando || _uiState.value.cargando) return
        camposTocados += campo
        _uiState.update { estado ->
            val editado = cambio(estado)
            val envio = if (editado.envio is EstadoEnvio.Error) EstadoEnvio.Inactivo else editado.envio
            validar(editado.copy(envio = envio))
        }
    }

    private fun validar(estado: FormularioUiState): FormularioUiState {
        val errores = ValidadorRecordatorio.validarFormulario(
            titulo = estado.titulo,
            descripcion = estado.descripcion,
            categoria = estado.categoria,
            prioridad = estado.prioridad,
            categoriasValidas = categoriasValidas,
            fechaLimite = estado.fechaLimite
        )
        val usoElFormulario = camposTocados.isNotEmpty()
        return estado.copy(
            errores = ErroresFormulario(
                titulo = errores.titulo?.takeIf { Campo.TITULO in camposTocados },
                descripcion = errores.descripcion?.takeIf { Campo.DESCRIPCION in camposTocados },
                categoria = errores.categoria?.takeIf { usoElFormulario },
                prioridad = errores.prioridad?.takeIf { usoElFormulario },
                fechaLimite = errores.fechaLimite?.takeIf { usoElFormulario }
            ),
            formularioValido = !errores.hayErrores
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FormularioViewModel(createSavedStateHandle(), contenedor().repositorio)
            }
        }
    }
}
