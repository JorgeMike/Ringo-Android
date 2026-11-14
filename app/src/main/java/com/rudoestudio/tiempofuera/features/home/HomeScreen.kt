package com.rudoestudio.tiempofuera.features.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.color
import com.rudoestudio.tiempofuera.core.designsystem.colorObscuro
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorNivelLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.espiral
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.features.estadisticas.duracion
import com.rudoestudio.tiempofuera.features.misiones.MisionesLista
import java.time.Duration

/** Las dos secciones de abajo de Inicio. En iOS: `HomeView.SeccionHome`. */
private enum class SeccionInicio(val titulo: String) { METAS("Metas"), LIGAS("Ligas") }

/**
 * Tab Inicio (M5.1). En iOS: `HomeView`. Arriba el tiempo en pantalla de hoy, el luchador
 * equipado (entra en espiral y flota), su nivel y los días invicto; abajo, Metas y Ligas.
 */
@Composable
fun HomeScreen(
    alAbrirEstadisticas: () -> Unit,
    alAbrirRacha: () -> Unit,
    alAbrirMision: (Mision) -> Unit,
    alAbrirLiga: (ligaId: String) -> Unit,
    alAbrirTiendaMonedas: () -> Unit,
    alAbrirGaleria: () -> Unit,
    alAbrirLaboratorio: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescarUso() }
    ContenidoInicio(
        estado = estado,
        alAbrirEstadisticas = alAbrirEstadisticas,
        alAbrirRacha = alAbrirRacha,
        metas = { MisionesLista(alAbrirMision) },
        herramientas = if (BuildConfig.DEBUG) {
            { HerramientasDesarrollo(alAbrirLiga, alAbrirTiendaMonedas, alAbrirGaleria, alAbrirLaboratorio) }
        } else {
            null
        },
    )
}

@Composable
private fun ContenidoInicio(
    estado: EstadoInicio,
    alAbrirEstadisticas: () -> Unit,
    alAbrirRacha: () -> Unit,
    metas: @Composable () -> Unit,
    herramientas: (@Composable () -> Unit)?,
) {
    var seccion by rememberSaveable { mutableStateOf(SeccionInicio.METAS) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Celeste)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(bottom = 24.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Rayos(Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TarjetaTiempoPantalla(estado.tiempoPantalla, alAbrirEstadisticas, Modifier.fillMaxWidth().padding(start = 32.dp))
                LuchadorInicio(estado)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    estado.luchador?.let { l ->
                        IndicadorNivelLuchador(l.nivel, estado.progresoNivel, l.rareza.color, l.rareza.colorObscuro, nombre = l.nombre)
                    }
                    InsigniaRacha(estado.diasInvicto, alAbrirRacha)
                    // En iOS el tipo todavía es fijo (TipoRocaIcon), sin acción.
                    Box(contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.placeholder_tipo), null)
                        Image(painterResource(R.drawable.tipo_roca_icon), null, Modifier.offset(y = (-10).dp))
                    }
                }
            }
        }

        SelectorSeccion(seccion, alCambiar = { seccion = it }, Modifier.padding(horizontal = 32.dp, vertical = 10.dp))
        AnimatedContent(seccion, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "seccion") { s ->
            when (s) {
                SeccionInicio.METAS -> metas()
                // M9.1: aquí va `LigasListView`.
                SeccionInicio.LIGAS -> SeccionPendiente("Las ligas se están armando.", "M9.1")
            }
        }
        herramientas?.invoke()
    }
}

/**
 * Los rayos de fondo, que se desvanecen hacia abajo. En iOS:
 * `Image("Rayos")` con máscara de degradado.
 */
@Composable
private fun Rayos(modifier: Modifier) {
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .requiredSize(560.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, 0.66f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }
}

