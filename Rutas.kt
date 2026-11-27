package com.example.recordatorios.ui.navigation

/** Rutas del grafo de navegación. */
object Rutas {
    const val ARG_ID = "id"

    const val LISTADO = "listado"
    const val CREAR = "crear"
    const val EDITAR = "editar/{$ARG_ID}"     // Actividad 3
    const val DETALLE = "detalle/{$ARG_ID}"
    const val AJUSTES = "ajustes"             // Actividad 3

    /** Solo viaja el id (ahora un UUID de texto), nunca el objeto completo. */
    fun detalle(id: String): String = "detalle/$id"
    fun editar(id: String): String = "editar/$id"
}
