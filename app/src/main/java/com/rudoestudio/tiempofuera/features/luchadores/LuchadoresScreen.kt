package com.rudoestudio.tiempofuera.features.luchadores

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.color
import com.rudoestudio.tiempofuera.core.designsystem.colorObscuro
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.FondoLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorNivelLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.RayosDesvanecidos
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import kotlinx.coroutines.launch

private val ArvoBold = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold)

private fun imagenDe(luchador: Luchador): Int =
    AssetsIos.imagen(luchador.imagen) ?: AssetsIos.imagen(luchador.nombre) ?: R.drawable.ringo

/**
 * Tab Luchadores (M8.1): tus puntos de poder, el luchador equipado flotando con su nivel
 * y "Subir de nivel", y la rejilla de los que ya tienes para cambiarlo. En iOS:
 * `LuchadoresView`.
 */
@Composable
fun LuchadoresScreen(viewModel: LuchadoresViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenidoLuchadores(estado, alEquipar = viewModel::equipar, alSubirNivel = viewModel::subirNivel)
}

@Composable
private fun ContenidoLuchadores(estado: EstadoLuchadores, alEquipar: (Luchador) -> Unit, alSubirNivel: (Luchador) -> Unit) {
    val desplazamiento = rememberScrollState()
    val alcance = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Celeste)
            .verticalScroll(desplazamiento)
            .safeDrawingPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
            InsigniaPuntos(estado.puntosPoder)
        }
        estado.equipado?.let { Equipado(it, alSubirNivel) }
        TextoContorno("Cambia tu luchador", ArvoBold.copy(fontSize = 16.sp), ColoresTF.Blanco, Color.Black, 2.dp, Modifier.padding(top = 48.dp))
        BannerError(estado.error, Modifier.padding(16.dp))
        // Tres por fila. Al elegir uno se regresa arriba para verlo equipado.
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            estado.desbloqueados.chunked(3).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    fila.forEach { l ->
                        TarjetaColeccion(l, Modifier.weight(1f)) {
                            alEquipar(l.luchador)
                            alcance.launch { desplazamiento.animateScrollTo(0) }
                        }
                    }
                    repeat(3 - fila.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun InsigniaPuntos(puntos: Int) {
    val forma = DiagonalShape(11.dp)
    Row(
        Modifier
            .shadow(4.dp, forma)
            .background(ColoresTF.Blanco, forma)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$puntos puntos de poder" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.puntos_poder), null, Modifier.size(24.dp))
        Text("$puntos", style = ArvoBold.copy(fontSize = 17.sp), color = ColoresTF.Azul, maxLines = 1)
    }
}

/** El luchador equipado, grande y flotando sobre los rayos. */
@Composable
private fun Equipado(e: LuchadorEnColeccion, alSubirNivel: (Luchador) -> Unit) {
    val l = e.luchador
    val flote = rememberInfiniteTransition(label = "equipado")
    val y by flote.animateFloat(8f, -8f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flote")
    Box(Modifier.fillMaxWidth().padding(top = 32.dp)) {
        RayosDesvanecidos(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TextoContorno(l.nombre, TextStyle(fontFamily = PirataOne, fontSize = 42.sp), ColoresTF.Blanco, Color.Black, 4.dp)
            ImagenPixel(imagenDe(l), 170.dp, Modifier.offset { IntOffset(0, y.dp.roundToPx()) }, descripcion = l.nombre)
            IndicadorNivelLuchador(
                l.nivel, e.progreso, l.rareza.color, l.rareza.colorObscuro,
                muestraNombre = false, muestraIndicadorSubirNivel = e.puedeSubir,
            )
            if (e.puedeSubir) {
                BotonL(
                    ColoresTF.Amarillo, "SUBIR DE NIVEL",
                    alTocar = { alSubirNivel(l) },
                    modifier = Modifier.padding(top = 12.dp).graphicsLayer { scaleX = 0.8f; scaleY = 0.8f },
                )
            }
        }
    }
}

/** En iOS: `LuchadorCardView`. */
@Composable
private fun TarjetaColeccion(e: LuchadorEnColeccion, modifier: Modifier, alTocar: () -> Unit) {
    val l = e.luchador
    Box(
        modifier
            .aspectRatio(0.82f)
            .clickable(role = Role.Button, onClickLabel = "Equipar", onClick = alTocar)
            .semantics(mergeDescendants = true) { contentDescription = "${l.nombre}, nivel ${l.nivel}" },
        contentAlignment = Alignment.Center,
    ) {
        FondoLuchador(l.rareza.color, l.rareza.colorObscuro, Modifier.fillMaxSize())
        ImagenPixel(imagenDe(l), 86.dp)
        TextoContorno(
            l.nombre, ArvoBold.copy(fontSize = 13.sp), ColoresTF.Blanco, Color.Black, 2.dp,
            Modifier.align(Alignment.TopCenter).padding(top = 6.dp).rotate(-10f), maxLineas = 1,
        )
        IndicadorNivelLuchador(
            l.nivel, e.progreso, l.rareza.color, l.rareza.colorObscuro,
            muestraNombre = false, muestraIndicadorSubirNivel = e.puedeSubir,
            modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { scaleX = 0.6f; scaleY = 0.6f },
        )
    }
}

@Preview(heightDp = 1000)
@Composable
private fun LuchadoresPreview() = TiempoFueraTheme {
    val todos = DatosPreview.luchadores.filter { it.cantidad > 0 }.map { LuchadorEnColeccion(it, 0.4f, it.id % 2 == 0) }
    ContenidoLuchadores(EstadoLuchadores(cargando = false, puntosPoder = 120, equipado = todos.first(), desbloqueados = todos), {}, {})
}
