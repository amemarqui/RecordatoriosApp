package com.example.recordatorios.data

/**
 * Contenedor de dependencias "a mano". Hay una sola instancia del repositorio
 * para toda la app, así las tres pantallas ven los mismos datos.
 * (Más adelante se podría reemplazar por Hilt.)
 */
object AppContainer {
    private val repositorioEnMemoria = RepositorioEnMemoria()

    val repositorio: RecordatoriosRepository = repositorioEnMemoria
    val simulador: SimuladorEscenarios = repositorioEnMemoria
}
