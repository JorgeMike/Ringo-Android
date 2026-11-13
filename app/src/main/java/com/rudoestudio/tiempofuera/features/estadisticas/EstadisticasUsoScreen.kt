package com.rudoestudio.tiempofuera.features.estadisticas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.EtiquetaHora
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoApp
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.domain.bloqueo.AppConUso
import com.rudoestudio.tiempofuera.domain.bloqueo.CalculoEstadisticas
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadisticasUso
import com.rudoestudio.tiempofuera.domain.bloqueo.ResumenSemana
import com.rudoestudio.tiempofuera.domain.bloqueo.UsoDelDia
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.servicios.bloqueo.nombreDeApp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Tiempo en pantalla (M3.10). En iOS: `ScreenTimeStatsView`, que se abre desde Inicio; la
 * gráfica de 7 días allá la dibuja la extensión `ScreenTimeReport`.
 */
@Composable
fun EstadisticasUsoScreen(
    alVolver: () -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
    viewModel: EstadisticasUsoViewModel = hiltViewModel(),
) {
    val estadisticas by viewModel.estadisticas.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    // Al volver del selector de apps o de otra app, los números ya cambiaron.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.cargar() }

    Column(
        Modifier.fillMaxSize().background(ColoresTF.Azul).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonRegresar(alVolver)
            Spacer(Modifier.width(12.dp))
            TextoContorno("Tiempo en pantalla", MaterialTheme.typography.displayMedium, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
        }
        val e = estadisticas ?: return@Column
        ContenidoEstadisticas(
            e,
            alAjustarLimite = viewModel::ajustarLimite,
            alCambiarDespertar = viewModel::cambiarHoraDespertar,
            alCambiarDormir = viewModel::cambiarHoraDormir,
            alAjustarColchon = viewModel::ajustarColchon,
            alElegirApps = alElegirApps,
        )
        BannerError(error)
    }
}

