package com.rudoestudio.tiempofuera.features.tiendamonedas

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente

/** Tienda de monedas: en iOS también dice "Próximamente" (M10.2). */
@Composable
fun TiendaMonedasScreen(alCerrar: () -> Unit) {
    PantallaPendiente(titulo = "Tienda de monedas", tarea = "M10.2") {
        Button(onClick = alCerrar) { Text("Cerrar") }
    }
}
