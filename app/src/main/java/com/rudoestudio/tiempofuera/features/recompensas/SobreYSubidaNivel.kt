package com.rudoestudio.tiempofuera.features.recompensas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.color
import com.rudoestudio.tiempofuera.core.designsystem.colorObscuro
import com.rudoestudio.tiempofuera.core.designsystem.colorRayos
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorNivelLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TarjetaLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoEnParalelogramo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.espiral
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoRecoleccion
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoSubidaNivel
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** En iOS: `SobrePersonajeView.FaseApertura`. */
private enum class FaseSobre { ESPERANDO, DESTELLO, REVELANDO, COMPLETADO }

private const val TOQUES_SOBRE = 3

/**
 * Abrir un sobre (M7.7): se toca tres veces (cada vez tiembla más), destella y sale la
 * carta del luchador girando, con un brillo. Si ya lo tenía, "Recolectar y subir de nivel".
 * En iOS: `SobrePersonajeView` + `SobrePersonajeViewModel`.
 */
@Composable
internal fun SobrePersonajePantalla(
    rareza: Rareza,
    viewModel: RecompensasViewModel,
    alFallar: () -> Unit,
    alRecolectar: (Luchador, ResultadoRecoleccion) -> Unit,
) {
    val haptics = LocalHaptics.current
    val alcance = rememberCoroutineScope()
    var toques by remember { mutableIntStateOf(0) }
    var sacudida by remember { mutableIntStateOf(0) }
    var fase by remember { mutableStateOf(FaseSobre.ESPERANDO) }
    var luchador by remember { mutableStateOf<Luchador?>(null) }
    var fallo by remember { mutableStateOf(false) }
    var errorRecoleccion by remember { mutableStateOf<String?>(null) }
    var recolectando by remember { mutableStateOf(false) }

    val flote = rememberInfiniteTransition(label = "sobre")
    val y by flote.animateFloat(0f, -14f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flote")
    val revelado = remember { Animatable(0f) }
    val destello = remember { Animatable(0f) }
    val visible = fase == FaseSobre.REVELANDO || fase == FaseSobre.COMPLETADO

    // La coreografía de apertura, en cuanto sale el luchador.
    LaunchedEffect(luchador) {
        if (luchador == null) return@LaunchedEffect
        haptics.sacudidaFuerte(0.25)
        sacudida += 2
        delay(250)
        haptics.impacto(EstiloImpacto.FUERTE)
        fase = FaseSobre.DESTELLO
        destello.animateTo(1f, tween(300))
        haptics.notificacion(TipoNotificacion.EXITO)
        fase = FaseSobre.REVELANDO
        launch { destello.animateTo(0f, tween(200)) }
        revelado.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 110f))
        delay(350)
        fase = FaseSobre.COMPLETADO
    }

    fun tocar() {
        if (luchador != null || fase != FaseSobre.ESPERANDO || fallo) return
        toques++
        haptics.vibrarTick(min(toques / TOQUES_SOBRE.toFloat(), 1f))
        sacudida = toques
        if (toques >= TOQUES_SOBRE) {
            alcance.launch {
                val sorteado = viewModel.sortear(rareza)
                if (sorteado == null) fallo = true else luchador = sorteado
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        FondoRareza(rareza) {
            EstrellaDecorativa(R.drawable.estrella_ifondo, 600.dp, rareza.colorObscuro)
            EstrellaDecorativa(R.drawable.estrella_i2_fondo, 1600.dp, rareza.colorObscuro)
        }
        if (fallo) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                TextoContorno("No se pudo abrir el sobre", pirata(32), ColoresTF.Blanco, Color.Black, 3.dp, alineacion = TextAlign.Center)
                BotonL(ColoresTF.Verde, "CONTINUAR", alTocar = alFallar)
            }
            return@Box
        }
        val l = luchador
        val duplicado = (l?.cantidad ?: 0) > 0
        Column(
            Modifier
                .fillMaxSize()
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = ::tocar)
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Titulo(
                if (fase == FaseSobre.COMPLETADO) "PERSONAJE" else "NUEVO SOBRE",
                when {
                    fase != FaseSobre.COMPLETADO -> "obtenido"
                    duplicado -> "repetido"
                    else -> "desbloqueado"
                },
            )
            Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                Destello(ColoresTF.Amarillo, Modifier.size(260.dp).graphicsLayer {
                    val e = 0.2f + 1.2f * destello.value
                    scaleX = e; scaleY = e; alpha = destello.value
                })
                if (l != null) {
                    TarjetaLuchador(
                        l,
                        Modifier
                            .graphicsLayer {
                                val r = revelado.value
                                cameraDistance = 12 * density
                                rotationY = 100f * (1 - r)
                                rotationX = 20f * (1 - r)
                                rotationZ = -12f * (1 - r)
                                val e = 0.01f + 0.99f * r
                                scaleX = e; scaleY = e
                                translationY = -18 * density * (1 - r)
                                alpha = if (visible) r.coerceIn(0f, 1f) else 0f
                            }
                            .brilloDeslizante(visible),
                    )
                }
                Image(
                    painterResource(AssetsIos.imagen(rareza.imagenSobre) ?: R.drawable.pack_comun), "Sobre ${rareza.nombreVisible}",
                    Modifier
                        .width(340.dp)
                        .offset { IntOffset(0, if (fase == FaseSobre.ESPERANDO) y.dp.roundToPx() else 0) }
                        .sacudidaAnsiosa(sacudida)
                        .graphicsLayer {
                            val abierto = fase != FaseSobre.ESPERANDO
                            val e = if (abierto) 2.2f else 1f
                            scaleX = e; scaleY = e
                            alpha = if (abierto) 0f else 1f
                        },
                )
            }
            if (fase == FaseSobre.ESPERANDO) {
                TextoEnParalelogramo(
                    "SOBRE ${rareza.nombreVisible.uppercase()}",
                    colorFondo = rareza.color,
                    modifier = Modifier.offset(65.dp, (-35).dp).rotate(-2f).graphicsLayer { scaleX = 1.2f; scaleY = 1.2f },
                )
                TextoContorno(
                    "TOCA ${TOQUES_SOBRE - toques} VECES LA PANTALLA PARA ABRIRLO",
                    arvo(16), ColoresTF.Blanco, Color.Black, 3.dp,
                    alineacion = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (fase == FaseSobre.COMPLETADO && l != null) {
                BannerError(errorRecoleccion)
                BotonL(
                    ColoresTF.Verde,
                    if (duplicado) "RECOLECTAR Y SUBIR DE NIVEL" else "RECOLECTAR",
                    habilitado = !recolectando,
                    alTocar = {
                        recolectando = true
                        alcance.launch {
                            viewModel.recolectar(l)
                                .onSuccess { alRecolectar(l, it) }
                                .onFailure { errorRecoleccion = "No se pudo guardar tu luchador. Intenta de nuevo." }
                            recolectando = false
                        }
                    },
                )
            }
        }
    }
}

