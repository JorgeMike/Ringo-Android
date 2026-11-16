package com.rudoestudio.tiempofuera.features.recompensas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.ligas.RecompensaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

private const val GOLPES_PINATA = 5
private val MensajesGolpe = listOf("¡Rómpela!", "¡Así se hace!", "¡Más fuerte!", "¡Ya casi!")
private val ColoresConfeti = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFEC407A), ColoresTF.Amarillo,
)

private class Confeti(val color: Color, val tamano: Float, val giro: Float, val dx: Float, val dy: Float, val deltaGiro: Float)

/**
 * La piñata (M7.3): varios sobres vienen dentro; cinco golpes y explota en confeti. Cada
 * golpe la voltea, sacude la pantalla y la deja temblando más fuerte. En iOS: `PinataView`
 * + `PinataViewModel`.
 */
@Composable
internal fun PinataPantalla(rarezas: List<Rareza>, alAbrir: () -> Unit) {
    val haptics = LocalHaptics.current
    val alcance = rememberCoroutineScope()
    var golpes by remember { mutableIntStateOf(0) }
    val explotando = golpes >= GOLPES_PINATA
    val giroY = remember { Animatable(0f) }
    val giroZ = remember { Animatable(0f) }
    val empujon = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val sacudida = remember { Animatable(0f) }
    val destello = remember { Animatable(0f) }
    val explosion = remember { Animatable(1f) }
    val onda = remember { Animatable(0f) }
    val vuelo = remember { Animatable(0f) }
    val confeti = remember {
        List(60) {
            Confeti(
                ColoresConfeti.random(), Random.nextFloat() * 12 + 8, Random.nextFloat() * 360,
                Random.nextFloat() * 400 - 200, Random.nextFloat() * 540 - 120, Random.nextFloat() * 1080 - 540,
            )
        }
    }

    // Tiembla sin parar, más rápido y más fuerte con cada golpe.
    val nivel = golpes.coerceAtMost(3)
    val temblor = rememberInfiniteTransition(label = "temblor")
    val fase by temblor.animateFloat(-1f, 1f, infiniteRepeatable(tween(listOf(1, 80, 50, 30)[nivel], easing = LinearEasing), RepeatMode.Reverse), label = "fase")
    val amplitud = if (explotando) 0f else listOf(0f, 3f, 7f, 14f)[nivel]

    fun golpear() {
        if (explotando) return
        golpes++
        val n = golpes
        if (n >= GOLPES_PINATA) {
            haptics.sacudidaFuerte(0.4)
            alcance.launch {
                coroutineScope {
                    launch { destello.snapTo(0.85f); destello.animateTo(0f, tween(400)) }
                    launch { for (x in listOf(22f, -22f, 18f, -14f, 0f)) sacudida.animateTo(x, tween(40)) }
                    launch { onda.snapTo(0f); onda.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
                    launch { explosion.animateTo(1.35f, tween(120)); explosion.animateTo(0f, tween(180)) }
                    launch { giroY.animateTo(360f, tween(300)) }
                    launch { delay(80); vuelo.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
                    launch { delay(880); alAbrir() }
                }
            }
            return
        }
        haptics.impacto(EstiloImpacto.FUERTE)
        val lado = if (n % 2 == 0) 1f else -1f
        alcance.launch {
            launch { destello.snapTo(0.4f); destello.animateTo(0f, tween(240)) }
            launch {
                val intensidad = n * 5f + 8f
                for (x in listOf(intensidad, -intensidad, intensidad / 2, 0f)) sacudida.animateTo(x, tween(40))
            }
            launch {
                giroY.animateTo(lado * 60, tween(70))
                giroY.animateTo(-lado * 25, spring(dampingRatio = 0.25f, stiffness = 180f))
                giroY.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 200f))
            }
            launch {
                giroZ.animateTo(lado * 14, tween(70))
                giroZ.animateTo(-lado * 6, spring(dampingRatio = 0.25f, stiffness = 180f))
                giroZ.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 200f))
            }
            launch {
                empujon.animateTo(Offset(lado * 18, -10f), tween(70))
                empujon.animateTo(Offset(-lado * 8, 4f), spring(dampingRatio = 0.25f, stiffness = 180f))
                empujon.animateTo(Offset.Zero, spring(dampingRatio = 0.35f, stiffness = 200f))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(ColoresTF.Verde).clipToBounds()) {
        val celeste = ColorFilter.tint(ColoresTF.Celeste)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.estrella_fondo), null, colorFilter = celeste, modifier = Modifier.wrapContentSize(unbounded = true))
            Image(painterResource(R.drawable.estrella2_fondo), null, colorFilter = celeste, modifier = Modifier.wrapContentSize(unbounded = true).offset(40.dp, (-50).dp))
            Image(painterResource(R.drawable.estrella3_fondo), null, colorFilter = celeste, modifier = Modifier.wrapContentSize(unbounded = true).offset(30.dp, (-60).dp))
        }
        Box(Modifier.fillMaxSize().alpha(destello.value).background(Color.White))
        if (explotando) {
            Canvas(Modifier.fillMaxSize()) {
                val origen = Offset(size.width / 2, size.height / 2 - 60.dp.toPx())
                val v = vuelo.value
                confeti.forEach { c ->
                    val centro = origen + Offset(c.dx.dp.toPx() * v, c.dy.dp.toPx() * v)
                    val ancho = c.tamano.dp.toPx()
                    rotate(c.giro + c.deltaGiro * v, centro) {
                        drawRoundRect(
                            c.color.copy(alpha = 1 - v), centro - Offset(ancho / 2, ancho / 4), Size(ancho, ancho / 2), CornerRadius(2.dp.toPx()),
                        )
                    }
                }
                if (onda.value in 0.001f..0.999f) {
                    drawCircle(Color.White.copy(alpha = 0.6f * (1 - onda.value)), 40.dp.toPx() * (0.1f + onda.value * 4.9f), center, style = Stroke(3.dp.toPx()))
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .offset { IntOffset(sacudida.value.dp.roundToPx(), 0) }
                .rotate(sacudida.value / 5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Titulo("DALE DURO", "¡a la piñata!")
            TextoContorno(
                "Trae ${rarezas.size} ${if (rarezas.size == 1) "sobre" else "sobres"} adentro",
                arvo(16), ColoresTF.Blanco, Color.Black, 2.dp,
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.height(300.dp), contentAlignment = Alignment.Center) {
                ImagenPixel(
                    R.drawable.pinata, 240.dp,
                    Modifier
                        .offset { IntOffset((empujon.value.x + fase * amplitud).dp.roundToPx(), empujon.value.y.dp.roundToPx()) }
                        .graphicsLayer {
                            val e = (1 + golpes.coerceAtMost(GOLPES_PINATA - 1) * 0.07f) * explosion.value
                            scaleX = e
                            scaleY = e
                            rotationY = giroY.value
                            rotationZ = giroZ.value
                            cameraDistance = 6 * density
                        }
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "Golpear la piñata") { golpear() },
                    descripcion = "Piñata",
                )
            }
            Spacer(Modifier.weight(1f))
            val mensaje = when {
                explotando -> "¡BOOM!"
                golpes == 0 -> "¡Toca la piñata para romperla!"
                else -> MensajesGolpe[(golpes - 1).coerceAtMost(MensajesGolpe.lastIndex)]
            }
            val colorMensaje = listOf(ColoresTF.Blanco, ColoresTF.Amarillo, ColoresTF.Naranja, ColoresTF.Rojo)[nivel]
            TextoContorno(
                mensaje, pirata(28), colorMensaje, Color.Black, 4.dp,
                alineacion = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        val e = 1 + golpes.coerceAtMost(4) * 0.12f
                        scaleX = e
                        scaleY = e
                    },
            )
            TextoContorno(
                "¡Pégale con ganas!", arvo(18), ColoresTF.Blanco.copy(alpha = 0.7f), Color.Transparent, 0.dp,
                modifier = Modifier.padding(top = 18.dp).alpha(if (golpes > 0 && !explotando) 1f else 0f),
            )
            Spacer(Modifier.weight(1f))
        }
    }
    LaunchedEffect(Unit) { haptics.notificacion(TipoNotificacion.EXITO) }
}

