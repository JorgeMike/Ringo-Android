package com.rudoestudio.tiempofuera.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.domain.perfil.EstadisticasRacha
import com.rudoestudio.tiempofuera.domain.perfil.RegistroCarrera
import com.rudoestudio.tiempofuera.domain.perfil.RegistroLagartijas
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as EstiloTexto
import java.util.Locale

private val ES_MX: Locale = Locale.forLanguageTag("es-MX")
private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(ES_MX)

private fun arvo(tamano: Int) = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = tamano.sp)

private fun pirata(tamano: TextUnit) = TextStyle(fontFamily = PirataOne, fontSize = tamano)

/**
 * "Días invicto" (M5.2): racha actual y récord, calendario con los días en que se sumó
 * racha y los récords de carrera y lagartijas. En iOS: `RachaStatsView`, en una hoja; aquí
 * es pantalla completa con botón de regresar, como "Tiempo en pantalla".
 */
@Composable
fun RachaStatsScreen(alVolver: () -> Unit, viewModel: RachaStatsViewModel = hiltViewModel()) {
    val estadisticas by viewModel.estadisticas.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    ContenidoRacha(estadisticas, error, alVolver)
}

@Composable
private fun ContenidoRacha(estadisticas: EstadisticasRacha?, error: String?, alVolver: () -> Unit, hoy: LocalDate = LocalDate.now()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Celeste)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonRegresar(alVolver)
            TextoConSombra("Días invicto", pirata(34.sp), colorSombra = Color.Black)
        }
        BannerError(error)
        if (estadisticas != null) {
            Resumen(estadisticas)
            Calendario(estadisticas.diasActivados, hoy)
            TextoConSombra("Récords de entrenamiento", pirata(27.sp), colorSombra = Color.Black)
            Records("Top 3 carreras", "Completa una carrera para registrar tu primer resultado.", estadisticas.topCarreras.map { textoCarrera(it) })
            Records("Top 3 lagartijas", "Completa una misión de lagartijas para registrar tu primer resultado.", estadisticas.topLagartijas.map { textoLagartijas(it) })
        }
    }
}

private val formaTarjeta = RoundedCornerShape(18.dp)

private fun Modifier.tarjeta(alpha: Float = 0.55f) =
    fillMaxWidth().background(ColoresTF.Azul.copy(alpha = alpha), formaTarjeta).padding(14.dp)

@Composable
private fun Resumen(e: EstadisticasRacha) {
    Row(Modifier.tarjeta(0.6f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.racha_icon), null, Modifier.size(42.dp))
        Column(Modifier.weight(1f)) {
            Text("Días invicto", style = arvo(15), color = ColoresTF.Blanco)
            TextoConSombra("${e.diasInvicto}", pirata(28.sp), colorSombra = Color.Black)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Récord de días invicto", style = arvo(13), color = ColoresTF.Blanco.copy(alpha = 0.9f))
            TextoConSombra("${e.mayorRacha} días", pirata(22.sp), colorSombra = Color.Black)
        }
    }
}

/** El mes con los días de racha marcados; no se avanza más allá del mes actual. */
@Composable
private fun Calendario(diasActivados: Set<LocalDate>, hoy: LocalDate) {
    val mesActual = YearMonth.from(hoy)
    var mes by rememberSaveable { mutableStateOf(mesActual) }
    val esMesActual = mes == mesActual
    // Semana de lunes a domingo, como se usa en México.
    val hueco = (mes.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val celdas: List<Int?> = List(hueco) { null } + (1..mes.lengthOfMonth()).toList()

    Column(Modifier.tarjeta().padding(2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlechaMes(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", habilitada = true) { mes = mes.minusMonths(1) }
            Spacer(Modifier.weight(1f))
            val nombre = mes.month.getDisplayName(EstiloTexto.FULL_STANDALONE, ES_MX).replaceFirstChar { it.uppercase() }
            Text("$nombre ${mes.year}", style = pirata(24.sp), color = ColoresTF.Blanco)
            Spacer(Modifier.weight(1f))
            FlechaMes(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", habilitada = !esMesActual) { mes = mes.plusMonths(1) }
        }
        Row {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(EstiloTexto.NARROW_STANDALONE, ES_MX).uppercase(),
                    style = arvo(11), color = ColoresTF.Blanco.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        celdas.chunked(7).forEach { semana ->
            Row {
                (0 until 7).forEach { i ->
                    val dia = semana.getOrNull(i)
                    Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                        if (dia != null) CeldaDia(mes.atDay(dia), dia, mes.atDay(dia) in diasActivados, hoy)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.racha_icon), null, Modifier.size(20.dp))
            Text("Día en que sumaste días invicto", style = arvo(11), color = ColoresTF.Blanco.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun CeldaDia(fecha: LocalDate, dia: Int, activado: Boolean, hoy: LocalDate) {
    if (activado) {
        Image(
            painterResource(R.drawable.racha_icon), null,
            Modifier.padding(3.dp).semantics { contentDescription = "$dia, día invicto" },
        )
    } else {
        Text("$dia", style = arvo(12), color = ColoresTF.Blanco.copy(alpha = if (fecha.isAfter(hoy)) 0.35f else 1f))
    }
}

@Composable
private fun FlechaMes(icono: ImageVector, descripcion: String, habilitada: Boolean, alTocar: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .alpha(if (habilitada) 1f else 0.4f)
            .background(ColoresTF.Azul, CircleShape)
            .clickable(enabled = habilitada, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) { Icon(icono, descripcion, tint = ColoresTF.Blanco) }
}

private data class TextoRecord(val principal: String, val fecha: String)

private fun fecha(instante: Instant) = instante.atZone(ZoneId.systemDefault()).toLocalDate().format(formatoFecha)

private fun textoCarrera(c: RegistroCarrera) =
    TextoRecord("%.2f km · %.1f km/h promedio".format(ES_MX, c.distanciaMetros / 1000, c.velocidadPromedioKmh), fecha(c.fecha))

private fun textoLagartijas(r: RegistroLagartijas) = TextoRecord("${r.cantidad} lagartijas", fecha(r.fecha))

@Composable
private fun Records(titulo: String, vacio: String, records: List<TextoRecord>) {
    Column(Modifier.tarjeta(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo, style = arvo(16), color = ColoresTF.Blanco)
        if (records.isEmpty()) {
            Text(vacio, style = arvo(12), color = ColoresTF.Blanco.copy(alpha = 0.75f))
        }
        records.forEachIndexed { i, r ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("#${i + 1}", style = pirata(21.sp), color = ColoresTF.Amarillo, modifier = Modifier.width(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(r.principal, style = arvo(13), color = ColoresTF.Blanco)
                    Text(r.fecha, style = arvo(11), color = ColoresTF.Blanco.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Preview(heightDp = 1100)
@Composable
private fun RachaPreview() = TiempoFueraTheme {
    val hoy = LocalDate.of(2026, 11, 13)
    ContenidoRacha(
        EstadisticasRacha(
            diasInvicto = 4,
            mayorRacha = 9,
            diasActivados = (0L..3L).map { hoy.minusDays(it) }.toSet() + hoy.minusDays(8),
            topCarreras = emptyList(),
            topLagartijas = listOf(RegistroLagartijas(fecha = Instant.parse("2026-11-10T18:00:00Z"), cantidad = 22)),
        ),
        error = null,
        alVolver = {},
        hoy = hoy,
    )
}
