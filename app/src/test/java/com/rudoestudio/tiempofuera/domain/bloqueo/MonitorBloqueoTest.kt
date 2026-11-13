package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.reloj
import com.rudoestudio.tiempofuera.domain.NotificacionesFake
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
        var ultimoHito = 0L
        var ultimoUso: Instant? = null
        override suspend fun obtener(grupoId: String) =
            ProgresoSeguimientoGrupo(hoy, segundosAcumulados = acumulado, limiteMaximoAlcanzado = limite, ultimoUsoEn = ultimoUso, segundosUltimoHito = ultimoHito)
        override suspend fun registrarTiempoAcumulado(grupoId: String, segundosTotales: Long) { acumulado = maxOf(acumulado, segundosTotales) }
        override suspend fun marcarLimiteAlcanzado(grupoId: String) { limite = true }
        override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) = Unit
        override suspend fun registrarUltimoUso(grupoId: String, fecha: Instant) { ultimoUso = fecha }
        override suspend fun registrarHitoAvisado(grupoId: String, segundos: Long) { ultimoHito = maxOf(ultimoHito, segundos) }
        override suspend fun marcarRachaRecompensada(grupoId: String, fecha: Instant) = Unit
        override suspend fun unidadesTrackingReclamadas(grupoId: String, misionId: Int) = 0
        override suspend fun registrarUnidadesTrackingReclamadas(grupoId: String, misionId: Int, unidades: Int) = Unit
        override suspend fun gruposConLimiteAlcanzado() = if (limite) listOf(distraccion) else emptyList()
    }

    private val notificaciones = NotificacionesFake()

    private fun monitor(reloj: Clock): MonitorBloqueo {
        tiempoBloqueado.zona = reloj.zone
        val estado = ObtenerEstadoBloqueoUseCase(selecciones, configuraciones, bloqueos, SuenoFake(), uso)
        return MonitorBloqueo(estado, tiempoBloqueado, progreso, notificaciones, reloj)
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

    @Test
    fun `avisa cada hito una sola vez, y el ultimo uso se mueve solo si se uso`() = runTest {
        configuraciones.guardar(ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, intervaloHito = 900), distraccion)
        uso.minutos = 14
        monitor(reloj(hoy, 12)).evaluar()
        assertTrue(notificaciones.mostradas.isEmpty())
        assertEquals(reloj(hoy, 12).instant(), progreso.ultimoUso)

        uso.minutos = 31 // dos hitos de golpe: solo se avisa el último
        monitor(reloj(hoy, 12, 30)).evaluar()
        monitor(reloj(hoy, 12, 31)).evaluar()
        assertEquals(listOf("Llevas 31m en el cel bro, déjalo tantito 😅"), notificaciones.mostradas.map { it.second })
        assertEquals(reloj(hoy, 12, 30).instant(), progreso.ultimoUso) // a las 12:31 no hubo uso nuevo
    }

    @Test
    fun `las educativas avisan cada 5 minutos aunque no tengan intervalo`() = runTest {
        val educativas = TipoGrupoApps.EDUCATIVAS.clave
        selecciones.guardarSeleccion(SeleccionAppsGrupo(educativas, setOf("com.instagram")))
        configuraciones.guardar(ConfiguracionGrupoApps(TipoGrupoApps.EDUCATIVAS), educativas)
        uso.minutos = 10
        monitor(reloj(hoy, 12)).evaluar()
        assertTrue(notificaciones.mostradas.any { it.second.startsWith("Acumulaste 10m en tus apps educativas") })
    }

    @Test
    fun `cambiar el intervalo no deja avisos sin mandar`() = runTest {
        // Lo que pasó en el teléfono: con 1 min se llegó al tope de 10 avisos; luego el
        // intervalo cambió a 118 min y, al pasar de 118 min de uso, no avisó.
        configuraciones.guardar(ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, intervaloHito = 60), distraccion)
        uso.minutos = 117
        monitor(reloj(hoy, 12)).evaluar()
        assertEquals(1, notificaciones.mostradas.size)

        configuraciones.guardar(ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, intervaloHito = 118 * 60), distraccion)
        monitor(reloj(hoy, 12, 1)).evaluar()
        assertEquals(1, notificaciones.mostradas.size)
        uso.minutos = 118
        monitor(reloj(hoy, 12, 2)).evaluar()
        assertEquals("Llevas 118m en el cel bro, déjalo tantito 😅", notificaciones.mostradas.last().second)
    }
}
