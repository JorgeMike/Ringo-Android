package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego.Fase
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego.ResultadoAtaque
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego.Sonido
import com.rudoestudio.tiempofuera.domain.ligas.Punto
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MotorMinijuegoTest {
    private val niveles = DatosPreview.ligas.first().niveles(Random(1))
    private fun barrera(id: Int) = niveles.first { it.id == id }

    private fun luchador(tipo: TipoLuchador, nivel: Int = 1) =
        Luchador(1, "Prueba", 1, nivel, 0, "", "", Rareza.COMUN, tipo)

    private class Partida(val motor: MotorMinijuego) {
        var t = 0.0

        /** Corre cuadros de 1/60 s hasta [segundos] o hasta que [parar] diga. */
        fun correr(segundos: Double, parar: () -> Boolean = { false }) {
            val fin = t + segundos
            while (t < fin && !parar()) {
                t += 1.0 / 60
                motor.actualizar(t)
            }
        }
    }

    private fun partida(nivelId: Int, usuario: Luchador, semilla: Int = 7) =
        Partida(MotorMinijuego(barrera(nivelId), usuario, DatosPreview.luchadorSeleccionado, 0.0, Random(semilla)))

    @Test
    fun `las vidas y el dano salen del nivel y de los tipos`() {
        // Barrera 10 es de tijera: piedra le gana.
        val facil = MotorMinijuego(barrera(10), luchador(TipoLuchador.PIEDRA), DatosPreview.luchadorSeleccionado, 0.0)
        assertFalse(facil.tieneDesventajaDeTipo)
        assertEquals(3, facil.vidasMaximasJugador)
        assertEquals(6, facil.vidaMaximaRival)

        // La 100 es versátil: siempre en desventaja; tu nivel la compensa.
        val dificil = MotorMinijuego(barrera(100), luchador(TipoLuchador.PAPEL, nivel = 91), DatosPreview.luchadorSeleccionado, 0.0)
        assertTrue(dificil.tieneDesventajaDeTipo)
        assertEquals(5, dificil.vidasMaximasJugador)
        assertEquals(16, dificil.vidaMaximaRival)
    }

    @Test
    fun `un golpe cerca le baja vida y luego hay que esperar`() {
        val p = partida(10, luchador(TipoLuchador.PIEDRA))
        p.motor.atacar(0.0)
        assertEquals(ResultadoAtaque.GOLPE, p.motor.resultadoAtaque)
        assertEquals(4, p.motor.vidaRival) // 6 - 2 por la ventaja de tipo
        assertEquals(listOf(Sonido.GOLPE), p.motor.tomarSonidos())
        assertFalse(p.motor.puedeAtacar(0.5))
        assertTrue(p.motor.puedeAtacar(1.2))
    }

    @Test
    fun `lejos el golpe falla y corta el combo`() {
        val p = partida(10, luchador(TipoLuchador.PIEDRA))
        p.motor.moverJoystick(Punto(1.0, 1.0), 0.0)
        // El rival te sigue: se golpea en cuanto quedó fuera de alcance.
        p.correr(5.0) { (p.motor.posicionJugador - p.motor.posicionRival).largo > MotorMinijuego.RADIO_GOLPE + 0.02 }
        p.motor.atacar(p.t)
        assertEquals(ResultadoAtaque.FALLO, p.motor.resultadoAtaque)
        assertEquals(0, p.motor.combo)
        assertEquals(p.motor.vidaMaximaRival, p.motor.vidaRival)
    }

    @Test
    fun `si no haces nada el rival te pega hasta ganarte y se puede reintentar`() {
        val p = partida(50, luchador(TipoLuchador.PIEDRA))
        p.correr(60.0) { p.motor.terminado }
        assertEquals(Fase.DERROTA, p.motor.fase)
        assertEquals(0, p.motor.vidasJugador)
        assertTrue(p.motor.contadorImpactoRecibido > 0)

        p.motor.reiniciar(p.t)
        assertEquals(Fase.PREPARACION, p.motor.fase)
        assertEquals(p.motor.vidasMaximasJugador, p.motor.vidasJugador)
        assertEquals(MotorMinijuego.INICIO_JUGADOR, p.motor.posicionJugador)
    }

    @Test
    fun `cubrirse al final del aviso es bloqueo perfecto y lo aturde`() {
        val p = partida(10, luchador(TipoLuchador.PIEDRA))
        p.correr(5.0) { p.motor.fase == Fase.ADVERTENCIA }
        assertEquals(Fase.ADVERTENCIA, p.motor.fase)
        // El aviso del nivel 10 dura ~0.85 s: la ventana perfecta es su último tercio.
        p.correr(0.7)
        p.motor.cambiarGuardia(true, p.t)
        p.correr(1.0) { p.motor.fase != Fase.ADVERTENCIA }
        assertEquals(ResultadoAtaque.BLOQUEADO, p.motor.resultadoAtaque)
        assertEquals(Fase.RIVAL_ATURDIDO, p.motor.fase)
        assertEquals(p.motor.vidasMaximasJugador, p.motor.vidasJugador)
    }

    @Test
    fun `cubrirse desde antes solo reduce el dano`() {
        val p = partida(100, luchador(TipoLuchador.PIEDRA))
        p.motor.cambiarGuardia(true, 0.0)
        // A veces el rival se pasa de largo y falla: se espera un ataque que sí llegue.
        repeat(10) {
            p.correr(5.0) { p.motor.fase == Fase.ADVERTENCIA }
            val antes = p.motor.vidasJugador
            p.correr(1.0) { p.motor.fase != Fase.ADVERTENCIA }
            if (p.motor.resultadoAtaque == ResultadoAtaque.BLOQUEADO) {
                // Versátil pega 2, pero cubierto solo 1.
                assertEquals(antes - 1, p.motor.vidasJugador)
                return
            }
        }
        throw AssertionError("El rival nunca conectó un ataque")
    }

    @Test
    fun `golpear sin parar cuando esta cerca gana la pelea`() {
        val p = partida(10, luchador(TipoLuchador.PIEDRA, nivel = 100))
        p.correr(60.0) {
            // Persigue al rival y pega en cuanto se puede.
            val hacia = p.motor.posicionRival - p.motor.posicionJugador
            p.motor.moverJoystick(hacia * (1 / hacia.largo.coerceAtLeast(0.01)), p.t)
            p.motor.atacar(p.t)
            p.motor.terminado
        }
        assertEquals(Fase.VICTORIA, p.motor.fase)
        assertEquals(0, p.motor.vidaRival)
        assertTrue(p.motor.golpes > 0)
    }

    @Test
    fun `nadie sale del ring`() {
        val p = partida(75, luchador(TipoLuchador.TIJERA))
        p.motor.moverJoystick(Punto(-1.0, -1.0), 0.0)
        p.correr(10.0) {
            val j = p.motor.posicionJugador
            val r = p.motor.posicionRival
            listOf(j.x, j.y, r.x, r.y).any { it < MotorMinijuego.MIN_RING || it > MotorMinijuego.MAX_RING }
        }
        assertTrue(p.t >= 10.0 || p.motor.terminado)
    }
}
