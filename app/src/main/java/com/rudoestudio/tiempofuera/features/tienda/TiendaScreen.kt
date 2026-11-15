package com.rudoestudio.tiempofuera.features.tienda

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.color
import com.rudoestudio.tiempofuera.core.designsystem.colorObscuro
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoEnParalelogramo
import com.rudoestudio.tiempofuera.domain.tienda.PinataTienda
import com.rudoestudio.tiempofuera.domain.tienda.ResultadoCompraTiempoExtra
import com.rudoestudio.tiempofuera.domain.tienda.SobreTienda
import com.rudoestudio.tiempofuera.domain.tienda.TiempoExtra

private val ArvoBold = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold)

/**
 * Tab Tienda: la tiendita (M10.1). Arriba tus monedas; si ya llegaste a tu límite de hoy,
 * tiempo extra; luego los sobres por rareza y la piñata. En iOS: `TiendaView`.
 */
@Composable
fun TiendaScreen(viewModel: TiendaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.actualizarGrupoBloqueado() }
    ContenidoTienda(
        estado,
        alComprarSobre = viewModel::comprarSobre,
        alComprarPinata = viewModel::comprarPinata,
        alPedirTiempoExtra = viewModel::pedirTiempoExtra,
    )

    estado.confirmacion?.let { c ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarTiempoExtra,
            title = { Text("¿Comprar ${c.opcion.nombre}?") },
            text = { Text(c.advertencia) },
            confirmButton = { TextButton(onClick = viewModel::confirmarTiempoExtra) { Text(c.titulo, color = ColoresTF.RojoError) } },
            dismissButton = { TextButton(onClick = viewModel::cancelarTiempoExtra) { Text("Cancelar") } },
        )
    }
    estado.resultadoTiempoExtra?.let { r ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarResultado,
            title = { Text("Tiempo extra listo") },
            text = {
                Text(
                    when (r) {
                        ResultadoCompraTiempoExtra.PROTECTOR_CONSUMIDO -> "Se fue 1 protector de racha, pero tu racha sigue viva."
                        ResultadoCompraTiempoExtra.RACHA_PERDIDA -> "Ya tienes tu tiempo extra. Tu racha se reinició."
                    },
                )
            },
            confirmButton = { TextButton(onClick = viewModel::cerrarResultado) { Text("Continuar") } },
        )
    }
}

@Composable
private fun ContenidoTienda(
    estado: EstadoTienda,
    alComprarSobre: (SobreTienda) -> Unit,
    alComprarPinata: () -> Unit,
    alPedirTiempoExtra: (TiempoExtra) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(ColoresTF.Celeste).clipToBounds()) {
        // Rayos que se desvanecen hacia abajo.
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f; compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(0f to Color.Black, 0.4f to Color.Black.copy(alpha = 0.8f), 0.75f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Encabezado(estado.monedas)
            if (estado.muestraTiempoExtra) SeccionTiempoExtra(estado, alPedirTiempoExtra)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloSeccion("Sobres", "Descubre nuevos luchadores")
                estado.sobres.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        fila.forEach { s ->
                            TarjetaSobre(s, estado.puedeComprar(s.precioEnergia), Modifier.weight(1f)) { alComprarSobre(s) }
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TituloSeccion("Piñata sorpresa", "¡Un golpe, cinco sobres!")
                TarjetaPinata(estado.pinata, estado.puedeComprar(estado.pinata.precioEnergia), alComprarPinata)
            }
            estado.error?.let { AvisoTienda(it, ColoresTF.Rojo) }
        }
    }
}

@Composable
private fun Encabezado(monedas: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextoConSombra("La tiendita", TextStyle(fontFamily = PirataOne, fontSize = 40.sp), colorSombra = ColoresTF.Azul, grosor = 2.dp)
        Spacer(Modifier.weight(1f))
        val forma = DiagonalShape(11.dp)
        Row(
            Modifier
                .shadow(4.dp, forma)
                .background(ColoresTF.Blanco, forma)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$monedas monedas" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.moneda_icon), null, Modifier.size(24.dp))
            Text("$monedas", style = ArvoBold.copy(fontSize = 17.sp), color = ColoresTF.Azul, maxLines = 1)
        }
    }
}

@Composable
private fun TituloSeccion(titulo: String, subtitulo: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        TextoEnParalelogramo(titulo.uppercase(), colorFondo = ColoresTF.Azul, colorSombra = Color.Black, tamanoFuente = 24.sp)
        TextoContorno(subtitulo, ArvoBold.copy(fontSize = 12.sp), ColoresTF.Blanco, Color.Black, 1.dp, Modifier.padding(start = 12.dp, top = 6.dp))
    }
}

/** Fondo de tarjeta: forma diagonal con contorno negro y sombra dura abajo a la derecha. */
private fun Modifier.fondoTarjeta(color: Color, inclinacion: Dp, sombra: Dp = 5.dp): Modifier {
    val forma = DiagonalShape(inclinacion)
    return this
        .drawWithContent {
            translate(sombra.toPx(), sombra.toPx() + 1.dp.toPx()) {
                drawOutline(forma.createOutline(size, layoutDirection, this), Color.Black)
            }
            drawContent()
        }
        .background(color, forma)
        .border(2.dp, Color.Black, forma)
}

@Composable
private fun Moneda(precio: Int, tamano: Dp, estilo: TextStyle, texto: String = "$precio") {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.moneda_icon), null, Modifier.size(tamano))
        Text(texto, style = estilo, color = ColoresTF.Blanco, maxLines = 1)
    }
}

