package com.example.recordatorios.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estado de sincronización tal como se guarda en la tabla (Room lo guarda como texto). */
enum class EstadoSyncEntity {
    SINCRONIZADO,          // confirmado por el servidor
    PENDIENTE,             // creado o modificado localmente, falta enviarlo
    PENDIENTE_ELIMINAR,    // borrado localmente, falta avisarle al servidor
    ERROR                  // el servidor lo rechazó (400): necesita que el usuario reintente
}

/**
 * Tabla "recordatorios" (esquema de Room). Es la FUENTE DE VERDAD de la interfaz.
 * Versión 2 de la base (Actividad 4): se agregó la columna uid.
 * Las fechas se guardan como epoch en milisegundos y los enums como texto.
 */
@Entity(
    tableName = "recordatorios",
    indices = [Index("estadoSync"), Index("prioridad"), Index("uid")]
)
data class RecordatorioEntity(
    @PrimaryKey val id: String,          // UUID generado en el dispositivo (estable)
    // Actividad 4: dueño del registro (uid de Firebase Authentication).
    // Todas las consultas filtran por uid: una cuenta nunca ve los datos de otra.
    @ColumnInfo(defaultValue = "") val uid: String,
    val titulo: String,
    val descripcion: String?,
    val categoriaId: Int,
    val prioridad: String,               // "ALTA" | "MEDIA" | "BAJA"
    val estado: String,                  // "PENDIENTE" | "COMPLETADO" | "POSPUESTO" | "VENCIDO"
    val fechaEstado: Long?,              // fecha de completado o "pospuesto hasta"
    val fechaLimite: Long?,
    val fechaCreacion: Long,
    val actualizadoEn: Long,             // sirve como "versión" del registro

    // Preparado para módulos siguientes (ubicación y avisos)
    val nombreUbicacion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val minutosAviso: Int? = null,

    // Sincronización
    val estadoSync: EstadoSyncEntity = EstadoSyncEntity.PENDIENTE,
    val existeEnServidor: Boolean = false,
    val intentosSync: Int = 0,
    val ultimoErrorSync: String? = null
)
