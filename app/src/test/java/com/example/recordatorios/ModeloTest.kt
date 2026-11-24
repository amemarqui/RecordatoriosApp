package com.example.recordatorios

import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.data.cantidadPorEstado
import com.example.recordatorios.data.filtrarPorEstado
import com.example.recordatorios.data.filtrarPorPrioridad
import com.example.recordatorios.data.ordenados
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.ResultadoValidacion
import com.example.recordatorios.model.ValidadorRecordatorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ModeloTest {

    private val ahora = LocalDateTime.of(2026, 10, 27, 12, 0)
    private val datos = RepositorioSimulado.recordatorios(ahora)

    @Test
    fun setEliminaCategoriasRepetidas() {
        assertEquals(5, RepositorioSimulado.categorias.size)
    }

    @Test
    fun cadaCategoriaTieneEstiloEnElMap() {
        RepositorioSimulado.categorias.forEach {
            assertTrue("Falta estilo para ${it.nombre}", RepositorioSimulado.estilosPorCategoria.containsKey(it))
        }
    }

    @Test
    fun filtraPorEstadoYPrioridad() {
        val completados = datos.filtrarPorEstado(EstadoRecordatorio.Completado::class)
        assertEquals(2, completados.size)
        assertTrue(completados.all { it.estado is EstadoRecordatorio.Completado })

        val altas = datos.filtrarPorPrioridad(Prioridad.Alta)
        assertEquals(4, altas.size)

        assertEquals(datos.size, datos.filtrarPorEstado(null).filtrarPorPrioridad(null).size)
    }

    @Test
    fun ordenaPrimeroLaPrioridadAlta() {
        assertEquals(Prioridad.Alta, datos.ordenados().first().prioridad)
        assertEquals(Prioridad.Baja, datos.ordenados().last().prioridad)
    }

    @Test
    fun cuentaPorEstado() {
        val conteo = datos.cantidadPorEstado()
        assertEquals(datos.size, conteo.values.sum())
    }

    @Test
    fun dataClassEsInmutableConCopy() {
        val original = datos.first()
        val modificado = original.copy(prioridad = Prioridad.Baja)
        assertNotEquals(original, modificado)
        assertEquals(Prioridad.Alta, original.prioridad)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noPermiteTituloVacio() {
        Recordatorio(id = 1, titulo = " ", categoria = Categoria(1, "X"), prioridad = Prioridad.Baja)
    }

    @Test
    fun validadorAcumulaErrores() {
        val r = ValidadorRecordatorio.validar(
            id = 0, titulo = "", categoria = null, prioridad = null,
            minutosAvisoPrevio = 5, ahora = ahora
        )
        assertTrue(r is ResultadoValidacion.Invalido)
        assertEquals(5, (r as ResultadoValidacion.Invalido).errores.size)
    }

    @Test
    fun formularioRechazaTituloCortoAunqueTengaEspacios() {
        val errores = ValidadorRecordatorio.validarFormulario(
            titulo = "   abc   ", descripcion = "", categoria = RepositorioSimulado.ESTUDIO,
            prioridad = Prioridad.Baja, categoriasValidas = RepositorioSimulado.categorias
        )
        assertNotNull(errores.titulo)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun formularioRechazaDescripcionLargaYCategoriaInexistente() {
        val errores = ValidadorRecordatorio.validarFormulario(
            titulo = "Comprar pan", descripcion = "x".repeat(201),
            categoria = Categoria(99, "Inventada"), prioridad = null,
            categoriasValidas = RepositorioSimulado.categorias
        )
        assertNotNull(errores.descripcion)
        assertNotNull(errores.categoria)
        assertNotNull(errores.prioridad)
    }

    @Test
    fun formularioValidoNoTieneErrores() {
        val errores = ValidadorRecordatorio.validarFormulario(
            titulo = "Comprar pan", descripcion = "x".repeat(200),
            categoria = RepositorioSimulado.HOGAR, prioridad = Prioridad.Media,
            categoriasValidas = RepositorioSimulado.categorias
        )
        assertFalse(errores.hayErrores)
    }

    @Test
    fun validadorAceptaDatosCorrectos() {
        val r = ValidadorRecordatorio.validar(
            id = 10, titulo = "  Estudiar Kotlin ", categoria = RepositorioSimulado.ESTUDIO,
            prioridad = Prioridad.Media, fechaLimite = ahora.plusDays(1), ahora = ahora
        )
        assertTrue(r is ResultadoValidacion.Valido)
        assertEquals("Estudiar Kotlin", (r as ResultadoValidacion.Valido).recordatorio.titulo)
    }
}
