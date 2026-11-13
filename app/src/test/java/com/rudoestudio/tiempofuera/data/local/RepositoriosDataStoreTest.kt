package com.rudoestudio.tiempofuera.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rudoestudio.tiempofuera.data.configuracion.ConfiguracionGrupoAppsDataStore
import com.rudoestudio.tiempofuera.data.configuracion.ConfiguracionHabitoSuenoDataStore
import com.rudoestudio.tiempofuera.data.ligas.ProgresoMisionLigaDataStore
import com.rudoestudio.tiempofuera.data.misiones.MisionesDiariasDataStore
import com.rudoestudio.tiempofuera.data.perfil.EstadisticasEjercicioDataStore
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoMisionDiaria
import com.rudoestudio.tiempofuera.domain.perfil.RegistroCarrera
import com.rudoestudio.tiempofuera.domain.perfil.RegistroLagartijas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.LocalDate

class RepositoriosDataStoreTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private val alcance = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val datos by lazy { PreferenceDataStoreFactory.create(scope = alcance) { carpeta.newFile("juego.preferences_pb") } }
    private val almacen by lazy { AlmacenJson(datos) }

    private val hoy = LocalDate.of(2026, 11, 13)

    @After
    fun cerrar() = alcance.cancel()

    @Test
    fun `misiones diarias solo valen el dia en que se asignaron`() = runTest {
        val repo = MisionesDiariasDataStore(almacen)
        repo.guardarMisionesAsignadas(listOf(1001, 1002, 1003), hoy)
        assertEquals(listOf(1001, 1002, 1003), repo.obtenerMisionesAsignadas(hoy))
        assertNull(repo.obtenerMisionesAsignadas(hoy.plusDays(1)))
    }

    @Test
    fun `sustituir mision la deja en el mismo lugar`() = runTest {
        val repo = MisionesDiariasDataStore(almacen)
        repo.guardarMisionesAsignadas(listOf(1001, 1002, 1003), hoy)
        assertEquals(listOf(1001, 1040, 1003), repo.sustituirMision(1002, 1040, hoy))
        assertEquals(listOf(1001, 1040, 1003), repo.obtenerMisionesAsignadas(hoy))
        assertNull(repo.sustituirMision(9999, 1041, hoy))
        assertNull(repo.sustituirMision(1001, 1041, hoy.plusDays(1)))
    }

    @Test
    fun `progreso de misiones de liga se guarda, borra y limpia`() = runTest {
        val repo = ProgresoMisionLigaDataStore(almacen)
        val fecha = Instant.parse("2026-11-13T18:00:00Z")
        repo.guardarProgresoMision(ProgresoMisionDiaria(101, fecha, 0.5, 20.0))
        repo.guardarProgresoMision(ProgresoMisionDiaria(301, fecha, 1.0, 15.0))
        assertEquals(ProgresoMisionDiaria(101, fecha, 0.5, 20.0), repo.obtenerProgresoMision(101))
        repo.eliminarProgresoMision(101)
        assertEquals(listOf(301), repo.obtenerTodosLosProgresos().map { it.misionId })
        repo.eliminarTodoElProgreso()
        assertEquals(emptyList<ProgresoMisionDiaria>(), repo.obtenerTodosLosProgresos())
    }

    @Test
    fun `solo se guardan las 3 mejores carreras y lagartijas`() = runTest {
        val repo = EstadisticasEjercicioDataStore(almacen)
        val t = Instant.parse("2026-11-13T10:00:00Z")
        repo.registrarCarrera(RegistroCarrera(fecha = t, distanciaMetros = 1000.0, velocidadPromedioKmh = 8.0))
        repo.registrarCarrera(RegistroCarrera(fecha = t, distanciaMetros = 3000.0, velocidadPromedioKmh = 9.0))
        repo.registrarCarrera(RegistroCarrera(fecha = t, distanciaMetros = 1000.0, velocidadPromedioKmh = 10.0))
        repo.registrarCarrera(RegistroCarrera(fecha = t, distanciaMetros = 500.0, velocidadPromedioKmh = 12.0))
        assertEquals(listOf(3000.0 to 9.0, 1000.0 to 10.0, 1000.0 to 8.0), repo.obtenerTopCarreras().map { it.distanciaMetros to it.velocidadPromedioKmh })

        repo.registrarLagartijas(RegistroLagartijas(fecha = t, cantidad = 10))
        repo.registrarLagartijas(RegistroLagartijas(fecha = t.plusSeconds(60), cantidad = 10))
        repo.registrarLagartijas(RegistroLagartijas(fecha = t, cantidad = 25))
        repo.registrarLagartijas(RegistroLagartijas(fecha = t, cantidad = 5))
        val top = repo.obtenerTopLagartijas()
        assertEquals(listOf(25, 10, 10), top.map { it.cantidad })
        assertEquals(t.plusSeconds(60), top[1].fecha) // empate: la más reciente primero
    }

    @Test
    fun `configuracion de grupos y de sueño`() = runTest {
        val grupos = ConfiguracionGrupoAppsDataStore(almacen)
        val distraccion = ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, tiempoMaximoDiario = 3600, intervaloHito = 900)
        grupos.guardar(distraccion, "distraccion")
        grupos.guardar(ConfiguracionGrupoApps(TipoGrupoApps.EDUCATIVAS, intervaloHito = 300), "lectura")
        assertEquals(distraccion, grupos.obtenerConfiguracion("distraccion"))
        grupos.eliminar("lectura")
        assertEquals(setOf("distraccion"), grupos.obtenerTodas().keys)

        val sueno = ConfiguracionHabitoSuenoDataStore(almacen)
        assertNull(sueno.obtenerConfiguracionHabitoSueno())
        sueno.guardarConfiguracionHabitoSueno(ConfiguracionHabitoSueno(7, 23, 30))
        assertEquals(ConfiguracionHabitoSueno(7, 23, 30), sueno.obtenerConfiguracionHabitoSueno())
        sueno.eliminarConfiguracionHabitoSueno()
        assertNull(sueno.obtenerConfiguracionHabitoSueno())
    }

    @Test
    fun `un valor corrupto se trata como vacio, sin tronar`() = runTest {
        datos.edit { it[stringPreferencesKey("misionesDiarias.seed")] = "{esto no es json" }
        assertNull(MisionesDiariasDataStore(almacen).obtenerMisionesAsignadas(hoy))
    }
}
