package com.example.recordatorios.data.mapeo

import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.Ubicacion
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/*
 * Mapeos explícitos entre las tres capas:
 *   DTO (servicio remoto)  <->  Entity (Room)  ->  Recordatorio (dominio / pantallas)
 * Los DTO nunca llegan a la interfaz y las entidades nunca salen de la capa de datos.
 */

/* ---------- Auxiliares ---------- */

private val zona: ZoneId get() = ZoneId.systemDefault()

fun LocalDateTime.aEpochMs(): Long = atZone(zona).toInstant().toEpochMilli()
fun Long.aLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zona)

private fun String.aLocalDateTimeONull(): LocalDateTime? = runCatching { LocalDateTime.parse(this) }.getOrNull()

private val CATEGORIA_DESCONOCIDA = Categoria(0, "Sin categoría")

fun categoriaPorId(id: Int): Categoria? = RepositorioSimulado.categorias.find { it.id == id }

fun Prioridad.codigo(): String = when (this) {
    Prioridad.Alta -> "ALTA"
    Prioridad.Media -> "MEDIA"
    Prioridad.Baja -> "BAJA"
}

fun prioridadDesdeCodigo(codigo: String?): Prioridad? = when (codigo) {
    "ALTA" -> Prioridad.Alta
    "MEDIA" -> Prioridad.Media
    "BAJA" -> Prioridad.Baja
    else -> null
}

private val CODIGOS_ESTADO = setOf("PENDIENTE", "COMPLETADO", "POSPUESTO", "VENCIDO")

fun EstadoRecordatorio.codigo(): String = when (this) {
    EstadoRecordatorio.Pendiente -> "PENDIENTE"
    is EstadoRecordatorio.Completado -> "COMPLETADO"
    is EstadoRecordatorio.Pospuesto -> "POSPUESTO"
    EstadoRecordatorio.Vencido -> "VENCIDO"
}

private fun EstadoRecordatorio.fecha(): LocalDateTime? = when (this) {
    is EstadoRecordatorio.Completado -> fechaCompletado
    is EstadoRecordatorio.Pospuesto -> hasta
    else -> null
}

private fun estadoDesde(codigo: String, fecha: LocalDateTime?): EstadoRecordatorio = when (codigo) {
    "COMPLETADO" -> EstadoRecordatorio.Completado(fecha ?: LocalDateTime.now())
    "POSPUESTO" -> fecha?.let { EstadoRecordatorio.Pospuesto(it) } ?: EstadoRecordatorio.Pendiente
    "VENCIDO" -> EstadoRecordatorio.Vencido
    else -> EstadoRecordatorio.Pendiente
}

fun EstadoSyncEntity.aDominio(): EstadoSincronizacion = when (this) {
    EstadoSyncEntity.SINCRONIZADO -> EstadoSincronizacion.SINCRONIZADO
    EstadoSyncEntity.PENDIENTE, EstadoSyncEntity.PENDIENTE_ELIMINAR -> EstadoSincronizacion.PENDIENTE
    EstadoSyncEntity.ERROR -> EstadoSincronizacion.ERROR
}

/* ---------- Entity -> dominio (lo que ven las pantallas) ---------- */

/**
 * Devuelve null si el registro guardado no cumple las reglas del modelo
 * (así un dato corrupto no rompe la app: simplemente no se muestra).
 */
fun RecordatorioEntity.aDominio(): Recordatorio? = runCatching {
    val ubicacion = if (latitud != null && longitud != null) {
        Ubicacion(nombreUbicacion ?: "Ubicación", latitud, longitud)
    } else null
    Recordatorio(
        id = id,
        titulo = titulo,
        categoria = categoriaPorId(categoriaId) ?: CATEGORIA_DESCONOCIDA,
        prioridad = prioridadDesdeCodigo(prioridad) ?: Prioridad.Media,
        estado = estadoDesde(estado, fechaEstado?.aLocalDateTime()),
        descripcion = descripcion,
        fechaLimite = fechaLimite?.aLocalDateTime(),
        ubicacion = ubicacion,
        minutosAvisoPrevio = minutosAviso,
        fechaCreacion = fechaCreacion.aLocalDateTime(),
        sincronizacion = estadoSync.aDominio(),
        errorSincronizacion = ultimoErrorSync
    )
}.getOrNull()

/* ---------- Entity -> DTO (para enviar al servidor) ---------- */

fun RecordatorioEntity.aDto(): RecordatorioDto = RecordatorioDto(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    categoriaId = categoriaId,
    prioridad = prioridad,
    estado = estado,
    fechaEstado = fechaEstado?.aLocalDateTime()?.toString(),
    fechaLimite = fechaLimite?.aLocalDateTime()?.toString(),
    fechaCreacion = fechaCreacion.aLocalDateTime().toString(),
    actualizadoEn = actualizadoEn
)

/* ---------- DTO -> Entity (lo que llega del servidor, ANTES de guardarlo en Room) ---------- */

/**
 * Valida lo que mandó el servidor. Si falta algo obligatorio o viene un valor
 * desconocido, devuelve null y ese registro se descarta (no se guarda en Room).
 */
fun RecordatorioDto.aEntityONull(uid: String): RecordatorioEntity? {
    val idValido = id?.takeIf { it.isNotBlank() } ?: return null
    val tituloValido = titulo?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val prioridadValida = prioridad?.takeIf { prioridadDesdeCodigo(it) != null } ?: return null
    val categoriaValida = categoriaId?.takeIf { categoriaPorId(it) != null } ?: return null
    val estadoValido = (estado ?: "PENDIENTE").takeIf { it in CODIGOS_ESTADO } ?: return null
    val creado = fechaCreacion?.aLocalDateTimeONull()?.aEpochMs() ?: System.currentTimeMillis()

    return RecordatorioEntity(
        id = idValido,
        uid = uid,
        titulo = tituloValido,
        descripcion = descripcion?.trim()?.ifEmpty { null },
        categoriaId = categoriaValida,
        prioridad = prioridadValida,
        estado = estadoValido,
        fechaEstado = fechaEstado?.aLocalDateTimeONull()?.aEpochMs(),
        fechaLimite = fechaLimite?.aLocalDateTimeONull()?.aEpochMs(),
        fechaCreacion = creado,
        actualizadoEn = actualizadoEn ?: creado,
        estadoSync = EstadoSyncEntity.SINCRONIZADO,   // viene del servidor: ya está confirmado
        existeEnServidor = true
    )
}

/* ---------- Dominio -> DTO (solo para cargar los datos iniciales del servidor simulado) ---------- */

fun Recordatorio.aDto(): RecordatorioDto {
    val creado = fechaCreacion ?: LocalDateTime.now()
    return RecordatorioDto(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        categoriaId = categoria.id,
        prioridad = prioridad.codigo(),
        estado = estado.codigo(),
        fechaEstado = estado.fecha()?.toString(),
        fechaLimite = fechaLimite?.toString(),
        fechaCreacion = creado.toString(),
        actualizadoEn = creado.aEpochMs()
    )
}
