package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne

/** Barra de progreso en paralelogramo. En iOS: `BarraProgresoParalelogramo`. [progreso] de 0 a 1. */
@Composable
fun BarraProgresoParalelogramo(
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier,
    inclinacion: Dp = 10.dp,
) {
    val valor by animateFloatAsState(
        progreso.coerceIn(0f, 1f), tween(300, easing = FastOutSlowInEasing), label = "progreso",
    )
    Box(modifier.clip(ParalelogramoShape(inclinacion)).background(Color.Black.copy(alpha = 0.35f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(valor).background(color))
    }
}

/** Barra de tiempo con los segundos a la derecha. En iOS: `IndicadorTiempo`. */
@Composable
fun IndicadorTiempo(progreso: Float, segundos: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarraProgresoParalelogramo(progreso, color, Modifier.weight(1f).height(18.dp))
        Text(
            "${segundos}s",
            style = MaterialTheme.typography.titleSmall,
            color = ColoresTF.Blanco,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 34.dp),
        )
    }
}

private val FormaBadgeTemporizador = FormaPoligono(
    42f, 66f, puntos(39.988f, 6.22642f, 6.53892f, 0f, 0f, 66f, 42f, 58.7774f),
)

/** Un dígito del temporizador sobre su badge chueco. En iOS: `BadgeTemporizador`. */
@Composable
fun BadgeTemporizador(caracter: Char, modifier: Modifier = Modifier) {
    Box(
        modifier.size(42.dp, 66.dp).background(ColoresTF.AzulNoche, FormaBadgeTemporizador),
        contentAlignment = Alignment.Center,
    ) {
        Text(caracter.toString(), fontFamily = PirataOne, fontSize = 40.sp, color = ColoresTF.Blanco)
    }
}

/** "12:34" con cada número en su badge. En iOS: `TemporizadorBadges`. */
@Composable
fun TemporizadorBadges(texto: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-4).dp), verticalAlignment = Alignment.CenterVertically) {
        texto.forEach { c ->
            if (c.isDigit()) {
                BadgeTemporizador(c)
            } else {
                Text(
                    c.toString(), fontFamily = PirataOne, fontSize = 40.sp, color = ColoresTF.Blanco,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * Aviso de error que entra desde arriba. Con [mensaje] nulo se oculta (en iOS: `BannerError`).
 */
@Composable
fun BannerError(mensaje: String?, modifier: Modifier = Modifier) {
    // Conservar el último texto para que no desaparezca antes que la animación de salida.
    val ultimo = remember { arrayOfNulls<String>(1) }
    if (mensaje != null) ultimo[0] = mensaje
    val forma = RoundedCornerShape(14.dp)
    AnimatedVisibility(
        visible = mensaje != null,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            Modifier
                .shadow(4.dp, forma)
                .background(ColoresTF.RojoError, forma)
                .border(1.dp, ColoresTF.Blanco.copy(alpha = 0.25f), forma)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = ColoresTF.Blanco, modifier = Modifier.size(18.dp))
            Text(
                ultimo[0].orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = ColoresTF.Blanco,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
