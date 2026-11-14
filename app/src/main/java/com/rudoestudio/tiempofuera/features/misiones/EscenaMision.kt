package com.rudoestudio.tiempofuera.features.misiones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorTiempo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.domain.misiones.JuegoEscena
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.PuntoEscena
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Memorizar escena (M6.7).

private val STICKERS = listOf(
    R.drawable.sticker_1, R.drawable.sticker_2, R.drawable.sticker_3, R.drawable.sticker_4, R.drawable.sticker_5,
    R.drawable.sticker_6, R.drawable.sticker_7, R.drawable.sticker_8, R.drawable.sticker_9,
)

/** Los stickers se ven al 40 % de su tamaño (en iOS: `escalaSticker`). */
private const val ESCALA_STICKER = 0.4f

/** En iOS: `EscenaViewModel.Estado`. */
private enum class FaseEscena { MEMORIZANDO, COLOCANDO, VERIFICADO, COMPLETADO }

/** Un sticker de la escena. [colocado] es dónde lo dejó el usuario; `null` = en la bandeja. */
private data class ElementoEscena(val id: Int, val sticker: Int, val original: PuntoEscena, val colocado: PuntoEscena? = null, val correcto: Boolean? = null)

private fun nuevosElementos(ids: List<Int>): List<ElementoEscena> {
    val posiciones = JuegoEscena.posiciones(ids.size)
    val stickers = JuegoEscena.stickers(ids.size)
    return ids.mapIndexed { i, id -> ElementoEscena(id, STICKERS[stickers[i] - 1], posiciones[i]) }
}

/**
 * Memorizar escena (M6.7): "Reacomoda la escena". Se ven unos stickers, desaparecen y hay
 * que arrastrarlos de la bandeja a su lugar antes de que se acabe el tiempo; si se acaba,
 * se cambian de lugar y vuelve a empezar. En iOS: `EscenaView` + `EscenaViewModel`.
 */
