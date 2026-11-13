package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.reloj
import com.rudoestudio.tiempofuera.domain.SuenoFake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MonitorBloqueoTest {
    private val hoy = LocalDate.of(2026, 11, 13)
    private val distraccion = TipoGrupoApps.DISTRACCION.clave

    private val selecciones = object : AppSelectionRepository {
        val estado = MutableStateFlow(mapOf(distraccion to SeleccionAppsGrupo(distraccion, setOf("com.instagram"))))
        override val selecciones: Flow<Map<String, SeleccionAppsGrupo>> = estado
        override suspend fun obtenerSeleccion(grupoId: String) = estado.value[grupoId]
        override suspend fun guardarSeleccion(seleccion: SeleccionAppsGrupo) { estado.value += (seleccion.grupoId to seleccion) }
    }
    private val bloqueos = object : AppBlockingRepository {
        override val misiones = MutableStateFlow<Map<String, BloqueoMision>>(emptyMap())
        override val tiemposExtra = MutableStateFlow<Map<String, Instant>>(emptyMap())
        override suspend fun iniciarBloqueo(grupoId: String, duracion: Duration?) {
            misiones.value += (grupoId to BloqueoMision(grupoId, null))
        }
        override suspend fun detenerBloqueo(grupoId: String) { misiones.value -= grupoId }
        override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) = Unit
        override suspend fun tieneTiempoExtraActivo(grupoId: String) = false
        override suspend fun estaBloqueoRigidoActivo(grupoId: String) = grupoId in misiones.value
    }
    private val configuraciones = object : ConfiguracionGrupoAppsRepository {
        val mapa = mutableMapOf<String, ConfiguracionGrupoApps>()
        override suspend fun obtenerConfiguracion(grupoId: String) = mapa[grupoId]
        override suspend fun obtenerTodas() = mapa.toMap()
        override suspend fun guardar(configuracion: ConfiguracionGrupoApps, grupoId: String) { mapa[grupoId] = configuracion }
        override suspend fun eliminar(grupoId: String) { mapa.remove(grupoId) }
    }
    private val uso = object : UsoApps {
        var minutos = 0L
        override suspend fun usoDeHoy() = mapOf("com.instagram" to Duration.ofMinutes(minutos))
        override suspend fun enPrimerPlano() = emptySet<String>()
    }
    private val tiempoBloqueado = object : TiempoBloqueadoRepository {
        var registro = RegistroTiempoBloqueado()
        lateinit var zona: ZoneId
        override suspend fun iniciarBloqueo(grupoId: String, motivo: MotivoBloqueo, fecha: Instant) { registro = registro.iniciar(grupoId, motivo.clave, fecha) }
        override suspend fun finalizarBloqueo(grupoId: String, motivo: MotivoBloqueo, fecha: Instant) { registro = registro.finalizar(grupoId, motivo.clave, fecha, zona) }
        override suspend fun sincronizar(grupos: Map<String, MotivoBloqueo>, fecha: Instant) { registro = registro.sincronizar(grupos, fecha, zona) }
        override suspend fun tiempoBloqueadoHoy(fecha: Instant, excluidos: List<PeriodoBloqueo>) = registro.segundosBloqueados(fecha, zona, excluidos)
    }
    private val progreso = object : ProgresoSeguimientoRepository {
        var acumulado = 0L
        var limite = false
        override suspend fun obtener(grupoId: String) = null
        override suspend fun registrarTiempoAcumulado(grupoId: String, segundosTotales: Long) { acumulado = maxOf(acumulado, segundosTotales) }
        override suspend fun marcarLimiteAlcanzado(grupoId: String) { limite = true }
        override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) = Unit
        override suspend fun registrarUltimoUso(grupoId: String, fecha: Instant) = Unit
        override suspend fun marcarRachaRecompensada(grupoId: String, fecha: Instant) = Unit
        override suspend fun unidadesTrackingReclamadas(grupoId: String, misionId: Int) = 0
        override suspend fun registrarUnidadesTrackingReclamadas(grupoId: String, misionId: Int, unidades: Int) = Unit
        override suspend fun gruposConLimiteAlcanzado() = if (limite) listOf(distraccion) else emptyList()
    }

    private fun monitor(reloj: Clock): MonitorBloqueo {
        tiempoBloqueado.zona = reloj.zone
        val estado = ObtenerEstadoBloqueoUseCase(selecciones, configuraciones, bloqueos, SuenoFake(), uso)
        return MonitorBloqueo(estado, tiempoBloqueado, progreso, reloj)
    }

    @Test
    fun `al cumplir el limite bloquea, lo marca y cuenta el tiempo bloqueado`() = runTest {
        configuraciones.guardar(ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, tiempoMaximoDiario = 3600), distraccion)
        uso.minutos = 59
        assertTrue(monitor(reloj(hoy, 12)).evaluar().isEmpty())
        assertEquals(59 * 60L, progreso.acumulado)

        uso.minutos = 60
        assertEquals(MotivoBloqueo.LIMITE, monitor(reloj(hoy, 12, 5)).evaluar()["com.instagram"]?.motivo)
        assertTrue(progreso.limite)

        // Una misión encima no cuenta doble; al quitar todo se cierra el periodo.
        bloqueos.iniciarBloqueo(distraccion)
        assertEquals(MotivoBloqueo.MISION, monitor(reloj(hoy, 12, 10)).evaluar()["com.instagram"]?.motivo)
        bloqueos.detenerBloqueo(distraccion)
        configuraciones.eliminar(distraccion)
        assertTrue(monitor(reloj(hoy, 12, 20)).evaluar().isEmpty())
        assertEquals(15 * 60L, tiempoBloqueado.tiempoBloqueadoHoy(reloj(hoy, 13).instant()))
    }

    @Test
    fun `si el servicio estuvo muerto, el periodo se cierra en la siguiente evaluacion`() = runTest {
        // Un bloqueo que quedó "abierto" en disco (el servicio murió antes de cerrarlo).
        tiempoBloqueado.zona = reloj().zone
        tiempoBloqueado.iniciarBloqueo(distraccion, MotivoBloqueo.MISION, reloj(hoy, 10).instant())
        monitor(reloj(hoy, 10, 30)).evaluar()
        assertEquals(30 * 60L, tiempoBloqueado.tiempoBloqueadoHoy(reloj(hoy, 18).instant()))
    }
}
