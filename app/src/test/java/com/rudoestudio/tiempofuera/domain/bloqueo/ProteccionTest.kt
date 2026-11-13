package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.HistoricoFake
import com.rudoestudio.tiempofuera.domain.NotificacionesFake
import com.rudoestudio.tiempofuera.domain.PerfilesFake
import com.rudoestudio.tiempofuera.domain.reloj
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** M3.12 (detectar) y M3.13 (consecuencia: perder la racha, decidido el 13/11/2026). */
class ProteccionTest {
    private val hoy = LocalDate.of(2026, 11, 13)
    private val ahora = reloj(hoy, 12).instant()
    private val conectada = EstadoProteccion(conectadaDesde = ahora.minusSeconds(3600), ultimoLatido = ahora.minusSeconds(60))

    @Test
    fun `diagnostico de la proteccion`() {
        val d = VigilanteProteccion::diagnosticar
        assertEquals(DiagnosticoProteccion.SIN_CONFIGURAR, d(conectada, false, false, ahora))
        assertEquals(DiagnosticoProteccion.SIN_CONFIGURAR, d(EstadoProteccion(), false, true, ahora)) // nunca la prendió
        assertEquals(DiagnosticoProteccion.ACTIVA, d(conectada, true, true, ahora))
        assertEquals(DiagnosticoProteccion.APAGADA, d(conectada, false, true, ahora))
        assertEquals(DiagnosticoProteccion.ATORADA, d(conectada, true, true, ahora.plusSeconds(20 * 60)))
        assertEquals(DiagnosticoProteccion.ATORADA, d(EstadoProteccion(), true, true, ahora)) // prendida pero nunca conectó
    }

    private val perfiles = PerfilesFake()
    private val historico = HistoricoFake()
    private val notificaciones = NotificacionesFake()
    private fun penalizar() = PenalizarProteccionApagadaUseCase(perfiles, historico, reloj(hoy, 12))

    @Test
    fun `apagarla gasta un protector y deja el dia protegido`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 7, protectoresRacha = 2)
        assertEquals(ResultadoPenalizacion.PROTECTOR_GASTADO, penalizar()())
        assertEquals(7, perfiles.actual.diasRacha)
        assertEquals(1, perfiles.actual.protectoresRacha)
        assertEquals(true, historico.obtenerRegistro(hoy)?.usoProtectorRacha)
    }

    @Test
    fun `sin protector la racha se va a cero, y sin racha no se pierde nada`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 7, protectoresRacha = 0)
        assertEquals(ResultadoPenalizacion.RACHA_PERDIDA, penalizar()())
        assertEquals(0, perfiles.actual.diasRacha)
        perfiles.actual = perfiles.actual.copy(protectoresRacha = 1)
        assertEquals(ResultadoPenalizacion.NADA_QUE_PERDER, penalizar()())
        assertEquals(1, perfiles.actual.protectoresRacha)
    }

    private class ProteccionFake(var estado: EstadoProteccion) : ProteccionRepository {
        override suspend fun estado() = estado
        override suspend fun registrarConexion(fecha: Instant) { estado = EstadoProteccion(fecha, fecha) }
        override suspend fun registrarLatido(fecha: Instant) { estado = estado.copy(ultimoLatido = fecha) }
        override suspend fun registrarApagado(fecha: Instant) { estado = estado.copy(apagadaDesde = estado.apagadaDesde ?: fecha) }
        override suspend fun marcarConsecuenciaAplicada() { estado = estado.copy(consecuenciaAplicada = true) }
    }

    private fun atender(proteccion: ProteccionRepository, accesibilidad: Boolean, minuto: Int = 0) = AtenderProteccionUseCase(
        permisos = object : RevisorPermisos {
            override fun estado() = EstadoPermisos(accesibilidad, accesoAlUso = true, notificaciones = true, sinAhorroBateria = true)
        },
        proteccion = proteccion,
        selecciones = object : AppSelectionRepository {
            override val selecciones: Flow<Map<String, SeleccionAppsGrupo>> =
                MutableStateFlow(mapOf("distraccion" to SeleccionAppsGrupo("distraccion", setOf("com.instagram"))))
            override suspend fun obtenerSeleccion(grupoId: String) = null
            override suspend fun guardarSeleccion(seleccion: SeleccionAppsGrupo) = Unit
        },
        penalizar = penalizar(),
        notificaciones = notificaciones,
        reloj = reloj(hoy, 12, minuto),
    )

    @Test
    fun `la consecuencia se cobra una vez por apagado y vuelve a contar al reconectar`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 7, protectoresRacha = 2)
        val proteccion = ProteccionFake(conectada)
        assertEquals(DiagnosticoProteccion.APAGADA, atender(proteccion, accesibilidad = false)())
        atender(proteccion, accesibilidad = false, minuto = 15)() // el vigilante la vuelve a ver apagada
        assertEquals(1, perfiles.actual.protectoresRacha)
        assertEquals(1, notificaciones.mostradas.size)

        proteccion.registrarConexion(ahora)
        atender(proteccion, accesibilidad = false, minuto = 30)()
        assertEquals(0, perfiles.actual.protectoresRacha)
    }

    @Test
    fun `atorada solo avisa, y al abrir la app no avisa`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 7)
        val vieja = ProteccionFake(conectada.copy(ultimoLatido = ahora.minusSeconds(3600)))
        assertEquals(DiagnosticoProteccion.ATORADA, atender(vieja, accesibilidad = true)(avisarAtorada = false))
        assertTrue(notificaciones.mostradas.isEmpty())
        atender(vieja, accesibilidad = true)()
        assertTrue(notificaciones.mostradas.single().second.contains("Apágala y vuélvela a prender"))
        assertEquals(7, perfiles.actual.diasRacha)
    }
}
