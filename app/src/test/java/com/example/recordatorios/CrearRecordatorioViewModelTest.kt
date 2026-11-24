package com.example.recordatorios

import com.example.recordatorios.data.Escenario
import com.example.recordatorios.data.RepositorioEnMemoria
import com.example.recordatorios.data.RepositorioSimulado
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.ui.crear.CrearRecordatorioViewModel
import com.example.recordatorios.ui.crear.EstadoEnvio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrearRecordatorioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope usa Dispatchers.Main, que en los tests de JVM no existe
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun completarFormulario(vm: CrearRecordatorioViewModel) {
        vm.onTituloChange("Estudiar Kotlin")
        vm.onCategoriaSeleccionada(RepositorioSimulado.ESTUDIO)
        vm.onPrioridadSeleccionada(Prioridad.Alta)
    }

    @Test
    fun alAbrirNoMuestraErroresPeroNoDejaGuardar() {
        val vm = CrearRecordatorioViewModel(RepositorioEnMemoria(0, 0))
        val estado = vm.uiState.value
        assertNull(estado.errores.titulo)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun tituloCortoMuestraErrorYDeshabilitaGuardar() {
        val vm = CrearRecordatorioViewModel(RepositorioEnMemoria(0, 0))
        vm.onTituloChange("ab ")
        val estado = vm.uiState.value
        assertNotNull(estado.errores.titulo)
        assertNotNull(estado.errores.categoria)
        assertFalse(estado.puedeGuardar)
    }

    @Test
    fun guardarPasaPorEnviandoYTerminaEnExito() = runTest(dispatcher) {
        val vm = CrearRecordatorioViewModel(RepositorioEnMemoria(demoraCargaMs = 0, demoraGuardadoMs = 1_000))
        completarFormulario(vm)
        assertTrue(vm.uiState.value.puedeGuardar)

        vm.onGuardar()
        runCurrent()   // arranca la corrutina y queda esperando el delay del repositorio
        assertTrue(vm.uiState.value.envio is EstadoEnvio.Enviando)
        assertFalse(vm.uiState.value.puedeGuardar)   // botón deshabilitado durante el envío

        advanceUntilIdle()
        assertTrue(vm.uiState.value.envio is EstadoEnvio.Exitoso)
    }

    @Test
    fun errorSimuladoAlGuardarQuedaEnElEstado() = runTest(dispatcher) {
        val repo = RepositorioEnMemoria(0, 0)
        repo.aplicarEscenario(Escenario.ERROR_GUARDADO)
        val vm = CrearRecordatorioViewModel(repo)
        completarFormulario(vm)

        vm.onGuardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.envio is EstadoEnvio.Error)
        assertTrue(vm.uiState.value.puedeGuardar)   // se puede reintentar
    }
}
