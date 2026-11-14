package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ArvoBold = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold)

/** Título de sección sobre su fondo, con subtítulo chueco. En iOS: `Titulo`. */
@Composable
fun Titulo(primario: String, secundario: String, modifier: Modifier = Modifier) {
    Box(modifier.offset(y = (-15).dp), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.fondo_titulos), contentDescription = null)
        TextoConSombra(
            primario,
            estilo = TextStyle(fontFamily = PirataOne, fontSize = 48.sp),
            colorSombra = ColoresTF.AzulNoche,
            alineacion = TextAlign.Center,
        )
        val subtitulo = ArvoBold.copy(fontSize = 28.sp)
        // Sombra gris detrás, luego el texto blanco con borde.
        TextoContorno(
            secundario, subtitulo, ColoresTF.GrisClaro, ColoresTF.GrisClaro, 4.dp,
            Modifier.offset(4.dp, 49.dp).rotate(-3.8f),
        )
        TextoContorno(
            secundario, subtitulo, ColoresTF.Blanco, ColoresTF.AzulNoche, 3.dp,
            Modifier.offset(10.dp, 45.dp).rotate(-3.8f),
        )
    }
}

/** Texto Pirata One sobre un paralelogramo con sombra. En iOS: `TextoEnParalelogramo`. */
@Composable
fun TextoEnParalelogramo(
    texto: String,
    modifier: Modifier = Modifier,
    colorFondo: Color = ColoresTF.AzulParalelogramo,
    colorTexto: Color = ColoresTF.Blanco,
    colorSombra: Color = ColoresTF.Blanco,
    tamanoFuente: TextUnit = 34.sp,
) {
    val forma = remember { ParalelogramoShape(18.dp) }
    Text(
        texto,
        fontFamily = PirataOne,
        fontSize = tamanoFuente,
        color = colorTexto,
        modifier = modifier
            .drawBehind {
                val contorno = forma.createOutline(size, layoutDirection, this)
                translate(6.dp.toPx(), 6.dp.toPx()) { drawOutline(contorno, colorSombra) }
                drawOutline(contorno, colorFondo)
            }
            .padding(start = 18.dp, top = 2.dp, end = 20.dp, bottom = 2.dp),
    )
}

/**
 * Fila de arriba con poder y monedas. En iOS: `HeaderPuntosView`. (El que además muestra
 * el temporizador de misión, `HeaderOverlayView`, llega con M5 porque depende del perfil.)
 */
@Composable
fun HeaderPuntos(
    puntosPoder: Int,
    monedas: Int,
    alTocarPerfil: () -> Unit,
    alTocarTienda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // En iOS el primer badge (perfil) también usa el ícono de poder y muestra 1.
        InsigniaValor(R.drawable.puntos_poder, 1, alTocarPerfil)
        Spacer(Modifier.weight(1f))
        InsigniaValor(R.drawable.puntos_poder, puntosPoder, alTocarTienda)
        InsigniaValor(R.drawable.moneda_icon, monedas, alTocarTienda)
    }
}

@Composable
private fun InsigniaValor(icono: Int, valor: Int, alTocar: () -> Unit) {
    val forma = remember { DiagonalShape(11.dp) }
    Box(
        Modifier.defaultMinSize(minWidth = 90.dp, minHeight = 40.dp).clickable(onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .shadow(6.dp, forma)
                .background(ColoresTF.Blanco, forma)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp))
            Text("$valor", style = ArvoBold.copy(fontSize = 18.sp), color = ColoresTF.Azul, maxLines = 1)
        }
    }
}

/**
 * Poder, monedas y experiencia ganados, chuecos sobre su badge. En iOS:
 * `SeccionRecompensas`. Los valores nulos o en cero no se muestran.
 */
@Composable
fun SeccionRecompensas(puntosPoder: Int?, monedas: Int?, exp: Int?, modifier: Modifier = Modifier) {
    val numero = TextStyle(fontFamily = PirataOne, fontSize = 28.sp)
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.badge_indicador),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            Modifier.padding(horizontal = 20.dp).offset((-18).dp, (-8).dp).rotate(10f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (puntosPoder != null && puntosPoder > 0) {
                ValorConIcono(R.drawable.puntos_poder_icon, "$puntosPoder", numero)
            }
            if (monedas != null && monedas > 0) {
                ValorConIcono(R.drawable.moneda_icon, "$monedas", numero)
            }
            if (exp != null && exp > 0) {
                TextoContorno(
                    "$exp EXP", numero.copy(fontSize = 38.sp), Color.Black, ColoresTF.Blanco, 3.dp,
                    Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ValorConIcono(icono: Int, texto: String, estilo: TextStyle) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp))
        TextoContorno(texto, estilo, Color.Black, ColoresTF.Blanco, 3.dp)
    }
}

private val formatoHora: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/**
 * Etiqueta con una hora que se cambia desde una hoja inferior. En iOS: `EtiquetaHora`
 * (allá con rueda; aquí con el reloj de Material).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtiquetaHora(
    titulo: String,
    hora: LocalTime,
    alCambiar: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrandoSelector by remember { mutableStateOf(false) }
    val forma = remember { DiagonalShape((-10).dp) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = ArvoBold.copy(fontSize = 14.sp), color = ColoresTF.Blanco)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(110.dp, 40.dp)
                .background(ColoresTF.Azul, forma)
                .clickable { mostrandoSelector = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                hora.format(formatoHora),
                style = ArvoBold.copy(fontSize = 16.sp),
                color = ColoresTF.Blanco,
                modifier = Modifier.offset(x = (-6).dp),
            )
        }
    }
    if (mostrandoSelector) {
        val estado = rememberTimePickerState(hora.hour, hora.minute)
        // Abierta por completo: a media altura el reloj quedaba cortado.
        ModalBottomSheet(
            onDismissRequest = { mostrandoSelector = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(titulo, fontFamily = PirataOne, fontSize = 24.sp, color = ColoresTF.Azul)
                TimePicker(estado)
                BotonL(ColoresTF.Verde, "LISTO", alTocar = {
                    alCambiar(LocalTime.of(estado.hour, estado.minute))
                    mostrandoSelector = false
                })
            }
        }
    }
}
