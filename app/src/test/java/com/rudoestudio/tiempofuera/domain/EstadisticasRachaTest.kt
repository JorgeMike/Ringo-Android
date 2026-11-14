package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.perfil.CalculoRacha
import com.rudoestudio.tiempofuera.domain.perfil.EstadisticasEjercicioRepository
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.ObtenerEstadisticasRachaUseCase
import com.rudoestudio.tiempofuera.domain.perfil.RegistroCarrera
import com.rudoestudio.tiempofuera.domain.perfil.RegistroLagartijas
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EstadisticasRachaTest {
    private val hoy = LocalDate.of(2026, 11, 13)

    @Test
    fun `la mayor racha cuenta dias seguidos aunque esten desordenados`() {
        val fechas = listOf(hoy, hoy.minusDays(10), hoy.minusDays(1), hoy.minusDays(9), hoy.minusDays(2), hoy.minusDays(8), hoy.minusDays(7))
        assertEquals(4, CalculoRacha.mayorRacha(fechas))
        assertEquals(0, CalculoRacha.mayorRacha(emptyList()))
    }

    @Test
    fun `los dias salvados con protector suman a la racha pero no se marcan en el calendario`() = runTest {
        val historico = HistoricoFake().apply {
            registros[hoy.minusDays(1)] = HistoricoRacha.completado(hoy.minusDays(1))
            registros[hoy] = HistoricoRacha.protegido(hoy)
        }
        val perfiles = PerfilesFake(perfilBase().copy(diasRacha = 1, lastActivityDate = reloj().instant()))
        val stats = ObtenerEstadisticasRachaUseCase(historico, perfiles, EjercicioVacio, reloj())()
        assertEquals(1, stats.diasInvicto)
        assertEquals(2, stats.mayorRacha)
        assertEquals(setOf(hoy.minusDays(1)), stats.diasActivados)
    }

    private object EjercicioVacio : EstadisticasEjercicioRepository {
        override suspend fun registrarCarrera(registro: RegistroCarrera) = Unit
        override suspend fun registrarLagartijas(registro: RegistroLagartijas) = Unit
        override suspend fun obtenerTopCarreras() = emptyList<RegistroCarrera>()
        override suspend fun obtenerTopLagartijas() = emptyList<RegistroLagartijas>()
    }
}
