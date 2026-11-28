package com.example.recordatorios.fakes

import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.remote.ResultadoRed
import kotlinx.coroutines.delay

/**
 * Nube falsa que imita Firestore: users / uid / reminders / id, con la misma regla
 * de seguridad (solo el dueño puede leer o escribir su colección).
 */
class FakeFuenteRemota(var uidAutenticado: () -> String?) : FuenteRemotaRecordatorios {

    enum class Falla { NINGUNA, SIN_RED, TIMEOUT, NO_DISPONIBLE, RECHAZO }

    /** nube[uid][id] */
    val nube = mutableMapOf<String, MutableMap<String, RecordatorioDto>>()
    var falla = Falla.NINGUNA
    var demoraMs = 0L
    var llamadasGuardar = 0
    var llamadasEliminar = 0

    fun documentosDe(uid: String) = nube.getOrPut(uid) { mutableMapOf() }

    private fun <T> controlar(uidDestino: String): ResultadoRed<T>? = when {
        falla == Falla.SIN_RED -> ResultadoRed.SinConexion
        falla == Falla.TIMEOUT -> ResultadoRed.Timeout
        falla == Falla.NO_DISPONIBLE -> ResultadoRed.ErrorServidor(503)
        falla == Falla.RECHAZO -> ResultadoRed.SolicitudInvalida(400, "Datos rechazados por el servidor")
        // Regla de seguridad: request.auth.uid == uid
        uidAutenticado() != uidDestino -> ResultadoRed.SolicitudInvalida(403, "Permiso denegado por las reglas de seguridad")
        else -> null
    }

    override suspend fun obtenerTodos(uid: String): ResultadoRed<List<RecordatorioDto>> {
        delay(demoraMs)
        controlar<List<RecordatorioDto>>(uid)?.let { return it }
        return ResultadoRed.Exito(documentosDe(uid).values.toList())
    }

    override suspend fun guardar(uid: String, recordatorio: RecordatorioDto): ResultadoRed<Unit> {
        llamadasGuardar++
        delay(demoraMs)
        controlar<Unit>(uid)?.let { return it }
        documentosDe(uid)[recordatorio.id!!] = recordatorio   // set(): mismo id = mismo documento
        return ResultadoRed.Exito(Unit)
    }

    override suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit> {
        llamadasEliminar++
        delay(demoraMs)
        controlar<Unit>(uid)?.let { return it }
        documentosDe(uid).remove(id)
        return ResultadoRed.Exito(Unit)
    }
}
