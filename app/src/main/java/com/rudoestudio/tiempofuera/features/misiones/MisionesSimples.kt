package com.rudoestudio.tiempofuera.features.misiones

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.SeccionRecompensas
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TemporizadorBadges
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

// Las misiones que no usan sensores: temporizador (M6.2) y contador de acciones (M6.4).

/**
 * Temporizador (M6.2): "Al ring contra el reloj". Bloquea las apps de distracción mientras
 * corre. Si no hay apps elegidas, primero pide elegirlas. En iOS: `TiempoMisionFlowView` +
 * `TiempoView`.
 */
@Composable
internal fun TiempoMision(mision: Mision, estado: EstadoFlujo, viewModel: MisionFlujoViewModel, alElegirApps: (TipoGrupoApps) -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.revisarApps() }
    val sesion = estado.sesion
    if (sesion == null && !estado.hayAppsDistraccion) {
        PedirApps { alElegirApps(TipoGrupoApps.DISTRACCION) }
        return
    }

    val ahora = rememberAhora(sesion, viewModel::revisarFin)
    val abierto = RelojMision.esModoAbierto(mision)
    val corriendo = RelojMision.corriendo(mision, sesion, ahora)
    val terminada = sesion != null && RelojMision.seAcabo(mision, sesion, ahora)
    val segundos = RelojMision.segundosEnPantalla(mision, sesion, ahora)
    val vueltas = if (abierto && sesion != null) RelojMision.vueltas(mision, RelojMision.transcurrido(sesion, ahora)) else 0
    val progreso by animateFloatAsState(RelojMision.progreso(mision, sesion, ahora), tween(1_000, easing = LinearEasing), label = "reloj")

    Column(Modifier.fillMaxSize().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Titulo("AL RING", "contra el reloj")
        TextoContorno(
            mision.descripcion, arvo(18), ColoresTF.Blanco, ColoresTF.Negro, 2.dp,
            alineacion = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(ColoresTF.Azul)
                val grosor = 14.dp.toPx()
                drawArc(
                    ColoresTF.Celeste, -90f, 360f * progreso, useCenter = false,
                    topLeft = Offset(grosor / 2, grosor / 2),
                    size = Size(size.width - grosor, size.height - grosor),
                    style = Stroke(grosor, cap = StrokeCap.Round),
                )
            }
            LuchadorTrotando(estado.luchador ?: R.drawable.ringo, corriendo)
            GotasDeSudor(corriendo)
            if (abierto) RecompensaVueltas(mision, vueltas, Modifier.align(Alignment.TopEnd))
        }
        RelojEnBadges(segundos, Modifier.offset(y = (-32).dp))
        Spacer(Modifier.weight(1f))
        when {
            terminada -> BotonL(
                ColoresTF.Amarillo, "RECLAMAR RECOMPENSA",
                alTocar = { viewModel.reclamar(if (abierto) vueltas else 1) },
            )
            !corriendo -> BotonL(
                ColoresTF.Verde, "INICIAR",
                alTocar = viewModel::iniciarTiempo,
                habilitado = !estado.otraEnCurso,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (abierto) {
                    BotonL(ColoresTF.Amarillo, "TERMINAR Y RECLAMAR", alTocar = viewModel::terminarTiempo, habilitado = vueltas > 0)
                }
                BotonL(ColoresTF.Rojo, "ME RINDO", alTocar = viewModel::rendirse)
            }
        }
        if (estado.otraEnCurso && sesion == null) {
            Text(
                "Ya tienes otra misión de tiempo corriendo. Termínala primero.",
                style = arvo(12), color = ColoresTF.Blanco, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
            )
        }
    }
}

/**
 * La hora de ahora, cada segundo, mientras la pantalla está abierta. El reloj de la misión
 * se recalcula de las fechas: si la app estuvo cerrada, ya cuadra. [alTick] revisa si se acabó.
 */
