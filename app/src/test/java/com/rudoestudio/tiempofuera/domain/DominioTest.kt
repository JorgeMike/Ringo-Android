package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.ligas.Liga
import com.rudoestudio.tiempofuera.domain.ligas.NivelLigaFactory
import com.rudoestudio.tiempofuera.domain.ligas.TipoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.NivelPoderCalculator
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.MotorDeMisiones
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision
import com.rudoestudio.tiempofuera.domain.onboarding.PasoOnboarding
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.RachaVigencia
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import com.rudoestudio.tiempofuera.domain.perfil.ValidadorEdad
import com.rudoestudio.tiempofuera.domain.tienda.CatalogoTienda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

private fun luchador(id: Int, rareza: Rareza = Rareza.COMUN, tipo: TipoLuchador = TipoLuchador.PIEDRA, puntos: Int = 0) =
    Luchador(id, "Luchador $id", 1, 1, puntos, "", "", rareza, tipo)

private fun mision(id: Int, division: DivisionMision?, tipo: TipoMision?) =
    Mision(id, "Misión $id", "", RecompensaMision(exp = 10), "Analíticos", ConfiguracionMision(), tipo, division)

private fun perfil(protectores: Int = 0, racha: Int = 5) = UserProfile(
    email = "chavo@ejemplo.com", personalidadId = TipoPersonalidad.ANALISTA, puntosPoder = 100, monedas = 50,
    diasRacha = racha, protectoresRacha = protectores, luchadorSeleccionado = luchador(1), progresoLigas = emptyList(),
    proposito = PropositoUsuario.DEJAR_CELULAR, divisionesSeleccionadas = setOf(DivisionMision.LECTURA),
    tiempoInicial = 300, tiempoMaxUso = 120, horaInicioDia = 420, horaFinDia = 1380, colchonInicioFin = 30,
)

/** Tests de Domain/Entities, Services y Factories (M2.1). Los marcados "(iOS)" son copia de Ringo-iOSTests. */
class DominioTest {

    // --- Luchadores y ligas ---

    @Test
    fun `(iOS) legendario siempre es versatil y todas las barreras usan a la estrella`() {
        val estrella = luchador(7, Rareza.LEGENDARIO, TipoLuchador.PIEDRA)
        assertEquals(TipoLuchador.VERSATIL, estrella.tipo)

        val liga = Liga(1, "Reyes de la Lona", estrella, emptyList(), emptyList())
        val barreras = NivelLigaFactory.generar(liga, Random(1)).filter { it.tipo == TipoNivel.BARRERA }
        assertEquals(5, barreras.size)
        assertTrue(barreras.all { it.luchadorBarrera?.id == estrella.id })
        assertEquals(
            listOf(TipoLuchador.TIJERA, TipoLuchador.PAPEL, TipoLuchador.PIEDRA, TipoLuchador.TIJERA, TipoLuchador.VERSATIL),
            barreras.map { it.tipoLuchadorBarrera },
        )

        // Si sube a legendario, queda versátil aunque se le asigne otro tipo.
        val ascenso = luchador(100, Rareza.EPICO, TipoLuchador.PIEDRA).copy(rareza = Rareza.LEGENDARIO, tipoBase = TipoLuchador.PAPEL)
        assertEquals(TipoLuchador.VERSATIL, ascenso.tipo)
    }

    @Test
    fun `niveles de liga - curva de experiencia y recompensas de barrera`() {
        assertEquals(0.0, NivelLigaFactory.expRequerida(1), 0.0)
        assertEquals(3.0, NivelLigaFactory.expRequerida(2), 0.0) // 0.28 + 3.2 = 3.48 → 3
        assertEquals(3061.0, NivelLigaFactory.expRequerida(100), 0.0) // 0.28·99² + 3.2·99 = 3061.08

        val nivel10 = NivelLigaFactory.recompensa(10, esBarrera = true)
        assertEquals(110, nivel10.puntosPoder) // 80 + 1·30
        assertEquals(150, nivel10.monedas)
        assertEquals(listOf(Rareza.COMUN), nivel10.sobres)
        assertTrue(nivel10.protectorRacha)

        val nivel100 = NivelLigaFactory.recompensa(100, esBarrera = true)
        assertEquals(80 + 5 * 30, nivel100.puntosPoder) // multiplicador 100/25+1 = 5
        assertEquals(listOf(Rareza.LEGENDARIO, Rareza.EPICO, Rareza.EPICO), nivel100.sobres)

        val nivel7 = NivelLigaFactory.recompensa(7, esBarrera = false)
        assertEquals(11, nivel7.puntosPoder)
        assertEquals(18, nivel7.monedas)
        assertTrue(nivel7.sobres.isEmpty())
        assertFalse(nivel7.protectorRacha)
    }

