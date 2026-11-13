package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
