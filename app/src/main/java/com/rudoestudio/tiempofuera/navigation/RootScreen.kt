package com.rudoestudio.tiempofuera.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente
import com.rudoestudio.tiempofuera.features.onboarding.OnboardingScreen

@Composable
fun RootScreen(
    deepLinkPendiente: DeepLink?,
    alAtenderDeepLink: () -> Unit,
    viewModel: RootViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoRaiz.Cargando -> Box(
            Modifier.fillMaxSize().background(ColoresTF.Azul),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = ColoresTF.Amarillo) }

        EstadoRaiz.Onboarding -> OnboardingScreen(alTerminar = viewModel::terminarOnboarding)

        EstadoRaiz.Principal -> MainScaffold(
            deepLinkPendiente = deepLinkPendiente,
            alAtenderDeepLink = alAtenderDeepLink,
        )

        is EstadoRaiz.Error -> PantallaPendiente(titulo = "¡Ups!", tarea = e.mensaje) {
            Button(onClick = viewModel::reintentar) { Text("Reintentar") }
        }
    }
}