    @Test
    fun `rareza por nivel de liga respeta hitos y rangos`() {
        val azar = Random(42)
        assertEquals(Rareza.COMUN, Rareza.calcularRareza(10, azar))
        assertEquals(Rareza.RARO, Rareza.calcularRareza(25, azar))
        assertEquals(Rareza.LEGENDARIO, Rareza.calcularRareza(100, azar))
        repeat(500) {
            assertEquals(Rareza.COMUN, Rareza.calcularRareza(15, azar))
            assertTrue(Rareza.calcularRareza(30, azar) in setOf(Rareza.COMUN, Rareza.RARO))
            assertTrue(Rareza.calcularRareza(60, azar) in setOf(Rareza.RARO, Rareza.EPICO))
            assertTrue(Rareza.calcularRareza(80, azar) != Rareza.COMUN)
        }
    }

    @Test
    fun `piedra papel tijera`() {
        assertTrue(TipoLuchador.PIEDRA.estaEnDesventaja(TipoLuchador.PAPEL))
        assertTrue(TipoLuchador.PAPEL.estaEnDesventaja(TipoLuchador.TIJERA))
        assertTrue(TipoLuchador.TIJERA.estaEnDesventaja(TipoLuchador.PIEDRA))
        assertFalse(TipoLuchador.PIEDRA.estaEnDesventaja(TipoLuchador.TIJERA))
        assertFalse(TipoLuchador.VERSATIL.estaEnDesventaja(TipoLuchador.PAPEL))
    }

    @Test
    fun `curva de nivel de luchador`() {
        val c = NivelPoderCalculator()
        assertEquals(0, c.puntosRequeridos(1))
        assertEquals(25, c.puntosRequeridos(2))
        assertEquals(59, c.puntosRequeridos(3)) // 25·2^1.25 = 59.46
        assertEquals(1, c.nivelActual(24))
        assertEquals(2, c.nivelActual(25))
        assertEquals(34, c.costoParaSubirA(3))
        assertEquals(0.5, c.progreso(42), 0.01) // a medio camino entre 25 y 59
        assertEquals(17, c.puntosParaSiguienteNivel(42))
        assertEquals(Rareza.RARO, c.rareza(10))
        assertEquals(Rareza.LEGENDARIO, c.rareza(250))
    }

    // --- Misiones ---

    @Test
    fun `(iOS) nunca mas de dos misiones por division`() {
        val pool = DivisionMision.entries.flatMapIndexed { i, division ->
            (0 until 5).map { mision(i * 10 + it + 10_000, division, TipoMision.ACCIONES) }
        }
        val pesos = DivisionMision.entries.associateWith { 1.0 }
        val azar = Random(7)
        repeat(50) {
            val elegidas = MotorDeMisiones.seleccionarMisionesDiarias(pool, pesos, cantidad = 5, azar = azar)
            assertEquals(5, elegidas.size)
            assertEquals(5, elegidas.map { it.id }.toSet().size)
            assertTrue(MotorDeMisiones.cumpleLimiteDeDivision(elegidas))
        }
    }

    @Test
    fun `(iOS) la mision de tracking cuenta para el limite de division`() {
        val lectura1 = mision(1, DivisionMision.LECTURA, TipoMision.LECTURAS)
        val lectura2 = mision(2, DivisionMision.LECTURA, TipoMision.TIEMPO)
        val tracking = mision(3, DivisionMision.LECTURA, TipoMision.TRACKING)
        assertTrue(MotorDeMisiones.puedeAgregar(tracking, listOf(lectura1)))
        assertFalse(MotorDeMisiones.puedeAgregar(tracking, listOf(lectura1, lectura2)))
    }

    @Test
    fun `sin misiones con division rellena con las que no tienen`() {
        val pool = listOf(mision(1, DivisionMision.CONECTAR, TipoMision.TIEMPO), mision(2, null, null), mision(3, null, null))
        val elegidas = MotorDeMisiones.seleccionarMisionesDiarias(pool, emptyMap(), cantidad = 3, azar = Random(3))
        assertEquals(setOf(1, 2, 3), elegidas.map { it.id }.toSet())
    }

