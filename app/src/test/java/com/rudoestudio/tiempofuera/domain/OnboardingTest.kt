package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.PasoOnboarding
import com.rudoestudio.tiempofuera.domain.onboarding.ReglasOnboarding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    @Test
    fun `los pasos van en el orden de iOS y no se regresa desde los que cierran una etapa`() {
        assertEquals(16, PasoOnboarding.entries.size)
        assertEquals(PasoOnboarding.CONF_PERMISOS, PasoOnboarding.CONF_USO_INICIAL.siguiente)
        assertNull(PasoOnboarding.HABITO_INICIAL.siguiente)
        assertFalse(PasoOnboarding.META.puedeRegresar)
        assertTrue(PasoOnboarding.CONF_PERMISOS.puedeRegresar)
    }

    @Test
    fun `el limite sugerido es media hora menos, minimo cinco minutos`() {
        assertEquals(450, ReglasOnboarding.limiteSugerido(480))
        assertEquals(5, ReglasOnboarding.limiteSugerido(30))
    }

    @Test
    fun `no deja elegir mas de tres divisiones pero si quitar`() {
        var seleccion = emptySet<DivisionMision>()
        DivisionMision.entries.forEach { seleccion = ReglasOnboarding.alternarDivision(seleccion, it) }
        assertEquals(DivisionMision.entries.take(3).toSet(), seleccion)
        seleccion = ReglasOnboarding.alternarDivision(seleccion, DivisionMision.entries.first())
        assertEquals(2, seleccion.size)
    }
}