/** `BotonL` encogido a [escala], como el `BotonTiendaCompacto` de iOS. */
@Composable
private fun BotonCompacto(color: Color, texto: String, escala: Float, habilitado: Boolean, etiqueta: String, alTocar: () -> Unit) {
    Box(
        Modifier
            .size((283 * escala).dp, (66 * escala).dp)
            .alpha(if (habilitado) 1f else 0.55f)
            .semantics(mergeDescendants = true) { contentDescription = etiqueta },
        contentAlignment = Alignment.Center,
    ) {
        BotonL(
            color, texto, alTocar = alTocar, habilitado = habilitado, colorInactivo = color,
            modifier = Modifier.requiredSize(283.dp, 66.dp).graphicsLayer { scaleX = escala; scaleY = escala },
        )
    }
}

@Composable
private fun TarjetaSobre(sobre: SobreTienda, puedeComprar: Boolean, modifier: Modifier, alComprar: () -> Unit) {
    val r = sobre.rareza
    Column(
        modifier.fondoTarjeta(r.colorObscuro, 12.dp).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(116.dp).background(r.colorObscuro, DiagonalShape(12.dp)).border(2.dp, Color.Black, DiagonalShape(12.dp)), contentAlignment = Alignment.Center) {
            Image(
                painterResource(AssetsIos.imagen(r.imagenSobre) ?: R.drawable.pack_comun), null,
                Modifier.padding(8.dp).height(100.dp),
                contentScale = ContentScale.Fit,
            )
        }
        TextoConSombra("Sobre ${r.nombreVisible.lowercase()}", TextStyle(fontFamily = PirataOne, fontSize = 18.sp), colorSombra = ColoresTF.Azul, grosor = 1.dp)
        Text(
            "Trae un luchador ${r.nombreVisible.lowercase()}.",
            style = ArvoBold.copy(fontSize = 10.sp), color = ColoresTF.Blanco, textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp),
        )
        Box(Modifier.background(ColoresTF.Azul, DiagonalShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Moneda(sobre.precioEnergia, 19.dp, ArvoBold.copy(fontSize = 14.sp))
        }
        BotonCompacto(r.color, "COMPRAR", 0.5f, puedeComprar, "Comprar sobre ${r.nombreVisible} por ${sobre.precioEnergia} monedas", alComprar)
    }
}

@Composable
private fun TarjetaPinata(pinata: PinataTienda, puedeComprar: Boolean, alComprar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().fondoTarjeta(ColoresTF.Naranja, 14.dp).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImagenPixel(R.drawable.pinata, 88.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TextoConSombra(pinata.nombre, TextStyle(fontFamily = PirataOne, fontSize = 19.sp), colorSombra = ColoresTF.Azul, grosor = 1.dp)
            Text("${pinata.numeroSobres} sobres sorpresa", style = ArvoBold.copy(fontSize = 12.sp), color = ColoresTF.Blanco)
            Text(pinata.descripcion, style = ArvoBold.copy(fontSize = 10.sp), color = ColoresTF.Blanco)
            Moneda(pinata.precioEnergia, 19.dp, ArvoBold.copy(fontSize = 12.sp), "${pinata.precioEnergia} monedas")
            BotonCompacto(ColoresTF.Amarillo, "ROMPER PIÑATA", 0.52f, puedeComprar, "Comprar piñata por ${pinata.precioEnergia} monedas", alComprar)
        }
    }
}

@Composable
private fun SeccionTiempoExtra(estado: EstadoTienda, alPedir: (TiempoExtra) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion("Tiempo extra", "Échale más minutos a tus apps")
        if (estado.tiempoExtraActivo) {
            AvisoTienda("Ya tienes tiempo extra activo. Espera a que se acabe para comprar más.", ColoresTF.Azul)
        } else {
            AvisoTienda("Llegaste a tu límite de hoy. Si de plano lo necesitas, compra tiempo extra.", ColoresTF.Azul)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                estado.tiemposExtra.forEach { o ->
                    TarjetaTiempoExtra(o, estado.puedeComprar(o), Modifier.weight(1f)) { alPedir(o) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaTiempoExtra(opcion: TiempoExtra, puedeComprar: Boolean, modifier: Modifier, alComprar: () -> Unit) {
    Column(
        modifier.fondoTarjeta(ColoresTF.Azul, 10.dp, sombra = 4.dp).padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⏱", fontSize = 28.sp, modifier = Modifier.height(42.dp))
        Text(opcion.nombre, style = ArvoBold.copy(fontSize = 12.sp), color = ColoresTF.Blanco, textAlign = TextAlign.Center)
        Text(
            opcion.descripcion, style = ArvoBold.copy(fontSize = 9.sp), color = ColoresTF.Blanco, textAlign = TextAlign.Center, maxLines = 3,
            modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
        )
        Moneda(opcion.precioEnergia, 18.dp, ArvoBold.copy(fontSize = 13.sp))
        BotonCompacto(ColoresTF.Amarillo, "COMPRAR", 0.34f, puedeComprar, "Comprar ${opcion.nombre} por ${opcion.precioEnergia} monedas", alComprar)
    }
}

@Composable
private fun AvisoTienda(mensaje: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().fondoTarjeta(color, 12.dp, sombra = 4.dp).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ℹ️", fontSize = 14.sp, modifier = Modifier.offset(y = 1.dp))
        Text(mensaje, style = ArvoBold.copy(fontSize = 12.sp), color = ColoresTF.Blanco)
    }
}

@Preview(heightDp = 1400)
@Composable
private fun TiendaPreview() = TiempoFueraTheme {
    ContenidoTienda(EstadoTienda(monedas = 950, grupoBloqueadoId = "redes"), {}, {}, {})
}
