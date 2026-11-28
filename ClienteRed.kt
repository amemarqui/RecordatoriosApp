package com.example.recordatorios.data.remote

import com.example.recordatorios.BuildConfig
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Arma OkHttp + Retrofit. La URL base viene de BuildConfig (api.properties),
 * así se puede cambiar sin tocar el código.
 */
object ClienteRed {

    fun crearApi(
        gson: Gson,
        baseUrl: String = BuildConfig.API_BASE_URL,
        interceptorExtra: Interceptor? = null   // el servidor simulado, si está activado
    ): RecordatoriosApi {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Muestra las peticiones en Logcat (filtrar por "okhttp")
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
                interceptorExtra?.let { addInterceptor(it) }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RecordatoriosApi::class.java)
    }
}
