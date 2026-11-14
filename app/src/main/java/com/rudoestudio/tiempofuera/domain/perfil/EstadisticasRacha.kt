package com.rudoestudio.tiempofuera.domain.perfil

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Lo que muestra "Días invicto" (M5.2). En iOS: los `@Published` de `RachaStatsViewModel`. */
data class EstadisticasRacha(
    val diasInvicto: Int,
    /** La racha más larga: del histórico o la actual, la que sea mayor. */
    val mayorRacha: Int,
    /** Días en que se sumó racha completando misiones (no los salvados con protector). */
    val diasActivados: Set<LocalDate>,
    val topCarreras: List<RegistroCarrera>,
    val topLagartijas: List<RegistroLagartijas>,
)

object CalculoRacha {
    /** Días seguidos más largos en [fechas] (con o sin protector cuentan). En iOS: `calcularMayorRacha`. */
    fun mayorRacha(fechas: Collection<LocalDate>): Int {
        val ordenadas = fechas.toSortedSet()
        if (ordenadas.isEmpty()) return 0
        var mayor = 1
        var seguidos = 1
        var anterior = ordenadas.first()
        for (fecha in ordenadas.drop(1)) {
            seguidos = if (anterior.plusDays(1) == fecha) seguidos + 1 else 1
            mayor = maxOf(mayor, seguidos)
            anterior = fecha
        }
        return mayor
    }
}

/** En iOS: `RachaStatsViewModel.cargar()`. */
class ObtenerEstadisticasRachaUseCase @Inject constructor(
    private val historico: HistoricoRachaRepository,
    private val perfiles: UserProfileRepository,
    private val ejercicio: EstadisticasEjercicioRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(): EstadisticasRacha {
        val registros = historico.obtenerTodosLosRegistros()
        val perfil = perfiles.obtenerPerfil()
        val diasInvicto = RachaVigencia.diasVigentes(perfil.diasRacha, perfil.lastActivityDate, reloj.instant(), reloj.zone)
        return EstadisticasRacha(
            diasInvicto = diasInvicto,
            mayorRacha = maxOf(CalculoRacha.mayorRacha(registros.map { it.fecha }), diasInvicto),
            diasActivados = registros.filterNot { it.usoProtectorRacha }.map { it.fecha }.toSet(),
            topCarreras = ejercicio.obtenerTopCarreras(),
            topLagartijas = ejercicio.obtenerTopLagartijas(),
        )
    }
}
