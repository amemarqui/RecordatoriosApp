package com.example.recordatorios.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatorios.ui.crear.CrearRecordatorioRoute
import com.example.recordatorios.ui.detalle.DetalleRoute
import com.example.recordatorios.ui.listado.ListadoRoute

/**
 * Grafo de navegación. El NavController se crea y se usa SOLO acá:
 * las pantallas reciben lambdas (onCrearRecordatorio, onRecordatorioClick,
 * onGuardado, onCancelar, onVolver) y no saben que existe Navigation.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Rutas.LISTADO,   // destino inicial
        modifier = modifier
    ) {
        composable(route = Rutas.LISTADO) {
            ListadoRoute(
                onCrearRecordatorio = {
                    // launchSingleTop evita abrir dos formularios si se toca rápido dos veces
                    navController.navigate(Rutas.CREAR) { launchSingleTop = true }
                },
                onRecordatorioClick = { id ->
                    navController.navigate(Rutas.detalle(id))
                }
            )
        }

        composable(route = Rutas.CREAR) {
            CrearRecordatorioRoute(
                // Después de guardar o cancelar se vuelve al listado que ya estaba
                // en la pila (no se agrega otra copia del listado al historial)
                onGuardado = { navController.volverAlListado() },
                onCancelar = { navController.volverAlListado() }
            )
        }

        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.IntType })
        ) {
            // El id lo lee el DetalleViewModel desde el SavedStateHandle
            DetalleRoute(onVolver = { navController.volverAlListado() })
        }
    }
}

/**
 * Vuelve al listado con popBackStack(). Uso la versión que recibe la ruta destino
 * porque si el usuario toca "Volver" dos veces rápido, el popBackStack() común
 * sacaría también el listado y la app quedaría en blanco. Así, el segundo toque no hace nada.
 */
private fun NavHostController.volverAlListado() {
    popBackStack(route = Rutas.LISTADO, inclusive = false)
}
