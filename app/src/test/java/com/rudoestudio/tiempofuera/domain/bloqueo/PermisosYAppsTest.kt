package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.features.bloqueo.EstadoSelectorApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosYAppsTest {
    private val apps = listOf(
        AppInstalada("com.spotify.music", "Spotify"),
        AppInstalada("com.android.music", "Música"),
        AppInstalada("com.google.android.youtube", "YouTube"),
        AppInstalada("com.instagram.android", "Instagram"),
    )

    @Test
    fun `la busqueda ignora mayusculas y acentos, y tambien busca por paquete`() {
        assertEquals(listOf("Música"), BuscadorApps.filtrar(apps, "MUSICA").map { it.nombre })
        assertEquals(listOf("Instagram"), BuscadorApps.filtrar(apps, "instagram.android").map { it.nombre })
        assertEquals(apps, BuscadorApps.filtrar(apps, "  "))
    }

    @Test
    fun `las elegidas salen primero sin perder el orden alfabetico`() {
        val ordenadas = BuscadorApps.ordenar(apps, setOf("com.google.android.youtube", "com.android.music"))
        assertEquals(listOf("Música", "YouTube", "Spotify", "Instagram"), ordenadas.map { it.nombre })
    }

    @Test
    fun `el selector filtra sin perder lo elegido`() {
        val estado = EstadoSelectorApps(cargando = false, apps = apps, elegidas = setOf("com.instagram.android"), consulta = "you")
        assertEquals(listOf("YouTube"), estado.visibles.map { it.nombre })
        assertEquals(setOf("com.instagram.android"), estado.elegidas)
    }

    @Test
    fun `la bateria es recomendada, no obligatoria`() {
        assertTrue(EstadoPermisos(accesibilidad = true, accesoAlUso = true, notificaciones = true, sinAhorroBateria = false).completos)
        assertFalse(EstadoPermisos(accesibilidad = false, accesoAlUso = true, notificaciones = true, sinAhorroBateria = true).completos)
    }
}