    @Test
    fun `pesos - la division elegida pesa 4 veces mas que una no elegida igual`() {
        val pesos = MotorDeMisiones.calcularPesos(perfil())
        // Lectura (elegida): 15 · 1.2 · 2.0 = 36. Conectar (no elegida): 15 · 0.6 · 0.5 = 4.5.
        assertEquals(36.0, pesos.getValue(DivisionMision.LECTURA), 1e-9)
        assertEquals(4.5, pesos.getValue(DivisionMision.CONECTAR), 1e-9)
    }

    @Test
    fun `(iOS) cero vueltas no regala la recompensa`() {
        val base = RecompensaMision(exp = 10, puntosPoder = 5, monedas = 2)
        assertEquals(0, base.multiplicada(0).exp)
        assertEquals(30, base.multiplicada(3).exp)
        assertEquals(15, base.multiplicada(3).puntosPoder)
        assertEquals(6, base.multiplicada(3).monedas)
    }

    @Test
    fun `misiones iguales por id`() {
        assertEquals(mision(1, DivisionMision.LECTURA, TipoMision.TIEMPO), mision(1, null, null).completar())
    }

    // --- Perfil y racha ---

    @Test
    fun `protectores de racha tienen tope de 3 y salvan la racha`() {
        val ahora = Instant.parse("2026-11-13T12:00:00Z")
        assertEquals(3, perfil(protectores = 2).sumarProtectorRacha(5, ahora).protectoresRacha)

        val salvado = perfil(protectores = 1, racha = 9).rachaPerdida(ahora)
        assertEquals(9, salvado.diasRacha)
        assertEquals(0, salvado.protectoresRacha)

        assertEquals(0, perfil(protectores = 0, racha = 9).rachaPerdida(ahora).diasRacha)
        assertNull(perfil(protectores = 0).usarProtectorRacha(ahora))
    }

    @Test
    fun `agregar recompensa suma poder monedas y protector`() {
        val ahora = Instant.parse("2026-11-13T12:00:00Z")
        val p = perfil().agregarRecompensa(RecompensaMision(exp = 99, puntosPoder = 10, monedas = 5, protectorRacha = 1), ahora)
        assertEquals(110, p.puntosPoder)
        assertEquals(55, p.monedas)
        assertEquals(1, p.protectoresRacha)
        assertEquals(ahora, p.lastActivityDate)
        assertEquals(0, perfil().gastarMonedas(500).monedas) // nunca negativo
    }

    @Test
    fun `racha vigente solo si la ultima actividad fue hoy o ayer`() {
        val utc = ZoneOffset.UTC
        val hoy = Instant.parse("2026-11-13T09:00:00Z")
        assertEquals(4, RachaVigencia.diasVigentes(4, Instant.parse("2026-11-13T01:00:00Z"), hoy, utc))
        assertEquals(4, RachaVigencia.diasVigentes(4, Instant.parse("2026-11-12T23:59:00Z"), hoy, utc))
        assertEquals(0, RachaVigencia.diasVigentes(4, Instant.parse("2026-11-11T12:00:00Z"), hoy, utc))
        assertEquals(0, RachaVigencia.diasVigentes(4, null, hoy, utc))
    }

    @Test
    fun `edad minima de 13 anos`() {
        val hoy = LocalDate.of(2026, 11, 13)
        assertTrue(ValidadorEdad.esMayorDeEdad(LocalDate.of(2013, 11, 13), hoy = hoy))
        assertFalse(ValidadorEdad.esMayorDeEdad(LocalDate.of(2013, 11, 14), hoy = hoy))
    }

    // --- Tienda y onboarding ---

    @Test
    fun `precio de la pinata como en iOS`() {
        // (120 + 500 + 800 + 2200) / 4 = 905 → ×5 = 4525 → ×2.2 = 9955
        assertEquals(9955, CatalogoTienda.pinata.precioEnergia)
        val rarezas = CatalogoTienda.generarRarezasPinata(azar = Random(9))
        assertEquals(5, rarezas.size)
        assertEquals(rarezas.sorted(), rarezas)
    }

    @Test
    fun `pasos del onboarding`() {
        assertEquals(16, PasoOnboarding.entries.size)
        assertEquals(PasoOnboarding.ESTADISTICA, PasoOnboarding.BIENVENIDA.siguiente)
        assertNull(PasoOnboarding.HABITO_INICIAL.siguiente)
        assertFalse(PasoOnboarding.META.puedeRegresar)
        assertTrue(PasoOnboarding.DATOS.puedeRegresar)
    }
}
