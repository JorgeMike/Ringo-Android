package com.rudoestudio.tiempofuera.features.recompensas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.color
import com.rudoestudio.tiempofuera.core.designsystem.colorObscuro
import com.rudoestudio.tiempofuera.core.designsystem.colorRayos
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import kotlinx.coroutines.delay
import kotlin.math.max

// Efectos de las pantallas de recompensa (M7.1). En iOS: Features/Recompensas/Components.

internal fun arvo(tamano: Int) = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = tamano.sp)

internal fun pirata(tamano: Int) = TextStyle(fontFamily = PirataOne, fontSize = tamano.sp)

/**
 * Fondo del color de la rareza con rayos girando y un velo oscuro; [decoracion] va encima
 * (estrellas). En iOS: `FondoRareza`.
 */
@Composable
internal fun FondoRareza(rareza: Rareza, decoracion: @Composable BoxScope.() -> Unit = {}) {
    val giro = rememberInfiniteTransition(label = "rayos")
    val angulo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "angulo")
    Box(Modifier.fillMaxSize().background(rareza.color).clipToBounds(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(rareza.colorRayos),
            modifier = Modifier.requiredSize(Dp(2200f)).rotate(angulo),
        )
        Box(Modifier.fillMaxSize().background(rareza.colorObscuro.copy(alpha = 0.5f)))
        decoracion()
    }
}

/** Estrella teñida, centrada a lo ancho y a [yFactor] del alto. En iOS: `EstrellaDecorativa`. */
@Composable
internal fun BoxScope.EstrellaDecorativa(imagen: Int, tamano: Dp, color: Color, yFactor: Float = 0.45f) {
    Image(
        painterResource(imagen), null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color),
        modifier = Modifier.matchParentSize().layout { medible, restricciones ->
            val lado = tamano.roundToPx()
            val p = medible.measure(Constraints.fixed(lado, lado))
            layout(restricciones.maxWidth, restricciones.maxHeight) {
                p.place((restricciones.maxWidth - lado) / 2, (restricciones.maxHeight * yFactor - lado / 2f).toInt())
            }
        },
    )
}

/** Círculo de luz que se suma al fondo. En iOS: `DestelloView`. */
@Composable
internal fun Destello(color: Color, modifier: Modifier) {
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawCircle(
                    Brush.radialGradient(0f to color.copy(alpha = 0.95f), 0.5f to color.copy(alpha = 0.35f), 1f to Color.Transparent),
                    blendMode = BlendMode.Plus,
                )
            },
    )
}

/**
 * Un brillo blanco que cruza de lado a lado cuando [activar] pasa a `true`, recortado a la
 * forma del contenido. En iOS: `BrilloDeslizanteModifier`.
 */
internal fun Modifier.brilloDeslizante(activar: Boolean): Modifier = composed {
    val avance = remember { Animatable(-1.5f) }
    LaunchedEffect(activar) {
        if (!activar) return@LaunchedEffect
        avance.snapTo(-1.5f)
        delay(100)
        avance.animateTo(1.5f, tween(750, easing = FastOutSlowInEasing))
    }
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val ancho = size.width * 0.45f
        val x = avance.value * size.width + size.width / 2
        rotate(18f, Offset(x, size.height / 2)) {
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.6f), 1f to Color.Transparent,
                    startX = x - ancho / 2, endX = x + ancho / 2,
                ),
                topLeft = Offset(x - ancho / 2, -size.height),
                size = Size(ancho, size.height * 3),
                blendMode = BlendMode.SrcAtop,
            )
        }
    }
}

/**
 * Sacudida que crece con cada [nivel] (cada toque al sobre): más grados y más rebotes. En
 * iOS: `SacudidaAnsiosaModifier`.
 */
internal fun Modifier.sacudidaAnsiosa(nivel: Int): Modifier = composed {
    val angulo = remember { Animatable(0f) }
    val escala = remember { Animatable(1f) }
    LaunchedEffect(nivel) {
        if (nivel <= 0) return@LaunchedEffect
        val amplitud = 4f * nivel
        val pasos = 3 + nivel
        val duracionPaso = (max(0.045, 0.095 - nivel * 0.012) * 1000).toLong()
        escala.animateTo(1f + 0.018f * nivel, tween(120))
        for (i in 0 until pasos) {
            val signo = if (i % 2 == 0) 1f else -1f
            angulo.animateTo(signo * amplitud * (pasos - i) / pasos, spring(dampingRatio = 0.3f, stiffness = 320f))
            delay(duracionPaso)
        }
        angulo.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 320f))
        escala.animateTo(1f, tween(150))
    }
    graphicsLayer {
        rotationZ = angulo.value
        scaleX = escala.value
        scaleY = escala.value
    }
}

/** Aparece "inflándose" con rebote. En iOS: `.pop(_:)`. */
internal fun Modifier.pop(visible: Boolean, rebote: Float = 0.55f): Modifier = composed {
    val v by animateFloatAsState(if (visible) 1f else 0f, spring(dampingRatio = rebote, stiffness = Spring.StiffnessMediumLow), label = "pop")
    graphicsLayer {
        val e = v.coerceAtLeast(0.001f)
        scaleX = e
        scaleY = e
        alpha = v.coerceIn(0f, 1f)
    }
}

/** Entrada suave para textos: aparece, sube un poco y crece apenas. En iOS: `.aparece(_:)`. */
internal fun Modifier.aparece(visible: Boolean): Modifier = composed {
    val v by animateFloatAsState(if (visible) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "aparece")
    graphicsLayer {
        alpha = v
        translationY = (1 - v) * 18 * density
        val e = 0.92f + 0.08f * v
        scaleX = e
        scaleY = e
    }
}
