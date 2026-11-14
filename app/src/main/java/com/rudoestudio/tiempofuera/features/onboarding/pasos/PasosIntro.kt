package com.rudoestudio.tiempofuera.features.onboarding.pasos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoEnParalelogramo
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Pasos de presentación del onboarding (0 a 3 y 8): solo animación, texto y un botón.

/**
 * Paso 0. El alien cae "desde la cámara", sacude la pantalla y entran dos franjas. En iOS:
 * `BienvenidaView` + `BienvenidaEscena` (SpriteKit); aquí la escena va con `Animatable`.
 */
@Composable
fun BienvenidaPaso(alContinuar: () -> Unit, alYaTengoPerfil: () -> Unit) {
    val escalaLuchador = remember { Animatable(2.6f) }
    val opacidadLuchador = remember { Animatable(0f) }
    val sacudida = remember { Animatable(0f) }
    val floteBase = remember { Animatable(0f) }
    val floteHover = remember { Animatable(0f) }
    val franjaArriba = remember { Animatable(0f) }
    val franjaAbajo = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { opacidadLuchador.animateTo(1f, tween(60)) }
        escalaLuchador.animateTo(0.9f, tween(220, easing = FastOutSlowInEasing))
        escalaLuchador.animateTo(1.08f, tween(90))
        // Impacto: la "cámara" se sacude y entran las franjas mientras se asienta.
        launch {
            listOf(1f, -1.3f, 0.9f, -0.6f, 0.3f, 0f).forEach { sacudida.animateTo(it, tween(30)) }
        }
        launch { delay(10); franjaArriba.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        launch { delay(90); franjaAbajo.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        escalaLuchador.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
        val flote = infiniteRepeatable<Float>(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
        launch { floteBase.animateTo(1f, flote) }
        launch { floteHover.animateTo(1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(250))) }
    }

    Box(Modifier.fillMaxSize().background(ColoresTF.Celeste)) {
        Image(painterResource(R.drawable.patron_azul), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

        // La escena: franja de arriba < alien < franja de abajo < brillo del alien.
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    translationX = sacudida.value * 22.dp.toPx()
                    translationY = sacudida.value * 14.dp.toPx()
                },
            contentAlignment = Alignment.Center,
        ) {
            Franja(franjaArriba.value, desdeIzquierda = true, Modifier.offset(y = (-280).dp))
            AlienBienvenida(R.drawable.i_5_kz, escalaLuchador.value, opacidadLuchador.value, floteBase.value)
            Franja(franjaAbajo.value, desdeIzquierda = false, Modifier.offset(y = 380.dp))
            AlienBienvenida(R.drawable.i_5_kz_hover, escalaLuchador.value, opacidadLuchador.value, floteHover.value)
        }

        Column(
            Modifier.fillMaxSize().padding(bottom = 24.dp).padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextoConSombra("Bienvenido a", pirata(32), colorSombra = ColoresTF.Azul, modifier = Modifier.offset(y = 18.dp))
            Image(painterResource(R.drawable.logo), "Tiempo Fuera", modifier = Modifier.width(320.dp))
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "¡A DARLE!", alTocar = alContinuar)
            Text(
                "Ya tenía mi perfil",
                style = arvo(20),
                color = ColoresTF.Blanco,
                modifier = Modifier.padding(top = 10.dp).clickable(onClick = alYaTengoPerfil).padding(8.dp),
            )
        }
    }
}

@Composable
private fun Franja(progreso: Float, desdeIzquierda: Boolean, modifier: Modifier) {
    val distancia = with(LocalDensity.current) { 660.dp.toPx() }
    Box(
        modifier
            .requiredSize(660.dp, 420.dp)
            .graphicsLayer {
                translationX = (1f - progreso) * distancia * if (desdeIzquierda) -1 else 1
                val escala = 0.6f + 0.4f * progreso
                scaleX = escala
                scaleY = escala
                alpha = (progreso / 0.6f).coerceAtMost(1f)
                rotationZ = -20f
            }
            .background(ColoresTF.Celeste),
    )
}

@Composable
private fun AlienBienvenida(imagen: Int, escala: Float, opacidad: Float, flote: Float) {
    ImagenPixel(
        imagen,
        ancho = 360.dp,
        modifier = Modifier
            .offset(y = 30.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                alpha = opacidad
                translationY = -flote * 12.dp.toPx()
            },
    )
}

