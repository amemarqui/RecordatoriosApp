package com.example.recordatorios.data.remote

/**
 * Fuente remota de la Actividad 3 (Retrofit + OkHttp). Se conserva como alternativa:
 * el servidor de prueba no maneja usuarios, por eso el uid no se envía.
 */
class FuenteRemotaRest(private val api: RecordatoriosApi) : FuenteRemotaRecordatorios {

    @Suppress("UNCHECKED_CAST")
    override suspend fun obtenerTodos(uid: String): ResultadoRed<List<RecordatorioDto>> =
        when (val r = llamadaSegura { api.obtenerTodos() }) {
            is ResultadoRed.Exito -> ResultadoRed.Exito(r.datos.orEmpty())
            else -> r
        }

    override suspend fun guardar(uid: String, recordatorio: RecordatorioDto): ResultadoRed<Unit> =
        llamadaSegura { api.guardar(recordatorio.id.orEmpty(), recordatorio) }.sinDatos()

    override suspend fun eliminar(uid: String, id: String): ResultadoRed<Unit> =
        llamadaSegura(codigosAceptados = setOf(404)) { api.eliminar(id) }.sinDatos()
}