/**
 * Subida de nivel o de rareza (M7.5), cuando el sobre trae un luchador repetido. Nivel: el
 * fondo se rompe como vidrio y la barra se llena con vibración. Rareza: entran dos franjas
 * y sale el nombre de la rareza nueva. En iOS: `SubidaNivelView`.
 */
@Composable
internal fun SubidaNivelPantalla(resultado: ResultadoSubidaNivel, luchador: Luchador?, viewModel: RecompensasViewModel, alContinuar: () -> Unit) {
    val haptics = LocalHaptics.current
    val rareza = resultado.rarezaNueva
    val imagen = luchador?.let { AssetsIos.imagen(it.imagen) ?: AssetsIos.imagen(it.nombre) } ?: R.drawable.chascarrin

    val entrada = remember { Animatable(0f) }
    val sacudida = remember { Animatable(1f) }
    val vidrio = remember { Animatable(0f) }
    val franjas = remember { Animatable(0f) }
    val puntos = remember { Animatable(resultado.puntosPoderAnterior.toFloat()) }
    val flote = rememberInfiniteTransition(label = "subida")
    val y by flote.animateFloat(0f, -10f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flote")

    LaunchedEffect(Unit) {
        if (resultado.subioDeRareza) {
            delay(150)
            launch { franjas.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 110f)) }
            delay(280)
            launch { entrada.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 50f)) }
            repeat(2) {
                sacudida.snapTo(0f)
                sacudida.animateTo(1f, tween(350, easing = LinearEasing))
                delay(150)
            }
        } else {
            delay(600)
            launch { vidrio.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            launch { entrada.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 50f)) }
            sacudida.snapTo(0f)
            sacudida.animateTo(1f, tween(500, easing = LinearEasing))
        }
    }
    // La barra se llena con vibraciones que suben (en iOS: `animarProgresoDeNivel`).
    LaunchedEffect(Unit) {
        if (resultado.puntosPoderNuevo <= resultado.puntosPoderAnterior) return@LaunchedEffect
        val duracion = ((resultado.nivelNuevo - resultado.nivelAnterior) * 550).coerceIn(900, 2200)
        launch { puntos.animateTo(resultado.puntosPoderNuevo.toFloat(), tween(duracion, easing = FastOutSlowInEasing)) }
        val pasos = (duracion / 90).coerceAtLeast(1)
        for (paso in 0 until pasos) {
            haptics.vibrarTick(min(0.35f + paso / pasos.toFloat() * 0.45f, 0.8f))
            delay(90)
        }
    }

    val amplitud = if (resultado.subioDeRareza) 8f else 14f
    val vueltas = if (resultado.subioDeRareza) 3f else 4f
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val a = sacudida.value
            translationX = amplitud * density * (1 - a) * sin(a * PI.toFloat() * vueltas * 2)
        },
    ) {
        if (resultado.subioDeRareza) {
            FondoRareza(rareza) {
                EstrellaDecorativa(R.drawable.estrella_i2_fondo, 720.dp, rareza.color, yFactor = 0.5f)
                Column(Modifier.matchParentSize()) {
                    Box(Modifier.fillMaxWidth().height(200.dp).background(rareza.color))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().height(200.dp).background(rareza.color))
                }
                Franjas(franjas.value, rareza)
            }
        } else {
            FondoRareza(rareza) { EstrellaDecorativa(R.drawable.estrella_i2_fondo, 1200.dp, rareza.colorObscuro) }
            VidrioRoto(vidrio.value, rareza.colorObscuro)
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Titulo(luchador?.nombre ?: "Tu luchador", if (resultado.subioDeRareza) "¡¡SUBE DE RAREZA!!" else "¡¡SUBE DE NIVEL!!")
            ImagenPixel(imagen, 260.dp, Modifier.offset(y = y.dp).espiral(entrada.value).graphicsLayer { alpha = entrada.value.coerceIn(0f, 1f) })
            if (luchador != null) {
                val p = puntos.value.toInt()
                IndicadorNivelLuchador(
                    viewModel.nivel(p), viewModel.progreso(p), rareza.color, rareza.colorObscuro,
                    muestraNombre = false, modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (resultado.subioDeRareza) {
                TextoEnParalelogramo(
                    rareza.nombreVisible, colorFondo = rareza.colorObscuro,
                    modifier = Modifier.offset(y = 20.dp).graphicsLayer { scaleX = 1.5f; scaleY = 1.5f },
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextoEnParalelogramo("Nivel ${resultado.nivelAnterior}", colorFondo = rareza.colorRayos, tamanoFuente = 28.sp)
                    Image(painterResource(R.drawable.derecha), null)
                    TextoEnParalelogramo("Nivel ${resultado.nivelNuevo}", colorFondo = rareza.colorObscuro, tamanoFuente = 28.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Verde, "CONTINUAR", alTocar = alContinuar, modifier = Modifier.padding(top = 20.dp))
        }
    }
}

/** Dos franjas enormes que entran desde las esquinas (subida de rareza). */
@Composable
private fun Franjas(avance: Float, rareza: Rareza) {
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val ancho = constraints.maxWidth.toFloat()
        val alto = constraints.maxHeight.toFloat()
        listOf(Triple(-0.2f, -1f, -40f), Triple(1.2f, 1f, 0f)).forEach { (yFactor, signo, anguloInicial) ->
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .requiredSize(800.dp)
                    .graphicsLayer {
                        translationY = alto * yFactor - size.height / 2 + (1 - avance) * signo * alto * 0.9f
                        translationX = (1 - avance) * signo * ancho * 0.5f
                        rotationZ = anguloInicial + (-20f - anguloInicial) * avance
                    }
                    .background(rareza.colorRayos),
            )
        }
    }
}

