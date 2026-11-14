package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_DIARIAS
import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.JuegoEscena
import com.rudoestudio.tiempofuera.domain.misiones.JuegoSecuencia
import com.rudoestudio.tiempofuera.domain.misiones.PuntoEscena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class JuegosMemoriaTest {
    private val base = CATALOGO_MISIONES_DIARIAS.first()

    @Test
    fun `cada ronda de la secuencia tiene un paso mas y usa solo los tambores de la mision`() {
        val mision = base.copy(configuracion = ConfiguracionMision(numSecuencias = 3, numTambores = 4))
        val rondas = JuegoSecuencia.generar(mision, Random(7))
        assertEquals(listOf(3, 4, 5), rondas.map { it.size })
        assertTrue(rondas.flatten().all { it in 0..3 })
    }

    @Test
    fun `el tiempo por paso baja cada ronda hasta el minimo`() {
        assertEquals(3 * 2.0 + 2, JuegoSecuencia.tiempoLimite(3, 0), 0.0001)
        assertEquals(8 * 0.8 + 2, JuegoSecuencia.tiempoLimite(8, 10), 0.0001)
    }

    @Test
    fun `los stickers de la escena caen dentro del marco y no se repiten mientras alcancen`() {
        val posiciones = JuegoEscena.posiciones(6, Random(3))
        assertTrue(posiciones.all { it.x in JuegoEscena.X_MIN..JuegoEscena.X_MAX && it.y in JuegoEscena.Y_MIN..JuegoEscena.Y_MAX })
        assertEquals(9, JuegoEscena.stickers(9, Random(1)).toSet().size)
        assertEquals(12, JuegoEscena.stickers(12, Random(1)).size)
    }

    @Test
    fun `tiempos de la escena y aciertos con su radio`() {
        assertEquals(4, JuegoEscena.tiempoMemorizar(3))
        assertEquals(7, JuegoEscena.tiempoMemorizar(20))
        assertEquals(10, JuegoEscena.tiempoColocar(3))
        val original = PuntoEscena(0.5, 0.5)
        assertTrue(JuegoEscena.esAcierto(PuntoEscena(0.55, 0.55), original))
        assertFalse(JuegoEscena.esAcierto(PuntoEscena(0.7, 0.5), original))
        assertFalse(JuegoEscena.esAcierto(null, original))
    }
}
