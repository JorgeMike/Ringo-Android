package com.rudoestudio.tiempofuera.features.ligas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.BuildConfig
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PantallaPendiente
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BarraProgresoParalelogramo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.domain.ligas.NivelLiga
import com.rudoestudio.tiempofuera.domain.ligas.TipoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.EstadoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.features.misiones.MisionCard
import kotlin.math.ceil
import kotlin.math.roundToInt

private val ArvoBold = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold)

private fun imagenDe(luchador: Luchador?): Int =
    luchador?.let { AssetsIos.imagen(it.imagen) ?: AssetsIos.imagen(it.nombre) } ?: R.drawable.ringo

/**
 * El camino de niveles de una liga (M9.2): la experiencia arriba, los niveles de abajo
 * hacia arriba sobre el camino, centrado en el nivel actual, y las misiones de la liga en
 * una bandeja abajo. En iOS: `LigaNivelesView`.
 */
@Composable
fun LigaNivelesScreen(
    alVolver: () -> Unit,
    alAbrirMision: (Mision) -> Unit,
    viewModel: LigaNivelesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescarMisiones() }
    var barrera by remember { mutableStateOf<NivelLiga?>(null) }

    Box(Modifier.fillMaxSize().background(estado.paleta.fondoOscuro)) {
        if (estado.cargando) {
            CircularProgressIndicator(color = ColoresTF.Blanco, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonRegresar(alVolver)
                    TextoContorno(estado.liga?.nombre.orEmpty(), TextStyle(fontFamily = PirataOne, fontSize = 26.sp), ColoresTF.Blanco, Color.Black, 2.dp, Modifier.padding(start = 8.dp), maxLineas = 1)
                }
                if (BuildConfig.DEBUG) BarraPruebas(viewModel::sumarExpDePrueba)
                EncabezadoExp(estado)
                Camino(estado, Modifier.weight(1f), alCobrar = viewModel::cobrar, alPelear = { barrera = it })
                BandejaMisiones(estado.paleta, estado.misiones, alAbrirMision)
            }
        }
    }

    // M9.4: aquí va el minijuego de la barrera (`MinijuegoBarreraView`).
    barrera?.let { nivel ->
        PeleaPendiente(alGanar = { viewModel.ganarBarrera(nivel); barrera = null }, alCerrar = { barrera = null })
    }

    estado.error?.let {
        AlertDialog(
            onDismissRequest = viewModel::limpiarError,
            confirmButton = { TextButton(onClick = viewModel::limpiarError) { Text("OK") } },
            title = { Text("Algo salió mal") },
            text = { Text(it) },
        )
    }
}

/** En iOS: `LigaExpHeaderView`. */
@Composable
private fun EncabezadoExp(estado: EstadoLiga) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡ ${estado.expTotal} exp", style = ArvoBold.copy(fontSize = 16.sp), color = estado.paleta.acento)
            Spacer(Modifier.weight(1f))
            val siguiente = estado.expSiguiente
            Text(
                if (siguiente == null) "¡Liga completada!" else "Faltan ${(siguiente - estado.expTotal).coerceAtLeast(0.0).toInt()} exp",
                style = ArvoBold.copy(fontSize = 12.sp), color = ColoresTF.Blanco.copy(alpha = 0.7f),
            )
        }
        BarraProgresoParalelogramo(estado.avance, estado.paleta.acento, Modifier.fillMaxWidth().height(20.dp))
    }
}

/** "+5 exp" y "+200 exp" para probar (solo debug). En iOS: `TestingBarView`. */
@Composable
private fun BarraPruebas(alSumar: (Double) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🔨 Pruebas", style = ArvoBold.copy(fontSize = 12.sp), color = ColoresTF.Blanco.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { alSumar(5.0) }) { Text("+5 exp", color = ColoresTF.Blanco) }
        OutlinedButton(onClick = { alSumar(200.0) }) { Text("+200 exp", color = ColoresTF.Blanco) }
    }
}

