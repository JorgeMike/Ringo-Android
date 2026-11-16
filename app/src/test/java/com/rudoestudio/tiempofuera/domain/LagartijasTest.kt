package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.ContadorRepeticiones
import com.rudoestudio.tiempofuera.domain.misiones.SesionLagartijas
import com.rudoestudio.tiempofuera.domain.misiones.SesionLagartijas.Estado
import com.rudoestudio.tiempofuera.domain.misiones.SuperficieLagartijas
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_DIARIAS
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LagartijasTest {
    private val base = CATALOGO_MISIONES_DIARIAS.first { it.tipoMision == TipoMision.LAGARTIJAS }

    @Test
    fun `plano es boca arriba y casi horizontal`() {
        assertTrue(SuperficieLagartijas.esPlana(9.81f))
        assertTrue(SuperficieLagartijas.esPlana(9.6f)) // ~12°
        assertFalse(SuperficieLagartijas.esPlana(9.0f)) // ~23°
        assertFalse(SuperficieLagartijas.esPlana(-9.81f)) // boca abajo
    }

    @Test
    fun `cuenta al subir despues de bajar, con histeresis y antirrebote`() {
        val c = ContadorRepeticiones()
        assertFalse(c.evaluar(0.2f, 0.0))
        assertFalse(c.evaluar(0.5f, 0.5)) // bajó
        assertTrue(c.cerca)
        assertFalse(c.evaluar(0.35f, 0.6)) // entre umbrales: sigue cerca
        assertTrue(c.evaluar(0.1f, 1.0)) // subió: una
        assertFalse(c.evaluar(0.5f, 1.1))
        assertFalse(c.evaluar(0.0f, 1.2)) // muy rápido: rebote, no cuenta
        assertFalse(c.evaluar(0.5f, 1.8))
        assertTrue(c.evaluar(0.0f, 2.0)) // sin cara cuenta como lejos
    }

    @Test
    fun `con meta se completa al llegar y paga una vez`() {
        var s = SesionLagartijas.para(base.copy(configuracion = ConfiguracionMision(repeticionesObjetivo = 2)))
        assertEquals(Estado.VERIFICANDO_SUPERFICIE, s.estado)
        assertEquals(s, s.iniciar()) // sin superficie no empieza
        s = s.superficie(false)
        assertEquals(Estado.SUPERFICIE_INVALIDA, s.estado)
        s = s.superficie(true).iniciar()
        assertTrue(s.enCurso)
        s = s.superficie(false).repeticion() // ya empezada, levantarlo no la detiene
        assertTrue(s.enCurso)
        s = s.repeticion()
        assertTrue(s.completado)
        assertEquals(1, s.unidadesRecompensa)
        assertEquals(s, s.repeticion())
    }

    @Test
    fun `con tiempo cuenta las que salgan y paga cada una`() {
        var s = SesionLagartijas.para(base.copy(configuracion = ConfiguracionMision(duracionSegundos = 3)))
        s = s.superficie(true).iniciar().repeticion().segundo().repeticion().segundo()
        assertEquals(1, s.restante)
        assertTrue(s.enCurso)
        s = s.segundo()
        assertTrue(s.completado)
        assertEquals(2, s.unidadesRecompensa)
        assertEquals("¡Tiempo! Hiciste 2 repeticiones 💪", s.mensajeCompletado)
    }
}
