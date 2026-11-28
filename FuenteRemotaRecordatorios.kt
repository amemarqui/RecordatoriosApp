package com.example.recordatorios.data.remote

/**
 * Contrato de la fuente remota (Actividad 4). El sincronizador y el repositorio
 * dependen de esta interfaz: hoy la implementación es Firestore, y la de la
 * Actividad 3 (REST con Retrofit) sigue disponible como alternativa.
 *
 * Todas las operaciones reciben el uid: los datos remotos siempre se asocian
 * a una identidad (users / uid / reminders / reminderId).
 * Los resultados ya vienen traducidos a ResultadoRed: ningún detalle del
 * proveedor (excepciones de Firebase, códigos HTTP) sale de la capa de datos.
 */
interface FuenteRemotaRecordatorios {
    suspend fun obtenerTodos(uid: String): ResultadoRed<List<RecordatorioDto>>
    suspend fun guardar(uid: String, recordatorio: RecordatorioDto): ResultadoRed<Unit>
    suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit>
}

/** Convierte un resultado con datos en uno sin datos, conservando el error si lo hay. */
@Suppress("UNCHECKED_CAST")
fun ResultadoRed<*>.sinDatos(): ResultadoRed<Unit> =
    if (this is ResultadoRed.Exito<*>) ResultadoRed.Exito(Unit) else this as ResultadoRed<Unit>
