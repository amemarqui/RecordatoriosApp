package com.example.recordatorios.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatorios.ui.ajustes.AjustesRoute
import com.example.recordatorios.ui.detalle.DetalleRoute
import com.example.recordatorios.ui.formulario.FormularioRoute
import com.example.recordatorios.ui.listado.ListadoRoute

/**
 * Grafo de navegación. El NavController se usa SOLO acá: las pantallas avisan
 * con lambdas y no saben que existe Navigation.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Rutas.LISTADO,
        modifier = modifier
    ) {
        composable(route = Rutas.LISTADO) {
            ListadoRoute(
                onCrearRecordatorio = { navController.navigate(Rutas.CREAR) { launchSingleTop = true } },
                onRecordatorioClick = { id -> navController.navigate(Rutas.detalle(id)) },
                onAbrirAjustes = { navController.navigate(Rutas.AJUSTES) { launchSingleTop = true } }
            )
        }

        composable(route = Rutas.CREAR) {
            FormularioRoute(
                onGuardado = { navController.volverDesde(Rutas.CREAR) },
                onCancelar = { navController.volverDesde(Rutas.CREAR) }
            )
        }

        composable(
            route = Rutas.EDITAR,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType })
        ) {
            // Al terminar de editar se vuelve al detalle, que se actualiza solo (Flow de Room)
            FormularioRoute(
                onGuardado = { navController.volverDesde(Rutas.EDITAR) },
                onCancelar = { navController.volverDesde(Rutas.EDITAR) }
            )
        }

        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType })
        ) {
            DetalleRoute(
                onVolver = { navController.volverDesde(Rutas.DETALLE) },
                onEditar = { id -> navController.navigate(Rutas.editar(id)) { launchSingleTop = true } }
            )
        }

        composable(route = Rutas.AJUSTES) {
            AjustesRoute(onVolver = { navController.volverDesde(Rutas.AJUSTES) })
        }
    }
}

/**
 * popBackStack() solo si la pantalla que pide volver es la que está arriba.
 * Así, si se toca "Volver" dos veces rápido, el segundo toque no saca también
 * el listado (lo que dejaría la app en blanco).
 */
private fun NavHostController.volverDesde(ruta: String) {
    if (currentDestination?.route == ruta) popBackStack()
}
