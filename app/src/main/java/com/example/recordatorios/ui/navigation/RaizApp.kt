package com.example.recordatorios.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.contenedor
import com.example.recordatorios.data.auth.EstadoSesion
import kotlinx.coroutines.flow.StateFlow
import com.example.recordatorios.ui.auth.AuthRoute

/** Expone el estado de sesión a la raíz sin que la UI toque Firebase. */
class SesionViewModel(val sesion: StateFlow<EstadoSesion>) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SesionViewModel(contenedor().autenticacion.sesion) }
        }
    }
}

/**
 * Raíz de la app: decide qué mostrar según la sesión.
 *  - Verificando → indicador de carga (se está validando la sesión guardada).
 *  - SinSesion   → pantalla de login / registro (con el motivo si la sesión se perdió).
 *  - Activa      → el grafo de navegación normal.
 * key(uid) hace que, si cambia la cuenta, se cree un NavHost nuevo con ViewModels nuevos:
 * no queda en memoria ninguna pantalla ni dato de la cuenta anterior.
 */
@Composable
fun RaizApp(viewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize()) {
        when (val estado = sesion) {
            EstadoSesion.Verificando -> PantallaCargando()
            is EstadoSesion.SinSesion -> AuthRoute(motivoSesionPerdida = estado.motivo)
            is EstadoSesion.Activa -> key(estado.uid) { AppNavHost() }
        }
    }
}

@Composable
private fun PantallaCargando() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Verificando la sesión…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
