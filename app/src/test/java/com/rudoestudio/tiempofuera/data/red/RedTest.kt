package com.rudoestudio.tiempofuera.data.red

import com.rudoestudio.tiempofuera.data.auth.AuthRepositoryImpl
import com.rudoestudio.tiempofuera.di.RedModule
import com.rudoestudio.tiempofuera.domain.auth.AuthError
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger

private class SesionStoreEnMemoria(inicial: SesionUsuario? = null) : SesionStore {
    private val estado = MutableStateFlow(inicial)
    override val sesion: StateFlow<SesionUsuario?> = estado
    override suspend fun actual() = estado.value
    override suspend fun guardar(sesion: SesionUsuario) { estado.value = sesion }
    override suspend fun limpiar() { estado.value = null }
}

private fun sesion(access: String, refresh: String = "refresh-1", creadaEnMs: Long = System.currentTimeMillis()) =
    SesionUsuario(access, refresh, 3600, "u-1", "chavo@ejemplo.com", false, creadaEnMs)

private fun sesionJson(access: String, refresh: String) =
    """{"accessToken":"$access","refreshToken":"$refresh","expiraEnSegundos":3600,
       "usuario":{"id":"u-1","email":"chavo@ejemplo.com","onboardingCompletado":true}}"""

private const val PERFIL_JSON = """{"id":"u-1","email":"chavo@ejemplo.com","onboardingCompletado":true,
    "puntosPoder":10,"monedas":5,"diasRacha":2,"protectoresRacha":0,"luchadores":[],"ligas":[],"campoNuevo":1}"""

class RedTest {
    private lateinit var servidor: MockWebServer
    private lateinit var store: SesionStoreEnMemoria
    private lateinit var api: ApiTiempoFuera
    private lateinit var repo: AuthRepositoryImpl

    private fun armar(inicial: SesionUsuario?) {
        store = SesionStoreEnMemoria(inicial)
        val json = RedModule.json()
        val url = servidor.url("/")
        val sinAuth = OkHttpClient()
        val renovador = RenovadorSesion(sinAuth, url, json, store)
        val cliente = RedModule.cliente(sinAuth, InterceptorAuth(store, renovador), AutenticadorRenovacion(renovador))
        api = RedModule.api(cliente, url, json)
        repo = AuthRepositoryImpl(api, json)
    }

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun cerrar() {
        servidor.shutdown()
    }

    @Test
    fun `verificar codigo regresa la sesion y no manda token`() = runBlocking {
        armar(null)
        servidor.enqueue(MockResponse().setBody(sesionJson("acc", "ref")))
        val s = repo.verificarCodigo("chavo@ejemplo.com", "123456")
        assertEquals("acc", s.accessToken)
        assertTrue(s.onboardingCompletado)
        val peticion = servidor.takeRequest()
        assertEquals("/auth/verificar", peticion.path)
        assertNull(peticion.getHeader("Authorization"))
        assertEquals("""{"email":"chavo@ejemplo.com","codigo":"123456"}""", peticion.body.readUtf8())
    }

    @Test
    fun `codigo incorrecto es CodigoInvalido con el mensaje del API`() = runBlocking {
        armar(null)
        servidor.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"Código incorrecto o vencido."}"""))
        try {
            repo.verificarCodigo("chavo@ejemplo.com", "000000")
            fail("Debió fallar")
        } catch (e: AuthError.CodigoInvalido) {
            assertEquals("Código incorrecto o vencido.", e.message)
        }
    }

    @Test
    fun `pedir codigo mapea 400, 429 y falta de red`() = runBlocking {
        armar(null)
        servidor.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"Correo inválido"}"""))
        servidor.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"Espera un minuto antes de pedir otro código."}"""))
        assertTrue(runCatching { repo.enviarCodigoVerificacion("x") }.exceptionOrNull() is AuthError.CorreoInvalido)
        val limite = runCatching { repo.enviarCodigoVerificacion("x@y.com") }.exceptionOrNull()
        assertTrue(limite is AuthError.LimiteExcedido)
        assertEquals("Espera un minuto antes de pedir otro código.", limite?.message)
        servidor.shutdown()
        assertTrue(runCatching { repo.enviarCodigoVerificacion("x@y.com") }.exceptionOrNull() is AuthError.SinConexion)
    }

    @Test
    fun `ruta privada lleva el token y tolera campos nuevos del API`() = runBlocking {
        armar(sesion("acc-1"))
        servidor.enqueue(MockResponse().setBody(PERFIL_JSON))
        assertEquals(10, api.perfilCompleto().puntosPoder)
        assertEquals("Bearer acc-1", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `un 401 renueva la sesion y reintenta`() = runBlocking {
        armar(sesion("viejo"))
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Token inválido"}"""))
        servidor.enqueue(MockResponse().setBody(sesionJson("nuevo", "refresh-2")))
        servidor.enqueue(MockResponse().setBody(PERFIL_JSON))

        api.perfilCompleto()

        assertEquals("Bearer viejo", servidor.takeRequest().getHeader("Authorization"))
        val refresh = servidor.takeRequest()
        assertEquals("/auth/refresh", refresh.path)
        assertEquals("""{"refreshToken":"refresh-1"}""", refresh.body.readUtf8())
        assertEquals("Bearer nuevo", servidor.takeRequest().getHeader("Authorization"))
        assertEquals("refresh-2", store.actual()?.refreshToken)
    }

    @Test
    fun `refresh rechazado cierra la sesion`() = runBlocking {
        armar(sesion("viejo"))
        servidor.enqueue(MockResponse().setResponseCode(401))
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Sesión inválida. Vuelve a iniciar sesión."}"""))
        try {
            api.perfilCompleto()
            fail("Debió fallar")
        } catch (e: HttpException) {
            assertEquals(401, e.code())
        }
        assertNull(store.actual())
    }

    @Test
    fun `sesion caducada se renueva antes de mandar la peticion`() = runBlocking {
        armar(sesion("caducado", creadaEnMs = System.currentTimeMillis() - 2 * 3_600_000))
        servidor.enqueue(MockResponse().setBody(sesionJson("fresco", "refresh-2")))
        servidor.enqueue(MockResponse().setBody(PERFIL_JSON))

        api.perfilCompleto()

        assertEquals("/auth/refresh", servidor.takeRequest().path)
        assertEquals("Bearer fresco", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `varios 401 al mismo tiempo renuevan una sola vez`() = runBlocking {
        armar(sesion("viejo"))
        val renovaciones = AtomicInteger()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/auth/refresh" -> {
                    renovaciones.incrementAndGet()
                    Thread.sleep(200) // que las demás peticiones lleguen mientras se renueva
                    MockResponse().setBody(sesionJson("nuevo", "refresh-2"))
                }
                else ->
                    if (request.getHeader("Authorization") == "Bearer nuevo") MockResponse().setBody(PERFIL_JSON)
                    else MockResponse().setResponseCode(401)
            }
        }
        val llamadas = (1..5).map { async(kotlinx.coroutines.Dispatchers.IO) { api.perfilCompleto() } }
        assertEquals(5, llamadas.awaitAll().size)
        assertEquals(1, renovaciones.get())
    }
}