/** "Tiempo en pantalla" con el uso de hoy; abre las estadísticas. */
@Composable
private fun TarjetaTiempoPantalla(tiempo: TiempoPantallaInicio, alTocar: () -> Unit, modifier: Modifier) {
    val texto = when (tiempo) {
        TiempoPantallaInicio.Cargando -> "…"
        TiempoPantallaInicio.SinPermiso -> "Autoriza Tiempo en pantalla"
        TiempoPantallaInicio.SinApps -> "Selecciona apps..."
        is TiempoPantallaInicio.Uso -> duracion(tiempo.duracion.seconds)
    }
    Column(
        modifier
            .clickable(onClick = alTocar)
            .semantics(mergeDescendants = true) { contentDescription = "Tiempo en pantalla: $texto. Toca para ver las estadísticas." },
    ) {
        TextoConSombra("Tiempo en pantalla", TextStyle(fontFamily = PirataOne, fontSize = 22.sp), colorSombra = Color.Black)
        Box(contentAlignment = Alignment.CenterStart) {
            Image(painterResource(R.drawable.tiempo_fondo), null, Modifier.offset(x = (-2).dp).graphicsLayer { scaleX = 1.15f; scaleY = 1.15f })
            TextoConSombra(
                texto,
                TextStyle(fontFamily = PirataOne, fontSize = if (tiempo is TiempoPantallaInicio.Uso) 24.sp else 16.sp),
                colorSombra = Color.Black,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** El luchador equipado: entra girando en espiral y después flota. */
@Composable
private fun LuchadorInicio(estado: EstadoInicio) {
    val entrada = remember { Animatable(0f) }
    val flote = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrada.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessVeryLow))
        flote.animateTo(1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    }
    val imagen = estado.luchador?.let { AssetsIos.imagen(it.imagen) ?: AssetsIos.imagen(it.nombre) } ?: R.drawable.ringo
    ImagenPixel(
        imagen,
        220.dp,
        Modifier
            .espiral(entrada.value)
            .alpha(entrada.value.coerceIn(0f, 1f))
            .offset { IntOffset(0, (8 - flote.value * 16).dp.roundToPx()) },
        descripcion = estado.luchador?.nombre,
    )
}

/** Días invicto sobre su placa; abre "Días invicto". */
@Composable
private fun InsigniaRacha(dias: Int, alTocar: () -> Unit) {
    Box(
        Modifier
            .clickable(onClick = alTocar)
            .semantics(mergeDescendants = true) { contentDescription = "$dias días invicto. Toca para ver las estadísticas." },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.placeholder_racha), null)
        Image(painterResource(R.drawable.racha_icon), null, Modifier.offset(y = (-22).dp))
        TextoContorno(
            "$dias",
            TextStyle(fontFamily = PirataOne, fontSize = 28.sp),
            ColoresTF.Blanco, Color.Black, 4.dp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).offset(y = 12.dp).rotate(-4f),
        )
    }
}

/** Metas | Ligas con su fondo chueco. En iOS: el switcher de `HomeView`. */
@Composable
private fun SelectorSeccion(actual: SeccionInicio, alCambiar: (SeccionInicio) -> Unit, modifier: Modifier) {
    val izquierda by animateFloatAsState(if (actual == SeccionInicio.METAS) 1f else 0f, label = "switcher")
    Box(modifier.size(186.dp, 44.dp)) {
        Image(painterResource(R.drawable.switcher_home_izquierda), null, Modifier.fillMaxSize().alpha(izquierda))
        Image(painterResource(R.drawable.switcher_home_derecha), null, Modifier.fillMaxSize().alpha(1f - izquierda))
        Row(Modifier.fillMaxSize()) {
            SeccionInicio.entries.forEach { s ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(end = if (s == SeccionInicio.LIGAS) 8.dp else 0.dp)
                        .clickable { alCambiar(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s.titulo, fontFamily = PirataOne, fontSize = 24.sp, color = ColoresTF.Blanco.copy(alpha = if (s == actual) 1f else 0.5f))
                }
            }
        }
    }
}

@Composable
private fun SeccionPendiente(texto: String, tarea: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(texto, fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColoresTF.Blanco, textAlign = TextAlign.Center)
        Text("Próximamente · $tarea", fontFamily = Arvo, fontSize = 12.sp, color = ColoresTF.Blanco.copy(alpha = 0.7f))
    }
}

/** Accesos a las herramientas de desarrollo. Solo salen en debug: la versión de Play no los trae. */
@Composable
private fun HerramientasDesarrollo(
    alAbrirLiga: (String) -> Unit,
    alAbrirTiendaMonedas: () -> Unit,
    alAbrirGaleria: () -> Unit,
    alAbrirLaboratorio: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Herramientas de desarrollo", fontFamily = Arvo, fontSize = 12.sp, color = ColoresTF.Blanco.copy(alpha = 0.7f))
        listOf(
            "Laboratorio de API" to alAbrirLaboratorio,
            "Galería de componentes" to alAbrirGaleria,
            "Tienda de monedas" to alAbrirTiendaMonedas,
            "Ver una liga" to { alAbrirLiga("liga-de-prueba") },
        ).forEach { (texto, accion) ->
            OutlinedButton(onClick = accion) { Text(texto, color = ColoresTF.Blanco) }
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun InicioPreview() = TiempoFueraTheme {
    ContenidoInicio(
        EstadoInicio(DatosPreview.luchadorSeleccionado, 0.4f, 12, TiempoPantallaInicio.Uso(Duration.ofMinutes(95))),
        {}, {}, metas = {}, herramientas = null,
    )
}
