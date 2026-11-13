package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.data.misiones.MisionRepositoryCatalogo
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.ligas.EvaluadorProgresoLiga
import com.rudoestudio.tiempofuera.domain.ligas.Liga
import com.rudoestudio.tiempofuera.domain.ligas.NivelLiga
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.ligas.RecolectarRecompensaLigaUseCase
import com.rudoestudio.tiempofuera.domain.ligas.RecompensaNivel
import com.rudoestudio.tiempofuera.domain.ligas.TipoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.ErrorSubidaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.IncrementoNivelDuplicado
import com.rudoestudio.tiempofuera.domain.luchadores.NivelPoderCalculator
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.RecolectarLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.SorteoLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.SubirDeNivelLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.misiones.CompletarMisionUseCase
import com.rudoestudio.tiempofuera.domain.misiones.MisionesEventos
import com.rudoestudio.tiempofuera.domain.misiones.MotorDeMisiones
import com.rudoestudio.tiempofuera.domain.misiones.ObtenerMisionesUseCase
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.perfil.ActivarRachaUseCase
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.ProcesarFinDeDiaUseCase
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiario
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.RecordatorioRacha
import com.rudoestudio.tiempofuera.domain.perfil.SembrarPerfilLocalUseCase
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.sincronizacion.ProgresoLocal
import com.rudoestudio.tiempofuera.domain.tienda.ComprarItemTiendaUseCase
import com.rudoestudio.tiempofuera.domain.tienda.ErrorTienda
import com.rudoestudio.tiempofuera.domain.tienda.ResultadoCompraTiempoExtra
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import kotlin.random.Random

/**
 * Casos de uso de M2. Los que dicen "iOS:" son copia de un test de Ringo-iOSTests
 * (SistemaRachaUseCaseTests, ProcesarFinDeDiaUseCaseTests, RecompensasAcumuladasTests).
 */
class CasosDeUsoTest {
    private val hoy = LocalDate.of(2026, 11, 13)
    private val perfiles = PerfilesFake()
    private val historico = HistoricoFake()
    private val notificaciones = NotificacionesFake()
    private val api = ApiFake()
    private val luchadores = LuchadoresFake()
    private val progresoLigas = ProgresoLigasFake()
    private val progresoDiario = ProgresoDiarioFake()
    private val misionesDiarias = MisionesDiariasFake()
    private val calculador = NivelPoderCalculator()

    private fun activarRacha() = ActivarRachaUseCase(historico, perfiles, notificaciones, reloj(hoy))

    private fun finDeDia(sueno: SuenoFake = SuenoFake()) = ProcesarFinDeDiaUseCase(perfiles, historico, api, sueno, reloj(hoy))

    private fun instante(hora: Int, minuto: Int = 0) = reloj(hoy, hora, minuto).instant()

    // region Racha

    @Test
    fun `iOS - activar la racha cancela los recordatorios del dia`() = runTest {
        activarRacha()()
        assertEquals(RecordatorioRacha.horas.map { RecordatorioRacha.identificador(hoy, it) }.toSet(), notificaciones.canceladas.toSet())
    }

    @Test
    fun `iOS - una mision despues de perder la racha empieza en un dia`() = runTest {
        historico.guardarRegistro(HistoricoRacha.completado(hoy.minusDays(1)))
        val resultado = activarRacha()()
        assertTrue(resultado.activada)
        assertEquals(1, resultado.diasDeRacha)
        assertEquals(1, perfiles.actual.diasRacha)
    }