@Composable
internal fun EscenaMision(mision: Mision, alReclamar: () -> Unit) {
    val ids = remember(mision.id) { mision.configuracion.elementosEscena.orEmpty().map { it.idElemento } }
    val elementos = remember(mision.id) { mutableStateListOf<ElementoEscena>().apply { addAll(nuevosElementos(ids)) } }
    var fase by remember { mutableStateOf(FaseEscena.MEMORIZANDO) }
    var restante by remember { mutableIntStateOf(0) }
    /** Sube para reiniciar el reloj de la fase actual. */
    var ronda by remember { mutableIntStateOf(0) }

    // Arrastre: qué sticker y dónde va el dedo (en coordenadas de la raíz).
    var arrastrando by remember { mutableStateOf<Int?>(null) }
    var dedo by remember { mutableStateOf(Offset.Zero) }
    var escena by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var raiz by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(fase, ronda) {
        val segundos = when (fase) {
            FaseEscena.MEMORIZANDO -> JuegoEscena.tiempoMemorizar(ids.size)
            FaseEscena.COLOCANDO -> JuegoEscena.tiempoColocar(ids.size)
            else -> return@LaunchedEffect
        }
        for (s in segundos downTo 1) {
            restante = s
            delay(1_000)
        }
        restante = 0
        if (fase == FaseEscena.MEMORIZANDO) {
            fase = FaseEscena.COLOCANDO
        } else {
            // Se acabó el tiempo: todo cambia de lugar y a memorizar otra vez.
            elementos.clear()
            elementos.addAll(nuevosElementos(ids))
            fase = FaseEscena.MEMORIZANDO
            ronda++
        }
    }
    LaunchedEffect(fase) {
        if (fase == FaseEscena.VERIFICADO && elementos.all { it.correcto == true }) {
            delay(1_200)
            fase = FaseEscena.COMPLETADO
        }
    }

    fun soltar(id: Int) {
        val zona = escena?.boundsInRoot()
        val i = elementos.indexOfFirst { it.id == id }
        arrastrando = null
        if (i < 0 || zona == null) return
        elementos[i] = if (zona.contains(dedo)) {
            elementos[i].copy(colocado = JuegoEscena.acotar(PuntoEscena(((dedo.x - zona.left) / zona.width).toDouble(), ((dedo.y - zona.top) / zona.height).toDouble())))
        } else {
            elementos[i].copy(colocado = null)
        }
    }

    val arrastre: @Composable (Int) -> Modifier = { id ->
        var coordenadas by remember { mutableStateOf<LayoutCoordinates?>(null) }
        Modifier
            .onGloballyPositioned { coordenadas = it }
            .pointerInput(id, fase) {
                if (fase != FaseEscena.COLOCANDO) return@pointerInput
                detectDragGestures(
                    onDragStart = { inicio ->
                        arrastrando = id
                        dedo = coordenadas?.localToRoot(inicio) ?: Offset.Zero
                    },
                    onDrag = { cambio, delta ->
                        cambio.consume()
                        dedo += delta
                    },
                    onDragEnd = { soltar(id) },
                    onDragCancel = { arrastrando = null },
                )
            }
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { raiz = it }) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Titulo("REACOMODA", "la escena")
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MarcoEscena(Modifier.onGloballyPositioned { escena = it }) { ancho, alto ->
                    elementos.forEach { e -> key(e.id) {
                        val punto = if (fase == FaseEscena.MEMORIZANDO) e.original else e.colocado
                        if (punto != null) {
                            if (fase == FaseEscena.VERIFICADO && e.correcto == false) {
                                // Dónde iba de verdad, con el margen que cuenta como acierto.
                                Canvas(Modifier.size(ancho * (JuegoEscena.RADIO_ACIERTO * 2).toFloat(), alto * (JuegoEscena.RADIO_ACIERTO * 2).toFloat()).centradoEn(e.original, ancho, alto)) {
                                    drawOval(ColoresTF.Verde.copy(alpha = 0.6f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                                }
                            }
                            Box(Modifier.centradoEn(punto, ancho, alto).alpha(if (arrastrando == e.id) 0f else 1f)) {
                                Sticker(e.sticker, modifier = arrastre(e.id))
                                e.correcto?.let { bien ->
                                    Image(
                                        painterResource(if (bien) R.drawable.check_mark else R.drawable.xmark), null,
                                        Modifier.align(Alignment.TopEnd).offset(10.dp, (-10).dp).size(22.dp),
                                    )
                                }
                            }
                        }
                    } }
                }
                Bandeja(fase == FaseEscena.COLOCANDO, elementos.filter { it.colocado == null }, arrastrando, arrastre, Modifier.align(Alignment.BottomCenter))
            }
            when (fase) {
                FaseEscena.MEMORIZANDO -> IndicadorTiempo(restante / JuegoEscena.tiempoMemorizar(ids.size).toFloat(), restante, ColoresTF.Verde)
                FaseEscena.COLOCANDO -> IndicadorTiempo(
                    restante / JuegoEscena.tiempoColocar(ids.size).toFloat(), restante,
                    if (restante <= 5) ColoresTF.Rojo else ColoresTF.Verde,
                )
                else -> Unit
            }
            when {
                fase == FaseEscena.COLOCANDO && elementos.isNotEmpty() && elementos.all { it.colocado != null } ->
                    BotonL(ColoresTF.Amarillo, "COMPROBAR", alTocar = {
                        elementos.indices.forEach { i -> elementos[i] = elementos[i].copy(correcto = JuegoEscena.esAcierto(elementos[i].colocado, elementos[i].original)) }
                        fase = FaseEscena.VERIFICADO
                    })
                fase == FaseEscena.VERIFICADO && elementos.any { it.correcto != true } ->
                    BotonL(ColoresTF.Amarillo, "REINTENTAR", alTocar = {
                        elementos.indices.forEach { i -> elementos[i] = elementos[i].copy(colocado = null, correcto = null) }
                        fase = FaseEscena.COLOCANDO
                        ronda++
                    })
                fase == FaseEscena.COMPLETADO -> BotonL(ColoresTF.Amarillo, "RECLAMAR", alTocar = alReclamar)
            }
        }

        // El sticker que sigue al dedo, encima de todo.
        val origen = raiz?.positionInRoot() ?: Offset.Zero
        elementos.firstOrNull { it.id == arrastrando }?.let { e ->
            Sticker(
                e.sticker, escalaExtra = 1.15f,
                modifier = Modifier.offset { IntOffset((dedo.x - origen.x).roundToInt(), (dedo.y - origen.y).roundToInt()) }.centrado(),
            )
        }
    }
}

/** Mueve el elemento para que su centro quede en (0, 0) de donde lo pongan. */
private fun Modifier.centrado(): Modifier = layout { medible, restricciones ->
    val p = medible.measure(restricciones.copy(minWidth = 0, minHeight = 0))
    layout(p.width, p.height) { p.place(-p.width / 2, -p.height / 2) }
}

/** Coloca el centro del elemento en [punto] de la escena (de 0 a 1). */
private fun Modifier.centradoEn(punto: PuntoEscena, ancho: Dp, alto: Dp): Modifier =
    offset(ancho * punto.x.toFloat(), alto * punto.y.toFloat()).centrado()

@Composable
private fun Sticker(imagen: Int, modifier: Modifier = Modifier, escalaExtra: Float = 1f) {
    val pintor = painterResource(imagen)
    val tamano = with(LocalDensity.current) { DpSize(pintor.intrinsicSize.width.toDp(), pintor.intrinsicSize.height.toDp()) }
    Image(pintor, null, modifier.size(tamano.width * ESCALA_STICKER * escalaExtra, tamano.height * ESCALA_STICKER * escalaExtra))
}

/** Los stickers que faltan por acomodar, sobre su placa chueca. */
@Composable
private fun Bandeja(
    visible: Boolean,
    pendientes: List<ElementoEscena>,
    arrastrando: Int?,
    arrastre: @Composable (Int) -> Modifier,
    modifier: Modifier,
) = AnimatedVisibility(visible, modifier) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        pendientes.forEach { e -> key(e.id) {
            Box(contentAlignment = Alignment.Center) {
                val forma = DiagonalShape(18.dp)
                Box(Modifier.size(32.dp).offset((-5).dp, 9.dp).background(Color.Black, forma))
                Box(Modifier.size(32.dp).offset((-8).dp, 6.dp).background(ColoresTF.Blanco, forma))
                Box(Modifier.alpha(if (arrastrando == e.id) 0f else 1f)) { Sticker(e.sticker, arrastre(e.id)) }
            }
        } }
    }
}