@Composable
private fun ColumnScope.ContenidoEstadisticas(
    e: EstadisticasUso,
    alAjustarLimite: (Int) -> Unit,
    alCambiarDespertar: (LocalTime) -> Unit,
    alCambiarDormir: (LocalTime) -> Unit,
    alAjustarColchon: (Int) -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
) {
    Tarjeta {
        Text("Tiempo bloqueado hoy", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        TextoContorno(duracion(e.tiempoBloqueadoHoySegundos), MaterialTheme.typography.displayMedium, ColoresTF.Amarillo, ColoresTF.AzulNoche, 2.dp)
        Nota("Lo que tus apps de distracción estuvieron bloqueadas, sin contar tus horas de sueño.")
    }

    Tarjeta {
        Text("Hoy en tus apps de distracción", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        TextoContorno(duracion(e.usoHoyDistraccion.seconds), MaterialTheme.typography.displayMedium, ColoresTF.Amarillo, ColoresTF.AzulNoche, 2.dp)
        when {
            !e.hayAppsElegidas -> Nota("Elige tus apps de distracción abajo para ver cuánto las usas.")
            e.appsHoy.isEmpty() -> Nota("Hoy no las has abierto. ¡Así se hace!")
            else -> e.appsHoy.take(5).forEach { FilaApp(it) }
        }
    }

    TextoContorno("Últimos 7 días", MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
    Tarjeta { GraficaSemana(e.semana) }

    TextoContorno("Configuración", MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
    Tarjeta {
        Text("Límite diario", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        Ajustador(
            valor = e.limiteDiarioSegundos?.let(::duracion) ?: "Sin límite",
            alBajar = { alAjustarLimite(-5) },
            alSubir = { alAjustarLimite(5) },
        )
        Nota("Ajusta de 5 en 5 minutos. Al llegar al límite, tus apps de distracción se bloquean el resto del día.")
    }
    Tarjeta {
        Text("Horario de sueño", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        EtiquetaHora("Despierto a las", hora(e.sueno.horaInicioDia), alCambiarDespertar)
        EtiquetaHora("Me duermo a las", hora(e.sueno.horaFinDia), alCambiarDormir)
        Text("Colchón de tiempo", style = MaterialTheme.typography.titleSmall, color = ColoresTF.Blanco)
        Ajustador("${e.sueno.colchonUso} min", alBajar = { alAjustarColchon(-5) }, alSubir = { alAjustarColchon(5) })
        Nota("Antes de dormir y después de despertar tus apps de distracción se bloquean este tiempo. Va de 20 minutos a 2 horas.")
    }
    Tarjeta {
        Text("Grupos de apps", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        BotonL(ColoresTF.Morado, "DISTRACCIÓN", alTocar = { alElegirApps(TipoGrupoApps.DISTRACCION) }, modifier = Modifier.align(Alignment.CenterHorizontally))
        BotonL(ColoresTF.Verde, "EDUCATIVAS", alTocar = { alElegirApps(TipoGrupoApps.EDUCATIVAS) }, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ColoresTF.Blanco.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = contenido,
    )
}

@Composable
private fun Nota(texto: String) = Text(texto, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco.copy(alpha = 0.85f))

@Composable
private fun Ajustador(valor: String, alBajar: () -> Unit, alSubir: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        BotonS(ColoresTF.Morado, IconoBotonS.ABAJO, alTocar = alBajar, escala = 0.8f)
        TextoContorno(valor, MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
        BotonS(ColoresTF.Verde, IconoBotonS.ARRIBA, alTocar = alSubir, escala = 0.8f)
    }
}

@Composable
private fun FilaApp(app: AppConUso) {
    val contexto = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconoApp(app.paquete, tamano = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            nombreDeApp(contexto, app.paquete), style = MaterialTheme.typography.bodyLarge, color = ColoresTF.Blanco,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(duracion(app.uso.seconds), style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
    }
}

/**
 * Una barra por día, con la línea punteada del límite. Una sola serie: sin leyenda. Los
 * días que se pasaron del límite van en naranja **y** con su valor escrito, para que no
 * dependa solo del color.
 */
@Composable
private fun GraficaSemana(semana: ResumenSemana) {
    val limite = semana.limiteSegundos
    val maximo = maxOf(semana.dias.maxOf { it.segundos }, limite ?: 0, 60L).toFloat()
    val descripcion = semana.dias.joinToString { "${dia(it.fecha)} ${duracion(it.segundos)}" }
    Column(Modifier.semantics { contentDescription = "Uso de los últimos 7 días: $descripcion" }) {
        Row(Modifier.fillMaxWidth()) {
            semana.dias.forEach { d ->
                Text(
                    if (!d.dentroDe(limite)) minutosCortos(d.segundos) else "",
                    style = MaterialTheme.typography.labelSmall, color = ColoresTF.Blanco,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val columna = size.width / semana.dias.size
            val ancho = columna * 0.5f
            val radio = 4.dp.toPx()
            semana.dias.forEachIndexed { i, d ->
                val alto = size.height * d.segundos / maximo
                if (alto <= 0f) return@forEachIndexed
                val x = columna * i + (columna - ancho) / 2
                // Solo las puntas de arriba van redondeadas: la barra sale de la base.
                clipRect(top = size.height - alto, bottom = size.height) {
                    drawRoundRect(
                        color = if (d.dentroDe(limite)) ColoresTF.Amarillo else ColoresTF.Naranja,
                        topLeft = Offset(x, size.height - alto),
                        size = Size(ancho, alto + radio),
                        cornerRadius = CornerRadius(radio),
                    )
                }
            }
            drawLine(ColoresTF.Blanco.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            if (limite != null) {
                val y = size.height - size.height * limite / maximo
                drawLine(
                    ColoresTF.Blanco, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            semana.dias.forEach { d ->
                Text(dia(d.fecha), style = MaterialTheme.typography.labelMedium, color = ColoresTF.Blanco, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Dato("Promedio al día", duracion(semana.promedioMinutos * 60))
            semana.diasDentroDelLimite?.let { Dato("Días en tu límite", "$it de ${semana.dias.size}") }
        }
        Nota(
            if (limite != null) "La línea punteada es tu límite diario (${duracion(limite)}). En naranja, los días que te pasaste."
            else "Pon un límite diario abajo para verlo en la gráfica.",
        )
    }
}

@Composable
private fun Dato(titulo: String, valor: String) {
    Column {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = ColoresTF.Blanco.copy(alpha = 0.85f))
        Text(valor, style = MaterialTheme.typography.titleLarge, color = ColoresTF.Blanco)
    }
}

private fun dia(fecha: LocalDate) = fecha.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es", "MX")).uppercase()

private fun hora(minutos: Int): LocalTime = LocalTime.of(minutos / 60 % 24, minutos % 60)

/** "1 h 25 min", "40 min", "0 min". */
fun duracion(segundos: Long): String {
    val horas = segundos / 3600
    val minutos = segundos % 3600 / 60
    return when {
        horas == 0L -> "$minutos min"
        minutos == 0L -> "$horas h"
        else -> "$horas h $minutos min"
    }
}

private fun minutosCortos(segundos: Long): String = if (segundos >= 3600) "${segundos / 3600}h${(segundos % 3600) / 60}" else "${segundos / 60}m"

@Preview(heightDp = 1500)
@Composable
private fun EstadisticasPreview() = TiempoFueraTheme {
    val hoy = LocalDate.of(2026, 11, 13)
    val dias = listOf(40L, 75, 20, 0, 95, 50, 62).mapIndexed { i, m -> UsoDelDia(hoy.minusDays(6L - i), m * 60) }
    Column(Modifier.background(ColoresTF.Azul).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ContenidoEstadisticas(
            EstadisticasUso(
                tiempoBloqueadoHoySegundos = 5400,
                usoHoyDistraccion = Duration.ofMinutes(62),
                appsHoy = listOf(AppConUso("com.instagram.android", Duration.ofMinutes(40)), AppConUso("com.google.android.youtube", Duration.ofMinutes(22))),
                semana = ResumenSemana(dias, limiteSegundos = 60 * 60),
                limiteDiarioSegundos = 60 * 60,
                sueno = CalculoEstadisticas.SUENO_POR_DEFECTO,
                hayAppsElegidas = true,
            ),
            {}, {}, {}, {}, {},
        )
    }
}
