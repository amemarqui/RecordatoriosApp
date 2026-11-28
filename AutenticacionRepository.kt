package com.example.recordatorios.data.auth

import kotlinx.coroutines.flow.StateFlow

/** Estado de la sesión. La interfaz decide qué mostrar según este valor. */
sealed interface EstadoSesion {
    /** Al abrir la app: se está comprobando si la sesión guardada sigue siendo válida. */
    data object Verificando : EstadoSesion
    /** No hay sesión. motivo != null cuando se perdió (ej: cuenta deshabilitada). */
    data class SinSesion(val motivo: String? = null) : EstadoSesion
    data class Activa(val uid: String, val email: String?) : EstadoSesion
}

sealed interface ResultadoAuth {
    data object Exito : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

/**
 * Autenticación (¿quién es?). La AUTORIZACIÓN (¿qué puede tocar?) la hacen las
 * reglas de Firestore en el servidor, y la PROTECCIÓN LOCAL se logra filtrando Room
 * por uid y limpiando los datos al cerrar sesión.
 * La contraseña nunca se guarda en la app: el token lo administra el SDK de Firebase.
 */
interface AutenticacionRepository {
    val sesion: StateFlow<EstadoSesion>
    val uidActual: String? get() = (sesion.value as? EstadoSesion.Activa)?.uid

    suspend fun registrar(email: String, contrasenia: String): ResultadoAuth
    suspend fun iniciarSesion(email: String, contrasenia: String): ResultadoAuth
    suspend fun cerrarSesion()
    /** Al abrir la app con una sesión guardada: comprueba que siga siendo válida. */
    suspend fun verificarSesionGuardada()
}
