package com.rudoestudio.tiempofuera.features.ligas

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente

/** Camino de niveles de una liga (M9.2). */
@Composable
fun LigaNivelesScreen(ligaId: String, alVolver: () -> Unit) {
    PantallaPendiente(titulo = "Liga: $ligaId", tarea = "M9.2", fondo = ColoresTF.FondoMisiones) {
        Button(onClick = alVolver) { Text("Volver") }
    }
}