/**
 * Los niveles de abajo (1) hacia arriba sobre el camino. Al abrir y al avanzar se centra
 * en el nivel actual.
 */
@Composable
private fun Camino(estado: EstadoLiga, modifier: Modifier, alCobrar: (NivelLiga) -> Unit, alPelear: (NivelLiga) -> Unit) {
    val desplazamiento = rememberScrollState()
    val posiciones = remember { mutableStateMapOf<Int, Pair<Float, Int>>() }
    val actual = estado.progreso?.nivelActualId
    var listo by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(estado.paleta.fondoScroll)
            .clipToBounds(),
    ) {
        val alto = constraints.maxHeight
        Image(painterResource(R.drawable.fondo_ligas), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().alpha(0.5f).align(Alignment.Center))
        LaunchedEffect(actual, estado.visibles.size, posiciones[actual]) {
            val (y, h) = posiciones[actual] ?: return@LaunchedEffect
            val destino = (y + h / 2f - alto / 2f).roundToInt().coerceAtLeast(0)
            if (listo) desplazamiento.animateScrollTo(destino) else desplazamiento.scrollTo(destino)
            listo = true
        }
        Column(
            Modifier
                .fillMaxWidth()
                .alpha(if (listo || posiciones.isEmpty()) 1f else 0f)
                .verticalScroll(desplazamiento)
                .heightIn(min = maxHeight)
                .padding(top = 44.dp, bottom = 16.dp)
                .drawCamino(estado.paleta.camino),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            estado.visibles.reversed().forEach { nivel ->
                FilaNivel(
                    nivel, estado.estado(nivel), estado.luchador,
                    Modifier.onGloballyPositioned { posiciones[nivel.id] = it.positionInParent().y to it.size.height },
                    alCobrar = { alCobrar(nivel) },
                    alPelear = { alPelear(nivel) },
                )
            }
        }
        // Bordes negros y la franja chueca de arriba, como en iOS.
        Box(Modifier.fillMaxHeight().width(5.dp).background(Color.Black).align(Alignment.CenterStart))
        Box(Modifier.fillMaxHeight().width(5.dp).background(Color.Black).align(Alignment.CenterEnd))
        Box(Modifier.fillMaxWidth().height(60.dp).offset(y = (-20).dp).rotate(-4f).graphicsLayer { scaleX = 1.2f }.background(Color.Black))
        Box(Modifier.fillMaxWidth().height(60.dp).offset(y = (-30).dp).rotate(-4f).graphicsLayer { scaleX = 1.2f }.background(estado.paleta.fondoOscuro))
    }
}

// El camino del SVG (lienzo 320x411.5), que se repite hacia abajo. En iOS: `CaminoShape`.
private const val CAMINO_ANCHO = 320f
private const val CAMINO_ALTO = 411.5f
private val PuntosCamino = listOf(
    183.5f to 0f, 87.5667f to 71.7401f, 245.789f to 121.151f, 246.077f to 121.241f, 319.16f to 146.258f,
    183.131f to 205.05f, 270.334f to 215.465f, 290.037f to 263.426f, 78.3688f to 291.672f, 55.1293f to 331.619f,
    270.334f to 331.619f, 183.5f to 411.5f, 134.158f to 411.5f, 200.111f to 358.148f, 0f to 358.148f,
    52.3454f to 268.172f, 243.353f to 242.681f, 241.818f to 238.945f, 78.4109f to 219.43f, 241.414f to 148.979f,
    231.052f to 145.431f, 23.6085f to 80.6501f, 134f to 0f,
)

private fun caminoPath(puntos: List<Pair<Float, Float>>, escala: Float, cerrar: Boolean) = Path().apply {
    puntos.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * escala, y * escala) else lineTo(x * escala, y * escala) }
    if (cerrar) close()
}