// Formas del marco, del SVG (viewBox 453x613). En iOS: `MarcoEscenaShape.swift`.
private const val MARCO_ANCHO = 453f
private const val MARCO_ALTO = 613f

private val PuntosMarco = listOf(374.397f to 532.871f, 55.5635f to 462.971f, 70.0742f to 28.0791f, 358.35f to 11.6416f)
private val PuntosChispaDerecha = listOf(
    236.308f to 549.124f, 225.655f to 582.558f, 278.917f to 561.6f, 301.624f to 612.998f, 334.142f to 567.588f, 393.571f to 582.558f,
    364.417f to 554.863f, 426.089f to 567.588f, 393.571f to 504.463f, 453f to 456.807f, 405.345f to 450.819f, 426.089f to 433.603f,
    364.417f to 438.094f, 324.05f to 359.998f, 291.532f to 406.905f, 278.917f to 396.426f, 262.378f to 444.83f, 178f to 444.83f,
    202.949f to 478.264f, 178f to 485.001f, 219.769f to 512.696f, 178f to 554.863f,
)
private val PuntosChispaIzquierda = listOf(
    80.519f to 40.5681f, 68.828f to 27.7799f, 64.7223f to 55.6012f, 37.1567f to 52.5574f, 48.6051f to 77.6f, 27.6124f to 99.1675f,
    46.6186f to 93.6147f, 26.0363f to 116.69f, 61.0279f to 118.46f, 66.8316f to 155.498f, 81.091f to 136.717f, 83.3656f to 149.789f,
    96.5816f to 122.461f, 139.905f to 124.592f, 127.816f to 99.1792f, 135.397f to 96.405f, 118.745f to 77.4159f, 139.456f to 41.5434f,
    119.026f to 43.8908f, 122.268f to 31.6198f, 100.165f to 42.5356f, 92.3755f to 14.3614f,
)

private fun poligono(puntos: List<Pair<Float, Float>>, size: Size) = Path().apply {
    val sx = size.width / MARCO_ANCHO
    val sy = size.height / MARCO_ALTO
    puntos.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * sx, y * sy) else lineTo(x * sx, y * sy) }
    close()
}

private fun sombraMarco(size: Size) = Path().apply {
    val sx = size.width / MARCO_ANCHO
    val sy = size.height / MARCO_ALTO
    moveTo(102.996f * sx, 79.8232f * sy)
    cubicTo(103.088f * sx, 78.7912f * sy, 103.952f * sx, 78f * sy, 104.988f * sx, 78f * sy)
    lineTo(379.856f * sx, 78f * sy)
    cubicTo(380.934f * sx, 78f * sy, 381.819f * sx, 78.8556f * sy, 381.854f * sx, 79.9339f * sy)
    lineTo(397.914f * sx, 565.526f * sy)
    cubicTo(397.956f * sx, 566.799f * sy, 396.814f * sx, 567.787f * sy, 395.56f * sx, 567.561f * sy)
    lineTo(66.7948f * sx, 508.25f * sy)
    cubicTo(65.7763f * sx, 508.066f * sy, 65.0662f * sx, 507.136f * sy, 65.1577f * sx, 506.105f * sy)
    close()
}

private object FormaMarco : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(poligono(PuntosMarco, size))
}

/** Borde del marco: #0079BE. */
private val AzulMarco = Color(0xFF0079BE)

/**
 * El marco ilustrado de la escena: chispas y sombra negras, y la foto de fondo recortada al
 * cuadrilátero inclinado, con borde azul. [contenido] recibe el tamaño del marco. En iOS:
 * `MarcoEscenaView`.
 */
@Composable
private fun MarcoEscena(modifier: Modifier, contenido: @Composable (ancho: Dp, alto: Dp) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val relacion = MARCO_ANCHO / MARCO_ALTO
        val (ancho, alto) = if (maxWidth / maxHeight > relacion) maxHeight * relacion to maxHeight else maxWidth to maxWidth / relacion
        Box(Modifier.size(ancho, alto)) {
            Canvas(Modifier.fillMaxSize()) {
                drawPath(poligono(PuntosChispaDerecha, size), Color.Black)
                drawPath(poligono(PuntosChispaIzquierda, size), Color.Black)
                drawPath(sombraMarco(size), Color.Black)
            }
            Box(modifier.fillMaxSize().clip(FormaMarco)) {
                Image(painterResource(R.drawable.fondo_escena_m), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                contenido(ancho, alto)
            }
            Canvas(Modifier.fillMaxSize()) {
                drawPath(poligono(PuntosMarco, size), AzulMarco, style = Stroke(5f * size.width / MARCO_ANCHO))
            }
        }
    }
}
