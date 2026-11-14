package com.rudoestudio.tiempofuera.features.ligas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego.Fase
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego.ResultadoAtaque
import com.rudoestudio.tiempofuera.domain.ligas.NivelLiga
import com.rudoestudio.tiempofuera.domain.ligas.Punto
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

private val ArvoBold = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold)
private val FondoNoche = Color(0xFF060F1F)
private val RingArriba = Color(0xFF213D57)
private val RingAbajo = Color(0xFF091626)

/** Segundos del reloj de cuadros (el mismo para el motor y los toques). */
private fun ahora(): Double = System.nanoTime() / 1e9

private fun imagen(l: Luchador): Int = AssetsIos.imagen(l.imagen) ?: AssetsIos.imagen(l.nombre) ?: R.drawable.ringo

/**
 * La pelea de la barrera (M9.4): la vida de los dos arriba, el ring y abajo el joystick
 * con "golpear" y "cubrirse". Al ganar, el botón de golpe se vuelve "continuar"; al perder,
 * "reintentar". La lógica vive en [MotorMinijuego]; aquí solo se dibuja cada cuadro. En
 * iOS: `MinijuegoBarreraView`.
 */
@Composable
fun MinijuegoBarrera(
    nivel: NivelLiga,
    usuario: Luchador,
    rival: Luchador,
    alSonar: (MotorMinijuego.Sonido) -> Unit,
    alGanar: () -> Unit,
    alCerrar: () -> Unit,
) {
    val motor = remember(nivel.id) { MotorMinijuego(nivel, usuario, rival, ahora()) }
    // Cambia en cada cuadro: leerlo vuelve a dibujar todo con lo nuevo del motor. Los hijos
    // que leen el motor son @NonSkippableComposable porque el motor no es estado de Compose.
    var cuadro by remember { mutableLongStateOf(0L) }
    fun sonar() = motor.tomarSonidos().forEach(alSonar)
    LaunchedEffect(motor) {
        while (true) {
            cuadro = withFrameNanos { it }
            motor.actualizar(ahora())
            sonar()
        }
    }
    val haptics = LocalHaptics.current
    LaunchedEffect(motor.golpes) { if (motor.golpes > 0) haptics.notificacion(TipoNotificacion.EXITO) }
    LaunchedEffect(motor.contadorImpactoRecibido) { if (motor.contadorImpactoRecibido > 0) haptics.notificacion(TipoNotificacion.ERROR) }
    LaunchedEffect(motor.contadorAnimacionGolpe) { if (motor.contadorAnimacionGolpe > 0) haptics.impacto(EstiloImpacto.FUERTE) }
    BackHandler(onBack = alCerrar)

    if (cuadro < 0) return
    Box(
        Modifier
            .fillMaxSize()
            .background(FondoNoche)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        FondoPelea()
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp)) {
            val joystick = (maxHeight * 0.23f).coerceIn(104.dp, 142.dp)
            val ring = minOf(maxWidth, maxHeight - joystick - 170.dp).coerceIn(150.dp, 560.dp)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonCerrarPelea(alCerrar)
                    Vidas(motor, Modifier.padding(start = 10.dp).weight(1f))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Ring(motor, ring) }
                Controles(
                    motor, joystick,
                    alGolpear = {
                        when (motor.fase) {
                            Fase.VICTORIA -> alGanar()
                            Fase.DERROTA -> motor.reiniciar(ahora())
                            else -> motor.atacar(ahora())
                        }
                        sonar()
                    },
                )
            }
        }
    }
}

/** El fondo de ligas casi invisible y dos manchas de color. */
@Composable
private fun FondoPelea() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.fondo_ligas), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.045f),
        )
        Canvas(Modifier.fillMaxSize()) {
            fun mancha(color: Color, x: Float, y: Float, radio: Float) =
                drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), Offset(x, y), radio), radio, Offset(x, y))
            mancha(ColoresTF.Rojo.copy(alpha = 0.16f), size.width * 0.9f, size.height * 0.25f, 200.dp.toPx())
            mancha(ColoresTF.Celeste.copy(alpha = 0.2f), size.width * 0.08f, size.height * 0.78f, 190.dp.toPx())
        }
    }
}