/**
 * "¡Lucha ganada!" (M7.6): tu luchador festeja, el rival quedó tendido y abajo lo que trae
 * la barrera. En iOS, `LuchaGanadaView` todavía es un borrador (texto y botón); este diseño
 * es nuestro, con el mismo guion que las otras celebraciones.
 */
@Composable
internal fun LuchaGanadaPantalla(recompensa: RecompensaNivel, luchador: Luchador, rival: Luchador?, alSonar: () -> Unit, alContinuar: () -> Unit) {
    LaunchedEffect(Unit) { alSonar() }
    // título, rival, luchador, recompensa, botón
    val v = rememberGuion(listOf(0, 250, 250, 450, 300))
    val rayos = rememberInfiniteTransition(label = "rayos")
    val giro by rayos.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "giro")
    Box(Modifier.fillMaxSize().background(ColoresTF.Rojo).clipToBounds()) {
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(ColoresTF.Amarillo),
            modifier = Modifier.align(Alignment.Center).requiredSize(2200.dp).rotate(giro).alpha(0.22f),
        )
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.aparece(v > 0)) {
                TextoConSombra("¡Lucha ganada!", pirata(46), colorSombra = Color.Black, grosor = 2.dp)
                TextoContorno(
                    rival?.let { "${it.nombre} besó la lona" } ?: "Le diste una paliza",
                    arvo(18), ColoresTF.Blanco, Color.Black, 2.dp,
                )
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                rival?.let {
                    // Tendido en la lona, a un lado.
                    ImagenPixel(
                        AssetsIos.imagen(it.imagen) ?: AssetsIos.imagen(it.nombre) ?: R.drawable.ringo, 110.dp,
                        Modifier.offset(x = 90.dp, y = (-4).dp).rotate(-90f).alpha(0.85f).pop(v > 1),
                    )
                }
                ImagenPixel(
                    AssetsIos.imagen(luchador.imagen) ?: AssetsIos.imagen(luchador.nombre) ?: R.drawable.ringo, 190.dp,
                    Modifier.offset(x = (-30).dp).espiralAlAparecer(v > 2),
                )
            }
            Box(Modifier.pop(v > 3, rebote = 0.7f), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.recompensa_lista), null, Modifier.matchParentSize().offset(8.dp, 20.dp))
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recompensa.puntosPoder.takeIf { it > 0 }?.let { FilaRecompensa("Puntos de poder:", R.drawable.puntos_poder_icon, "$it") }
                    recompensa.monedas.takeIf { it > 0 }?.let { FilaRecompensa("Monedas:", R.drawable.moneda_icon, "$it") }
                    if (recompensa.protectorRacha) FilaRecompensa("Protector de racha:", R.drawable.racha_icon, "1")
                    if (recompensa.sobres.isNotEmpty()) SobresGanados(recompensa.sobres)
                }
            }
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "¡A COBRAR!", alTocar = alContinuar, habilitado = v > 4, modifier = Modifier.pop(v > 4).ocultoAlCompartir())
        }
    }
}

@Composable
private fun SobresGanados(sobres: List<Rareza>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextoContorno(if (sobres.size == 1) "Sobre:" else "Sobres:", arvo(16), ColoresTF.Blanco, Color.Black, 2.dp)
        Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
            sobres.sorted().forEachIndexed { i, r ->
                Image(
                    painterResource(AssetsIos.imagen(r.imagenSobre) ?: R.drawable.pack_comun), "Sobre ${r.nombreVisible}",
                    Modifier.size(44.dp).rotate(if (i % 2 == 0) -8f else 8f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PinataPreview() = TiempoFueraTheme { PinataPantalla(listOf(Rareza.COMUN, Rareza.RARO, Rareza.EPICO)) {} }

@Preview
@Composable
private fun LuchaGanadaPreview() = TiempoFueraTheme {
    LuchaGanadaPantalla(
        RecompensaNivel(puntosPoder = 120, monedas = 50, sobres = listOf(Rareza.RARO, Rareza.EPICO)),
        DatosPreview.luchadorSeleccionado, DatosPreview.luchadores[3], {}, {},
    )
}
