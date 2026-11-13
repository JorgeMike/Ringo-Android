package com.rudoestudio.tiempofuera.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rudoestudio.tiempofuera.data.ligas.LigaRepositoryCatalogo
import com.rudoestudio.tiempofuera.data.ligas.ProgresoLigaRepositoryRoom
import com.rudoestudio.tiempofuera.data.luchadores.LuchadorRepositoryRoom
import com.rudoestudio.tiempofuera.data.misiones.MisionRepositoryCatalogo
import com.rudoestudio.tiempofuera.data.onboarding.PerfilLocalSeederRoom
import com.rudoestudio.tiempofuera.data.perfil.HistoricoRachaRepositoryRoom
import com.rudoestudio.tiempofuera.data.perfil.ProgresoDiarioRepositoryRoom
import com.rudoestudio.tiempofuera.data.perfil.UserProfileRepositoryRoom
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoMisionDiaria
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.luchadores.ErrorSubidaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorNoEncontradoEnCatalogo
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiario
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoriosRoomTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BaseDatosTiempoFuera::class.java)
        .allowMainThreadQueries()
        .build()
    private val luchadores = LuchadorRepositoryRoom(db.luchadores())
    private val perfiles = UserProfileRepositoryRoom(db.perfil(), db.luchadores())
    private val progresoLigas = ProgresoLigaRepositoryRoom(db.progresoLigas())

    @After
    fun cerrar() = db.close()

    @Test
    fun `recolectar suma uno, fija el nivel y la rareza sale del nivel`() = runTest {
        assertTrue(luchadores.obtenerInventario().isEmpty())
        luchadores.recolectarLuchador(1, nuevoNivel = 12)
        luchadores.recolectarLuchador(1, nuevoNivel = 250)
        val dummy = luchadores.obtenerPorId(1)!!
        assertEquals(2, dummy.cantidad)
        assertEquals(Rareza.LEGENDARIO, dummy.rareza)
        assertEquals(TipoLuchador.VERSATIL, dummy.tipo)
        assertEquals("No importa cuanto le peguen, siempre se levanta.", dummy.frase) // lo demás sale del catálogo
        assertEquals(listOf(1), luchadores.obtenerInventario().map { it.id })
    }

    @Test
    fun `subir nivel y sumar puntos a un luchador que no existe truena`() = runTest {
        luchadores.sumarPuntosPoder(5, 40)
        luchadores.sumarPuntosPoder(5, -10) // se ignora
        luchadores.subirNivel(5, 30)
        assertEquals(40, luchadores.obtenerPorId(5)!!.puntosPoder)
        assertEquals(Rareza.RARO, luchadores.obtenerPorId(5)!!.rareza)
        try {
            luchadores.subirNivel(999, 2)
            fail()
        } catch (e: ErrorSubidaNivel.LuchadorNoEncontrado) {
            // esperado
        }
    }

    @Test
    fun `sembrar ignora luchadores que no estan en el catalogo y sincroniza el inventario`() = runTest {
        luchadores.sembrar(
            listOf(
                LuchadorProgresoRemoto(2, 3, 60, 500, "epico", "default", null),
                LuchadorProgresoRemoto(999, 1, 1, 0, "comun", "default", null),
                LuchadorProgresoRemoto(6, 1, 300, 0, "inventada", "default", null),
            ),
        )
        assertEquals(listOf(2, 6), luchadores.obtenerParaSincronizar().map { it.luchadorId })
        assertEquals("legendario", luchadores.obtenerParaSincronizar().last().rareza) // rareza desconocida: sale del nivel
    }

    @Test
    fun `perfil nuevo arranca vacio con Ringo y se guarda completo`() = runTest {
        val nuevo = perfiles.obtenerPerfil()
        assertEquals(0, nuevo.luchadorSeleccionado.id)
        assertEquals(TipoPersonalidad.DIPLOMATICO, nuevo.personalidadId)
        assertEquals(PropositoUsuario.DEJAR_CELULAR, nuevo.proposito)

        val liga = ProgresoUsuarioLiga(
            ligaId = 2, totalExp = 120.5, nivelActualId = 4, nivelesDesbloqueados = setOf(1, 2, 3),
            progresoMisionesDiarias = listOf(ProgresoMisionDiaria(201, Instant.parse("2026-11-13T12:00:00Z"), 0.3, 15.0)),
        )
        val editado = nuevo.copy(
            personalidadId = TipoPersonalidad.EXPLORADOR, monedas = 300, diasRacha = 4, protectoresRacha = 2,
            lastActivityDate = Instant.parse("2026-11-13T12:00:00Z"), luchadorSeleccionado = luchadores.obtenerPorId(3)!!,
            progresoLigas = listOf(liga), proposito = PropositoUsuario.CUIDAR_CUERPO,
            divisionesSeleccionadas = setOf(DivisionMision.EJERCICIO, DivisionMision.LECTURA), horaInicioDia = 7, horaFinDia = 23,
        )
        perfiles.guardarPerfil(editado)
        assertEquals(editado, perfiles.obtenerPerfil())
    }

    @Test
    fun `el flujo del perfil avisa cuando sube de nivel su luchador`() = runTest {
        perfiles.guardarPerfil(perfiles.obtenerPerfil().copy(luchadorSeleccionado = luchadores.obtenerPorId(1)!!))
        luchadores.subirNivel(1, 15)
        assertEquals(15, perfiles.perfil.first().luchadorSeleccionado.nivel)
    }

    @Test
    fun `sembrar ligas deja exactamente las de la nube`() = runTest {
        assertEquals(listOf(ProgresoLigaRemoto(1, 0.0, 1, setOf(0))), progresoLigas.obtenerParaSincronizar())
        progresoLigas.guardarProgreso(ProgresoUsuarioLiga(ligaId = 1, totalExp = 50.0))
        progresoLigas.guardarProgreso(ProgresoUsuarioLiga(ligaId = 3, totalExp = 10.0))
        progresoLigas.sembrar(listOf(ProgresoLigaRemoto(3, 99.0, 5, setOf(1, 2)), ProgresoLigaRemoto(4, 1.0, 1, emptySet())))
        assertNull(progresoLigas.obtenerProgreso(1))
        assertEquals(ProgresoUsuarioLiga(3, 99.0, 5, setOf(1, 2)), progresoLigas.obtenerProgreso(3))
        assertEquals(listOf(3, 4), progresoLigas.obtenerParaSincronizar().map { it.ligaId })
    }

    @Test
    fun `el seeder es todo o nada`() = runTest {
        val seeder = PerfilLocalSeederRoom(db)
        try {
            seeder.sincronizar(listOf(LuchadorProgresoRemoto(1, 1, 5, 0, "comun", "default", null), LuchadorProgresoRemoto(999, 1, 1, 0, "comun", "default", null)), emptyList())
            fail()
        } catch (e: LuchadorNoEncontradoEnCatalogo) {
            assertEquals(999, e.luchadorId)
        }
        assertTrue(luchadores.obtenerInventario().isEmpty())

        progresoLigas.guardarProgreso(ProgresoUsuarioLiga(ligaId = 1, totalExp = 5.0))
        seeder.sincronizar(listOf(LuchadorProgresoRemoto(1, 2, 5, 10, "comun", "default", null)), listOf(ProgresoLigaRemoto(2, 30.0, 2, setOf(1))))
        assertEquals(2, luchadores.obtenerPorId(1)!!.cantidad)
        assertEquals(listOf(1, 2), progresoLigas.obtenerParaSincronizar().map { it.ligaId }) // no borra la liga 1
    }

    @Test
    fun `las ligas resuelven luchadores con su progreso y respetan repetidos`() = runTest {
        luchadores.recolectarLuchador(12, 40)
        val ligas = LigaRepositoryCatalogo(luchadores, MisionRepositoryCatalogo()).obtenerLigas()
        val saber = ligas.first { it.id == 1 }
        assertEquals(40, saber.luchadorEstrella.nivel)
        assertEquals(listOf(9, 9, 10, 11), saber.luchadoresLiga.map { it.id })
        assertEquals(listOf(101, 102), saber.misiones.map { it.id })
    }

    @Test
    fun `progreso diario e historico de racha por dia`() = runTest {
        val diario = ProgresoDiarioRepositoryRoom(db.progresoDiario())
        val hoy = LocalDate.of(2026, 11, 13)
        diario.guardarProgreso(ProgresoDiario(hoy, listOf(1001)))
        diario.guardarProgreso(ProgresoDiario(hoy.minusDays(1), listOf(1002, 1003)))
        diario.guardarProgreso(ProgresoDiario(hoy, listOf(1001, 1004)))
        assertEquals(listOf(1001, 1004), diario.obtenerProgreso(hoy)!!.misiones)
        assertEquals(listOf(hoy.minusDays(1), hoy), diario.obtenerHistorico().map { it.dia })

        val racha = HistoricoRachaRepositoryRoom(db.historicoRacha())
        racha.guardarRegistro(HistoricoRacha.completado(hoy))
        racha.guardarRegistro(HistoricoRacha.protegido(hoy))
        assertEquals(HistoricoRacha.protegido(hoy), racha.obtenerRegistro(hoy))
        assertNull(racha.obtenerRegistro(hoy.minusDays(1)))
    }
}
