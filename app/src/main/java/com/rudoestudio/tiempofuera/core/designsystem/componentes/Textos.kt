package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF

/**
 * Texto con contorno. En iOS: `StrokedText`, que lo simula con 16 copias desplazadas;
 * aquí se dibuja un trazo real (`drawStyle = Stroke`). [grosor] es cuánto sobresale el
 * contorno de la letra, igual que `strokeWidth` en iOS.
 */
@Composable
fun TextoContorno(
    texto: String,
    estilo: TextStyle,
    relleno: Color,
    contorno: Color,
    grosor: Dp,
    modifier: Modifier = Modifier,
    alineacion: TextAlign? = null,
    maxLineas: Int = Int.MAX_VALUE,
) {
    val trazo = with(LocalDensity.current) { grosor.toPx() } * 2 // el trazo va mitad dentro, mitad fuera
    val desborde = if (maxLineas == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis
    Box(modifier) {
        Text(
            texto,
            style = estilo.copy(drawStyle = Stroke(width = trazo, join = StrokeJoin.Round)),
            color = contorno,
            textAlign = alineacion,
            maxLines = maxLineas,
            overflow = desborde,
        )
        Text(texto, style = estilo, color = relleno, textAlign = alineacion, maxLines = maxLineas, overflow = desborde)
    }
}

/**
 * Texto con sombra desplazada y un contorno del color de la sombra. En iOS:
 * `ShadowedText` (su `strokeColor` no se usa: el contorno sale del color de la sombra).
 */
@Composable
fun TextoConSombra(
    texto: String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = ColoresTF.Blanco,
    colorSombra: Color = Color.Black.copy(alpha = 0.6f),
    desplazamientoSombra: DpOffset = DpOffset((-4).dp, 4.dp),
    grosor: Dp = 1.dp,
    alineacion: TextAlign? = null,
) {
    Box(modifier) {
        TextoContorno(
            texto, estilo, relleno = colorSombra, contorno = colorSombra, grosor = grosor,
            modifier = Modifier.offset(desplazamientoSombra.x, desplazamientoSombra.y),
            alineacion = alineacion,
        )
        TextoContorno(texto, estilo, relleno = color, contorno = colorSombra, grosor = grosor, alineacion = alineacion)
    }
}
