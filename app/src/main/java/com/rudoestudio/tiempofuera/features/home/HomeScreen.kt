package com.rudoestudio.tiempofuera.features.home

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente

/** Tab Inicio (M5.1). Los botones son provisionales: prueban la navegación de M0.3. */
@Composable
fun HomeScreen(
    alAbrirLiga: (ligaId: String) -> Unit,
    alAbrirTiendaMonedas: () -> Unit,
    alAbrirPerfil: () -> Unit,
) {
    PantallaPendiente(titulo = "Inicio", tarea = "M5.1") {
        Button(onClick = { alAbrirLiga("liga-de-prueba") }) { Text("Ver una liga") }
        Button(onClick = alAbrirTiendaMonedas) { Text("Tienda de monedas") }
        OutlinedButton(onClick = alAbrirPerfil) { Text("Mi perfil", color = ColoresTF.Blanco) }
    }
}
