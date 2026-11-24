package com.example.recordatorios.ui.navigation

/** Rutas del grafo de navegación (consigna 5). */
object Rutas {
    const val LISTADO = "listado"
    const val CREAR = "crear"

    const val ARG_ID = "id"
    const val DETALLE = "detalle/{$ARG_ID}"

    /** Arma la ruta concreta del detalle. Solo viaja el id, no el objeto completo. */
    fun detalle(id: Int): String = "detalle/$id"
}
