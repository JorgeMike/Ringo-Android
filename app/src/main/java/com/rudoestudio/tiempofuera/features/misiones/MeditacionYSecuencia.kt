package com.rudoestudio.tiempofuera.features.misiones

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorTiempo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.domain.misiones.JuegoSecuencia
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

// Meditación (M6.3) y memorizar secuencia (M6.6).

/** Medio ciclo de respiración: 4 s inhalando, 4 s exhalando (en iOS: `duracionMedioCiclo`). */
private const val MEDIO_CICLO_MS = 4_000
private const val INTERVALO_VIBRACION_MS = 120L
private const val INTENSIDAD_MAXIMA = 0.6f

/**
 * Meditación (M6.3): "Medita un ratito". Un círculo respira con el luchador, con vibración
 * que crece al inhalar y baja al exhalar. Bloquea distracción mientras corre y se puede
 * pausar. En iOS: `MeditacionView` + `MeditacionViewModel` (sin audio en esta versión).
 */
@Composable
internal fun MeditacionMision(mision: Mision, estado: EstadoFlujo, viewModel: MisionFlujoViewModel) {
    val haptics = LocalHaptics.current
    val sesion = estado.sesion
    val ahora = rememberAhora(sesion, viewModel::revisarFin)
    val abierto = RelojMision.esModoAbierto(mision)
    val corriendo = RelojMision.corriendo(mision, sesion, ahora)
    val terminada = sesion != null && RelojMision.seAcabo(mision, sesion, ahora)
    val vueltas = if (abierto && sesion != null) RelojMision.vueltas(mision, RelojMision.transcurrido(sesion, ahora)) else 0

    // Inhala y exhala mientras corre; la vibración sube y baja con cada medio ciclo.
    var inhalando by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(corriendo) {
        if (!corriendo) {
            inhalando = null
            return@LaunchedEffect
        }
        var fase = true
        while (isActive) {
            inhalando = fase
            val pasos = MEDIO_CICLO_MS / INTERVALO_VIBRACION_MS
            for (paso in 1..pasos) {
                val avance = paso / pasos.toFloat()
                haptics.vibrarRespiracion(INTENSIDAD_MAXIMA * if (fase) avance else 1 - avance)
                delay(INTERVALO_VIBRACION_MS)
            }
            fase = !fase
        }
    }
    val respiracion = tween<Float>(MEDIO_CICLO_MS, easing = FastOutSlowInEasing)
    val escalaCirculo by animateFloatAsState(if (inhalando == true) 1f else 0.65f, respiracion, label = "circulo")
    val opacidadCirculo by animateFloatAsState(if (inhalando == true) 0.85f else 0.25f, respiracion, label = "opacidad")
    val escalaLuchador by animateFloatAsState(if (inhalando == true) 1.08f else 0.95f, respiracion, label = "luchador")

    Column(Modifier.fillMaxSize().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Titulo("Medita", "un ratito")
        TextoContorno(mision.descripcion, arvo(18), ColoresTF.Blanco, ColoresTF.Negro, 2.dp, alineacion = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(ColoresTF.Blanco.copy(alpha = 0.3f), style = Stroke(3.dp.toPx()))
                drawCircle(ColoresTF.Celeste.copy(alpha = opacidadCirculo), radius = size.minDimension / 2 * escalaCirculo)
            }
            ImagenPixel(estado.luchador ?: R.drawable.ringo, 150.dp, Modifier.scale(escalaLuchador))
            if (abierto) RecompensaVueltas(mision, vueltas, Modifier.align(Alignment.TopEnd))
        }
        RelojEnBadges(RelojMision.segundosEnPantalla(mision, sesion, ahora), Modifier.offset(y = (-32).dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (inhalando) {
                    true -> "Inhala"
                    false -> "Exhala"
                    null -> "Listo"
                },
                style = arvo(20), color = ColoresTF.Blanco,
            )
            mision.configuracion.segundosParaRecompensa?.let { unidad ->
                Text("$vueltas vueltas completadas", style = arvo(12), color = ColoresTF.Blanco)
                Text("Cada vuelta: ${RelojMision.formato(unidad)}", style = arvo(12), color = ColoresTF.Blanco)
            }
        }
        Spacer(Modifier.weight(1f))
        if (terminada) {
            BotonL(ColoresTF.Amarillo, "RECLAMAR RECOMPENSA", alTocar = { viewModel.reclamar(if (abierto) vueltas else 1) })
        } else {
            BotonL(
                if (corriendo) ColoresTF.Amarillo else ColoresTF.Verde,
                when {
                    corriendo -> "PAUSAR"
                    sesion?.pausada == true -> "SEGUIR"
                    else -> "INICIAR"
                },
                alTocar = {
                    when {
                        corriendo -> viewModel.pausar()
                        sesion?.pausada == true -> viewModel.reanudar()
                        else -> viewModel.iniciarTiempo()
                    }
                },
                habilitado = !estado.otraEnCurso,
            )
            if (abierto) {
                BotonL(ColoresTF.Amarillo, "TERMINAR Y RECLAMAR", alTocar = viewModel::terminarTiempo, habilitado = vueltas > 0)
            }
            if (sesion != null) BotonL(ColoresTF.Rojo, "ME RINDO", alTocar = viewModel::rendirse)
        }
    }
}

