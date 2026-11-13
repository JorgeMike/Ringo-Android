package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class MotorBloqueoTest {
    private val zona: ZoneId = ZoneId.of("America/Mexico_City")
    private val dia = LocalDate.of(2026, 11, 13)
    private fun a(hora: Int, minuto: Int = 0): Instant = LocalDateTime.of(dia, LocalTime.of(hora, minuto)).atZone(zona).toInstant()

    private val distraccion = TipoGrupoApps.DISTRACCION.clave
    private val educativas = TipoGrupoApps.EDUCATIVAS.clave
    private val base = EstadoBloqueo(
        selecciones = mapOf(
            distraccion to SeleccionAppsGrupo(distraccion, setOf("com.instagram", "com.youtube")),
            educativas to SeleccionAppsGrupo(educativas, setOf("com.duolingo", "com.youtube")),
        ),
    )

    @Test
    fun `sin nada configurado no se bloquea nada`() {
        assertTrue(MotorBloqueo.paquetesBloqueados(base, a(12), zona).isEmpty())
    }

    @Test
    fun `un bloqueo de mision dura hasta su fin, o siempre si no tiene`() {
        val conFin = base.copy(misiones = mapOf(distraccion to BloqueoMision(distraccion, a(12, 30))))
        assertEquals(MotivoBloqueo.MISION, MotorBloqueo.gruposBloqueados(conFin, a(12), zona)[distraccion])
        assertTrue(MotorBloqueo.gruposBloqueados(conFin, a(12, 30), zona).isEmpty())
        val sinFin = base.copy(misiones = mapOf(educativas to BloqueoMision(educativas, null)))
        assertEquals(setOf("com.duolingo", "com.youtube"), MotorBloqueo.paquetesBloqueados(sinFin, a(23), zona).keys)
    }

    @Test
    fun `el sueño bloquea solo distraccion, con el colchon y cruzando la medianoche`() {
        val estado = base.copy(sueno = ConfiguracionHabitoSueno(horaInicioDia = 7 * 60, horaFinDia = 23 * 60, colchonUso = 30))
        assertTrue(MotorBloqueo.gruposBloqueados(estado, a(22, 29), zona).isEmpty())
        assertEquals(mapOf(distraccion to MotivoBloqueo.SUENO), MotorBloqueo.gruposBloqueados(estado, a(22, 30), zona))
        assertEquals(mapOf(distraccion to MotivoBloqueo.SUENO), MotorBloqueo.gruposBloqueados(estado, a(3), zona))
        assertEquals(mapOf(distraccion to MotivoBloqueo.SUENO), MotorBloqueo.gruposBloqueados(estado, a(7, 29), zona))
        assertTrue(MotorBloqueo.gruposBloqueados(estado, a(7, 30), zona).isEmpty())
    }

    @Test
    fun `el limite suma las apps del grupo y el tiempo extra lo suspende`() {
        val estado = base.copy(
            configuraciones = mapOf(distraccion to ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, tiempoMaximoDiario = 3600)),
            usoHoy = mapOf("com.instagram" to Duration.ofMinutes(40), "com.youtube" to Duration.ofMinutes(19)),
        )
        assertTrue(MotorBloqueo.gruposBloqueados(estado, a(12), zona).isEmpty())
        val cumplido = estado.copy(usoHoy = estado.usoHoy + ("com.youtube" to Duration.ofMinutes(20)))
        assertEquals(MotivoBloqueo.LIMITE, MotorBloqueo.gruposBloqueados(cumplido, a(12), zona)[distraccion])
        val conExtra = cumplido.copy(tiemposExtra = mapOf(distraccion to a(12, 15)))
        assertTrue(MotorBloqueo.gruposBloqueados(conExtra, a(12), zona).isEmpty())
        assertEquals(MotivoBloqueo.LIMITE, MotorBloqueo.gruposBloqueados(conExtra, a(12, 15), zona)[distraccion])
    }

    @Test
    fun `una app en dos grupos toma el motivo mas fuerte`() {
        val estado = base.copy(
            misiones = mapOf(educativas to BloqueoMision(educativas, null)),
            sueno = ConfiguracionHabitoSueno(7 * 60, 23 * 60, 30),
        )
        val bloqueados = MotorBloqueo.paquetesBloqueados(estado, a(23), zona)
        assertEquals(GrupoBloqueado(educativas, MotivoBloqueo.MISION), bloqueados["com.youtube"])
        assertEquals(GrupoBloqueado(distraccion, MotivoBloqueo.SUENO), bloqueados["com.instagram"])
    }

    @Test
    fun `un grupo sin apps no se bloquea aunque tenga mision`() {
        val vacio = EstadoBloqueo(
            selecciones = mapOf(distraccion to SeleccionAppsGrupo(distraccion, emptySet())),
            misiones = mapOf(distraccion to BloqueoMision(distraccion, null)),
        )
        assertTrue(MotorBloqueo.gruposBloqueados(vacio, a(12), zona).isEmpty())
    }

    @Test
    fun `ventana de sueño sin colchon o con horas iguales`() {
        assertFalse(VentanaSueno.contiene(ConfiguracionHabitoSueno(8 * 60, 8 * 60, 0), LocalTime.of(8, 0)))
        assertTrue(VentanaSueno.contiene(ConfiguracionHabitoSueno(6 * 60, 22 * 60, 0), LocalTime.of(5, 59)))
        assertEquals(5L, MotorBloqueo.hitosAlcanzados(Duration.ofMinutes(26), 300))
    }

    // region Tiempo bloqueado (iOS: Ringo_iOSTests)

    private val utc = ZoneOffset.UTC
    private fun utc(dia: Int, hora: Int, minuto: Int = 0): Instant = LocalDateTime.of(2026, 10, dia, hora, minuto).toInstant(utc)

    @Test
    fun `iOS - motivos que se enciman cuentan una vez y se parten en la medianoche`() {
        val inicio = utc(12, 23)
        val medianoche = utc(13, 0)
        val registro = RegistroTiempoBloqueado()
            .iniciar("distraccion", "limite", inicio)
            .iniciar("distraccion", "sueno", medianoche)
            .finalizar("distraccion", "limite", medianoche.plusSeconds(1800), utc)
            .finalizar("distraccion", "sueno", medianoche.plusSeconds(3600), utc)
        assertEquals(3600L, registro.segundosBloqueados(inicio, utc))
        assertEquals(3600L, registro.segundosBloqueados(medianoche, utc))
    }

    @Test
    fun `iOS - el tiempo bloqueado no cuenta el sueño central pero si sus colchones`() {
        val inicioBloqueo = utc(11, 22, 30)
        val inicioSueno = utc(11, 23)
        val finSueno = utc(12, 7)
        val finBloqueo = utc(12, 7, 30)
        val registro = RegistroTiempoBloqueado()
            .iniciar("distraccion", "sueno", inicioBloqueo)
            .finalizar("distraccion", "sueno", finBloqueo, utc)
        val central = listOf(PeriodoBloqueo(inicioSueno, finSueno))
        assertEquals(1800L, registro.segundosBloqueados(finBloqueo, utc, central))
        assertEquals(1800L, registro.segundosBloqueados(inicioSueno, utc, central))
    }

    @Test
    fun `un bloqueo que sigue activo cuenta hasta ahora y se guardan 30 dias`() {
        val activo = RegistroTiempoBloqueado().iniciar("distraccion", "manual", utc(13, 10))
        assertEquals(1800L, activo.segundosBloqueados(utc(13, 10, 30), utc))
        val viejo = RegistroTiempoBloqueado()
            .iniciar("g", "manual", utc(1, 10)).finalizar("g", "manual", utc(1, 11), utc)
            .iniciar("g", "manual", LocalDateTime.of(2026, 11, 13, 10, 0).toInstant(utc))
            .finalizar("g", "manual", LocalDateTime.of(2026, 11, 13, 11, 0).toInstant(utc), utc)
        assertEquals(setOf(LocalDate.of(2026, 11, 13)), viejo.periodosPorDia.keys)
    }

    // endregion
}
