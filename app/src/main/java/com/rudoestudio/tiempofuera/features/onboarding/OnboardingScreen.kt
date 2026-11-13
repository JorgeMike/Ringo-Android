package com.rudoestudio.tiempofuera.features.onboarding

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente

/** Onboarding de 16 pasos (M4). Por ahora solo deja pasar a las tabs. */
@Composable
fun OnboardingScreen(alTerminar: () -> Unit) {
    PantallaPendiente(titulo = "¡Bienvenido al ring!", tarea = "M4", fondo = ColoresTF.Rojo) {
        Button(
            onClick = alTerminar,
            colors = ButtonDefaults.buttonColors(
                containerColor = ColoresTF.Amarillo,
                contentColor = ColoresTF.Negro,
            ),
        ) { Text("Entrar (provisional)") }
    }
}
