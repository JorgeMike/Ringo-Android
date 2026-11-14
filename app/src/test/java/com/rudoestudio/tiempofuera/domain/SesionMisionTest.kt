package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_DIARIAS
import com.rudoestudio.tiempofuera.domain.bloqueo.AppBlockingRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.BloqueoMision
import com.rudoestudio.tiempofuera.domain.bloqueo.DetenerBloqueoAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.IniciarBloqueoAppsUseCase
import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionMisionRepository
import com.rudoestudio.tiempofuera.domain.misiones.SesionTiempo
import com.rudoestudio.tiempofuera.domain.misiones.SesionesMision
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class SesionMisionTest {
    private val inicio = reloj().instant()
    private val fija = CATALOGO_MISIONES_DIARIAS.first { it.tipoMision == TipoMision.TIEMPO }
        .let { it.copy(configuracion = ConfiguracionMision(duracionSegundos = 600)) }
    private val abierta = fija.copy(id = fija.id + 1, configuracion = ConfiguracionMision(segundosParaRecompensa = 300))

    @Test
    fun `tiempo fijo cuenta hacia abajo redondeando hacia arriba y se acaba solo`() {
        val sesion = SesionTiempo(fija.id, inicio, inicio.plusSeconds(600))
        assertEquals(600, RelojMision.segundosEnPantalla(fija, sesion, inicio))
        assertEquals(1, RelojMision.restante(sesion, inicio.plusMillis(599_500)))
        assertTrue(RelojMision.seAcabo(fija, sesion, inicio.plusSeconds(601)))
        assertEquals("10:00", RelojMision.formato(600))
    }

    @Test
    fun `modo abierto junta vueltas y el progreso es de la vuelta actual`() {
        val sesion = SesionTiempo(abierta.id, inicio, fin = null)
        val ahora = inicio.plusSeconds(750)
        assertEquals(2, RelojMision.vueltas(abierta, RelojMision.transcurrido(sesion, ahora)))
        assertEquals(0.5f, RelojMision.progreso(abierta, sesion, ahora), 0.001f)
    }

    @Test
    fun `iniciar bloquea con la duracion y rendirse libera y borra`() = runTest {
        val bloqueos = BloqueosFake()
        val repo = RepoFake()
        val notificaciones = NotificacionesFake()
        val sesiones = SesionesMision(repo, IniciarBloqueoAppsUseCase(bloqueos), DetenerBloqueoAppsUseCase(bloqueos), notificaciones, reloj())

        sesiones.iniciar(fija)
        assertEquals(Duration.ofSeconds(600), bloqueos.duraciones["distraccion"])
        assertEquals(inicio.plusSeconds(600), repo.estado.value?.fin)

        sesiones.rendirse(fija)
        assertNull(repo.estado.value)
        assertTrue("distraccion" !in bloqueos.duraciones)
        assertEquals(listOf(SesionesMision.idNotificacion(fija.id)), notificaciones.canceladas)
    }

    @Test
    fun `terminar en modo abierto congela los segundos para reclamar`() = runTest {
        val repo = RepoFake()
        val bloqueos = BloqueosFake()
        val iniciar = SesionesMision(repo, IniciarBloqueoAppsUseCase(bloqueos), DetenerBloqueoAppsUseCase(bloqueos), NotificacionesFake(), reloj())
        iniciar.iniciar(abierta)
        val despues = Clock.offset(reloj(), Duration.ofSeconds(650))
        SesionesMision(repo, IniciarBloqueoAppsUseCase(bloqueos), DetenerBloqueoAppsUseCase(bloqueos), NotificacionesFake(), despues).terminar(abierta)
        val sesion = repo.estado.value!!
        assertTrue(sesion.terminada)
        assertEquals(650, RelojMision.transcurrido(sesion, inicio.plusSeconds(9_999)))
        assertTrue("distraccion" !in bloqueos.duraciones)
    }

    private class RepoFake : SesionMisionRepository {
        val estado = MutableStateFlow<SesionTiempo?>(null)
        override val sesion = estado
        override suspend fun obtener() = estado.value
        override suspend fun guardar(sesion: SesionTiempo) { estado.value = sesion }
        override suspend fun borrar() { estado.value = null }
    }

    private class BloqueosFake : AppBlockingRepository {
        val duraciones = mutableMapOf<String, Duration?>()
        override val misiones = MutableStateFlow<Map<String, BloqueoMision>>(emptyMap())
        override val tiemposExtra = MutableStateFlow<Map<String, Instant>>(emptyMap())
        override suspend fun iniciarBloqueo(grupoId: String, duracion: Duration?) { duraciones[grupoId] = duracion }
        override suspend fun detenerBloqueo(grupoId: String) { duraciones -= grupoId }
        override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) = Unit
        override suspend fun tieneTiempoExtraActivo(grupoId: String) = false
        override suspend fun estaBloqueoRigidoActivo(grupoId: String) = grupoId in duraciones
    }
}
