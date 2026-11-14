package com.rudoestudio.tiempofuera.features.onboarding.pasos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min

// Pasos 13 a 15 del onboarding: firmar, recompensa y primer hábito.

private const val DIAMETRO_BASE = 150f

/**
 * Paso 13: mantener presionada la pantalla para firmar. Un círculo verde crece a jalones
 * (avanza y retrocede un poco, con vibración) hasta llenar la pantalla; al soltar antes,
 * regresa. En iOS: `FirmaView`.
 */
@Composable
fun FirmaPaso(alContinuar: () -> Unit) {
    val haptics = LocalHaptics.current
    val alcance = rememberCoroutineScope()
    val progreso = remember { Animatable(0f) }
    var completada by remember { mutableStateOf(false) }
    var pulso by remember { mutableStateOf<Job?>(null) }
    val flote = rememberInfiniteTransition(label = "firma")
    val y by flote.animateFloat(12f, -12f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flote")

    fun iniciarPulso() {
        pulso?.cancel()
        pulso = alcance.launch {
            while (isActive && progreso.targetValue < 1f) {
                haptics.vibrarTick(min(0.4f + progreso.value, 1f))
                launch { progreso.animateTo(min(progreso.value + 0.09f, 1f), spring(dampingRatio = 0.47f, stiffness = 220f)) }
                delay(90)
                if (progreso.targetValue >= 1f) break
                haptics.vibrarRespiracion(0.3f)
                launch { progreso.animateTo((progreso.targetValue - 0.03f).coerceAtLeast(0f), spring(dampingRatio = 0.62f, stiffness = 260f)) }
                delay(90)
            }
            if (!isActive || progreso.targetValue < 1f) return@launch
            completada = true
            haptics.impacto(EstiloImpacto.FUERTE)
            haptics.notificacion(TipoNotificacion.EXITO)
            delay(400)
            alContinuar()
        }
    }

    fun soltar() {
        if (completada) return
        pulso?.cancel()
        pulso = null
        alcance.launch { progreso.animateTo(0f, tween(350, easing = FastOutSlowInEasing)) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.VerdeObscuro)
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!completada) iniciarPulso()
                    waitForUpOrCancellation()
                    soltar()
                }
            },
    ) {
        val diametroMax = hypot(maxWidth.value, maxHeight.value) * 1.15f
        val diametro = DIAMETRO_BASE + progreso.value * (diametroMax - DIAMETRO_BASE)
        val recorteCirculo = Modifier.drawWithContent {
            val radio = diametro / 2 * density
            val circulo = Path().apply { addOval(Rect(Offset(size.width / 2, size.height / 2), radio)) }
            clipPath(circulo) { this@drawWithContent.drawContent() }
        }
        val verde = ColorFilter.tint(ColoresTF.Verde)

        // Las estrellas miden más que la pantalla y se desbordan a propósito, como en iOS:
        // sin `unbounded`, la caja las encogería al tamaño de la pantalla.
        Box(Modifier.fillMaxSize().then(recorteCirculo), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.estrella_fondo), null, colorFilter = verde, modifier = Modifier.wrapContentSize(unbounded = true))
            Image(painterResource(R.drawable.estrella2_fondo), null, colorFilter = verde, modifier = Modifier.wrapContentSize(unbounded = true).offset(40.dp, (-50).dp))
            Image(painterResource(R.drawable.estrella3_fondo), null, colorFilter = verde, modifier = Modifier.wrapContentSize(unbounded = true).offset(30.dp, (-60).dp))
            ImagenPixel(R.drawable.i_5_kz, 300.dp, Modifier.offset(y = y.dp))
        }
        ImagenPixel(R.drawable.i_5_kz_hover, 300.dp, Modifier.align(Alignment.Center).offset(y = y.dp))

        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 120.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.fondo_titulos2), null)
                Text("¡Todo listo!", style = pirata(46), color = ColoresTF.Blanco, modifier = Modifier.offset(y = (-5).dp))
            }
            Spacer(Modifier.weight(1f))
            TextoContorno(
                if (completada) "¡Compromiso firmado!" else "Mantén presionada la pantalla para firmar tu compromiso",
                arvo(26), ColoresTF.Blanco, ColoresTF.Azul, 2.dp,
                alineacion = TextAlign.Center,
                modifier = Modifier.padding(bottom = 120.dp),
            )
        }
    }
}

/**
 * Paso 14: "Aquí tienes tu primer sobre". En iOS: `RecompensaOnboardingView`, que abre un
 * sobre común con `.recompensaFlow`. M7: ese flujo no existe todavía en Android, así que
 * por ahora el botón solo avanza (el usuario no recibe su primer luchador).
 */
@Composable
fun RecompensaPaso(alContinuar: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(ColoresTF.Celeste).clipToBounds()) {
        Image(
            painterResource(R.drawable.fondo_estrella), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(maxWidth).height(maxHeight * 1.4f).offset(y = (-220).dp),
        )
        Column(Modifier.fillMaxSize().padding(top = 56.dp, bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Titulo("¡Felicidades!", "terminaste")
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(top = 15.dp), contentAlignment = Alignment.Center) {
                ImagenPixel(R.drawable.flay_fernandez, 160.dp, Modifier.offset(x = (-110).dp).rotate(-15f))
                ImagenPixel(R.drawable.tito_rito, 160.dp, Modifier.offset(x = 110.dp).rotate(15f).graphicsLayer { scaleX = -1f })
                ImagenPixel(R.drawable.la_viuda, 180.dp)
            }
            Spacer(Modifier.weight(1f))
            TextoContorno(
                "Aquí tienes tu primer sobre.", arvo(24), ColoresTF.Blanco, ColoresTF.Azul, 2.dp,
                alineacion = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            BotonL(ColoresTF.Amarillo, "ABRIR SOBRE", alTocar = alContinuar, modifier = Modifier.padding(top = 20.dp))
        }
    }
}

/** Paso 15. En iOS también es provisional (`HabitoInicialView`: un texto y "Continuar"). */
@Composable
fun HabitoInicialPaso(alContinuar: () -> Unit) {
    PantallaPendiente(titulo = "Tu primer hábito", tarea = "M4.16", fondo = ColoresTF.Celeste) {
        BotonL(ColoresTF.Amarillo, "¡AL RING!", alTocar = alContinuar)
    }
}

@Preview(heightDp = 860)
@Composable
private fun RecompensaPreview() = TiempoFueraTheme { RecompensaPaso {} }
