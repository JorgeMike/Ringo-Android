package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.configuracion.CampoConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.EdicionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdicionGrupoAppsTest {
    private val distraccion = TipoGrupoApps.DISTRACCION

    @Test
    fun `sin configuracion los campos salen apagados con una hora`() {
        val campos = EdicionGrupoApps.campos(distraccion, null)
        assertEquals(distraccion.camposConfigurables, campos.map { it.campo })
        assertTrue(campos.all { !it.habilitado && it.minutos == 60 })
        assertEquals("Sin configurar", EdicionGrupoApps.resumen(distraccion, null))
    }

    @Test
    fun `lo guardado se lee en minutos y se puede apagar`() {
        val actual = ConfiguracionGrupoApps(distraccion, tiempoMaximoDiario = 90 * 60L, intervaloHito = 15 * 60L)
        val campos = EdicionGrupoApps.campos(distraccion, actual)
        assertEquals(90, campos.first { it.campo == CampoConfiguracionGrupoApps.TIEMPO_MAXIMO_DIARIO }.minutos)

        val sinHito = campos.map { if (it.campo == CampoConfiguracionGrupoApps.INTERVALO_HITO) it.copy(habilitado = false) else it }
        val guardada = EdicionGrupoApps.aplicar(distraccion, actual, sinHito)
        assertEquals(90 * 60L, guardada.tiempoMaximoDiario)
        assertNull(guardada.intervaloHito)
        assertEquals("Tiempo máximo diario: 90min", EdicionGrupoApps.resumen(distraccion, guardada))
    }

    @Test
    fun `los minutos no pasan de su rango`() {
        val limite = EdicionGrupoApps.campos(distraccion, null).first()
        assertEquals(1, limite.ajustar(-500).minutos)
        assertEquals(24 * 60, limite.ajustar(5000).minutos)
        val hito = EdicionGrupoApps.campos(TipoGrupoApps.EDUCATIVAS, null).single()
        assertEquals(600, hito.ajustar(5000).minutos)
        assertFalse(TipoGrupoApps.SALUD.camposConfigurables.isNotEmpty())
    }
}