/**
 * El fondo se rompe desde un punto: 9 rebanadas, cada una en un pedazo interior y otro
 * exterior, que salen volando y se desvanecen. En iOS: `vidrioRoto` + `GlassShardShape`.
 */
@Composable
private fun VidrioRoto(avance: Float, color: Color) {
    val angulos = listOf(0.0, 34.0, 70.0, 108.0, 152.0, 190.0, 223.0, 265.0, 312.0)
    fun ruido(semilla: Double): Float {
        val x = sin(semilla * 12.9898) * 43758.5453
        return (x - floor(x)).toFloat()
    }
    Canvas(Modifier.fillMaxSize()) {
        val centro = Offset(size.width * 0.5f, size.height * 0.42f)
        val radioMax = hypot(size.width, size.height)
        angulos.forEachIndexed { i, inicio ->
            val fin = if (i == angulos.lastIndex) angulos[0] + 360 else angulos[i + 1]
            val medio = (inicio + fin) / 2 * PI / 180
            val dir = Offset(cos(medio).toFloat(), sin(medio).toFloat())
            // (radio interior, radio exterior, distancia de vuelo, giro, retraso)
            listOf(
                listOf(0f, 0.14f, 30f + ruido(i * 1.7) * 30f, (ruido(i * 3.3) - 0.5f) * 40f, ruido(i * 5.1) * 0.05f),
                listOf(0.14f, 1f, 70f + ruido(i * 2.3) * 90f, (ruido(i * 4.4) - 0.5f) * 70f, 0.05f + ruido(i * 6.6) * 0.12f),
            ).forEach { (adentro, afuera, vuelo, giro, retraso) ->
                val t = ((avance - retraso) / (1 - retraso)).coerceIn(0f, 1f)
                if (t >= 1f) return@forEach
                val r0 = radioMax * adentro
                val r1 = radioMax * afuera
                val a0 = inicio * PI / 180
                val a1 = fin * PI / 180
                val pedazo = Path().apply {
                    if (r0 < 0.001f) moveTo(centro.x, centro.y)
                    else moveTo(centro.x + r0 * cos(a0).toFloat(), centro.y + r0 * sin(a0).toFloat())
                    lineTo(centro.x + r1 * cos(a0).toFloat(), centro.y + r1 * sin(a0).toFloat())
                    lineTo(centro.x + r1 * cos(a1).toFloat(), centro.y + r1 * sin(a1).toFloat())
                    if (r0 >= 0.001f) lineTo(centro.x + r0 * cos(a1).toFloat(), centro.y + r0 * sin(a1).toFloat())
                    close()
                }
                val pivote = Offset(centro.x + dir.x * radioMax * (adentro + afuera) / 4, centro.y + dir.y * radioMax * (adentro + afuera) / 4)
                translate(dir.x * vuelo * t * density, dir.y * vuelo * t * density) {
                    rotate(giro * t, pivote) { drawPath(pedazo, color, alpha = 1 - t) }
                }
            }
        }
    }
}
