package com.example.recordatorios.data.auth

import android.content.SharedPreferences
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementación con Firebase Authentication (email y contraseña).
 *
 * marcaSesion: guarda SOLO un booleano "había una sesión iniciada" (no guarda email,
 * contraseña ni token). Sirve para avisar al usuario si Firebase cerró la sesión por su
 * cuenta (por ejemplo, la cuenta se deshabilitó mientras la app estaba cerrada).
 */
class AutenticacionFirebase(
    private val auth: FirebaseAuth,
    private val marcaSesion: SharedPreferences
) : AutenticacionRepository {

    private companion object {
        const val CLAVE_HABIA_SESION = "habia_sesion"
        const val MOTIVO_INVALIDA =
            "Tu sesión ya no es válida (la cuenta fue deshabilitada o eliminada). Iniciá sesión de nuevo."
    }

    private var habiaSesion: Boolean
        get() = marcaSesion.getBoolean(CLAVE_HABIA_SESION, false)
        set(valor) { marcaSesion.edit().putBoolean(CLAVE_HABIA_SESION, valor).apply() }

    /** true cuando el usuario tocó "Cerrar sesión" (no hay que mostrar ningún aviso). */
    @Volatile private var cierreVoluntario = false

    private val _sesion = MutableStateFlow<EstadoSesion>(EstadoSesion.Verificando)
    override val sesion: StateFlow<EstadoSesion> = _sesion.asStateFlow()

    /** Motivo a mostrar la próxima vez que quede sin sesión (ej: sesión inválida). */
    @Volatile private var motivoPendiente: String? = null
    @Volatile private var verificada = false

    init {
        // Firebase avisa cada vez que cambia el usuario (login, logout, sesión perdida)
        auth.addAuthStateListener { firebaseAuth ->
            val usuario = firebaseAuth.currentUser
            if (usuario == null) {
                // Si no fue un cierre voluntario y había sesión, Firebase la invalidó
                val motivo = motivoPendiente
                    ?: if (verificada && habiaSesion && !cierreVoluntario) MOTIVO_INVALIDA else null
                // Firebase puede avisar "sin usuario" más de una vez seguida: si ya estamos
                // sin sesión, el segundo aviso no debe borrar el motivo que ya se mostró.
                if (verificada && !(motivo == null && _sesion.value is EstadoSesion.SinSesion)) {
                    _sesion.value = EstadoSesion.SinSesion(motivo)
                }
                if (verificada) habiaSesion = false
                motivoPendiente = null
                cierreVoluntario = false
            } else if (verificada) {
                habiaSesion = true
                _sesion.value = EstadoSesion.Activa(usuario.uid, usuario.email)
            }
        }
    }

    override suspend fun verificarSesionGuardada() {
        val usuario = auth.currentUser
        if (usuario != null) {
            try {
                usuario.reload().await()          // consulta a Firebase si la cuenta sigue válida
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthInvalidUserException) {
                // La cuenta se eliminó o deshabilitó desde la consola: la sesión ya no sirve
                motivoPendiente = MOTIVO_INVALIDA
                verificada = true
                auth.signOut()
                return
            } catch (e: Exception) {
                // Sin conexión u otro error temporal: se mantiene la sesión (la app funciona offline)
            }
        }
        verificada = true
        val actual = auth.currentUser
        _sesion.value = if (actual != null) {
            habiaSesion = true
            EstadoSesion.Activa(actual.uid, actual.email)
        } else {
            // Había sesión pero Firebase ya la cerró (token revocado mientras la app estaba cerrada)
            val motivo = if (habiaSesion) MOTIVO_INVALIDA else null
            habiaSesion = false
            EstadoSesion.SinSesion(motivo)
        }
    }

    override suspend fun registrar(email: String, contrasenia: String): ResultadoAuth = ejecutar {
        auth.createUserWithEmailAndPassword(email.trim(), contrasenia).await()
    }

    override suspend fun iniciarSesion(email: String, contrasenia: String): ResultadoAuth = ejecutar {
        auth.signInWithEmailAndPassword(email.trim(), contrasenia).await()
    }

    override suspend fun cerrarSesion() {
        cierreVoluntario = true
        habiaSesion = false
        auth.signOut()
    }

    private suspend fun ejecutar(bloque: suspend () -> Unit): ResultadoAuth = try {
        bloque()
        verificada = true
        auth.currentUser?.let {
            habiaSesion = true
            _sesion.value = EstadoSesion.Activa(it.uid, it.email)
        }
        ResultadoAuth.Exito
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoAuth.Error(mensajeDeError(e))
    }

    /** Traduce las excepciones de Firebase a mensajes para la persona usuaria. */
    private fun mensajeDeError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil: usá al menos 6 caracteres."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese email."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese email o fue deshabilitada."
        is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos."
        is FirebaseNetworkException -> "Sin conexión. Para iniciar sesión o registrarte hace falta internet."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Esperá un momento y probá de nuevo."
        else -> "No se pudo completar la operación. Intentá de nuevo."
    }
}
