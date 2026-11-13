package com.rudoestudio.tiempofuera.features.tienda

import androidx.compose.runtime.Composable
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente

/** Tab Tienda: la "Tiendita" de iOS (M10.1). */
@Composable
fun TiendaScreen() {
    PantallaPendiente(titulo = "Tiendita", tarea = "M10.1", fondo = ColoresTF.NaranjaObscuro)
}
