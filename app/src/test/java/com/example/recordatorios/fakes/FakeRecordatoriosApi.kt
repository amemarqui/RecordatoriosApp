package com.example.recordatorios.fakes

import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.remote.RecordatoriosApi
import kotlinx.coroutines.delay
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/** Servicio remoto falso: guarda en un mapa y cuenta cuántas veces se llamó a cada operación. */
class FakeRecordatoriosApi : RecordatoriosApi {

    enum class Falla { NINGUNA, SIN_RED, TIMEOUT, ERROR_500, ERROR_400 }

    val servidor = mutableMapOf<String, RecordatorioDto>()
    var falla = Falla.NINGUNA
    var demoraMs = 0L
    var llamadasGuardar = 0
    var llamadasEliminar = 0
    var llamadasObtener = 0

    private fun <T> fallaComoRespuesta(): Response<T>? = when (falla) {
        Falla.NINGUNA -> null
        Falla.SIN_RED -> throw IOException("sin red")
        Falla.TIMEOUT -> throw SocketTimeoutException("timeout")
        Falla.ERROR_500 -> Response.error(500, "error".toResponseBody())
        Falla.ERROR_400 -> Response.error(400, "invalido".toResponseBody())
    }

    override suspend fun obtenerTodos(): Response<List<RecordatorioDto>> {
        llamadasObtener++
        delay(demoraMs)
        fallaComoRespuesta<List<RecordatorioDto>>()?.let { return it }
        return Response.success(servidor.values.toList())
    }

    override suspend fun guardar(id: String, recordatorio: RecordatorioDto): Response<RecordatorioDto> {
        llamadasGuardar++
        delay(demoraMs)
        fallaComoRespuesta<RecordatorioDto>()?.let { return it }
        servidor[id] = recordatorio   // upsert: mismo id = mismo registro
        return Response.success(recordatorio)
    }

    override suspend fun eliminar(id: String): Response<Unit> {
        llamadasEliminar++
        delay(demoraMs)
        fallaComoRespuesta<Unit>()?.let { return it }
        return if (servidor.remove(id) != null) Response.success(Unit)
        else Response.error(404, "no existe".toResponseBody())
    }
}
