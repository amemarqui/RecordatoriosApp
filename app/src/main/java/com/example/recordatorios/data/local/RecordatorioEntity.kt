package com.example.recordatorios.data.local

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
 * Las fechas se guardan como epoch en milisegundos y los enums como texto.
 */
@Entity(
    tableName = "recordatorios",
    indices = [Index("estadoSync"), Index("prioridad")]
)
data class RecordatorioEntity(
    @PrimaryKey val id: String,          // UUID generado en el dispositivo (estable)
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