/** Paso 1: "5 horas al día", con el Ringo girando sobre las elipses. En iOS: `EstadisticaView`. */
@Composable
fun EstadisticaPaso(alContinuar: () -> Unit) {
    val giro = rememberInfiniteTransition(label = "estadistica")
    val anguloRingo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ringo")

    Box(Modifier.fillMaxSize().background(ColoresTF.Celeste).clipToBounds()) {
        Box(Modifier.align(Alignment.Center).offset((-80).dp, (-40).dp).wrapContentSize(unbounded = true), contentAlignment = Alignment.Center) {
            ImagenPixel(R.drawable.centro, 139.dp)
            ElipseGiratoria(R.drawable.elipse1, 226, 30_000, horario = true)
            ElipseGiratoria(R.drawable.elipse2, 326, 25_000, horario = false)
            ElipseGiratoria(R.drawable.elipse3, 409, 20_000, horario = true)
            ElipseGiratoria(R.drawable.elipse4, 499, 15_000, horario = false)
            ElipseGiratoria(R.drawable.elipse5, 600, 10_000, horario = true)
        }

        Column(
            Modifier.fillMaxSize().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TextoConSombra("En Latinoamérica", pirata(36), colorSombra = ColoresTF.Azul)
            TextoPaso("las personas usan el celular")
            Column(
                Modifier.fillMaxWidth().padding(top = 280.dp, end = 40.dp),
                horizontalAlignment = Alignment.End,
            ) {
                TextoPaso("un promedio de")
                TextoEnParalelogramo("5 horas", colorFondo = ColoresTF.Azul, tamanoFuente = 52.sp)
                TextoEnParalelogramo("al día", colorFondo = ColoresTF.Azul, tamanoFuente = 40.sp, modifier = Modifier.offset(4.dp, (-20).dp))
            }
            BotonL(ColoresTF.Amarillo, "SÁCATE A...", alTocar = alContinuar, modifier = Modifier.padding(top = 20.dp))
        }

        ImagenPixel(
            R.drawable.ringo_tonto,
            220.dp,
            Modifier.align(Alignment.Center).offset((-80).dp, (-40).dp).rotate(anguloRingo),
        )
    }
}

/** En iOS: `RotatingEllipse`. */
@Composable
private fun ElipseGiratoria(imagen: Int, tamano: Int, duracionMs: Int, horario: Boolean) {
    val giro = rememberInfiniteTransition(label = "elipse")
    val angulo by giro.animateFloat(
        0f, if (horario) 360f else -360f,
        infiniteRepeatable(tween(duracionMs, easing = LinearEasing)),
        label = "angulo",
    )
    ImagenPixel(imagen, tamano.dp, Modifier.rotate(angulo))
}

/** Paso 2: "+15 años de tu vida desperdiciados". En iOS: `ProblemaView`. */
@Composable
fun ProblemaPaso(alContinuar: () -> Unit) {
    val barras = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(100)
        barras.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxSize().background(ColoresTF.Celeste).clipToBounds()) {
        BarraImpacto((-220).dp, 27, barras.value, desdeIzquierda = true)
        BarraImpacto((-60).dp, 260, barras.value, desdeIzquierda = false)
        BarraImpacto(100.dp, 27, barras.value, desdeIzquierda = true)

        Column(Modifier.fillMaxSize().padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Eso significa", style = arvo(24), color = ColoresTF.Blanco, modifier = Modifier.fillMaxWidth())
                ImagenPixel(R.drawable.ringo_tonto, 280.dp)
                TextoEnParalelogramo("+15 años de tu vida", colorFondo = ColoresTF.Azul, tamanoFuente = 40.sp)
                Text("desperdiciados", style = arvo(24), color = ColoresTF.Blanco, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "YO NO QUIERO ESO", alTocar = alContinuar)
        }
    }
}

@Composable
private fun BoxScope.BarraImpacto(y: Dp, alto: Int, progreso: Float, desdeIzquierda: Boolean) {
    val oculto = with(LocalDensity.current) { 400.dp.toPx() }
    Box(
        Modifier
            .align(Alignment.Center)
            .offset { IntOffset(((1f - progreso) * oculto * if (desdeIzquierda) -1 else 1).toInt(), y.roundToPx()) }
            .fillMaxWidth()
            .height(alto.dp)
            .background(AzulMedio),
    )
}