@Composable
internal fun rememberAhora(clave: Any?, alTick: (Instant) -> Unit = {}): Instant {
    var ahora by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(clave) {
        while (true) {
            ahora = Instant.now()
            alTick(ahora)
            delay(1_000)
        }
    }
    return ahora
}

/** "x3" y lo ganado en las vueltas del modo abierto, arriba a la derecha del reloj. */
@Composable
internal fun RecompensaVueltas(mision: Mision, vueltas: Int, modifier: Modifier) {
    Column(
        modifier.offset(32.dp, (-20).dp).graphicsLayer {
            scaleX = 0.75f; scaleY = 0.75f; transformOrigin = TransformOrigin(1f, 0f)
        },
        horizontalAlignment = Alignment.End,
    ) {
        val r = mision.recompensa.multiplicada(vueltas)
        TextoContorno("x$vueltas", pirata(32), ColoresTF.Amarillo, ColoresTF.Negro, 2.dp, Modifier.rotate(-4f))
        SeccionRecompensas(r.puntosPoder, r.monedas, r.exp)
    }
}

/** "05:30" en badges entre dos líneas decorativas. */
@Composable
internal fun RelojEnBadges(segundos: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "Tiempo: ${RelojMision.formato(segundos)}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineaDecorativa(espejada = false)
        TemporizadorBadges(RelojMision.formato(segundos))
        LineaDecorativa(espejada = true)
    }
}

@Composable
private fun LineaDecorativa(espejada: Boolean) {
    Image(
        painterResource(R.drawable.linea_decor), null,
        colorFilter = ColorFilter.tint(ColoresTF.Negro),
        modifier = Modifier.graphicsLayer { scaleX = if (espejada) -1f else 1f },
    )
}

/** Antes de empezar hay que tener apps que bloquear. En iOS: `solicitudSeleccion`. */
@Composable
private fun PedirApps(alElegir: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text("✋", style = pirata(48))
        Text(
            "Antes de empezar, elige qué apps quieres bloquear durante esta misión",
            style = arvo(18), color = ColoresTF.Blanco, textAlign = TextAlign.Center,
        )
        BotonL(ColoresTF.Morado, "SELECCIONAR APPS", alTocar = alElegir)
    }
}

/** Salta y se balancea mientras corre el reloj. En iOS: `luchadorTrotando` (TimelineView). */
@Composable
private fun LuchadorTrotando(imagen: Int, corriendo: Boolean) {
    val pasos = rememberInfiniteTransition(label = "trote")
    // Un ciclo de seno son dos pasos: 2.8 pasos por segundo, como iOS.
    val fase by pasos.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(DURACION_PASO_DOBLE_MS, easing = LinearEasing)), label = "fase")
    val salto = if (corriendo) abs(sin(fase)) else 0f
    val balanceo = if (corriendo) sin(fase) else 0f
    ImagenPixel(
        imagen,
        150.dp,
        Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 1f)
            scaleY = 1f - (1f - salto) * 0.06f + salto * 0.03f
            rotationZ = (if (corriendo) 5f else 0f) + balanceo * 6f
            translationX = (balanceo * 3).dp.toPx()
            translationY = (-salto * 14).dp.toPx()
        },
    )
}

/**
 * Gotas que salen de la cabeza mientras corre. En iOS: `GotasDeSudor`; allá son el
 * símbolo "drop.fill", aquí una gota dibujada (celeste con borde blanco).
 */
