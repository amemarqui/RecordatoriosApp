package com.example.recordatorios.data.preferencias

enum class TemaApp(val etiqueta: String) {
    SISTEMA("Igual que el sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

enum class OrdenRecordatorios(val etiqueta: String) {
    PRIORIDAD("Prioridad (alta primero)"),
    FECHA_LIMITE("Fecha límite (más próxima primero)"),
    TITULO("Título (A-Z)"),
    RECIENTES("Más recientes primero")
}

/** Nombres de las preferencias (para informar cuáles usan valor predeterminado). */
object NombresPreferencia {
    const val TEMA = "tema"
    const val MOSTRAR_COMPLETADOS = "mostrar_completados"
    const val ORDEN = "orden"
}

/**
 * Modelo propio de preferencias (la app nunca trabaja con el objeto Preferences de DataStore).
 * - porDefecto: preferencias que todavía no se guardaron y usan el valor predeterminado.
 * - invalidas: preferencias con un valor guardado que no se reconoce (también usan el predeterminado).
 */
data class PreferenciasUsuario(
    val tema: TemaApp = TemaApp.SISTEMA,
    val mostrarCompletados: Boolean = true,
    val orden: OrdenRecordatorios = OrdenRecordatorios.PRIORIDAD,
    val porDefecto: Set<String> = emptySet(),
    val invalidas: Set<String> = emptySet(),
    val errorLectura: Boolean = false
)

/**
 * Convierte los valores crudos guardados en el modelo. Es una función pura
 * (sin Android) para poder probarla con tests unitarios.
 */
fun interpretarPreferencias(
    temaGuardado: String?,
    mostrarCompletadosGuardado: Boolean?,
    ordenGuardado: String?,
    errorLectura: Boolean = false
): PreferenciasUsuario {
    val porDefecto = mutableSetOf<String>()
    val invalidas = mutableSetOf<String>()
    val predeterminado = PreferenciasUsuario()

    val tema = when (temaGuardado) {
        null -> { porDefecto += NombresPreferencia.TEMA; predeterminado.tema }
        else -> TemaApp.entries.find { it.name == temaGuardado }
            ?: run { invalidas += NombresPreferencia.TEMA; predeterminado.tema }
    }
    val orden = when (ordenGuardado) {
        null -> { porDefecto += NombresPreferencia.ORDEN; predeterminado.orden }
        else -> OrdenRecordatorios.entries.find { it.name == ordenGuardado }
            ?: run { invalidas += NombresPreferencia.ORDEN; predeterminado.orden }
    }
    val mostrarCompletados = mostrarCompletadosGuardado
        ?: run { porDefecto += NombresPreferencia.MOSTRAR_COMPLETADOS; predeterminado.mostrarCompletados }

    return PreferenciasUsuario(
        tema = tema,
        mostrarCompletados = mostrarCompletados,
        orden = orden,
        porDefecto = porDefecto,
        invalidas = invalidas,
        errorLectura = errorLectura
    )
}