/** Dibuja el camino repetido detrás de los niveles. En iOS: `CaminoNivelesView`. */
private fun Modifier.drawCamino(color: Color): Modifier = drawBehind {
    val trazo = 4.dp.toPx()
    val escala = size.width / (CAMINO_ANCHO + 4)
    val altoTile = CAMINO_ALTO * escala
    val relleno = caminoPath(PuntosCamino, escala, cerrar = true)
    val derecho = caminoPath(PuntosCamino.subList(0, 12), escala, cerrar = false)
    val izquierdo = caminoPath(PuntosCamino.subList(12, PuntosCamino.size), escala, cerrar = false)
    val cantidad = ceil(size.height / altoTile).toInt() + 1
    val borde = Stroke(trazo * escala, join = StrokeJoin.Round)
    repeat(cantidad) { i -> translate(0f, i * altoTile) { drawPath(relleno, color) } }
    repeat(cantidad) { i ->
        translate(0f, i * altoTile) {
            drawPath(derecho, Color.White.copy(alpha = 0.6f), style = borde)
            drawPath(izquierdo, Color.White.copy(alpha = 0.6f), style = borde)
        }
    }
}

/** Un nivel: placa con número, experiencia y recompensa, o la pelea si es barrera. En iOS: `NivelRowView`. */
@Composable
private fun FilaNivel(nivel: NivelLiga, estado: EstadoNivel, luchador: Luchador?, modifier: Modifier, alCobrar: () -> Unit, alPelear: () -> Unit) {
    val haptics = LocalHaptics.current
    val par = nivel.id % 2 == 0
    val barrera = nivel.tipo == TipoNivel.BARRERA
    val actual = estado == EstadoNivel.ACTUAL
    Column(modifier.offset(x = if (barrera) 0.dp else if (par) (-32).dp else 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!barrera) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!par && actual) ImagenPixel(imagenDe(luchador), 94.dp)
                PlacaNivel(nivel, estado, par)
                if (par && actual) ImagenPixel(imagenDe(luchador), 94.dp)
            }
        } else {
            PeleaBarrera(luchador, nivel, bloqueado = estado == EstadoNivel.BLOQUEADO, Modifier.padding(top = 4.dp))
        }
        Box(Modifier.offset(y = (-14).dp).rotate(if (par) 3f else -3f).graphicsLayer { scaleX = 0.6f; scaleY = 0.6f }) {
            when (estado) {
                EstadoNivel.BARRERA_PENDIENTE -> BotonL(ColoresTF.Rojo, "A PELEAR", alTocar = alPelear)
                EstadoNivel.RECOMPENSA_PENDIENTE -> BotonL(ColoresTF.Verde, "RECLAMAR", alTocar = { haptics.impacto(EstiloImpacto.MEDIO); alCobrar() })
                else -> Unit
            }
        }
    }
}

@Composable
private fun PlacaNivel(nivel: NivelLiga, estado: EstadoNivel, par: Boolean) {
    val fondo = when {
        estado == EstadoNivel.ACTUAL && par -> R.drawable.nivel_badge_actual_par
        estado == EstadoNivel.ACTUAL -> R.drawable.nivel_badge_actual_impar
        par -> R.drawable.nivel_badge_par
        else -> R.drawable.nivel_badge_impar
    }
    val pirata = TextStyle(fontFamily = PirataOne)
    Box(Modifier.alpha(if (estado == EstadoNivel.BLOQUEADO) 0.55f else 1f), contentAlignment = Alignment.Center) {
        Image(painterResource(fondo), null)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.offset(x = if (par) (-28).dp else 28.dp, y = 14.dp).rotate(if (par) 6f else -6f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TextoContorno("Nivel ${nivel.id}", pirata.copy(fontSize = if (par) 32.sp else 24.sp), ColoresTF.Blanco, Color.Black, 2.dp, maxLineas = 1)
                TextoContorno("${nivel.expRequerida.toInt()} exp", pirata.copy(fontSize = if (par) 24.sp else 32.sp), ColoresTF.Blanco, Color.Black, 2.dp, maxLineas = 1)
            }
            Row(Modifier.offset(x = if (par) 32.dp else (-46).dp, y = (-6).dp), verticalAlignment = Alignment.CenterVertically) {
                if (nivel.recompensa.puntosPoder > 0) ValorPremio(R.drawable.puntos_poder_icon, nivel.recompensa.puntosPoder)
                if (nivel.recompensa.monedas > 0) ValorPremio(R.drawable.moneda_icon, nivel.recompensa.monedas)
            }
        }
    }
}

