package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Entra girando en espiral desde lejos hasta su lugar. En iOS: `EspiralEffect`.
 * Animar [progreso] de 0 (lejos, diminuto) a 1 (en su lugar, tamaño normal).
 */
fun Modifier.espiral(progreso: Float, vueltas: Float = 1.5f, radio: Dp = 160.dp): Modifier = graphicsLayer {
    val restante = 1f - progreso
    val angulo = restante * vueltas * 2f * PI.toFloat()
    val r = radio.toPx() * restante
    translationX = cos(angulo) * r
    translationY = sin(angulo) * r
    rotationZ = Math.toDegrees(angulo.toDouble()).toFloat()
    val escala = max(progreso, 0.001f)
    scaleX = escala
    scaleY = escala
}

/**
 * Rayos de fondo que se desvanecen hacia abajo, detrás del luchador en Inicio y en la
 * colección. En iOS: `Image("Rayos")` con máscara de `LinearGradient`.
 */
@Composable
fun RayosDesvanecidos(modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(560.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, 0.66f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }
}
