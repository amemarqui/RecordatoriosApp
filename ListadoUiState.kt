package com.example.recordatorios.ui.listado

import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlin.reflect.KClass

/**
 * Estados posibles de la pantalla de listado. Son mutuamente excluyentes
 * (sealed interface): nunca se puede estar cargando y con error a la vez.
 */
sealed interface ListadoUiState {

    data object Cargando : ListadoUiState

    data object Vacio : ListadoUiState

    data class ConDatos(
        val recordatorios: List<Recordatorio>,   // todos, para el resumen
        val visibles: List<Recordatorio>,        // los que pasan los filtros
        val filtroEstado: KClass<out EstadoRecordatorio>? = null,
        val filtroPrioridad: Prioridad? = null
    ) : ListadoUiState

    data class Error(val mensaje: String) : ListadoUiState
}