    @Test
    fun `iOS - una mision despues de usar un protector activa la racha del dia`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 1)
        historico.guardarRegistro(HistoricoRacha.completado(hoy.minusDays(1)))
        historico.guardarRegistro(HistoricoRacha.protegido(hoy))
        val resultado = activarRacha()()
        assertTrue(resultado.activada)
        assertEquals(2, resultado.diasDeRacha)
        assertEquals(false, historico.obtenerRegistro(hoy)?.usoProtectorRacha)
    }

    @Test
    fun `iOS - completar otra mision no restaura la racha perdida ese mismo dia`() = runTest {
        historico.guardarRegistro(HistoricoRacha.completado(hoy))
        historico.guardarRegistro(HistoricoRacha.completado(hoy.minusDays(1)))
        val resultado = activarRacha()()
        assertFalse(resultado.activada)
        assertEquals(0, resultado.diasDeRacha)
    }

    @Test
    fun `iOS - al sembrar un perfil se descarta la racha remota vencida`() = runTest {
        api.perfilRemoto = PerfilOnboardingExistente(
            puntosPoder = 0, monedas = 0, diasRacha = 5, protectoresRacha = 1,
            lastActivityDate = reloj(hoy.minusDays(30)).instant(), luchadorEquipadoId = 0,
            proposito = PropositoUsuario.DEJAR_CELULAR, personalidadId = TipoPersonalidad.DIPLOMATICO,
            divisionesSeleccionadas = emptySet(), tiempoInicial = 0, horaInicioDia = 0, horaFinDia = 0, colchonUso = 0,
            luchadores = emptyList(), ligas = emptyList(),
        )
        SembrarPerfilLocalUseCase(api, perfiles, luchadores, progresoLigas, reloj(hoy))("racha@example.com")
        assertEquals(0, perfiles.actual.diasRacha)
        assertEquals(1, perfiles.actual.protectoresRacha)
        assertEquals("racha@example.com", perfiles.actual.email)
    }

    // endregion

    // region Fin de día

    @Test
    fun `iOS - un fallo de red permite reintentar el guardado`() = runTest {
        api.falla = IOException("sin internet")
        try {
            finDeDia()(instante(23, 30))
            fail("El error de red debe salir para poder reintentar")
        } catch (e: IOException) {
            // esperado
        }
        api.falla = null
        finDeDia()(instante(23, 30))
        assertEquals(1, api.progresosGuardados.size)
    }

    @Test
    fun `iOS - abrir antes del cierre no impide guardar despues`() = runTest {
        finDeDia()(instante(10))
        assertTrue(api.progresosGuardados.isEmpty())
        finDeDia()(instante(23, 30))
        assertEquals(1, api.progresosGuardados.size)
    }

    @Test
    fun `iOS - la racha vuelve a cero si no se activo ayer y no hay protectores`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 5, protectoresRacha = 0, lastActivityDate = reloj(hoy.minusDays(2)).instant())
        assertEquals(0, finDeDia()(instante(12)).diasRacha)
    }

    @Test
    fun `iOS - se usa un protector si no se activo la racha ayer`() = runTest {
        val anteayer = hoy.minusDays(2)
        perfiles.actual = perfiles.actual.copy(diasRacha = 4, protectoresRacha = 1, lastActivityDate = reloj(anteayer).instant())
        historico.guardarRegistro(HistoricoRacha.completado(anteayer))
        val perfil = finDeDia()(instante(12))
        assertEquals(4, perfil.diasRacha)
        assertEquals(0, perfil.protectoresRacha)
    }

    @Test
    fun `iOS - dias perdidos en una ausencia larga agotan los protectores y rompen la racha`() = runTest {
        val ultimoDia = hoy.minusDays(5)
        perfiles.actual = perfiles.actual.copy(diasRacha = 5, protectoresRacha = 2, lastActivityDate = reloj(ultimoDia).instant())
        historico.guardarRegistro(HistoricoRacha.completado(ultimoDia))
        val perfil = finDeDia()(instante(12))
        assertEquals(0, perfil.diasRacha)
        assertEquals(0, perfil.protectoresRacha)
        assertEquals(true, historico.obtenerRegistro(hoy.minusDays(4))?.usoProtectorRacha)
        assertEquals(true, historico.obtenerRegistro(hoy.minusDays(3))?.usoProtectorRacha)
        assertNull(historico.obtenerRegistro(hoy.minusDays(2)))
    }

    @Test
    fun `iOS - guarda en el API solo despues de la hora de fin del dia`() = runTest {
        val sueno = SuenoFake(ConfiguracionHabitoSueno(horaInicioDia = 7 * 60, horaFinDia = 23 * 60, colchonUso = 30))
        finDeDia(sueno)(instante(14))
        assertTrue(api.progresosGuardados.isEmpty())
        finDeDia(sueno)(instante(23, 15))
        assertEquals(1, api.progresosGuardados.size)
    }

    // endregion

    // region Tienda

    @Test
    fun `iOS - comprar tiempo extra consume el protector y registra el dia como protegido`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 7, protectoresRacha = 1, monedas = 100)
        val tiempoExtra = TiempoExtraFake()
        val resultado = ComprarItemTiendaUseCase(perfiles, historico, tiempoExtra, reloj(hoy)).comprarTiempoExtra("distraccion", 15 * 60, 30)
        assertEquals(ResultadoCompraTiempoExtra.PROTECTOR_CONSUMIDO, resultado)
        assertEquals(0, perfiles.actual.protectoresRacha)
        assertEquals(7, perfiles.actual.diasRacha)
        assertEquals(70, perfiles.actual.monedas)
        assertEquals(true, historico.obtenerRegistro(hoy)?.usoProtectorRacha)
        assertEquals(listOf("distraccion" to 900), tiempoExtra.otorgados)
    }

    @Test
    fun `iOS - comprar tiempo extra sin protector rompe la racha aunque no se haya activado hoy`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 7, protectoresRacha = 0, monedas = 100)
        val resultado = ComprarItemTiendaUseCase(perfiles, historico, TiempoExtraFake(), reloj(hoy)).comprarTiempoExtra("distraccion", 15 * 60, 30)
        assertEquals(ResultadoCompraTiempoExtra.RACHA_PERDIDA, resultado)
        assertEquals(0, perfiles.actual.diasRacha)
        assertNull(historico.obtenerRegistro(hoy))
    }

    @Test
    fun `sin monedas o con tiempo extra activo no se compra nada`() = runTest {
        perfiles.actual = perfiles.actual.copy(monedas = 10)
        val tienda = ComprarItemTiendaUseCase(perfiles, historico, TiempoExtraFake(activo = true), reloj(hoy))
        try {
            tienda.comprarPinata(9955)
            fail()
        } catch (e: ErrorTienda.MonedasInsuficientes) {
            // esperado
        }
        try {
            tienda.comprarTiempoExtra("distraccion", 900, 5)
            fail()
        } catch (e: ErrorTienda.TiempoExtraYaActivo) {
            // esperado
        }
        assertEquals(10, perfiles.actual.monedas)
    }

    // endregion

    // region Misiones

    private fun obtenerMisiones() =
        ObtenerMisionesUseCase(MisionRepositoryCatalogo(), progresoDiario, misionesDiarias, perfiles, reloj(hoy), Random(7))

    private fun completarMision() = CompletarMisionUseCase(
        perfiles, progresoDiario, progresoLigas, MisionRepositoryCatalogo(), misionesDiarias, activarRacha(), MisionesEventos(), reloj(hoy), Random(7),
    )

    @Test
    fun `iOS - la experiencia acumulada se guarda en la liga`() = runTest {
        val lagartijas = MisionRepositoryCatalogo().obtenerPorId(402)!! // liga 4, 10 de exp por lagartija
        val resultado = completarMision()(lagartijas, unidades = 7)
        assertEquals(70, resultado.recompensaOtorgada.exp)
        assertEquals(70.0, progresoLigas.obtenerProgreso(4)!!.totalExp, 0.0)
        assertNull(resultado.misionReemplazo)
    }

    @Test
    fun `las misiones del dia no cambian al volver a pedirlas`() = runTest {
        val primeras = obtenerMisiones()()
        assertEquals(ObtenerMisionesUseCase.CANTIDAD_MISIONES_VARIABLES, primeras.size)
        assertTrue(MotorDeMisiones.cumpleLimiteDeDivision(primeras))
        progresoDiario.guardarProgreso(ProgresoDiario(hoy, listOf(primeras[0].id)))
        val otraVez = obtenerMisiones()()
        assertEquals(primeras.map { it.id }.toSet(), otraVez.map { it.id }.toSet())
        assertTrue(otraVez.first { it.id == primeras[0].id }.completada)
    }

    @Test
    fun `completar una mision diaria paga, activa la racha y la sustituye en su lugar`() = runTest {
        val delDia = obtenerMisiones()()
        val elegida = delDia[2]
        val resultado = completarMision()(elegida, unidades = 1)

        assertTrue(resultado.racha.activada)
        assertEquals(elegida.recompensa.monedas ?: 0, perfiles.actual.monedas)
        val reemplazo = resultado.misionReemplazo!!
        assertTrue(reemplazo.id !in delDia.map { it.id })
        val asignadas = misionesDiarias.obtenerMisionesAsignadas(hoy)!!
        assertEquals(delDia.map { it.id }.indexOf(elegida.id), asignadas.indexOf(reemplazo.id))
        assertEquals(listOf(elegida.id), progresoDiario.obtenerProgreso(hoy)!!.misiones)
    }

    // endregion

    // region Luchadores

    @Test
    fun `recolectar por primera vez solo desbloquea, con los puntos de su nivel`() = runTest {
        val recolectar = RecolectarLuchadorUseCase(luchadores, api, calculador, backgroundScope)
        val resultado = recolectar(2) // Fénix, nivel 50
        assertFalse(resultado.esDuplicado)
        assertEquals(50, resultado.nivelNuevo)
        assertEquals(calculador.puntosRequeridos(50), luchadores.obtenerPorId(2)!!.puntosPoder)
        runCurrent() // el envío al API corre en el alcance de fondo
        assertEquals(listOf(2), api.luchadoresActualizados.map { it.id })
    }

    @Test
    fun `recolectar un repetido suma puntos y sube de nivel, aunque el API falle`() = runTest {
        api.falla = IOException("sin internet")
        val recolectar = RecolectarLuchadorUseCase(luchadores, api, calculador, backgroundScope)
        recolectar(1) // El Dummy, nivel 1
        val resultado = recolectar(1)
        assertTrue(resultado.esDuplicado)
        assertEquals(2, resultado.cantidadNueva)
        assertEquals(calculador.puntosRequeridos(IncrementoNivelDuplicado.incremento(Rareza.COMUN)), resultado.puntosPoderNuevo)
        assertEquals(2, resultado.nivelNuevo)
        assertEquals(2, luchadores.obtenerPorId(1)!!.nivel)
    }

    @Test
    fun `el primer sobre siempre trae a Ringo y los legendarios solo salen de sobres legendarios`() = runTest {
        val sorteo = SorteoLuchadorUseCase(luchadores, Random(3))
        assertEquals(0, sorteo(Rareza.EPICO).id)
        luchadores.recolectarLuchador(0, 1)
        repeat(20) { assertEquals(Rareza.COMUN, sorteo(Rareza.COMUN).rareza) }
        repeat(20) { assertEquals(Rareza.LEGENDARIO, sorteo(Rareza.LEGENDARIO).rareza) }
    }

    @Test
    fun `subir de nivel sin puntos suficientes no cambia nada`() = runTest {
        val dummy = luchadores.obtenerPorId(1)!!
        try {
            SubirDeNivelLuchadorUseCase(luchadores, calculador)(dummy, puntosPoderDisponibles = 1)
            fail()
        } catch (e: ErrorSubidaNivel.PuntosInsuficientes) {
            // esperado
        }
        assertEquals(dummy, luchadores.obtenerPorId(1))
        val resultado = SubirDeNivelLuchadorUseCase(luchadores, calculador)(dummy, puntosPoderDisponibles = 1000)
        assertEquals(2, resultado.nivelNuevo)
        assertEquals(calculador.costoParaSubirA(2), resultado.puntosGastados)
    }

    // endregion

    // region Ligas

    private val liga = Liga(1, "La llave del Saber", CATALOGO_LUCHADORES.first { it.id == 12 }, emptyList(), CATALOGO_LUCHADORES.take(3))

    @Test
    fun `una barrera sin vencer detiene el avance aunque sobre experiencia`() {
        val niveles = liga.niveles(Random(1))
        val progreso = ProgresoUsuarioLiga(1, totalExp = 1_000_000.0, nivelActualId = 1)
        val detenido = EvaluadorProgresoLiga.evaluarAvance(progreso, niveles)!!
        assertEquals(10, detenido.nivelActualId)
        assertNull(EvaluadorProgresoLiga.evaluarAvance(detenido, niveles))
        val vencida = EvaluadorProgresoLiga.evaluarAvance(detenido.copy(nivelesDesbloqueados = setOf(10)), niveles)!!
        assertEquals(25, vencida.nivelActualId)
    }

    @Test
    fun `cobrar un nivel da poder al luchador equipado, monedas y protector`() = runTest {
        luchadores.recolectarLuchador(1, 1)
        perfiles.actual = perfiles.actual.copy(luchadorSeleccionado = luchadores.obtenerPorId(1)!!)
        val nivel = NivelLiga(5, 0.0, TipoNivel.NORMAL, null, RecompensaNivel(puntosPoder = 100, monedas = 50, sobres = emptyList(), protectorRacha = true), false)

        val subida = RecolectarRecompensaLigaUseCase(perfiles, progresoLigas, luchadores, calculador, reloj(hoy))(1, nivel)!!

        assertEquals(calculador.nivelActual(100), subida.nivelNuevo)
        assertNotEquals(subida.nivelAnterior, subida.nivelNuevo)
        assertEquals(subida.nivelNuevo, luchadores.obtenerPorId(1)!!.nivel)
        assertEquals(50, perfiles.actual.monedas)
        assertEquals(1, perfiles.actual.protectoresRacha)
        assertTrue(5 in progresoLigas.obtenerProgreso(1)!!.nivelesDesbloqueados)
    }

    // endregion

    @Test
    fun `al sincronizar se manda la racha solo si sigue vigente`() = runTest {
        perfiles.actual = perfiles.actual.copy(diasRacha = 9, lastActivityDate = reloj(hoy.minusDays(3)).instant())
        assertEquals(0, ProgresoLocal(perfiles, luchadores, progresoLigas, reloj(hoy)).leer().progreso.diasRacha)
        perfiles.actual = perfiles.actual.copy(lastActivityDate = reloj(hoy.minusDays(1)).instant())
        assertEquals(9, ProgresoLocal(perfiles, luchadores, progresoLigas, reloj(hoy)).leer().progreso.diasRacha)
    }
}