@Composable
private fun BotonCerrarPelea(alCerrar: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ColoresTF.Azul.copy(alpha = 0.82f))
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Salir de la pelea", onClick = alCerrar),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", style = ArvoBold.copy(fontSize = 18.sp), color = Color.White)
    }
}

@Composable
@NonSkippableComposable
private fun Vidas(motor: MotorMinijuego, modifier: Modifier) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier
            .background(ColoresTF.Azul.copy(alpha = 0.82f), forma)
            .border(1.5.dp, Color.White.copy(alpha = 0.2f), forma)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BarraVida(motor.rival, motor.vidaRival, motor.vidaMaximaRival, motor.progresoVidaRival, ColoresTF.Rojo, invertir = false)
        BarraVida(motor.usuario, motor.vidasJugador, motor.vidasMaximasJugador, motor.progresoVidaJugador, ColoresTF.Celeste, invertir = true)
    }
}

@Composable
private fun BarraVida(luchador: Luchador, vida: Int, maxima: Int, progreso: Float, color: Color, invertir: Boolean) {
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "${luchador.nombre}: vida $vida de $maxima" },
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!invertir) ImagenPixel(imagen(luchador), 30.dp)
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f)),
            contentAlignment = if (invertir) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progreso)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(if (invertir) listOf(color, color.copy(alpha = 0.78f)) else listOf(color.copy(alpha = 0.78f), color))),
            )
        }
        Text("$vida/$maxima", style = ArvoBold.copy(fontSize = 11.sp), color = Color.White, modifier = Modifier.widthIn(min = 32.dp))
        if (invertir) ImagenPixel(imagen(luchador), 30.dp)
    }
}

/** El cuadrilátero visto desde arriba, con los dos luchadores y los efectos de cada golpe. */
@Composable
@NonSkippableComposable
private fun Ring(motor: MotorMinijuego, lado: Dp) {
    val forma = RoundedCornerShape(22.dp)
    val densidad = LocalDensity.current
    val ladoPx = with(densidad) { lado.toPx() }
    val t = ahora()
    val reciente = t - motor.momentoResultado < 0.45
    fun Modifier.en(p: Punto, ancho: Dp) = offset {
        val medio = ancho.toPx() / 2
        IntOffset((p.x * ladoPx - medio).roundToInt(), (p.y * ladoPx - medio).roundToInt())
    }

    Box(
        Modifier
            .size(lado)
            .clip(forma)
            .background(Brush.verticalGradient(listOf(RingArriba, RingAbajo)))
            .border(2.dp, Color.White.copy(alpha = 0.35f), forma)
            .drawBehind {
                drawOval(
                    Brush.radialGradient(listOf(ColoresTF.Celeste.copy(alpha = 0.18f), Color.Transparent)),
                    Offset(size.width * 0.14f, size.height * 0.555f), Size(size.width * 0.72f, size.height * 0.25f),
                )
                // Las cuerdas: la de afuera fuerte y tres por dentro más tenues.
                val margen = 12.dp.toPx() + size.minDimension * 0.075f
                fun cuerda(extra: Float, ancho: Float, alpha: Float, radio: Float) = drawRoundRect(
                    Color.White.copy(alpha = alpha), Offset(margen + extra, margen + extra),
                    Size(size.width - 2 * (margen + extra), size.height - 2 * (margen + extra)),
                    CornerRadius(radio), style = Stroke(ancho),
                )
                cuerda(0f, 4.dp.toPx(), 0.84f, 8.dp.toPx())
                for (i in 1..3) cuerda(i * 7.dp.toPx(), 2.dp.toPx(), 0.26f, 6.dp.toPx())
            },
    ) {
        if (motor.fase == Fase.ADVERTENCIA) {
            val zona = lado * 0.43f
            Box(Modifier.en(motor.posicionRival, zona).size(zona).background(ColoresTF.Rojo.copy(alpha = 0.14f), RoundedCornerShape(80.dp)))
        }
        val aturdido = motor.fase == Fase.RIVAL_ATURDIDO
        LuchadorEnRing(motor.rival, Modifier.en(motor.posicionRival, 58.dp), aturdido = aturdido, golpeado = reciente && motor.resultadoAtaque.esGolpe)
        Box(Modifier.en(motor.posicionJugador, 58.dp), contentAlignment = Alignment.Center) {
            LuchadorEnRing(motor.usuario, Modifier, aturdido = false, golpeado = reciente && motor.resultadoAtaque == ResultadoAtaque.RECIBIDO)
            if (motor.guardiaActiva) {
                Box(Modifier.size(lado * 0.17f).border(3.dp, ColoresTF.Celeste.copy(alpha = 0.95f), CircleShape))
            }
        }
        if (reciente) {
            val (emoji, donde, tamano) = when (motor.resultadoAtaque) {
                ResultadoAtaque.GOLPE -> Triple("💥", motor.posicionRival, 30.sp)
                ResultadoAtaque.GOLPE_POTENTE -> Triple("💥", motor.posicionRival, 42.sp)
                ResultadoAtaque.BLOQUEADO -> Triple("🛡️", motor.posicionRival, 30.sp)
                ResultadoAtaque.FALLO -> Triple("💨", motor.posicionJugador, 26.sp)
                ResultadoAtaque.ESQUIVADO -> Triple("💨", motor.posicionRival, 26.sp)
                else -> Triple(null, motor.posicionRival, 0.sp)
            }
            emoji?.let { Text(it, fontSize = tamano, modifier = Modifier.en(donde, 48.dp).size(48.dp).padding(top = 4.dp), color = Color.White) }
        }
        if (motor.terminado) {
            Text(
                if (motor.fase == Fase.VICTORIA) "🏆" else "💔",
                fontSize = 52.sp,
                modifier = Modifier.align(Alignment.Center).aparecer(),
            )
        }
    }
}

