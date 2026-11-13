package com.rudoestudio.tiempofuera.data.bloqueo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.domain.bloqueo.ErrorBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.MotivoBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.SeleccionAppsGrupo
import com.rudoestudio.tiempofuera.domain.bloqueo.TiempoExtraAppsImpl
import com.rudoestudio.tiempofuera.domain.reloj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration
import java.time.LocalDate

class BloqueoRepositoriosTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val almacen by lazy { AlmacenJson(PreferenceDataStoreFactory.create(scope = alcance) { carpeta.newFile("juego.preferences_pb") }) }
    private val hoy = LocalDate.of(2026, 11, 13)
    private val mediodia = reloj(hoy, 12)

    @After
    fun cerrar() = alcance.cancel()

    @Test
    fun `bloquear sin apps o con duracion invalida falla`() = runTest {
        val selecciones = AppSelectionDataStore(almacen)
        val bloqueos = AppBlockingDataStore(almacen, selecciones, mediodia)
        try {
            bloqueos.iniciarBloqueo("distraccion", Duration.ofMinutes(10))
            fail()
        } catch (e: ErrorBloqueo.SinSeleccion) {
            // esperado
        }
        selecciones.guardarSeleccion(SeleccionAppsGrupo("distraccion", setOf("com.instagram")))
        try {
            bloqueos.iniciarBloqueo("distraccion", Duration.ofDays(1))
            fail()
        } catch (e: ErrorBloqueo.DuracionInvalida) {
            // esperado
        }
    }

    @Test
    fun `un bloqueo de mision vence solo y se limpia al revisarlo`() = runTest {
        val selecciones = AppSelectionDataStore(almacen)
        selecciones.guardarSeleccion(SeleccionAppsGrupo("distraccion", setOf("com.instagram")))
        AppBlockingDataStore(almacen, selecciones, mediodia).iniciarBloqueo("distraccion", Duration.ofMillis(1500))
        val fin = AppBlockingDataStore(almacen, selecciones, mediodia).misiones.first()["distraccion"]!!.fin
        assertEquals(mediodia.instant().plusSeconds(2), fin) // el fin se redondea hacia arriba

        assertTrue(AppBlockingDataStore(almacen, selecciones, mediodia).estaBloqueoRigidoActivo("distraccion"))
        val despues = AppBlockingDataStore(almacen, selecciones, reloj(hoy, 12, 1))
        assertFalse(despues.estaBloqueoRigidoActivo("distraccion"))
        assertTrue(despues.misiones.first().isEmpty())
    }

    @Test
    fun `tiempo extra activo hasta que vence`() = runTest {
        val selecciones = AppSelectionDataStore(almacen)
        val progreso = ProgresoSeguimientoDataStore(almacen, mediodia)
        TiempoExtraAppsImpl(AppBlockingDataStore(almacen, selecciones, mediodia), progreso).otorgarTiempoExtra("distraccion", 900)
        assertTrue(AppBlockingDataStore(almacen, selecciones, reloj(hoy, 12, 14)).tieneTiempoExtraActivo("distraccion"))
        assertFalse(AppBlockingDataStore(almacen, selecciones, reloj(hoy, 12, 15)).tieneTiempoExtraActivo("distraccion"))
        assertEquals(900L, progreso.obtener("distraccion")!!.segundosExtraOtorgados)
    }

    @Test
    fun `iOS - cobrar vueltas de tracking no consume el tiempo acumulado`() = runTest {
        val repo = ProgresoSeguimientoDataStore(almacen, mediodia)
        repo.registrarTiempoAcumulado("lectura", 900)
        repo.registrarUnidadesTrackingReclamadas("lectura", 102, 1)
        assertEquals(900L, repo.obtener("lectura")!!.segundosAcumulados)
        assertEquals(1, repo.unidadesTrackingReclamadas("lectura", 102))
        assertEquals(0, repo.unidadesTrackingReclamadas("lectura", 204))
    }

    @Test
    fun `el progreso de seguimiento es solo de hoy y el acumulado nunca baja`() = runTest {
        ProgresoSeguimientoDataStore(almacen, reloj(hoy.minusDays(1))).apply {
            registrarTiempoAcumulado("distraccion", 5000)
            marcarLimiteAlcanzado("distraccion")
        }
        val repo = ProgresoSeguimientoDataStore(almacen, mediodia)
        assertNull(repo.obtener("distraccion"))
        assertTrue(repo.gruposConLimiteAlcanzado().isEmpty())
        repo.registrarTiempoAcumulado("distraccion", 600)
        repo.registrarTiempoAcumulado("distraccion", 300)
        assertEquals(600L, repo.obtener("distraccion")!!.segundosAcumulados)
    }

    @Test
    fun `el tiempo bloqueado se guarda entre lecturas`() = runTest {
        TiempoBloqueadoDataStore(almacen, mediodia).iniciarBloqueo("distraccion", MotivoBloqueo.LIMITE, mediodia.instant())
        val repo = TiempoBloqueadoDataStore(almacen, mediodia)
        repo.finalizarBloqueo("distraccion", MotivoBloqueo.LIMITE, mediodia.instant().plusSeconds(600))
        assertEquals(600L, repo.tiempoBloqueadoHoy(mediodia.instant().plusSeconds(3600)))
    }
}
