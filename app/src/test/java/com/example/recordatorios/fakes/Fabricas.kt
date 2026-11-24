package com.example.recordatorios.fakes

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.preferencias.OrdenRecordatorios
import com.example.recordatorios.data.preferencias.PreferenciasRepository
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import kotlinx.coroutines.flow.MutableStateFlow

fun entidadDePrueba(
    id: String,
    titulo: String = "Recordatorio $id",
    estadoSync: EstadoSyncEntity = EstadoSyncEntity.PENDIENTE,
    actualizadoEn: Long = 1_000L,
    prioridad: String = "MEDIA",
    estado: String = "PENDIENTE"
) = RecordatorioEntity(
    id = id,
    titulo = titulo,
    descripcion = null,
    categoriaId = 1,
    prioridad = prioridad,
    estado = estado,
    fechaEstado = null,
    fechaLimite = null,
    fechaCreacion = actualizadoEn,
    actualizadoEn = actualizadoEn,
    estadoSync = estadoSync
)

class FakePreferencias : PreferenciasRepository {
    override val preferencias = MutableStateFlow(PreferenciasUsuario())
    override suspend fun cambiarTema(tema: TemaApp) { preferencias.value = preferencias.value.copy(tema = tema) }
    override suspend fun cambiarMostrarCompletados(mostrar: Boolean) {
        preferencias.value = preferencias.value.copy(mostrarCompletados = mostrar)
    }
    override suspend fun cambiarOrden(orden: OrdenRecordatorios) { preferencias.value = preferencias.value.copy(orden = orden) }
    override suspend fun restablecer() { preferencias.value = PreferenciasUsuario() }
    override suspend fun guardarValorInvalidoDePrueba() = Unit
}
