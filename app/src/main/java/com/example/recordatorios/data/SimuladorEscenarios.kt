package com.example.recordatorios.data

/** Escenarios para probar los estados de la interfaz sin base de datos ni servidor. */
enum class Escenario(val descripcion: String) {
    NORMAL("Datos de ejemplo"),
    VACIO("Sin recordatorios"),
    ERROR_CARGA("Error al cargar (1 vez)"),
    ERROR_GUARDADO("Error al guardar (1 vez)")
}

/**
 * Mecanismo de simulación (consigna 3). Está separado del repositorio
 * para que la interfaz RecordatoriosRepository quede "limpia": cuando haya
 * una base de datos real, esto simplemente se deja de usar.
 */
interface SimuladorEscenarios {
    fun aplicarEscenario(escenario: Escenario)
}
