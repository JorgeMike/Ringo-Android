package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.ligas.EvaluadorProgresoLiga
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class LigasCaminoTest {
    private val niveles = DatosPreview.ligas.first().niveles(Random(1))

    @Test
    fun `se ve hasta la siguiente barrera sin vencer`() {
        val inicio = ProgresoUsuarioLiga(1)
        assertEquals(10, EvaluadorProgresoLiga.nivelesVisibles(niveles, inicio).last().id)
        val vencida = inicio.copy(nivelesDesbloqueados = setOf(10))
        assertEquals(25, EvaluadorProgresoLiga.nivelesVisibles(niveles, vencida).last().id)
    }

    @Test
    fun `la experiencia del siguiente nivel sale de la curva`() {
        val progreso = ProgresoUsuarioLiga(1, nivelActualId = 3)
        assertEquals(niveles.first { it.id == 4 }.expRequerida, EvaluadorProgresoLiga.expSiguienteNivel(niveles, progreso)!!, 0.0)
        assertEquals(null, EvaluadorProgresoLiga.expSiguienteNivel(niveles, progreso.copy(nivelActualId = 100)))
    }
}
