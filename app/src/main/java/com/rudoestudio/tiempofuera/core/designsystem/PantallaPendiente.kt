package com.rudoestudio.tiempofuera.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Pantalla provisional mientras llega la tarea del tablero que la construye.
 * [tarea] es el ID del tablero (p. ej. "M5.1") para saber qué falta.
 */
@Composable
fun PantallaPendiente(
    titulo: String,
    tarea: String,
    modifier: Modifier = Modifier,
    fondo: Color = ColoresTF.Azul,
    acciones: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.displayMedium,
            color = ColoresTF.Amarillo,
            textAlign = TextAlign.Center,
        )
        Text(
            "Próximamente · $tarea",
            style = MaterialTheme.typography.titleSmall,
            color = ColoresTF.Blanco,
        )
        acciones()
    }
}

@Preview
@Composable
private fun PantallaPendientePreview() {
    TiempoFueraTheme { PantallaPendiente("Luchadores", "M8.1") }
}
