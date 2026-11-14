package com.rudoestudio.tiempofuera.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.onboarding.PasoOnboarding
import com.rudoestudio.tiempofuera.features.bloqueo.AvisoAccesibilidadScreen
import com.rudoestudio.tiempofuera.features.bloqueo.PermisosBloqueoScreen
import com.rudoestudio.tiempofuera.features.bloqueo.SelectorAppsScreen
import com.rudoestudio.tiempofuera.features.onboarding.pasos.AyudaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.BandoPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.BienvenidaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.ConfHabitoPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.ConfPermisosPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.ConfUsoInicialPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.ConfUsoMaxPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.DatosPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.DivisionesPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.EstadisticaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.FirmaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.HabitoInicialPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.MetaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.ProblemaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.RecompensaPaso
import com.rudoestudio.tiempofuera.features.onboarding.pasos.TranquilizantePaso
import com.rudoestudio.tiempofuera.navigation.AvisoAccesibilidad
import com.rudoestudio.tiempofuera.navigation.PasosOnboarding
import com.rudoestudio.tiempofuera.navigation.PermisosBloqueo
import com.rudoestudio.tiempofuera.navigation.SelectorApps

/**
 * El onboarding completo (M4). Tiene su propia navegación porque el paso de permisos
 * (M4.11) abre las pantallas de M3: permisos, aviso de accesibilidad y selector de apps.
 * Al terminar, `RootScreen` cambia a las tabs solo.
 */
@Composable
fun OnboardingScreen() {
    val navegacion = rememberNavController()
    NavHost(navegacion, startDestination = PasosOnboarding) {
        composable<PasosOnboarding> {
            CoordinadorOnboarding(
                alAbrirPermisos = { navegacion.navigate(PermisosBloqueo) },
                alElegirApps = { grupo -> navegacion.navigate(SelectorApps(grupo.clave)) },
            )
        }
        composable<PermisosBloqueo> {
            PermisosBloqueoScreen(
                alAbrirAvisoAccesibilidad = { navegacion.navigate(AvisoAccesibilidad) },
                alTerminar = { navegacion.popBackStack() },
                alVolver = { navegacion.popBackStack() },
            )
        }
        composable<AvisoAccesibilidad> {
            AvisoAccesibilidadScreen(alVolver = { navegacion.popBackStack() })
        }
        composable<SelectorApps> {
            SelectorAppsScreen(alTerminar = { navegacion.popBackStack() })
        }
    }
}

/** Un paso a la vez, con el botón de regresar donde se puede. En iOS: `OnboardingCoordinatorView`. */
@Composable
private fun CoordinadorOnboarding(
    alAbrirPermisos: () -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    if (estado.cargando) {
        Box(Modifier.fillMaxSize().background(ColoresTF.Celeste))
        return
    }
    // Los ViewModels de los pasos viven lo que dura el onboarding, como los `lazy var` de iOS.
    val datos: DatosViewModel = hiltViewModel()
    val confPermisos: ConfPermisosViewModel = hiltViewModel()
    val confUsoMax: ConfUsoMaxViewModel = hiltViewModel()
    val confHabito: ConfHabitoViewModel = hiltViewModel()

    BackHandler(enabled = estado.paso.puedeRegresar) { viewModel.anterior() }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = estado.paso,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "paso",
        ) { paso ->
            val b = estado.borrador
            when (paso) {
                PasoOnboarding.BIENVENIDA -> BienvenidaPaso(viewModel::siguiente, viewModel::yaTengoPerfil)
                PasoOnboarding.ESTADISTICA -> EstadisticaPaso(viewModel::siguiente)
                PasoOnboarding.PROBLEMA -> ProblemaPaso(viewModel::siguiente)
                PasoOnboarding.AYUDA -> AyudaPaso(viewModel::siguiente)
                PasoOnboarding.DATOS -> DatosPaso(datos, viewModel::datosCompletados)
                PasoOnboarding.META -> MetaPaso(b.proposito) { p -> viewModel.continuar { it.copy(proposito = p) } }
                PasoOnboarding.BANDO -> BandoPaso(b.personalidadId) { p -> viewModel.continuar { it.copy(personalidadId = p) } }
                PasoOnboarding.DIVISIONES -> DivisionesPaso(b.divisionesSeleccionadas) { d -> viewModel.continuar { it.copy(divisionesSeleccionadas = d) } }
                PasoOnboarding.TRANQUILIZANTE -> TranquilizantePaso(viewModel::siguiente)
                PasoOnboarding.CONF_USO_INICIAL -> ConfUsoInicialPaso(b.tiempoInicial) { m -> viewModel.continuar { it.copy(tiempoInicial = m) } }
                PasoOnboarding.CONF_PERMISOS -> ConfPermisosPaso(confPermisos, alAbrirPermisos, alElegirApps, viewModel::siguiente)
                PasoOnboarding.CONF_USO_MAX -> ConfUsoMaxPaso(confUsoMax, b.tiempoInicial) { c ->
                    viewModel.continuar {
                        it.copy(
                            tiempoMaxUso = ((c.tiempoMaximoDiario ?: 0L) / 60).toInt(),
                            intervaloHitoUso = ((c.intervaloHito ?: 0L) / 60).toInt(),
                        )
                    }
                }
                PasoOnboarding.CONF_HABITO -> ConfHabitoPaso(confHabito) { c ->
                    viewModel.continuar { it.copy(horaInicioDia = c.horaInicioDia, horaFinDia = c.horaFinDia, colchonInicioFin = c.colchonUso) }
                }
                PasoOnboarding.FIRMA -> FirmaPaso(viewModel::siguiente)
                PasoOnboarding.RECOMPENSA -> RecompensaPaso(viewModel::siguiente)
                PasoOnboarding.HABITO_INICIAL -> HabitoInicialPaso(viewModel::siguiente)
            }
        }

        if (estado.paso.puedeRegresar) {
            BotonRegresar(viewModel::anterior, Modifier.safeDrawingPadding().padding(start = 6.dp, top = 6.dp))
        }

        if (estado.finalizando) {
            // Tapa los toques mientras se guarda el perfil (en iOS: `.disabled`).
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = ColoresTF.Amarillo) }
        }
    }

    estado.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = viewModel::limpiarError,
            confirmButton = { TextButton(onClick = viewModel::limpiarError) { Text("Cerrar") } },
            title = { Text("No se pudo completar el registro") },
            text = { Text(mensaje) },
        )
    }
}
