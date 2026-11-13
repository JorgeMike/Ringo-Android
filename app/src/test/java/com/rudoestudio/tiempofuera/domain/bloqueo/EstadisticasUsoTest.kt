package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.reloj
import com.rudoestudio.tiempofuera.features.estadisticas.duracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class EstadisticasUsoTest {
    private val hoy = LocalDate.of(2026, 11, 13)
    private val zona = ZoneId.of("America/Mexico_City")
    private fun en(dia: LocalDate, hora: Int, minuto: Int = 0) = LocalDateTime.of(dia, LocalTime.of(hora, minuto)).atZone(zona).toInstant()

    @Test
    fun `la semana tiene 7 dias terminando hoy, con 0 donde no hay datos`() {
        val historial = mapOf(hoy to mapOf("distraccion" to 3600L), hoy.minusDays(2) to mapOf("distraccion" to 1200L, "lectura" to 900L))
        val semana = CalculoEstadisticas.semana(historial, hoy, "distraccion", limite = 1800)
        assertEquals((6 downTo 0).map { hoy.minusDays(it.toLong()) }, semana.dias.map { it.fecha })
        assertEquals(listOf(0L, 0, 0, 0, 1200, 0, 3600), semana.dias.map { it.segundos })
        assertEquals(6, semana.diasDentroDelLimite)
        assertEquals((3600 + 1200) / 7 / 60L, semana.promedioMinutos)
        assertNull(CalculoEstadisticas.semana(historial, hoy, "distraccion", limite = null).diasDentroDelLimite)
    }

    @Test
    fun `el sueño central de anoche y de esta noche toca a hoy`() {
        val sueno = ConfiguracionHabitoSueno(horaInicioDia = 7 * 60, horaFinDia = 23 * 60, colchonUso = 30)
        val periodos = CalculoEstadisticas.periodosCentralesSueno(sueno, en(hoy, 12), zona)
        assertEquals(
            listOf(PeriodoBloqueo(en(hoy.minusDays(1), 23), en(hoy, 7)), PeriodoBloqueo(en(hoy, 23), en(hoy.plusDays(1), 7))),
            periodos,
        )
    }

    @Test
    fun `quien duerme despues de medianoche tambien se cuenta bien`() {
        val sueno = ConfiguracionHabitoSueno(horaInicioDia = 9 * 60, horaFinDia = 1 * 60, colchonUso = 30)
        val periodos = CalculoEstadisticas.periodosCentralesSueno(sueno, en(hoy, 12), zona)
        assertEquals(listOf(PeriodoBloqueo(en(hoy, 1), en(hoy, 9))), periodos)
    }

    @Test
    fun `el tiempo bloqueado de hoy descuenta el sueño central`() {
        val registro = RegistroTiempoBloqueado()
            .iniciar("distraccion", "programado", en(hoy.minusDays(1), 22, 30))
            .finalizar("distraccion", "programado", en(hoy, 7, 30), zona)
        val sueno = CalculoEstadisticas.periodosCentralesSueno(CalculoEstadisticas.SUENO_POR_DEFECTO, en(hoy, 12), zona)
        assertEquals(30 * 60L, registro.segundosBloqueados(reloj(hoy, 12).instant(), zona, sueno)) // solo el colchón de la mañana
    }

    @Test
    fun `duraciones legibles`() {
        assertEquals("0 min", duracion(0))
        assertEquals("40 min", duracion(40 * 60))
        assertEquals("2 h", duracion(2 * 3600))
        assertEquals("1 h 25 min", duracion(85 * 60))
    }

    @Test
    fun `guias por marca con alias y una generica`() {
        assertEquals("Samsung", GuiasFabricante.para("samsung").marca)
        assertEquals("Xiaomi", GuiasFabricante.para("POCO").marca)
        assertEquals("OPPO", GuiasFabricante.para("OnePlus").marca)
        assertEquals("Android", GuiasFabricante.para("Google").marca)
    }
}
