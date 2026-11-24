package com.example.recordatorios.data.preferencias

import kotlinx.coroutines.flow.Flow

/** Lo que el resto de la app conoce de las preferencias (no sabe que hay DataStore detrás). */
interface PreferenciasRepository {
    val preferencias: Flow<PreferenciasUsuario>
    suspend fun cambiarTema(tema: TemaApp)
    suspend fun cambiarMostrarCompletados(mostrar: Boolean)
    suspend fun cambiarOrden(orden: OrdenRecordatorios)
    suspend fun restablecer()
    /** Solo para probar: guarda un valor que la app no reconoce. */
    suspend fun guardarValorInvalidoDePrueba()
}
