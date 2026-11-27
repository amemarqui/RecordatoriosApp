package com.example.recordatorios.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.RecordatoriosRepository
import com.example.recordatorios.data.ResultadoOperacion
import com.example.recordatorios.data.auth.AutenticacionRepository
import com.example.recordatorios.data.auth.EstadoSesion
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Actividad 4: datos de la cuenta y estado del cierre de sesión / prueba de seguridad. */
data class CuentaUiState(
    val email: String? = null,
    val uid: String? = null,
    val cambiosPendientes: Int = 0,
    val confirmandoCierre: Boolean = false,
    val cerrandoSesion: Boolean = false,
    val errorCierre: String? = null,
    val uidPrueba: String = "",
    val probando: Boolean = false,
    val resultadoPrueba: String? = null
)

/**
 * Preferencias (DataStore), la cuenta (Actividad 4) y, solo para pruebas, el modo del
 * servidor simulado. La pantalla no conoce Firebase: todo pasa por los repositorios.
 */
class AjustesViewModel(
    private val preferenciasRepo: PreferenciasRepository,
    private val controlServidor: ControlServidorSimulado?,
    private val autenticacion: AutenticacionRepository? = null,
    private val repositorio: RecordatoriosRepository? = null
) : ViewModel() {

    val preferencias: StateFlow<PreferenciasUsuario> = preferenciasRepo.preferencias
        .stateIn(viewModelScope, SharingStarted.Eagerly, PreferenciasUsuario())

    private val _modoServidor = MutableStateFlow(controlServidor?.modo)
    /** null = se usa un servidor real (no hay nada para simular). */
    val modoServidor: StateFlow<ModoServidor?> = _modoServidor.asStateFlow()

    private val _cuenta = MutableStateFlow(CuentaUiState())
    val cuenta: StateFlow<CuentaUiState> = _cuenta.asStateFlow()

    init {
        autenticacion?.let { auth ->
            viewModelScope.launch {
                auth.sesion.collect { s ->
                    val activa = s as? EstadoSesion.Activa
                    _cuenta.update { it.copy(email = activa?.email, uid = activa?.uid) }
                }
            }
        }
        repositorio?.let { repo ->
            viewModelScope.launch {
                repo.observarSincronizacion().map { it.cambiosSinSincronizar }.collect { n ->
                    _cuenta.update { it.copy(cambiosPendientes = n) }
                }
            }
        }
    }

    /* ---------- Cuenta (Actividad 4) ---------- */

    fun onPedirCierreSesion() = _cuenta.update { it.copy(confirmandoCierre = true, errorCierre = null) }
    fun onCancelarCierreSesion() = _cuenta.update { it.copy(confirmandoCierre = false) }

    /**
     * 1) intenta subir lo pendiente y borra del teléfono lo que ya está en la nube,
     * 2) cierra la sesión. La raíz de la app vuelve sola a la pantalla de login.
     */
    fun onConfirmarCierreSesion() {
        val auth = autenticacion ?: return
        val repo = repositorio ?: return
        if (_cuenta.value.cerrandoSesion) return
        _cuenta.update { it.copy(cerrandoSesion = true) }
        viewModelScope.launch {
            when (val r = repo.prepararCierreDeSesion()) {
                is ResultadoOperacion.Exito -> auth.cerrarSesion()
                is ResultadoOperacion.Error -> _cuenta.update {
                    it.copy(cerrandoSesion = false, confirmandoCierre = false, errorCierre = r.mensaje)
                }
            }
        }
    }

    fun onUidPruebaChange(valor: String) = _cuenta.update { it.copy(uidPrueba = valor, resultadoPrueba = null) }

    /** Prueba de seguridad: intenta leer y escribir los datos de OTRA cuenta en Firestore. */
    fun onProbarAccesoAjeno() {
        val repo = repositorio ?: return
        if (_cuenta.value.probando) return
        _cuenta.update { it.copy(probando = true, resultadoPrueba = null) }
        viewModelScope.launch {
            val resultado = repo.probarAccesoAOtraCuenta(_cuenta.value.uidPrueba)
            _cuenta.update { it.copy(probando = false, resultadoPrueba = resultado) }
        }
    }

    /* ---------- Preferencias ---------- */

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
                AjustesViewModel(
                    contenedor.preferencias,
                    contenedor.controlServidor,
                    contenedor.autenticacion,
                    contenedor.repositorio
                )
            }
        }
    }
}