@Composable
private fun ValorPremio(icono: Int, valor: Int) {
    Image(painterResource(icono), null, Modifier.size(28.dp))
    TextoContorno("$valor", TextStyle(fontFamily = PirataOne, fontSize = 22.sp), Color.Black, ColoresTF.Blanco, 3.dp, Modifier.padding(end = 6.dp))
}

/** Tu luchador contra el de la barrera. En iOS: `PeleaBarreraView`. */
@Composable
private fun PeleaBarrera(luchador: Luchador?, nivel: NivelLiga, bloqueado: Boolean, modifier: Modifier) {
    val rival = nivel.luchadorBarrera
    Box(modifier.fillMaxWidth().aspectRatio(393f / 144f).alpha(if (bloqueado) 0.55f else 1f), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.fondo_pelea), null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize())
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ImagenPixel(imagenDe(luchador), 110.dp, Modifier.offset(y = (-20).dp))
            TextoContorno("VS", TextStyle(fontFamily = PirataOne, fontSize = 64.sp), ColoresTF.Blanco, Color.Black, 4.dp, Modifier.rotate(8f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ImagenPixel(imagenDe(rival), 110.dp, Modifier.offset(y = 20.dp))
                Text(
                    (nivel.tipoLuchadorBarrera ?: rival?.tipo)?.clave?.uppercase().orEmpty(),
                    style = ArvoBold.copy(fontSize = 10.sp), color = ColoresTF.Blanco,
                    modifier = Modifier.background(ColoresTF.Azul.copy(alpha = 0.9f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Las misiones de la liga, en una bandeja que se abre. En iOS: `MisionesDiariasView`. */
@Composable
private fun BandejaMisiones(paleta: PaletaLiga, misiones: List<Mision>, alAbrirMision: (Mision) -> Unit) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    val giro by animateFloatAsState(if (abierta) 180f else 0f, label = "flecha")
    Column(
        Modifier
            .fillMaxWidth()
            .background(paleta.fondoOscuro, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding()
            .animateContentSize(),
    ) {
        Row(Modifier.fillMaxWidth().clickable { abierta = !abierta }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextoContorno("Misiones diarias", TextStyle(fontFamily = PirataOne, fontSize = 22.sp), Color.Black, ColoresTF.Blanco, 2.dp, Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowUp, if (abierta) "Cerrar" else "Abrir", tint = ColoresTF.Blanco, modifier = Modifier.rotate(giro))
        }
        AnimatedVisibility(abierta) {
            Column(
                Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                misiones.forEach { m -> MisionCard(m, completada = m.completada, bloqueada = false, alSeleccionar = { alAbrirMision(m) }) }
            }
        }
    }
}

/** M9.4: mientras llega el minijuego, la pelea se gana con un botón en debug. */
@Composable
private fun PeleaPendiente(alGanar: () -> Unit, alCerrar: () -> Unit) {
    Box(Modifier.fillMaxSize().clickable(enabled = false) {}) {
        PantallaPendiente(titulo = "¡A pelear!", tarea = "M9.4", fondo = ColoresTF.Rojo) {
            if (BuildConfig.DEBUG) BotonL(ColoresTF.Amarillo, "GANAR (PRUEBA)", alTocar = alGanar)
            BotonL(ColoresTF.Celeste, "REGRESAR", alTocar = alCerrar)
        }
    }
}
