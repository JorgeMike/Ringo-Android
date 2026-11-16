package com.rudoestudio.tiempofuera.features.onboarding.pasos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenIos1x
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra

// Piezas que se repiten en los pasos del onboarding de iOS (allá copiadas en cada vista).

internal fun arvo(tamano: Int) = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = tamano.sp)

internal fun pirata(tamano: Int) = TextStyle(fontFamily = PirataOne, fontSize = tamano.sp)

/** Azul de las barras de `ProblemaView` y del bando Centinelas (0, 0.471, 0.741). */
internal val AzulMedio = Color(0xFF0078BD)

/**
 * Fondo de color con el patrón de estrellas arriba y abajo (de cabeza), y el contenido
 * dentro del área segura. En iOS se repite en casi todos los pasos.
 */
@Composable
internal fun FondoOnboarding(
    modifier: Modifier = Modifier,
    color: Color = ColoresTF.Celeste,
    patron: Boolean = true,
    contenido: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(color)) {
        if (patron) {
            val franja = Modifier.fillMaxWidth().height(120.dp)
            Image(
                painterResource(R.drawable.patron_estrellas_azul), null,
                contentScale = ContentScale.Crop,
                modifier = franja.align(Alignment.TopCenter),
            )
            Image(
                painterResource(R.drawable.patron_estrellas_azul), null,
                contentScale = ContentScale.Crop,
                modifier = franja.align(Alignment.BottomCenter).scale(scaleX = 1f, scaleY = -1f),
            )
        }
        Box(Modifier.fillMaxSize().safeDrawingPadding(), content = contenido)
    }
}

/** Pregunta en Pirata One entre las líneas decorativas. */
@Composable
internal fun PreguntaDecorada(texto: String, modifier: Modifier = Modifier, tamano: TextUnit = 36.sp, bajarLineas: Dp = 0.dp) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-2).dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImagenIos1x(R.drawable.linea_decor_l, Modifier.offset(y = bajarLineas))
            TextoConSombra(
                texto,
                estilo = TextStyle(fontFamily = PirataOne, fontSize = tamano, textAlign = TextAlign.Center),
                colorSombra = ColoresTF.Azul,
                alineacion = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            ImagenIos1x(R.drawable.linea_decor_r, Modifier.offset(y = bajarLineas))
        }
        ImagenIos1x(R.drawable.linea_decor_c)
    }
}

/** Texto blanco centrado en Arvo Bold, el que más usan los pasos. */
@Composable
internal fun TextoPaso(texto: String, modifier: Modifier = Modifier, tamano: Int = 18, alpha: Float = 1f) {
    Text(texto, style = arvo(tamano), color = ColoresTF.Blanco.copy(alpha = alpha), textAlign = TextAlign.Center, modifier = modifier)
}

/**
 * Placa chueca azul con su sombra blanca (dos `DiagonalShape(slant: -18)` en iOS), con el
 * contenido encima, subido 6 dp como la placa azul.
 */
@Composable
internal fun PlacaChueca(ancho: Dp, alto: Dp, modifier: Modifier = Modifier, contenido: @Composable BoxScope.() -> Unit) {
    val forma = DiagonalShape((-18).dp)
    Box(modifier.size(ancho, alto)) {
        Box(Modifier.fillMaxSize().background(ColoresTF.Blanco, forma))
        Box(Modifier.fillMaxSize().offset((-6).dp, (-6).dp).background(ColoresTF.Azul, forma), content = contenido)
    }
}

/** Número grande con flechas para bajar y subir de 5 en 5. En iOS se repite en los pasos 11 y 12. */
@Composable
internal fun ContadorMinutos(
    minutos: Int,
    alBajar: () -> Unit,
    alSubir: () -> Unit,
    modifier: Modifier = Modifier,
    colorBajar: Color = ColoresTF.Morado,
    colorSubir: Color = ColoresTF.Morado,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        BotonS(colorBajar, IconoBotonS.ABAJO, alBajar, escala = 0.9f)
        Column(Modifier.widthIn(min = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextoConSombra("$minutos", pirata(30), colorSombra = ColoresTF.Azul)
            Text("min", style = arvo(11), color = ColoresTF.Blanco.copy(alpha = 0.85f))
        }
        BotonS(colorSubir, IconoBotonS.ARRIBA, alSubir, escala = 0.9f)
    }
}

/** Tarjeta con el listón de fondo (`Ribbon1-Form`) detrás del contenido. */
@Composable
internal fun ConListon(modifier: Modifier = Modifier, contenido: @Composable BoxScope.() -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ribbon1_form), null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        contenido()
    }
}