private val ResultadoAtaque.esGolpe get() = this == ResultadoAtaque.GOLPE || this == ResultadoAtaque.GOLPE_POTENTE

@Composable
private fun LuchadorEnRing(luchador: Luchador, modifier: Modifier, aturdido: Boolean, golpeado: Boolean) {
    Box(
        modifier
            .size(58.dp)
            .graphicsLayer {
                rotationZ = if (aturdido) 14f else 0f
                val e = if (aturdido) 0.87f else 1f
                scaleX = e
                scaleY = e
                alpha = if (golpeado) 0.65f else 1f
            }
            .drawBehind {
                // Sombra en el piso; amarilla si está aturdido.
                drawOval(
                    if (aturdido) ColoresTF.Amarillo.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.45f),
                    Offset(size.width * 0.15f, size.height * 0.82f), Size(size.width * 0.7f, size.height * 0.18f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        ImagenPixel(imagen(luchador), 52.dp)
    }
}

@Composable
private fun Modifier.aparecer(): Modifier {
    val escala = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { escala.animateTo(1f, spring(dampingRatio = 0.5f)) }
    return graphicsLayer {
        scaleX = escala.value
        scaleY = escala.value
    }
}

@Composable
@NonSkippableComposable
private fun Controles(motor: MotorMinijuego, joystick: Dp, alGolpear: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Joystick(joystick, alMover = { motor.moverJoystick(it, ahora()) })
        Spacer(Modifier.weight(1f))
        val alto = minOf(62.dp, joystick * 0.44f)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonGolpe(motor, alto, alGolpear)
            BotonGuardia(motor, alto)
        }
    }
}

