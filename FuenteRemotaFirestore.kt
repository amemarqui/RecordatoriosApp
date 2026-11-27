package com.example.recordatorios.data.firebase

import com.example.recordatorios.data.remote.FuenteRemotaRecordatorios
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.remote.ResultadoRed
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Fuente remota basada en Cloud Firestore.
 *
 * Estructura:  users / {uid} / reminders / {reminderId}
 * El reminderId es el mismo UUID que usa Room: un reintento escribe sobre el
 * MISMO documento (set es idempotente) y nunca crea una copia.
 *
 * Las reglas de seguridad (firestore.rules) solo permiten leer y escribir en
 * users/{uid} si request.auth.uid == uid. La app filtra por uid, pero la
 * autorización real la hace el servidor.
 */
class FuenteRemotaFirestore(
    private val firestore: FirebaseFirestore,
    private val tiempoMaximoMs: Long = 10_000
) : FuenteRemotaRecordatorios {

    private fun coleccion(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("reminders")

    override suspend fun obtenerTodos(uid: String): ResultadoRed<List<RecordatorioDto>> = llamadaFirestore {
        // Source.SERVER: se pide al servidor (Room ya es la copia local; sin red falla enseguida)
        coleccion(uid).get(Source.SERVER).await().documents.map { it.aDto() }
    }

    override suspend fun guardar(uid: String, recordatorio: RecordatorioDto): ResultadoRed<Unit> = llamadaFirestore {
        val id = requireNotNull(recordatorio.id) { "El recordatorio no tiene id" }
        coleccion(uid).document(id).set(recordatorio.aMapaFirestore()).await()
        Unit
    }

    override suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit> = llamadaFirestore {
        // Borrar un documento que no existe no es error en Firestore: también es idempotente
        coleccion(uid).document(id).delete().await()
        Unit
    }

    /** Traduce las excepciones de Firebase a ResultadoRed (la interfaz no conoce a Firebase). */
    private suspend fun <T> llamadaFirestore(bloque: suspend () -> T): ResultadoRed<T> = try {
        ResultadoRed.Exito(withTimeout(tiempoMaximoMs) { bloque() })
    } catch (e: TimeoutCancellationException) {
        ResultadoRed.Timeout                      // va antes: es una subclase de CancellationException
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                ResultadoRed.SolicitudInvalida(403, "Permiso denegado por las reglas de seguridad")
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                ResultadoRed.SolicitudInvalida(401, "La sesión no es válida. Volvé a iniciar sesión")
            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            FirebaseFirestoreException.Code.ALREADY_EXISTS,
            FirebaseFirestoreException.Code.OUT_OF_RANGE ->
                ResultadoRed.SolicitudInvalida(400, "Datos rechazados por el servidor")
            FirebaseFirestoreException.Code.UNAVAILABLE -> ResultadoRed.SinConexion
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ResultadoRed.Timeout
            else -> ResultadoRed.ErrorServidor(500)
        }
    } catch (e: FirebaseNetworkException) {
        ResultadoRed.SinConexion
    } catch (e: Exception) {
        ResultadoRed.Desconocido(e.message ?: "Error inesperado de Firebase")
    }
}

/* ---------- Mapeo DTO <-> documento de Firestore (sin reflexión: funciona igual con R8) ---------- */

fun RecordatorioDto.aMapaFirestore(): Map<String, Any?> = mapOf(
    "id" to id,
    "titulo" to titulo,
    "descripcion" to descripcion,
    "categoriaId" to categoriaId,
    "prioridad" to prioridad,
    "estado" to estado,
    "fechaEstado" to fechaEstado,
    "fechaLimite" to fechaLimite,
    "fechaCreacion" to fechaCreacion,
    "actualizadoEn" to actualizadoEn
)

fun DocumentSnapshot.aDto(): RecordatorioDto = RecordatorioDto(
    id = getString("id") ?: id,
    titulo = getString("titulo"),
    descripcion = getString("descripcion"),
    categoriaId = getLong("categoriaId")?.toInt(),
    prioridad = getString("prioridad"),
    estado = getString("estado"),
    fechaEstado = getString("fechaEstado"),
    fechaLimite = getString("fechaLimite"),
    fechaCreacion = getString("fechaCreacion"),
    actualizadoEn = getLong("actualizadoEn")
)
