package com.example.recordatorios.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.auth.AutenticacionRepository
import com.example.recordatorios.data.auth.ResultadoAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Modo de la pantalla: entrar con una cuenta que ya existe o crear una nueva. */
enum class ModoAuth { INICIAR_SESION, REGISTRARSE }

data class AuthUiState(
    val modo: ModoAuth = ModoAuth.INICIAR_SESION,
    val email: String = "",
    val contrasenia: String = "",
    val confirmacion: String = "",
    val errorEmail: String? = null,
    val errorContrasenia: String? = null,
    val errorConfirmacion: String? = null,
    /** Autenticación en curso: se bloquean los campos y el botón para no mandar dos veces. */
    val enCurso: Boolean = false,
    /** Error que devolvió el proveedor, ya traducido a un mensaje para el usuario. */
    val errorGeneral: String? = null
)

/**
 * La pantalla NO conoce Firebase: solo llama a este ViewModel, que usa la interfaz
 * AutenticacionRepository. Si la sesión se inicia bien no hace falta navegar a mano:
 * el estado de sesión cambia a Activa y la raíz de la app muestra el listado.
 * La contraseña vive solo en memoria mientras se escribe; nunca se guarda.
 */
class AuthViewModel(private val autenticacion: AutenticacionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onModoChange(modo: ModoAuth) = _uiState.update {
        // Al cambiar de modo se limpian errores y la confirmación, pero se conserva el email
        AuthUiState(modo = modo, email = it.email)
    }

    fun onEmailChange(valor: String) = _uiState.update {
        it.copy(email = valor.trim(), errorEmail = null, errorGeneral = null)
    }

    fun onContraseniaChange(valor: String) = _uiState.update {
        it.copy(contrasenia = valor, errorContrasenia = null, errorGeneral = null)
    }

    fun onConfirmacionChange(valor: String) = _uiState.update {
        it.copy(confirmacion = valor, errorConfirmacion = null, errorGeneral = null)
    }

    fun onEnviar() {
        val estado = _uiState.value
        if (estado.enCurso) return

        val errorEmail = validarEmail(estado.email)
        val errorContrasenia = validarContrasenia(estado.contrasenia)
        val errorConfirmacion = if (estado.modo == ModoAuth.REGISTRARSE && estado.confirmacion != estado.contrasenia) {
            "Las contraseñas no coinciden."
        } else null

        if (errorEmail != null || errorContrasenia != null || errorConfirmacion != null) {
            _uiState.update {
                it.copy(errorEmail = errorEmail, errorContrasenia = errorContrasenia, errorConfirmacion = errorConfirmacion)
            }
            return
        }

        _uiState.update { it.copy(enCurso = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = when (estado.modo) {
                ModoAuth.INICIAR_SESION -> autenticacion.iniciarSesion(estado.email, estado.contrasenia)
                ModoAuth.REGISTRARSE -> autenticacion.registrar(estado.email, estado.contrasenia)
            }
            _uiState.update {
                when (resultado) {
                    // Se borra la contraseña de la memoria en cuanto ya no se necesita
                    ResultadoAuth.Exito -> it.copy(enCurso = false, contrasenia = "", confirmacion = "")
                    is ResultadoAuth.Error -> it.copy(enCurso = false, errorGeneral = resultado.mensaje)
                }
            }
        }
    }

    companion object {
        const val MIN_CONTRASENIA = 6   // mínimo que pide Firebase Auth

        private val PATRON_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun validarEmail(email: String): String? = when {
            email.isBlank() -> "Ingresá tu email."
            !PATRON_EMAIL.matches(email) -> "El email no tiene un formato válido."
            else -> null
        }

        fun validarContrasenia(contrasenia: String): String? = when {
            contrasenia.isEmpty() -> "Ingresá la contraseña."
            contrasenia.length < MIN_CONTRASENIA -> "Debe tener al menos $MIN_CONTRASENIA caracteres."
            else -> null
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(contenedor().autenticacion) }
        }
    }
}
