package com.rudoestudio.tiempofuera.data.red

import com.rudoestudio.tiempofuera.data.usuario.UserApiRepositoryImpl
import com.rudoestudio.tiempofuera.di.RedModule
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.sincronizacion.ActualizacionPerfil
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import com.rudoestudio.tiempofuera.domain.sincronizacion.ProgresoUsuarioRemoto
import com.rudoestudio.tiempofuera.domain.sincronizacion.SincronizacionProgreso
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** `UserApiRepositoryImpl` contra un servidor falso: rutas, cuerpos, mapeo y errores. */
class UserApiTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: UserApiRepositoryImpl
    private val json = RedModule.json()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        repo = UserApiRepositoryImpl(RedModule.api(OkHttpClient(), servidor.url("/"), json), json)
    }

    @After
    fun cerrar() {
        servidor.shutdown()
    }

    private fun responder(cuerpo: String, codigo: Int = 200) {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    private fun cuerpoEnviado(): JsonObject = json.parseToJsonElement(servidor.takeRequest().body.readUtf8()).jsonObject

    private inline fun <reified E : Throwable> esperar(bloque: () -> Unit): E {
        try {
            bloque()
        } catch (e: Throwable) {
            if (e is E) return e
            throw e
        }
        fail("Se esperaba ${E::class.simpleName}")
        error("")
    }

    @Test
    fun `el perfil completo se convierte al dominio`() = runBlocking {
        responder(
            """{"id":"u-1","email":"chavo@ejemplo.com","onboardingCompletado":true,"proposito":"cuidarCuerpo",
            "personalidad":"Exploradores","divisionesSeleccionadas":["ejercicio","lectura"],"tiempoInicial":120,
            "horaInicioDia":420,"horaFinDia":1380,"colchonUso":30,"puntosPoder":450,"monedas":120,"diasRacha":7,
            "protectoresRacha":1,"lastActivityDate":"2026-11-12T23:30:00.123Z","luchadorId":3,
            "luchadores":[{"luchadorId":3,"cantidad":2,"nivel":250,"puntosPoder":900,"rareza":"legendario","skinEquipada":"default","desbloqueadoEn":"2026-10-01T10:00:00Z"}],
            "ligas":[{"ligaId":2,"totalExp":1250.5,"nivelActualId":7,"nivelesDesbloqueados":[1,2,3]}]}""",
        )
        val perfil = repo.obtenerPerfil()
        assertEquals(PropositoUsuario.CUIDAR_CUERPO, perfil.proposito)
        assertEquals(TipoPersonalidad.EXPLORADOR, perfil.personalidadId)
        assertEquals(setOf(DivisionMision.EJERCICIO, DivisionMision.LECTURA), perfil.divisionesSeleccionadas)
        assertEquals(3, perfil.luchadorEquipadoId)
        assertEquals(Instant.parse("2026-11-12T23:30:00.123Z"), perfil.lastActivityDate)
        assertEquals(Instant.parse("2026-10-01T10:00:00Z"), perfil.luchadores.single().desbloqueadoEn)
        assertEquals(ProgresoLigaRemoto(2, 1250.5, 7, setOf(1, 2, 3)), perfil.ligas.single())
        assertEquals("/me", servidor.takeRequest().path)
    }

    @Test
    fun `un perfil a medio onboarding usa valores por defecto, pero un valor desconocido es error`() = runBlocking {
        responder("""{"id":"u-1","email":"a@b.c","onboardingCompletado":false,"puntosPoder":0,"monedas":0,"diasRacha":0,"protectoresRacha":0}""")
        val nuevo = repo.obtenerPerfil()
        assertEquals(PropositoUsuario.DEJAR_CELULAR, nuevo.proposito)
        assertEquals(0, nuevo.luchadorEquipadoId)

        responder("""{"id":"u-1","email":"a@b.c","onboardingCompletado":true,"proposito":"volarAlEspacio","puntosPoder":0,"monedas":0,"diasRacha":0,"protectoresRacha":0}""")
        esperar<ErrorApi.RespuestaDesconocida> { runBlocking { repo.obtenerPerfil() } }
        Unit
    }

    @Test
    fun `PATCH me con perfil incompleto respeta el mensaje del API`() = runBlocking {
        responder("""{"error":"Faltan respuestas del onboarding"}""", 400)
        val cambios = ActualizacionPerfil(PropositoUsuario.DEJAR_CELULAR, TipoPersonalidad.ANALISTA, setOf(DivisionMision.LECTURA, DivisionMision.EJERCICIO), 60, 420, 1380, 30, true)
        val error = esperar<ErrorApi.PerfilIncompleto> { runBlocking { repo.actualizarPerfil(cambios) } }
        assertEquals("Faltan respuestas del onboarding", error.message)
        val cuerpo = cuerpoEnviado()
        assertEquals("Analíticos", cuerpo["personalidad"]!!.jsonPrimitive.content)
        assertEquals(listOf("ejercicio", "lectura"), cuerpo["divisionesSeleccionadas"]!!.jsonArray.map { it.jsonPrimitive.content })

        responder("""{"error":"No autenticado"}""", 401)
        esperar<ErrorApi.NoAutenticado> { runBlocking { repo.actualizarPerfil(cambios) } }
        Unit
    }

    @Test
    fun `luchador y progreso van a sus rutas con la fecha en ISO`() = runBlocking {
        responder("""{"luchadorId":5,"nivel":12,"puntosPoder":300,"skinEquipada":"default","cantidad":3,"rareza":"raro","desbloqueadoEn":"2026-10-01T10:00:00Z"}""")
        repo.actualizarLuchador(Luchador(5, "EnchiLalo", 3, 12, 300, "", "", Rareza.RARO, TipoLuchador.PAPEL))
        val pedido = servidor.takeRequest()
        assertEquals("PUT /me/luchadores/5", "${pedido.method} ${pedido.path}")
        assertEquals("raro", json.parseToJsonElement(pedido.body.readUtf8()).jsonObject["rareza"]!!.jsonPrimitive.content)

        responder("""{"luchadorId":5,"puntosPoder":1,"monedas":2,"diasRacha":3,"protectoresRacha":0,"lastActivityDate":"2026-11-13T18:00:00Z"}""")
        repo.guardarProgresoUsuario(ProgresoUsuarioRemoto(5, 1, 2, 3, 0, Instant.parse("2026-11-13T18:00:00Z")))
        val progreso = servidor.takeRequest()
        assertEquals("PATCH /me/progreso", "${progreso.method} ${progreso.path}")
        assertEquals("2026-11-13T18:00:00Z", json.parseToJsonElement(progreso.body.readUtf8()).jsonObject["lastActivityDate"]!!.jsonPrimitive.content)
    }

    @Test
    fun `la sincronizacion omite luchadores vacios y conserva lo que el API no regresa`() = runBlocking {
        val local = SincronizacionProgreso(
            ligas = listOf(ProgresoLigaRemoto(1, 0.0, 1, setOf(0))),
            luchadores = emptyList(),
            progreso = ProgresoUsuarioRemoto(0, 10, 20, 1, 0, null),
        )
        responder("""{"progreso":{"luchadorId":null,"puntosPoder":10,"monedas":25,"diasRacha":1,"protectoresRacha":0,"lastActivityDate":null}}""")
        val resultado = repo.sincronizar(local)

        val cuerpo = cuerpoEnviado()
        assertFalse("luchadores" in cuerpo)
        assertEquals(1, cuerpo["ligas"]!!.jsonArray.size)
        assertEquals(local.ligas, resultado.ligas) // no vino en la respuesta: no se pierde
        assertEquals(0, resultado.progreso.luchadorId)
        assertEquals(25, resultado.progreso.monedas)
    }

    @Test
    fun `sin red es SinConexion`() = runBlocking {
        servidor.shutdown()
        esperar<ErrorApi.SinConexion> { runBlocking { repo.guardarProgresoUsuario(ProgresoUsuarioRemoto(0, 0, 0, 0, 0, null)) } }
        Unit
    }
}
