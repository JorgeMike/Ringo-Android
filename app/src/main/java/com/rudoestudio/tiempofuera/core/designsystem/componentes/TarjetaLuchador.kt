package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador

private val FORMA_TARJETA = RoundedCornerShape(16.dp)

/** Fondo, sombra del nombre y brillo de cada rareza. En iOS: `TarjetaStyle`. */
private data class EstiloTarjeta(val fondo: Int, val sombraNombre: Color, val brillo: Color)

private fun estilo(rareza: Rareza) = when (rareza) {
    Rareza.COMUN -> EstiloTarjeta(R.drawable.tarjeta_comun, Color(0xFF202020), Color.Transparent)
    Rareza.RARO -> EstiloTarjeta(R.drawable.tarjeta_rara, Color(0xFF202020), Color(0xFF4AF0FF))
    Rareza.EPICO -> EstiloTarjeta(R.drawable.tarjeta_epica, Color(0xFF202020), Color(0xFFB44FFF))
    Rareza.LEGENDARIO -> EstiloTarjeta(R.drawable.tarjeta_legendaria, Color(0xFF4F3600), Color(0xFFFFB800))
}

private fun emoji(tipo: TipoLuchador) = when (tipo) {
    TipoLuchador.PIEDRA -> "🪨"
    TipoLuchador.PAPEL -> "📄"
    TipoLuchador.TIJERA -> "✂️"
    TipoLuchador.VERSATIL -> "🫯"
}

/**
 * Carta de un luchador (290x386) con fondo de su rareza, el luchador flotando, su nombre,
 * frase y tipo. Común y raro van centrados; épico y legendario con el nombre chueco. En
 * iOS: `TarjetaLuchadorView`. M6.12: allá la carta se inclina con el giroscopio
 * (`DeviceMotionService`); aquí todavía no.
 */
@Composable
fun TarjetaLuchador(luchador: Luchador, modifier: Modifier = Modifier) {
    val e = estilo(luchador.rareza)
    val flote = rememberInfiniteTransition(label = "tarjeta")
    val y by flote.animateFloat(0f, -12f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flote")
    val imagen = AssetsIos.imagen(luchador.imagen) ?: AssetsIos.imagen(luchador.nombre) ?: R.drawable.ringo
    val nombre = TextStyle(fontFamily = PirataOne, fontSize = 36.sp)
    val frase = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val chueca = luchador.rareza == Rareza.EPICO || luchador.rareza == Rareza.LEGENDARIO

    Box(
        modifier
            .size(290.dp, 386.dp)
            .shadow(if (e.brillo == Color.Transparent) 16.dp else 24.dp, FORMA_TARJETA, ambientColor = e.brillo, spotColor = e.brillo)
            .clip(FORMA_TARJETA),
    ) {
        Image(painterResource(e.fondo), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (!chueca) {
            Column(Modifier.fillMaxSize().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                ImagenPixel(imagen, 220.dp, Modifier.offset(y = y.dp))
                TextoConSombra(luchador.nombre, nombre, colorSombra = e.sombraNombre, alineacion = TextAlign.Center)
                Text(luchador.frase, style = frase, color = ColoresTF.Blanco, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 10.dp))
            }
            InsigniaTipo(luchador.tipo, Modifier.align(Alignment.TopStart).padding(16.dp))
        } else {
            ImagenPixel(imagen, 220.dp, Modifier.align(Alignment.Center).offset(y = y.dp))
            TextoConSombra(
                luchador.nombre, nombre, colorSombra = e.sombraNombre,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).offset(x = 32.dp).rotate(-18f),
            )
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (luchador.rareza == Rareza.LEGENDARIO) InsigniaTipo(luchador.tipo)
                Text(luchador.frase, style = frase, color = ColoresTF.Blanco, textAlign = TextAlign.End)
            }
            if (luchador.rareza == Rareza.EPICO) InsigniaTipo(luchador.tipo, Modifier.align(Alignment.TopStart).padding(16.dp))
        }
    }
}

@Composable
private fun InsigniaTipo(tipo: TipoLuchador, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(ColoresTF.Blanco.copy(alpha = 0.25f), CircleShape)
            .border(1.5.dp, ColoresTF.Blanco.copy(alpha = 0.4f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(emoji(tipo), fontSize = 26.sp) }
}

@Preview
@Composable
private fun TarjetaPreview() = TiempoFueraTheme { TarjetaLuchador(DatosPreview.luchadorSeleccionado) }