/** Paso 3: "Nosotros te tiramos paro". En iOS: `AyudaView`. */
@Composable
fun AyudaPaso(alContinuar: () -> Unit) {
    val flote = rememberInfiniteTransition(label = "ayuda")
    val y by flote.animateFloat(0f, -14f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flote")
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp).padding(bottom = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center) {
                ImagenPixel(R.drawable.estrella_celeste, 380.dp)
                ImagenPixel(R.drawable.enchi_lalo, 280.dp, Modifier.offset(y = y.dp))
            }
            TextoPaso("¡Pero no te preocupes!", tamano = 24)
            TextoConSombra("Nosotros te tiramos paro", pirata(40), colorSombra = ColoresTF.Azul, alineacion = TextAlign.Center)
            TextoPaso("para recuperar tu tiempo", tamano = 24)
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "SÍ QUIERO AYUDA", alTocar = alContinuar)
        }
    }
}

/**
 * Paso 8: Ringo cae de golpe y todo se sacude, con vibración. En iOS: `TranquilizanteView`.
 */
@Composable
fun TranquilizantePaso(alContinuar: () -> Unit) {
    val haptics = LocalHaptics.current
    val caida = remember { Animatable(0f) } // 0 = arriba y grande, 1 = en su lugar
    val sacudida = remember { Animatable(0f) }
    val escalaSacudida = remember { Animatable(1f) }
    val flote = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptics.sacudidaFuerte(0.6)
        launch { caida.animateTo(1f, spring(dampingRatio = 0.43f, stiffness = 300f), initialVelocity = 2f) }
        delay(350)
        // Onda de choque: (desplazamiento, escala, vibración).
        val golpes = listOf(
            Triple(14f, 1.04f, EstiloImpacto.FUERTE),
            Triple(-10f, 0.97f, EstiloImpacto.FUERTE),
            Triple(7f, 1.02f, EstiloImpacto.MEDIO),
            Triple(-5f, 0.99f, EstiloImpacto.MEDIO),
            Triple(3f, 1.01f, EstiloImpacto.LIGERO),
            Triple(0f, 1f, null),
        )
        launch {
            golpes.forEach { (y, escala, estilo) ->
                estilo?.let(haptics::impacto)
                launch { escalaSacudida.animateTo(escala, tween(70)) }
                sacudida.animateTo(y, tween(70))
            }
        }
        delay(50)
        flote.animateTo(1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(ColoresTF.Celeste).clipToBounds()) {
        Image(
            painterResource(R.drawable.fondo_estrella), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(maxWidth).height(maxHeight * 1.4f).offset(y = (-220).dp),
        )
        Column(Modifier.fillMaxSize().padding(bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            TextoEnParalelogramo(
                "¡Ya mero, güerx!",
                colorFondo = ColoresTF.Azul,
                tamanoFuente = 40.sp,
                modifier = Modifier.rotate(-4f).offset { IntOffset(0, sacudida.value.dp.roundToPx()) }.scale(escalaSacudida.value),
            )
            Spacer(Modifier.weight(1f))
            ImagenPixel(
                R.drawable.ringo,
                280.dp,
                Modifier.graphicsLayer {
                    val c = caida.value
                    val escala = 2.5f - 1.5f * c
                    scaleX = escala
                    scaleY = escala
                    alpha = 1f
                    translationY = ((1f - c) * -600f + (flote.value * 2f - 1f) * -12f).dp.toPx()
                },
            )
            Spacer(Modifier.weight(1f))
            TextoPaso("No te me desesperes", Modifier.offset { IntOffset(0, sacudida.value.dp.roundToPx()) }.scale(escalaSacudida.value), tamano = 24)
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "Ta' bueno pues..", alTocar = alContinuar, modifier = Modifier.offset { IntOffset(0, (-sacudida.value).dp.roundToPx()) })
        }
    }
}

@Preview(heightDp = 860)
@Composable
private fun BienvenidaPreview() = TiempoFueraTheme { BienvenidaPaso({}, {}) }

@Preview(heightDp = 860)
@Composable
private fun EstadisticaPreview() = TiempoFueraTheme { EstadisticaPaso {} }

@Preview(heightDp = 860)
@Composable
private fun AyudaPreview() = TiempoFueraTheme { AyudaPaso {} }
