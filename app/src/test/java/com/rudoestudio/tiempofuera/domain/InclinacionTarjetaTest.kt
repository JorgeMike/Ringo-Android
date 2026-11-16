package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.core.sensores.InclinacionTarjeta
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class InclinacionTarjetaTest {
    @Test
    fun `sostenido a 45 grados la tarjeta queda derecha`() {
        val (x, y) = InclinacionTarjeta.grados(cabeceo = (PI / 4).toFloat(), alabeo = 0f)
        assertEquals(0f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    @Test
    fun `cada radian son 18 grados al frente y 9 de lado`() {
        val (x, y) = InclinacionTarjeta.grados(cabeceo = (PI / 4).toFloat() + 0.5f, alabeo = 1f)
        assertEquals(9f, x, 0.001f)
        assertEquals(-9f, y, 0.001f)
    }

    @Test
    fun `no se pasa de 25 grados`() {
        val (x, y) = InclinacionTarjeta.grados(cabeceo = 5f, alabeo = -10f)
        assertEquals(25f, x, 0.001f)
        assertEquals(25f, y, 0.001f)
    }
}
