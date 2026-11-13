package com.rudoestudio.tiempofuera.servicios.bloqueo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los casos de la prueba 4 del spike: abrir, cerrar, pantalla dividida y ventana flotante. */
class SeguidorVentanasTest {
    private val pantalla = Zona(0, 0, 1080, 2340)
    private val barra = 100
    private val seguidor = SeguidorVentanas(barraFlotantePx = barra)

    @Test
    fun `pantalla completa se tapa al instante`() {
        val d = seguidor.decidir("yt", pantalla, pantalla, ahoraMs = 0)
        assertEquals(pantalla, d.zona)
        assertFalse(d.flotante)
    }

    @Test
    fun `una ventana que crece al abrirse tapa toda la pantalla sin esperar la animacion`() {
        val d = seguidor.decidir("yt", Zona(400, 900, 680, 1400), pantalla, ahoraMs = 0)
        assertEquals(pantalla, d.zona)
        assertNotNull(d.revisarEnMs)
    }

    @Test
    fun `una ventana que se encoge al cerrarse se destapa`() {
        seguidor.decidir("yt", Zona(100, 300, 980, 2000), pantalla, ahoraMs = 0)
        val d = seguidor.decidir("yt", Zona(300, 700, 780, 1600), pantalla, ahoraMs = 100)
        assertNull(d.zona)
    }

    @Test
    fun `en pantalla dividida tapa solo su mitad`() {
        val mitad = Zona(0, 0, 1080, 1170)
        assertEquals(mitad, seguidor.decidir("yt", mitad, pantalla, ahoraMs = 0).zona)
    }

    @Test
    fun `ventana flotante quieta deja libre su barra, y arrastrarla no la destapa`() {
        val flotante = Zona(163, 590, 811, 1911)
        seguidor.decidir("yt", flotante, pantalla, ahoraMs = 0)
        val quieta = seguidor.decidir("yt", flotante, pantalla, ahoraMs = 400)
        assertTrue(quieta.flotante)
        assertEquals(flotante.copy(arriba = 590 + barra), quieta.zona)

        val movida = Zona(263, 690, 911, 2011)
        val arrastrando = seguidor.decidir("yt", movida, pantalla, ahoraMs = 450)
        assertTrue(arrastrando.flotante)
        assertEquals(movida.copy(arriba = 690 + barra), arrastrando.zona)
    }

    @Test
    fun `olvida apps que ya no estan bloqueadas`() {
        seguidor.decidir("yt", Zona(100, 300, 980, 2000), pantalla, ahoraMs = 0)
        seguidor.conservarSolo(emptySet())
        // Sin historia, una ventana chica cuenta como "abriéndose": se tapa todo.
        assertEquals(pantalla, seguidor.decidir("yt", Zona(300, 700, 780, 1600), pantalla, ahoraMs = 100).zona)
    }
}
