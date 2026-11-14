package com.rudoestudio.tiempofuera.data.local

import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LIGAS
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_DIARIAS
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_LIGA
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** El catálogo se genera desde iOS con un script: esto atrapa una conversión rota. */
class CatalogoTest {
    private val idsLuchadores = CATALOGO_LUCHADORES.map { it.id }.toSet()

    @Test
    fun `mismos conteos que las semillas de iOS`() {
        assertEquals(22, CATALOGO_LUCHADORES.size)
        assertEquals(49, CATALOGO_MISIONES_DIARIAS.size)
        assertEquals(11, CATALOGO_MISIONES_LIGA.size)
        assertEquals(4, CATALOGO_LIGAS.size)
    }

    @Test
    fun `ids unicos y Ringo es el luchador 0`() {
        assertEquals(CATALOGO_LUCHADORES.size, idsLuchadores.size)
        val misiones = CATALOGO_MISIONES_DIARIAS + CATALOGO_MISIONES_LIGA
        assertEquals(misiones.size, misiones.map { it.id }.toSet().size)
        assertEquals("Ringo", CATALOGO_LUCHADORES.first { it.id == 0 }.nombre)
    }

    @Test
    fun `los luchadores arrancan sin recolectar y los legendarios son versatiles`() {
        assertTrue(CATALOGO_LUCHADORES.all { it.cantidad == 0 && it.puntosPoder == 0 })
        assertTrue(CATALOGO_LUCHADORES.filter { it.rareza == Rareza.LEGENDARIO }.all { it.tipo == TipoLuchador.VERSATIL })
    }

    @Test
    fun `misiones diarias tienen division, tipo y personalidad validos`() {
        CATALOGO_MISIONES_DIARIAS.forEach {
            assertTrue("${it.id}", it.id >= 1000)
            assertNotNull("${it.id}", it.division)
            assertNotNull("${it.id}", it.tipoMision)
            assertNotNull("${it.id}: ${it.categoria}", TipoPersonalidad.desdeClave(it.categoria))
        }
    }

    @Test
    fun `las ligas apuntan a luchadores y misiones que existen`() {
        val idsMisionesLiga = CATALOGO_MISIONES_LIGA.map { it.id }.toSet()
        CATALOGO_LIGAS.forEach { liga ->
            assertTrue(liga.luchadorEstrellaId in idsLuchadores)
            assertTrue(idsLuchadores.containsAll(liga.luchadoresLigaIds))
            assertTrue(idsMisionesLiga.containsAll(liga.misionIds))
            assertTrue(CATALOGO_MISIONES_LIGA.filter { it.id in liga.misionIds }.all { it.ligaId == liga.id })
        }
        assertEquals(listOf(404, 402, 403), CATALOGO_LIGAS.first { it.id == 4 }.misionIds)
    }

    @Test
    fun `las misiones de tracking dicen que grupo de apps miden`() {
        val tracking = CATALOGO_MISIONES_LIGA.filter { it.tipoMision == TipoMision.TRACKING }
        assertEquals(2, tracking.size)
        assertTrue(tracking.all { it.identificadorGrupoApps != null })
    }

    @Test
    fun `los datos de preview salen del catalogo`() {
        assertEquals("El Serenísimo", DatosPreview.perfil.luchadorSeleccionado.nombre)
        assertEquals(4, DatosPreview.ligas.size)
        assertNotNull(DatosPreview.mision(TipoMision.SECUENCIA))
    }

    @Test
    fun `configuraciones con decimales y listas se convirtieron`() {
        val carrera = CATALOGO_MISIONES_LIGA.first { it.id == 404 }
        assertEquals(1_000.0, carrera.configuracion.distanciaObjetivoMetros!!, 0.0)
        val secuencia = CATALOGO_MISIONES_DIARIAS.first { it.id == 1041 }
        assertEquals(3, secuencia.configuracion.secuencias!!.size)
        assertEquals(listOf("1", "3", "4", "2"), secuencia.configuracion.secuencias!!.first().secuencia)
    }

    @Test
    fun `cada luchador del catalogo tiene su imagen importada de iOS`() {
        val sinImagen = CATALOGO_LUCHADORES.filter { AssetsIos.imagen(it.imagen) == null }.map { it.nombre }
        assertEquals(emptyList<String>(), sinImagen)
    }
}
