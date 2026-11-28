package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.ui.ajustes.AjustesViewModel
import com.example.recordatorios.ui.navigation.RaizApp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El tema sale de la preferencia guardada en DataStore
            val ajustes: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory)
            val preferencias by ajustes.preferencias.collectAsStateWithLifecycle()
            val oscuro = when (preferencias.tema) {
                TemaApp.SISTEMA -> isSystemInDarkTheme()
                TemaApp.CLARO -> false
                TemaApp.OSCURO -> true
            }
            RecordatoriosTheme(darkTheme = oscuro) {
                RaizApp()
            }
        }
    }
}
