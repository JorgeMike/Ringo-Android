package com.rudoestudio.tiempofuera.data.bloqueo

import com.rudoestudio.tiempofuera.data.bloqueo.EventoUso.Tipo.CIERRE_GENERAL
import com.rudoestudio.tiempofuera.data.bloqueo.EventoUso.Tipo.PAUSADA
import com.rudoestudio.tiempofuera.data.bloqueo.EventoUso.Tipo.REANUDADA
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Los dos errores que encontró el spike contra Bienestar digital, más el contador propio. */
class CalculoUsoTest {
    private val min = 60_000L

    @Test
    fun `la sesion sigue mientras alguna pantalla de la app este reanudada`() {
        // Facebook: el PAUSED de la pantalla anterior llega después del RESUMED de la nueva.
        val eventos = listOf(
            EventoUso(REANUDADA, "fb", "Feed", 0),
            EventoUso(REANUDADA, "fb", "Video", 5 * min),
            EventoUso(PAUSADA, "fb", "Feed", 5 * min + 200),
            EventoUso(PAUSADA, "fb", "Video", 20 * min),
        )
        assertEquals(20 * min, CalculoUso.usoMs(eventos, inicio = 0, ahora = 30 * min)["fb"])
    }

    @Test
    fun `apagar el telefono cierra las sesiones abiertas`() {
        val eventos = listOf(
            EventoUso(REANUDADA, "ajustes", "Main", 0),
            EventoUso(CIERRE_GENERAL, "android", "", 1 * min),
        )
        assertEquals(1 * min, CalculoUso.usoMs(eventos, 0, 60 * min)["ajustes"])
        assertEquals(emptySet<String>(), CalculoUso.reanudadas(eventos))
    }

    @Test
    fun `solo cuenta desde el inicio pedido y una app abierta cuenta hasta ahora`() {
        val eventos = listOf(EventoUso(REANUDADA, "yt", "Main", 0), EventoUso(REANUDADA, "ig", "Main", 50 * min))
        val uso = CalculoUso.usoMs(eventos, inicio = 30 * min, ahora = 60 * min)
        assertEquals(30 * min, uso["yt"])
        assertEquals(10 * min, uso["ig"])
        assertEquals(setOf("yt", "ig"), CalculoUso.reanudadas(eventos))
    }

    @Test
    fun `reiniciar el mismo dia conserva lo medido y nunca baja`() {
        val hoy = LocalDate.of(2026, 11, 13)
        val antes = ContadorUsoDiario.nuevo(hoy, arranque = 7, inicioDiaMs = 0).con(mapOf("yt" to 40 * min))
        // Reinicio: UsageStats perdió lo de antes y solo mide 2 min desde el arranque.
        val despues = antes.vigente(hoy, arranqueActual = 8, horaArranqueMs = 500 * min, inicioDiaMs = 0).con(mapOf("yt" to 2 * min))
        assertEquals(42 * min, despues.ultimo["yt"])
        assertEquals(500 * min, despues.desdeMs)
        // Si UsageStats "olvida" algo dentro del mismo arranque, no baja.
        assertEquals(42 * min, despues.con(mapOf("yt" to 1 * min)).ultimo["yt"])
        // Día nuevo: desde cero.
        val manana = despues.vigente(hoy.plusDays(1), 8, 500 * min, inicioDiaMs = 1440 * min).con(emptyMap())
        assertEquals(emptyMap<String, Long>(), manana.ultimo)
    }
}
