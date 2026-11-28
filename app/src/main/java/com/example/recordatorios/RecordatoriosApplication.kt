package com.example.recordatorios

import android.app.Application
import com.example.recordatorios.data.AppContainer

class RecordatoriosApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.iniciar()
    }
}

/**
 * Para las factories de los ViewModel: obtiene el contenedor desde los CreationExtras
 * (así los ViewModel reciben sus dependencias sin guardar referencias a la Activity).
 */
fun androidx.lifecycle.viewmodel.CreationExtras.contenedor(): AppContainer =
    (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        as RecordatoriosApplication).container
