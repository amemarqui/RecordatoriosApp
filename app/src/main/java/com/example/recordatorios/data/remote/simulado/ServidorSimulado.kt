package com.example.recordatorios.data.remote.simulado

import com.example.recordatorios.data.remote.ErrorDto
import com.example.recordatorios.data.remote.RecordatorioDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Cómo responde el servidor simulado. Se cambia desde Ajustes → Pruebas de red. */
enum class ModoServidor(val descripcion: String) {
    NORMAL("Responde normalmente"),
    SIN_CONEXION("Sin conexión"),
    TIMEOUT("Timeout (no responde a tiempo)"),
    ERROR_500("Error del servidor (500)"),
    SOLICITUD_INVALIDA("Rechaza los datos (400)")
}

/** Lo que la interfaz puede controlar del servidor simulado (solo para pruebas). */
interface ControlServidorSimulado {
    var modo: ModoServidor
}

/**
 * "Base de datos" del servidor simulado. Se guarda en un archivo JSON aparte de Room,
 * así se comporta como un servidor de verdad: sus datos no se borran al cerrar la app.
 */
class AlmacenServidorSimulado(
    private val archivo: File,
    private val gson: Gson,
    private val datosIniciales: () -> List<RecordatorioDto>
) {
    private val tipoLista = object : TypeToken<List<RecordatorioDto>>() {}.type
    private var datos: MutableMap<String, RecordatorioDto>? = null

    @Synchronized
    private fun cargar(): MutableMap<String, RecordatorioDto> {
        datos?.let { return it }
        val lista: List<RecordatorioDto> = if (archivo.exists()) {
            runCatching { gson.fromJson<List<RecordatorioDto>>(archivo.readText(), tipoLista) }
                .getOrNull() ?: emptyList()
        } else {
            datosIniciales()   // primera vez: el servidor arranca con datos de ejemplo
        }
        val mapa = lista.filter { it.id != null }.associateBy { it.id!! }.toMutableMap()
        datos = mapa
        guardarArchivo(mapa)
        return mapa
    }

    private fun guardarArchivo(mapa: Map<String, RecordatorioDto>) {
        archivo.writeText(gson.toJson(mapa.values.toList()))
    }

    @Synchronized
    fun todos(): List<RecordatorioDto> = cargar().values.sortedBy { it.fechaCreacion }

    /** Upsert: si el id ya existe lo reemplaza, así nunca hay duplicados. */
    @Synchronized
    fun guardar(dto: RecordatorioDto): RecordatorioDto {
        val mapa = cargar()
        mapa[dto.id!!] = dto
        guardarArchivo(mapa)
        return dto
    }

    @Synchronized
    fun eliminar(id: String): Boolean {
        val mapa = cargar()
        val existia = mapa.remove(id) != null
        guardarArchivo(mapa)
        return existia
    }
}

/**
 * Interceptor de OkHttp que responde como si fuera el servidor, sin salir a internet.
 * Retrofit y OkHttp funcionan igual que con un servidor real: se arma la request,
 * se recibe una Response con código HTTP y JSON, y Gson lo convierte en DTO.
 *
 * Rutas: GET /recordatorios · PUT /recordatorios/{id} · DELETE /recordatorios/{id}
 */
class ServidorSimuladoInterceptor(
    private val almacen: AlmacenServidorSimulado,
    private val gson: Gson,
    private val hayConexionReal: () -> Boolean,
    private val latenciaMs: Long = 600
) : Interceptor, ControlServidorSimulado {

    @Volatile
    override var modo: ModoServidor = ModoServidor.NORMAL

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Si el dispositivo no tiene red (ej: modo avión), falla igual que un servidor real
        if (!hayConexionReal() || modo == ModoServidor.SIN_CONEXION) {
            throw UnknownHostException("Sin conexión con ${request.url.host}")
        }
        Thread.sleep(latenciaMs)   // simula la demora de la red

        when (modo) {
            ModoServidor.TIMEOUT -> {
                Thread.sleep(1_500)
                throw SocketTimeoutException("timeout")
            }
            ModoServidor.ERROR_500 -> return respuesta(request, 500, ErrorDto("Error interno del servidor"))
            ModoServidor.SOLICITUD_INVALIDA ->
                return respuesta(request, 400, ErrorDto("Datos inválidos (simulado)"))
            else -> Unit
        }

        val segmentos = request.url.pathSegments.filter { it.isNotBlank() }
        val indice = segmentos.indexOf("recordatorios")
        if (indice == -1) return respuesta(request, 404, ErrorDto("Ruta inexistente"))
        val id = segmentos.getOrNull(indice + 1)

        return when {
            request.method == "GET" && id == null -> respuesta(request, 200, almacen.todos())
            request.method == "PUT" && id != null -> guardar(request, id)
            request.method == "DELETE" && id != null -> {
                if (almacen.eliminar(id)) respuesta(request, 204, null)
                else respuesta(request, 404, ErrorDto("No existe el recordatorio $id"))
            }
            else -> respuesta(request, 405, ErrorDto("Método no permitido"))
        }
    }

    private fun guardar(request: Request, id: String): Response {
        val json = request.body?.let { cuerpo ->
            val buffer = okio.Buffer()
            cuerpo.writeTo(buffer)
            buffer.readUtf8()
        }
        val dto = runCatching { gson.fromJson(json, RecordatorioDto::class.java) }.getOrNull()
            ?: return respuesta(request, 400, ErrorDto("JSON inválido"))

        // Validaciones del lado del servidor
        val error = when {
            dto.id != id -> "El id del cuerpo no coincide con la URL"
            dto.titulo.isNullOrBlank() || dto.titulo.trim().length < 4 -> "Título inválido"
            dto.prioridad.isNullOrBlank() -> "Falta la prioridad"
            else -> null
        }
        if (error != null) return respuesta(request, 400, ErrorDto(error))

        val guardado = almacen.guardar(dto.copy(actualizadoEn = dto.actualizadoEn ?: System.currentTimeMillis()))
        return respuesta(request, 200, guardado)
    }

    private fun respuesta(request: Request, codigo: Int, cuerpo: Any?): Response {
        val json = if (cuerpo == null) "" else gson.toJson(cuerpo)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(mensajeHttp(codigo))
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun mensajeHttp(codigo: Int) = when (codigo) {
        200 -> "OK"
        204 -> "No Content"
        400 -> "Bad Request"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        else -> "Internal Server Error"
    }
}
