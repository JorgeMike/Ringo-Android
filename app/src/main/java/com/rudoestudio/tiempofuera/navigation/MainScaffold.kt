package com.rudoestudio.tiempofuera.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.features.bloqueo.AvisoAccesibilidadScreen
import com.rudoestudio.tiempofuera.features.bloqueo.PermisosBloqueoScreen
import com.rudoestudio.tiempofuera.features.bloqueo.SelectorAppsScreen
import com.rudoestudio.tiempofuera.features.galeria.GaleriaScreen
import com.rudoestudio.tiempofuera.features.home.HomeScreen
import com.rudoestudio.tiempofuera.features.laboratorio.LaboratorioApiScreen
import com.rudoestudio.tiempofuera.features.ligas.LigaNivelesScreen
import com.rudoestudio.tiempofuera.features.luchadores.LuchadoresScreen
import com.rudoestudio.tiempofuera.features.perfil.PerfilScreen
import com.rudoestudio.tiempofuera.features.tienda.TiendaScreen
import com.rudoestudio.tiempofuera.features.tiendamonedas.TiendaMonedasScreen
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private enum class TabPrincipal(
    val ruta: Any,
    val claseRuta: KClass<*>,
    val etiqueta: String,
    val icono: ImageVector,
) {
    INICIO(GrafoInicio, GrafoInicio::class, "Inicio", Icons.Filled.Home),
    LUCHADORES(Luchadores, Luchadores::class, "Luchadores", Icons.Filled.Person),
    TIENDA(Tienda, Tienda::class, "Tienda", Icons.Filled.ShoppingCart),
    PERFIL(Perfil, Perfil::class, "Perfil", Icons.Filled.AccountCircle),
}

/** Las 4 tabs y todo lo que se abre desde ellas (en iOS: `MainTabView`). */
@Composable
fun MainScaffold(
    deepLinkPendiente: DeepLink?,
    alAtenderDeepLink: () -> Unit,
) {
    val navController = rememberNavController()
    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    // Las misiones llegan en M6: por ahora solo confirmamos que el enlace se recibió.
    LaunchedEffect(deepLinkPendiente) {
        val link = deepLinkPendiente ?: return@LaunchedEffect
        when (link) {
            is DeepLink.IdMision -> {
                AppLogger.info("Deep link: misión ${link.id}")
                // Fuera de este efecto: alAtenderDeepLink() lo reinicia y cancelaría el aviso.
                alcance.launch { snackbar.showSnackbar("Abrir misión ${link.id}: llega con M6") }
            }
        }
        alAtenderDeepLink()
    }

    val enTab = TabPrincipal.entries.any { tab -> destino?.hierarchy?.any { it.hasRoute(tab.claseRuta) } == true }

    Scaffold(
        // Sin márgenes del sistema: cada pantalla pinta su fondo detrás de la barra de
        // estado y se aparta sola (PantallaPendiente usa safeDrawingPadding).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = ColoresTF.Azul,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (enTab) {
                NavigationBar(containerColor = ColoresTF.Azul) {
                    TabPrincipal.entries.forEach { tab ->
                        val seleccionada = destino?.hierarchy?.any { it.hasRoute(tab.claseRuta) } == true
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = {
                                navController.navigate(tab.ruta) {
                                    // Cada tab conserva su pila al cambiar de tab y volver.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icono, contentDescription = null) },
                            label = { Text(tab.etiqueta) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColoresTF.Azul,
                                selectedTextColor = ColoresTF.Amarillo,
                                indicatorColor = ColoresTF.Amarillo,
                                unselectedIconColor = ColoresTF.Blanco,
                                unselectedTextColor = ColoresTF.Blanco,
                            ),
                        )
                    }
                }
            }
        },
    ) { relleno ->
        NavHost(
            navController = navController,
            startDestination = GrafoInicio,
            modifier = Modifier.padding(relleno).consumeWindowInsets(relleno),
        ) {
            navigation<GrafoInicio>(startDestination = Inicio) {
                composable<Inicio> {
                    HomeScreen(
                        alAbrirLiga = { ligaId -> navController.navigate(LigaNiveles(ligaId)) },
                        alAbrirTiendaMonedas = { navController.navigate(TiendaMonedas) },
                        alAbrirGaleria = { navController.navigate(Galeria) },
                        alAbrirLaboratorio = { navController.navigate(LaboratorioApi) },
                        alAbrirPerfil = {
                            navController.navigate(Perfil) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable<LigaNiveles> { entradaLiga ->
                    LigaNivelesScreen(
                        ligaId = entradaLiga.toRoute<LigaNiveles>().ligaId,
                        alVolver = { navController.popBackStack() },
                    )
                }
            }
            composable<Luchadores> { LuchadoresScreen() }
            composable<Tienda> { TiendaScreen() }
            composable<Perfil> { PerfilScreen() }
            composable<TiendaMonedas> {
                TiendaMonedasScreen(alCerrar = { navController.popBackStack() })
            }
            composable<Galeria> {
                GaleriaScreen(alVolver = { navController.popBackStack() })
            }
            composable<LaboratorioApi> {
                LaboratorioApiScreen(
                    alVolver = { navController.popBackStack() },
                    alAbrirPermisos = { navController.navigate(PermisosBloqueo) },
                    alElegirApps = { grupo -> navController.navigate(SelectorApps(grupo.clave)) },
                )
            }
            composable<PermisosBloqueo> {
                PermisosBloqueoScreen(
                    alAbrirAvisoAccesibilidad = { navController.navigate(AvisoAccesibilidad) },
                    alTerminar = { navController.popBackStack() },
                    alVolver = { navController.popBackStack() },
                )
            }
            composable<AvisoAccesibilidad> {
                AvisoAccesibilidadScreen(alVolver = { navController.popBackStack() })
            }
            composable<SelectorApps> {
                SelectorAppsScreen(alTerminar = { navController.popBackStack() })
            }
        }
    }
}
