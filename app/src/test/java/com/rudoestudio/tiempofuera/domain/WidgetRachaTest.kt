package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.ObtenerEstadoWidgetRachaUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetRachaTest {
    private val hoy = LocalDate.of(2026, 11, 13)
    private val historico = HistoricoFake()

    private fun perfiles(dias: Int, ultimaActividad: LocalDate?) = PerfilesFake(
        perfilBase().copy(diasRacha = dias, lastActivityDate = ultimaActividad?.let { reloj(it).instant() }, protectoresRacha = 2),
    )

    @Test
    fun `con la mision de hoy la racha esta activa`() = runTest {
        historico.guardarRegistro(HistoricoRacha.completado(hoy))
        val estado = ObtenerEstadoWidgetRachaUseCase(perfiles(5, hoy), historico, reloj(hoy))()
        assertEquals(5, estado.diasRacha)
        assertTrue(estado.activadaHoy)
        assertEquals("¡Racha activa hoy!", estado.mensaje)
        assertEquals(2, estado.protectores)
    }

    @Test
    fun `sin mision hoy avisa, y un dia protegido no cuenta`() = runTest {
        historico.guardarRegistro(HistoricoRacha.protegido(hoy))
        val estado = ObtenerEstadoWidgetRachaUseCase(perfiles(5, hoy.minusDays(1)), historico, reloj(hoy))()
        assertFalse(estado.activadaHoy)
        assertEquals("Haz una misión o la pierdes", estado.mensaje)
    }

    @Test
    fun `una racha vieja ya no cuenta`() = runTest {
        val estado = ObtenerEstadoWidgetRachaUseCase(perfiles(9, hoy.minusDays(3)), historico, reloj(hoy))()
        assertEquals(0, estado.diasRacha)
        assertEquals("Haz una misión y arranca tu racha", estado.mensaje)
    }
}
