package com.example.recordatorios.data.remote

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Resultado de una llamada al servicio, ya interpretado. Las pantallas nunca ven
 * códigos HTTP ni excepciones de red: todo se resuelve acá, en la capa de datos.
 */
sealed interface ResultadoRed<out T> {
    data class Exito<T>(val datos: T?) : ResultadoRed<T>
    /** 4xx: la solicitud es inválida; reintentarla igual no sirve. */
    data class SolicitudInvalida(val codigo: Int, val mensaje: String) : ResultadoRed<Nothing>
    /** 5xx: problema del servidor; se puede reintentar más tarde. */
    data class ErrorServidor(val codigo: Int) : ResultadoRed<Nothing>
    data object Timeout : ResultadoRed<Nothing>
    data object SinConexion : ResultadoRed<Nothing>
    data class Desconocido(val mensaje: String) : ResultadoRed<Nothing>
}

/** true si conviene reintentar automáticamente más tarde. */
val ResultadoRed<*>.esTemporal: Boolean
    get() = this is ResultadoRed.ErrorServidor || this is ResultadoRed.Timeout ||
        this is ResultadoRed.SinConexion || this is ResultadoRed.Desconocido

fun ResultadoRed<*>.mensaje(): String = when (this) {
    is ResultadoRed.Exito -> "OK"
    is ResultadoRed.SolicitudInvalida -> "El servidor rechazó los datos ($codigo): $mensaje"
    is ResultadoRed.ErrorServidor -> "Error del servidor ($codigo)"
    ResultadoRed.Timeout -> "El servidor tardó demasiado en responder"
    ResultadoRed.SinConexion -> "Sin conexión a internet"
    is ResultadoRed.Desconocido -> mensaje
}

/**
 * Ejecuta una llamada de Retrofit y traduce cualquier resultado a ResultadoRed.
 * @param codigosAceptados códigos que se consideran éxito además de 2xx (ej: 404 al eliminar).
 */
suspend fun <T> llamadaSegura(
    codigosAceptados: Set<Int> = emptySet(),
    llamada: suspend () -> Response<T>
): ResultadoRed<T> = try {
    val respuesta = llamada()
    val codigo = respuesta.code()
    when {
        respuesta.isSuccessful || codigo in codigosAceptados -> ResultadoRed.Exito(respuesta.body())
        codigo in 400..499 -> ResultadoRed.SolicitudInvalida(
            codigo,
            mensajeDeError(respuesta.errorBody()?.string(), respuesta.message())
        )
        codigo >= 500 -> ResultadoRed.ErrorServidor(codigo)
        else -> ResultadoRed.Desconocido("Respuesta inesperada ($codigo)")
    }
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    ResultadoRed.Timeout          // va antes que IOException porque es una subclase
} catch (e: IOException) {
    ResultadoRed.SinConexion      // UnknownHostException, ConnectException, etc.
} catch (e: Exception) {
    ResultadoRed.Desconocido(e.message ?: "Error inesperado de red")
}

private val PATRON_MENSAJE = Regex("\"mensaje\"\\s*:\\s*\"([^\"]*)\"")

/** El servidor responde {"mensaje": "..."}: se muestra solo el texto, no el JSON. */
internal fun mensajeDeError(cuerpo: String?, porDefecto: String): String {
    if (cuerpo.isNullOrBlank()) return porDefecto
    return PATRON_MENSAJE.find(cuerpo)?.groupValues?.get(1) ?: cuerpo.take(200)
}
