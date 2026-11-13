package com.rudoestudio.tiempofuera.data.red

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// En iOS todo esto vive en URLSessionHTTPClient + TokenRefresher. Aquí se reparte en las
// piezas de OkHttp: un interceptor pone el token y un authenticator reacciona al 401.
// Corren en hilos de OkHttp, por eso usan runBlocking para hablar con el SesionStore.

/**
 * Canjea el refresh token por una sesión nueva. Es una sola a la vez: si varias
 * peticiones reciben 401 juntas, la primera renueva y las demás reusan su resultado.
 */
@Singleton
class RenovadorSesion @Inject constructor(
    @Named(CLIENTE_SIN_AUTH) private val cliente: OkHttpClient,
    @Named(URL_API) private val urlApi: HttpUrl,
    private val json: Json,
    private val sesionStore: SesionStore,
) {
    private val candado = Any()

    /**
     * Renueva si [tokenUsado] sigue siendo el vigente; si otro hilo ya lo cambió, regresa
     * esa sesión sin llamar al API. `null` si no hay sesión o no se pudo renovar.
     */
    fun renovar(tokenUsado: String?): SesionUsuario? = synchronized(candado) {
        val actual = runBlocking { sesionStore.actual() } ?: return null
        if (tokenUsado != null && actual.accessToken != tokenUsado) return actual

        val peticion = Request.Builder()
            .url(urlApi.resolve("auth/refresh")!!)
            .post(json.encodeToString(RefreshDto.serializer(), RefreshDto(actual.refreshToken)).toRequestBody(TIPO_JSON))
            .build()
        val respuesta = try {
            cliente.newCall(peticion).execute()
        } catch (e: IOException) {
            // Sin red no se cierra la sesión: el refresh token sigue sirviendo.
            AppLogger.warning("No se pudo renovar la sesión: ${e.message}")
            return null
        }
        respuesta.use { r ->
            when {
                r.isSuccessful -> {
                    val nueva = json.decodeFromString(SesionDto.serializer(), r.body!!.string()).aDominio()
                    runBlocking { sesionStore.guardar(nueva) }
                    nueva
                }
                // Refresh vencido, revocado o ya rotado: hay que volver a iniciar sesión.
                r.code == 400 || r.code == 401 -> {
                    AppLogger.warning("Refresh token rechazado (${r.code}): se cierra la sesión")
                    runBlocking { sesionStore.limpiar() }
                    null
                }
                else -> null
            }
        }
    }

    private companion object {
        val TIPO_JSON = "application/json".toMediaType()
    }
}

/** Pone `Authorization` en las rutas privadas, y renueva antes si el token ya caducó. */
class InterceptorAuth @Inject constructor(
    private val sesionStore: SesionStore,
    private val renovador: RenovadorSesion,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        if (peticion.url.encodedPath in RUTAS_PUBLICAS) return chain.proceed(peticion)
        var sesion = runBlocking { sesionStore.actual() } ?: return chain.proceed(peticion)
        if (sesion.estaExpirada()) {
            sesion = renovador.renovar(sesion.accessToken) ?: return chain.proceed(peticion)
        }
        return chain.proceed(peticion.newBuilder().header("Authorization", "Bearer ${sesion.accessToken}").build())
    }
}

/** Ante un 401 con token: renueva una vez y reintenta. Si vuelve a fallar, se rinde. */
class AutenticadorRenovacion @Inject constructor(
    private val renovador: RenovadorSesion,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val tokenUsado = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        val nueva = renovador.renovar(tokenUsado) ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer ${nueva.accessToken}").build()
    }
}

const val CLIENTE_SIN_AUTH = "clienteSinAuth"
const val URL_API = "urlApi"