/** Arrastra la bolita; al soltarla regresa al centro. En iOS: `JoystickControl`. */
@Composable
private fun Joystick(tamano: Dp, alMover: (Punto) -> Unit) {
    val desplazamiento = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val alcance = rememberCoroutineScope()
    val radio = 42.dp
    Box(
        Modifier
            .size(tamano)
            .semantics { contentDescription = "Joystick para moverte dentro del cuadrilátero" }
            .pointerInput(Unit) {
                val radioPx = radio.toPx()
                val centro = Offset(size.width / 2f, size.height / 2f)
                fun mover(posicion: Offset) {
                    var v = posicion - centro
                    val d = hypot(v.x, v.y)
                    if (d > radioPx) v *= radioPx / d
                    alcance.launch { desplazamiento.snapTo(v) }
                    alMover(Punto(v.x / radioPx.toDouble(), v.y / radioPx.toDouble()))
                }
                awaitEachGesture {
                    val abajo = awaitFirstDown()
                    mover(abajo.position)
                    while (true) {
                        val evento = awaitPointerEvent()
                        val dedo = evento.changes.firstOrNull { it.id == abajo.id } ?: break
                        if (!dedo.pressed) break
                        dedo.consume()
                        mover(dedo.position)
                    }
                    alMover(Punto.CERO)
                    alcance.launch { desplazamiento.animateTo(Offset.Zero, spring(dampingRatio = 0.62f, stiffness = 500f)) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.Black.copy(alpha = 0.36f))
            drawCircle(Color.White.copy(alpha = 0.32f), style = Stroke(2.dp.toPx()))
            drawCircle(
                Color.White.copy(alpha = 0.18f), radio.toPx(),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))),
            )
        }
        Box(
            Modifier
                .offset { IntOffset(desplazamiento.value.x.roundToInt(), desplazamiento.value.y.roundToInt()) }
                .size(56.dp)
                .background(Brush.linearGradient(listOf(ColoresTF.Celeste, ColoresTF.Azul)), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        )
    }
}

@Composable
@NonSkippableComposable
private fun BotonGolpe(motor: MotorMinijuego, alto: Dp, alTocar: () -> Unit) {
    val (icono, etiqueta) = when (motor.fase) {
        Fase.VICTORIA -> "✔" to "Continuar"
        Fase.DERROTA -> "↻" to "Reintentar"
        else -> "👊" to "Golpear"
    }
    val activo = motor.terminado || motor.puedeAtacar(ahora())
    val forma = RoundedCornerShape(19.dp)
    Box(
        Modifier
            .size(82.dp, alto)
            .alpha(if (activo) 1f else 0.55f)
            .clip(forma)
            .background(Brush.linearGradient(listOf(ColoresTF.Rojo, ColoresTF.Naranja)))
            .drawBehind {
                // El enfriamiento se llena de abajo hacia arriba.
                if (!motor.terminado && motor.progresoEnfriamiento < 1) {
                    drawRect(Color.Black.copy(alpha = 0.25f), size = Size(size.width, size.height * (1 - motor.progresoEnfriamiento.toFloat())))
                }
            }
            .border(1.5.dp, Color.White.copy(alpha = 0.48f), forma)
            .clickable(enabled = activo, role = Role.Button, onClickLabel = etiqueta, onClick = alTocar)
            .semantics { contentDescription = etiqueta },
        contentAlignment = Alignment.Center,
    ) {
        Text(icono, fontSize = 26.sp, color = Color.White, style = ArvoBold)
    }
}

/** Se cubre mientras lo mantienes presionado. */
@Composable
@NonSkippableComposable
private fun BotonGuardia(motor: MotorMinijuego, alto: Dp) {
    val activa = motor.guardiaActiva
    val forma = RoundedCornerShape(19.dp)
    val haptics = LocalHaptics.current
    Box(
        Modifier
            .size(82.dp, alto)
            .background(if (activa) ColoresTF.Celeste.copy(alpha = 0.88f) else ColoresTF.Azul.copy(alpha = 0.94f), forma)
            .border(1.5.dp, if (activa) ColoresTF.Amarillo else Color.White.copy(alpha = 0.34f), forma)
            .semantics { contentDescription = "Cubrirse y parar el golpe (mantén presionado)" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    motor.cambiarGuardia(true, ahora())
                    haptics.seleccion()
                    do {
                        val evento = awaitPointerEvent()
                    } while (evento.changes.any { it.pressed })
                    motor.cambiarGuardia(false, ahora())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("🛡️", fontSize = 26.sp, color = if (activa) ColoresTF.Amarillo else Color.White)
    }
}

@Preview(heightDp = 800)
@Composable
private fun MinijuegoPreview() = TiempoFueraTheme {
    val nivel = DatosPreview.ligas.first().niveles().first { it.id == 50 }
    MinijuegoBarrera(nivel, DatosPreview.luchadorSeleccionado, DatosPreview.luchadores[2], {}, {}, {})
}