@Composable
private fun GotasDeSudor(activo: Boolean) {
    if (!activo) return
    val tiempo = rememberInfiniteTransition(label = "gotas")
    val t by tiempo.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    Canvas(Modifier.size(260.dp)) {
        val centro = Offset(size.width / 2, size.height / 2)
        repeat(CANTIDAD_GOTAS) { i ->
            val ciclo = t / DURACION_GOTA + i / CANTIDAD_GOTAS.toFloat()
            val progreso = ciclo - floor(ciclo)
            val variacion = pseudoAleatorio(i, floor(ciclo).toInt())
            val lado = if (i % 2 == 0) -1f else 1f
            val x = lado * 28f + lado * (30f + variacion * 30f) * progreso
            val y = -62f - 26f * progreso + 110f * progreso * progreso
            val opacidad = (progreso * 8).coerceAtMost(1f) * (1 - progreso * progreso)
            val tamano = (10f + variacion * 6f) * (0.6f + progreso * 0.4f)
            translate(centro.x + x.dp.toPx(), centro.y + y.dp.toPx()) {
                drawPath(gota((tamano + 5f).dp.toPx()), ColoresTF.Blanco, alpha = opacidad)
                drawPath(gota(tamano.dp.toPx()), ColoresTF.Celeste, alpha = opacidad)
            }
        }
    }
}

private const val DURACION_PASO_DOBLE_MS = 714
private const val CANTIDAD_GOTAS = 6
private const val DURACION_GOTA = 1.1f

/** Gota centrada en (0, 0): punta arriba, panza redonda abajo. */
private fun gota(alto: Float): Path = Path().apply {
    val r = alto * 0.36f
    moveTo(0f, -alto / 2)
    cubicTo(r * 0.6f, -alto * 0.15f, r, alto * 0.05f, r, alto * 0.14f)
    cubicTo(r, alto * 0.38f, -r, alto * 0.38f, -r, alto * 0.14f)
    cubicTo(-r, alto * 0.05f, -r * 0.6f, -alto * 0.15f, 0f, -alto / 2)
    close()
}

/** Número entre 0 y 1, estable para la misma gota y el mismo ciclo. */
private fun pseudoAleatorio(indice: Int, ciclo: Int): Float {
    val semilla = sin((indice * 127 + ciclo * 311).toDouble()) * 43758.5453
    return (semilla - floor(semilla)).toFloat()
}

/** Contador de acciones (M6.4): "Haz las repeticiones". En iOS: `AccionesView` + `AccionesViewModel`. */
@Composable
internal fun AccionesMision(mision: Mision, luchador: Int?, alReclamar: () -> Unit) {
    val haptics = LocalHaptics.current
    val objetivo = mision.configuracion.cantidadObjetivo ?: 5
    var cantidad by rememberSaveable { mutableIntStateOf(0) }
    val completado = cantidad >= objetivo

    Column(Modifier.fillMaxSize().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImagenPixel(luchador ?: R.drawable.ringo, 120.dp, Modifier.rotate(-12f))
            Titulo("Haz las", "repeticiones", Modifier.offset(x = (-40).dp))
        }
        Spacer(Modifier.weight(1f))
        TextoConSombra(mision.titulo, pirata(38), colorSombra = ColoresTF.Celeste, alineacion = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        Text(mision.descripcion, style = arvo(20), color = ColoresTF.Blanco, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        Row(
            Modifier
                .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                .semantics(mergeDescendants = true) { contentDescription = "Progreso de la misión: $cantidad de $objetivo" },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TemporizadorBadges("$cantidad")
            Text("/", style = pirata(40), color = ColoresTF.Blanco)
            TemporizadorBadges("$objetivo")
        }
        Spacer(Modifier.weight(1f))
        if (completado) {
            BotonL(ColoresTF.Amarillo, "RECLAMAR", alTocar = alReclamar)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BotonS(
                    ColoresTF.Amarillo, IconoBotonS.ABAJO,
                    alTocar = { haptics.impacto(EstiloImpacto.LIGERO); cantidad = (cantidad - 1).coerceAtLeast(0) },
                    escala = 1.5f, habilitado = cantidad > 0,
                )
                BotonS(
                    ColoresTF.Verde, IconoBotonS.ARRIBA,
                    alTocar = { haptics.impacto(EstiloImpacto.LIGERO); cantidad = (cantidad + 1).coerceAtMost(objetivo) },
                    escala = 1.5f,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
