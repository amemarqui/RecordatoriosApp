package com.example.recordatorios.data.sync

/** Resultado de una vuelta de sincronización, ya traducido para mostrarlo. */
sealed interface ResultadoSincronizacion {
    /** enviados/recibidos: cambios aplicados; rechazados: el servidor los rechazó (400); descartados: datos remotos inválidos. */
    data class Exito(
        val enviados: Int,
        val recibidos: Int,
        val rechazados: Int = 0,
        val descartados: Int = 0
    ) : ResultadoSincronizacion
    data object SinConexion : ResultadoSincronizacion
    data object Timeout : ResultadoSincronizacion
    data class ErrorServidor(val codigo: Int) : ResultadoSincronizacion
    data class Error(val mensaje: String) : ResultadoSincronizacion
    /** Ya había una sincronización corriendo: no se inicia otra en paralelo. */
    data object YaEnCurso : ResultadoSincronizacion
}

fun ResultadoSincronizacion.mensaje(): String = when (this) {
    is ResultadoSincronizacion.Exito -> buildString {
        append("Sincronizado")
        if (enviados > 0 || recibidos > 0) append(": $enviados enviados, $recibidos recibidos")
        if (rechazados > 0) append(". $rechazados rechazados por el servidor")
        if (descartados > 0) append(". $descartados datos remotos inválidos descartados")
    }
    ResultadoSincronizacion.SinConexion -> "Sin conexión. Los cambios quedan guardados en el teléfono."
    ResultadoSincronizacion.Timeout -> "El servidor no respondió a tiempo. Se reintentará."
    is ResultadoSincronizacion.ErrorServidor -> "Error del servidor ($codigo). Se reintentará."
    is ResultadoSincronizacion.Error -> mensaje
    ResultadoSincronizacion.YaEnCurso -> "Ya hay una sincronización en curso."
}

/** Lo que el sincronizador publica para la interfaz. */
data class EstadoSincronizador(
    val sincronizando: Boolean = false,
    val ultimoResultado: ResultadoSincronizacion? = null,
    val ultimaVezMs: Long? = null
)
