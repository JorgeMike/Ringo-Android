package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

// M3.10: estadísticas de uso. En iOS: `ScreenTimeStatsView`/`ScreenTimeStatsViewModel` y
// la extensión `ScreenTimeReport`, que es la única que puede leer el uso (por privacidad,
// la app no ve los números). En Android los números los tenemos nosotros.

/**
 * Uso de cada grupo por día, guardado por el monitor. iOS lo pide al sistema cada vez;
 * Android solo guarda unos días de eventos, así que llevamos nuestro propio historial.
 */
interface HistorialUsoRepository {
    /** Nunca baja dentro del mismo día. */
    suspend fun registrar(fecha: LocalDate, grupoId: String, segundos: Long)

    /** Segundos por grupo de los [dias] días que terminan en [hasta], incluido. */
    suspend fun ultimosDias(hasta: LocalDate, dias: Int): Map<LocalDate, Map<String, Long>>

    companion object {
        const val DIAS_GUARDADOS = 30L
    }
}

data class UsoDelDia(val fecha: LocalDate, val segundos: Long) {
    fun dentroDe(limiteSegundos: Long?): Boolean = limiteSegundos == null || segundos <= limiteSegundos
}

data class ResumenSemana(val dias: List<UsoDelDia>, val limiteSegundos: Long?) {
    /** Promedio de los días con datos, en minutos (en iOS, de los 7). */
    val promedioMinutos: Long get() = dias.takeIf { it.isNotEmpty() }?.let { d -> d.sumOf { it.segundos } / d.size / 60 } ?: 0

    /** Cuántos días se quedó dentro del límite; `null` sin límite. */
    val diasDentroDelLimite: Int? get() = limiteSegundos?.let { limite -> dias.count { it.dentroDe(limite) } }
}

data class AppConUso(val paquete: String, val uso: Duration)

data class EstadisticasUso(
    val tiempoBloqueadoHoySegundos: Long,
    val usoHoyDistraccion: Duration,
    /** Las apps de distracción usadas hoy, de más a menos. */
    val appsHoy: List<AppConUso>,
    val semana: ResumenSemana,
    val limiteDiarioSegundos: Long?,
    val sueno: ConfiguracionHabitoSueno,
    val hayAppsElegidas: Boolean,
)

object CalculoEstadisticas {
    /** Horario de sueño cuando el usuario no ha configurado uno (como iOS: 7:00 a 23:00, 30 min). */
    val SUENO_POR_DEFECTO = ConfiguracionHabitoSueno(horaInicioDia = 7 * 60, horaFinDia = 23 * 60, colchonUso = 30)

    /** Los 7 días que terminan hoy, con 0 en los que no hay datos. */
    fun semana(historial: Map<LocalDate, Map<String, Long>>, hoy: LocalDate, grupoId: String, limite: Long?): ResumenSemana =
        ResumenSemana(
            dias = (6 downTo 0).map { atras ->
                val dia = hoy.minusDays(atras.toLong())
                UsoDelDia(dia, historial[dia]?.get(grupoId) ?: 0)
            },
            limiteSegundos = limite,
        )

    /**
     * El sueño "de verdad" (de dormir a despertar, sin el colchón) que toca a hoy: el de
     * anoche y el de esta noche. No cuenta como tiempo bloqueado: dormido no usas el cel.
     * En iOS: `periodosCentralesDeSueno`.
     */
    fun periodosCentralesSueno(sueno: ConfiguracionHabitoSueno, ahora: Instant, zona: ZoneId): List<PeriodoBloqueo> {
        val hoy = ahora.atZone(zona).toLocalDate()
        val dormir = LocalTime.of(sueno.horaFinDia / 60 % 24, sueno.horaFinDia % 60)
        val despertar = LocalTime.of(sueno.horaInicioDia / 60 % 24, sueno.horaInicioDia % 60)
        val cruzaMedianoche = !dormir.isBefore(despertar)
        val inicioHoy = hoy.atStartOfDay(zona).toInstant()
        val finHoy = hoy.plusDays(1).atStartOfDay(zona).toInstant()
        return (-1L..0L).mapNotNull { desplazamiento ->
            val inicio = hoy.plusDays(desplazamiento).atTime(dormir).atZone(zona).toInstant()
            val fin = hoy.plusDays(desplazamiento + if (cruzaMedianoche) 1 else 0).atTime(despertar).atZone(zona).toInstant()
            PeriodoBloqueo(inicio, fin).takeIf { fin.isAfter(inicio) && fin.isAfter(inicioHoy) && inicio.isBefore(finHoy) }
        }
    }
}

class ObtenerEstadisticasUsoUseCase @Inject constructor(
    private val selecciones: AppSelectionRepository,
    private val configuraciones: ConfiguracionGrupoAppsRepository,
    private val sueno: ConfiguracionHabitoSuenoRepository,
    private val tiempoBloqueado: TiempoBloqueadoRepository,
    private val historial: HistorialUsoRepository,
    private val uso: UsoApps,
    private val reloj: Clock,
) {
    suspend operator fun invoke(): EstadisticasUso {
        val grupo = TipoGrupoApps.DISTRACCION.clave
        val ahora = reloj.instant()
        val hoy = LocalDate.now(reloj)
        val paquetes = selecciones.selecciones.first()[grupo]?.paquetes.orEmpty()
        val usoHoy = uso.usoDeHoy()
        val appsHoy = paquetes.mapNotNull { p -> usoHoy[p]?.takeIf { !it.isZero }?.let { AppConUso(p, it) } }.sortedByDescending { it.uso }
        val limite = configuraciones.obtenerConfiguracion(grupo)?.tiempoMaximoDiario
        val horario = sueno.obtenerConfiguracionHabitoSueno() ?: CalculoEstadisticas.SUENO_POR_DEFECTO
        val conHoy = historial.ultimosDias(hoy, 7).toMutableMap()
        // Hoy sale de la medición en vivo (el historial va unos segundos atrás).
        val usoHoyDistraccion = appsHoy.fold(Duration.ZERO) { total, app -> total + app.uso }
        conHoy[hoy] = conHoy[hoy].orEmpty() + (grupo to usoHoyDistraccion.seconds)
        return EstadisticasUso(
            tiempoBloqueadoHoySegundos = tiempoBloqueado.tiempoBloqueadoHoy(ahora, CalculoEstadisticas.periodosCentralesSueno(horario, ahora, reloj.zone)),
            usoHoyDistraccion = usoHoyDistraccion,
            appsHoy = appsHoy,
            semana = CalculoEstadisticas.semana(conHoy, hoy, grupo, limite),
            limiteDiarioSegundos = limite,
            sueno = horario,
            hayAppsElegidas = paquetes.isNotEmpty(),
        )
    }
}
