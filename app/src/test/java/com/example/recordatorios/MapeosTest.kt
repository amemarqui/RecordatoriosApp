package com.example.recordatorios

import com.example.recordatorios.data.local.EstadoSyncEntity
import com.example.recordatorios.data.mapeo.aDominio
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.mapeo.aEntityONull
import com.example.recordatorios.data.remote.RecordatorioDto
import com.example.recordatorios.data.remote.mensajeDeError
import com.example.recordatorios.fakes.entidadDePrueba
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MapeosTest {

    @Test
    fun dtoValidoSeConvierteEnEntidadSincronizada() {
        val entidad = RecordatorioDto(
            id = "x", titulo = "  Pagar luz ", categoriaId = 4, prioridad = "ALTA",
            estado = "COMPLETADO", fechaEstado = "2026-11-20T10:00", fechaCreacion = "2026-11-01T09:00"
        ).aEntityONull("usuario-a")

        assertNotNull(entidad)
        assertEquals("Pagar luz", entidad!!.titulo)
        assertEquals(EstadoSyncEntity.SINCRONIZADO, entidad.estadoSync)
        assertEquals(true, entidad.existeEnServidor)
        assertEquals("usuario-a", entidad.uid)
    }

    @Test
    fun dtoInvalidoSeDescarta() {
        assertNull(RecordatorioDto(id = null, titulo = "Sin id", categoriaId = 1, prioridad = "ALTA").aEntityONull("usuario-a"))
        assertNull(RecordatorioDto(id = "a", titulo = " ", categoriaId = 1, prioridad = "ALTA").aEntityONull("usuario-a"))
        assertNull(RecordatorioDto(id = "a", titulo = "Ok", categoriaId = 99, prioridad = "ALTA").aEntityONull("usuario-a"))
        assertNull(RecordatorioDto(id = "a", titulo = "Ok", categoriaId = 1, prioridad = "URGENTE").aEntityONull("usuario-a"))
        assertNull(RecordatorioDto(id = "a", titulo = "Ok", categoriaId = 1, prioridad = "ALTA", estado = "BORRADO").aEntityONull("usuario-a"))
    }

    @Test
    fun entidadSeConvierteEnModeloDeDominio() {
        val recordatorio = entidadDePrueba("a", prioridad = "ALTA").aDominio()!!
        assertEquals(Prioridad.Alta, recordatorio.prioridad)
        assertEquals(EstadoRecordatorio.Pendiente, recordatorio.estado)
        assertEquals(EstadoSincronizacion.PENDIENTE, recordatorio.sincronizacion)
    }

    @Test
    fun entidadIdaYVueltaPorElDtoConservaLosDatos() {
        val original = entidadDePrueba("a", prioridad = "BAJA", estado = "PENDIENTE")
        val vuelta = original.aDto().aEntityONull("usuario-a")!!
        assertEquals(original.titulo, vuelta.titulo)
        assertEquals(original.prioridad, vuelta.prioridad)
        assertEquals(original.categoriaId, vuelta.categoriaId)
        assertEquals(original.actualizadoEn, vuelta.actualizadoEn)
    }

    @Test
    fun delErrorDelServidorSeMuestraSoloElMensaje() {
        assertEquals("Título inválido", mensajeDeError("{\"mensaje\":\"Título inválido\"}", "Bad Request"))
        assertEquals("Bad Request", mensajeDeError("", "Bad Request"))
        assertEquals("texto plano", mensajeDeError("texto plano", "Bad Request"))
    }
}
