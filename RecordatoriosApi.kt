package com.example.recordatorios.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Operaciones del servicio remoto (Retrofit). Son funciones suspendidas y devuelven
 * Response<T> para poder leer el código HTTP y decidir qué hacer en la capa de datos.
 */
interface RecordatoriosApi {

    @GET("recordatorios")
    suspend fun obtenerTodos(): Response<List<RecordatorioDto>>

    /**
     * Crea o actualiza (upsert) usando el id generado en el dispositivo.
     * Es IDEMPOTENTE: enviar lo mismo dos veces deja un solo registro en el servidor.
     */
    @PUT("recordatorios/{id}")
    suspend fun guardar(@Path("id") id: String, @Body recordatorio: RecordatorioDto): Response<RecordatorioDto>

    @DELETE("recordatorios/{id}")
    suspend fun eliminar(@Path("id") id: String): Response<Unit>
}