/** En iOS: `SecuenciaViewModel.Estado`. */
private enum class FaseSecuencia(val mensaje: String?) {
    MOSTRANDO("Memoriza la secuencia..."),
    TURNO("Tu turno, repite la secuencia"),
    CORRECTO("¡CAMPEÓN!"),
    INCORRECTO("Ups, intenta de nuevo"),
    TIEMPO_AGOTADO("Se acabó el tiempo"),
    COMPLETADO(null),
}

/** Colores de los tambores, en orden. */
private val ColoresTambor = listOf(ColoresTF.Celeste, ColoresTF.Verde, ColoresTF.Rojo, ColoresTF.Amarillo, ColoresTF.Naranja, ColoresTF.Morado)

/**
 * Memorizar secuencia (M6.6): los tambores se encienden en orden y hay que repetirlo antes
 * de que se acabe el tiempo; cada ronda es más larga y con menos tiempo por paso. Fallar
 * repite la ronda. En iOS: `SecuenciaView` + `SecuenciaViewModel`.
 */
@Composable
internal fun SecuenciaMision(mision: Mision, luchador: Int?, alReclamar: () -> Unit) {
    val rondas = remember(mision.id) { JuegoSecuencia.generar(mision) }
    val tambores = JuegoSecuencia.numeroTambores(mision)
    val alcance = rememberCoroutineScope()
    val haptics = LocalHaptics.current

    var ronda by remember { mutableIntStateOf(0) }
    /** Sube para volver a mostrar la ronda (después de un error). */
    var intento by remember { mutableIntStateOf(0) }
    var fase by remember { mutableStateOf(FaseSecuencia.MOSTRANDO) }
    var paso by remember { mutableIntStateOf(0) }
    var encendido by remember { mutableStateOf<Int?>(null) }
    var restante by remember { mutableDoubleStateOf(0.0) }
    var total by remember { mutableDoubleStateOf(1.0) }

    fun encender(tambor: Int) {
        encendido = tambor
        alcance.launch {
            delay(JuegoSecuencia.ENCENDIDO_MS)
            if (encendido == tambor) encendido = null
        }
    }

    fun fallar(porTiempo: Boolean) {
        fase = if (porTiempo) FaseSecuencia.TIEMPO_AGOTADO else FaseSecuencia.INCORRECTO
        haptics.notificacion(TipoNotificacion.ERROR)
        alcance.launch {
            delay(1_000)
            intento++
        }
    }

    // Muestra la ronda y luego corre el tiempo del turno.
    LaunchedEffect(ronda, intento) {
        if (ronda >= rondas.size) return@LaunchedEffect
        fase = FaseSecuencia.MOSTRANDO
        paso = 0
        delay(600)
        for (tambor in rondas[ronda]) {
            encender(tambor)
            delay(JuegoSecuencia.ENTRE_PASOS_MS)
        }
        fase = FaseSecuencia.TURNO
        total = JuegoSecuencia.tiempoLimite(rondas[ronda].size, ronda)
        val fin = System.currentTimeMillis() + (total * 1000).toLong()
        while (isActive && fase == FaseSecuencia.TURNO) {
            restante = (fin - System.currentTimeMillis()) / 1000.0
            if (restante <= 0) {
                restante = 0.0
                fallar(porTiempo = true)
                break
            }
            delay(50)
        }
    }

    fun tocar(tambor: Int) {
        if (fase != FaseSecuencia.TURNO) return
        encender(tambor)
        if (tambor != rondas[ronda][paso]) return fallar(porTiempo = false)
        paso++
        if (paso < rondas[ronda].size) return
        fase = FaseSecuencia.CORRECTO
        alcance.launch {
            delay(1_000)
            if (ronda + 1 >= rondas.size) {
                ronda = rondas.size
                fase = FaseSecuencia.COMPLETADO
            } else {
                ronda++
            }
        }
    }

    val fraccion = (restante / total).coerceIn(0.0, 1.0).toFloat()
    Column(Modifier.fillMaxSize().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImagenPixel(luchador ?: R.drawable.ringo, 120.dp, Modifier.rotate(-12f))
            Titulo("¡Memoriza!", "la secuencia", Modifier.offset(x = (-40).dp))
        }
        Row(Modifier.fillMaxWidth().height(90.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                fase.mensaje?.let {
                    val color = when (fase) {
                        FaseSecuencia.CORRECTO -> ColoresTF.Verde
                        FaseSecuencia.INCORRECTO, FaseSecuencia.TIEMPO_AGOTADO -> ColoresTF.RojoError
                        else -> ColoresTF.Blanco
                    }
                    TextoContorno(it, pirata(28), color, Color.Black, 3.dp, alineacion = TextAlign.Center)
                }
            }
            Box(Modifier.weight(1f).scale(0.7f), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.badge_indicador), null)
                TextoConSombra(
                    "${minOf(ronda, rondas.size)} / ${rondas.size}", pirata(56),
                    color = Color.Black, colorSombra = ColoresTF.Blanco,
                    modifier = Modifier.rotate(8f).offset((-10).dp, (-10).dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(horizontal = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            (0 until tambores).chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    fila.forEach { i ->
                        Tambor(
                            color = ColoresTambor[i % ColoresTambor.size],
                            encendido = encendido == i,
                            espejado = i % 2 == 1,
                            habilitado = fase == FaseSecuencia.TURNO,
                            alTocar = { tocar(i) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IndicadorTiempo(
            fraccion, ceil(restante).toInt(),
            color = when {
                fraccion < 0.25f -> ColoresTF.RojoError
                fraccion < 0.5f -> ColoresTF.Amarillo
                else -> ColoresTF.Verde
            },
            modifier = Modifier.alpha(if (fase == FaseSecuencia.TURNO) 1f else 0f),
        )
        if (fase == FaseSecuencia.COMPLETADO) BotonL(ColoresTF.Amarillo, "RECLAMAR", alTocar = alReclamar)
    }
}

/** Lienzo del SVG del tambor (144x120). En iOS: `TamborBaseShape` y `TamborCaraShape`. */
private const val TAMBOR_ANCHO = 144f
private const val TAMBOR_ALTO = 120f

private fun baseTambor(size: Size): Path {
    val x = size.width / TAMBOR_ANCHO
    val y = size.height / TAMBOR_ALTO
    return Path().apply {
        moveTo(142.912f * x, 14.252f * y)
        cubicTo(142.963f * x, 13.6113f * y, 142.405f * x, 13.0896f * y, 141.769f * x, 13.1835f * y)
        lineTo(11.7825f * x, 32.3861f * y)
        cubicTo(11.3293f * x, 32.4531f * y, 10.9796f * x, 32.8194f * y, 10.9337f * x, 33.2753f * y)
        lineTo(3.11588f * x, 110.965f * y)
        cubicTo(3.05904f * x, 111.53f * y, 3.4834f * x, 112.029f * y, 4.05013f * x, 112.063f * y)
        lineTo(133.532f * x, 119.94f * y)
        cubicTo(134.076f * x, 119.973f * y, 134.546f * x, 119.565f * y, 134.589f * x, 119.021f * y)
        close()
    }
}

private fun caraTambor(size: Size): Path {
    val x = size.width / TAMBOR_ANCHO
    val y = size.height / TAMBOR_ALTO
    return Path().apply {
        moveTo(125.426f * x, 1.16749f * y)
        cubicTo(125.426f * x, 0.553958f * y, 124.878f * x, 0.0851033f * y, 124.272f * x, 0.179358f * y)
        lineTo(0.848068f * x, 19.3684f * y)
        cubicTo(0.360947f * x, 19.4441f * y, 0.0016861f * x, 19.8636f * y, 0.0016861f * x, 20.3565f * y)
        lineTo(0.0016861f * x, 96.5f * y)
        cubicTo(0.0016861f * x, 97.0523f * y, 0.449402f * x, 97.5f * y, 1.00169f * x, 97.5f * y)
        lineTo(124.426f * x, 97.5f * y)
        cubicTo(124.978f * x, 97.5f * y, 125.426f * x, 97.0523f * y, 125.426f * x, 96.5f * y)
        close()
    }
}

/** Apagado, en gris (en iOS: `.saturation(0)`); encendido, con su color y un poco más grande. */
@Composable
private fun Tambor(color: Color, encendido: Boolean, espejado: Boolean, habilitado: Boolean, alTocar: () -> Unit, modifier: Modifier) {
    val escala by animateFloatAsState(if (encendido) 1.05f else 1f, tween(200), label = "tambor")
    val luminancia = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    val relleno = if (encendido) color else Color(luminancia, luminancia, luminancia)
    Canvas(
        modifier
            .aspectRatio(TAMBOR_ANCHO / TAMBOR_ALTO)
            .graphicsLayer {
                scaleX = if (espejado) -escala else escala
                scaleY = escala
            }
            .clickable(remember { MutableInteractionSource() }, indication = null, enabled = habilitado, role = Role.Button, onClick = alTocar)
            .semantics { contentDescription = "Tambor" },
    ) {
        drawPath(baseTambor(size), ColoresTF.AzulNoche)
        drawPath(caraTambor(size), relleno)
    }
}
