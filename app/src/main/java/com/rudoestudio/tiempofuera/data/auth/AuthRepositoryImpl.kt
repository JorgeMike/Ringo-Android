package com.rudoestudio.tiempofuera.data.auth

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.data.red.ApiTiempoFuera
import com.rudoestudio.tiempofuera.data.red.PedirCodigoDto
import com.rudoestudio.tiempofuera.data.red.RefreshDto
import com.rudoestudio.tiempofuera.data.red.VerificarCodigoDto
import com.rudoestudio.tiempofuera.data.red.mensajeApi
import com.rudoestudio.tiempofuera.domain.auth.AuthError
import com.rudoestudio.tiempofuera.domain.auth.AuthRepository
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * En iOS: `AuthRepositoryImpl`. Allá un código incorrecto (400) se mapea como "correo
 * inválido" porque usa un solo `mapear` para todo; aquí cada endpoint traduce sus códigos.
 */
class AuthRepositoryImpl @Inject constructor(
    private val api: ApiTiempoFuera,
    private val json: Json,
) : AuthRepository {

    override suspend fun enviarCodigoVerificacion(email: String) {
        llamar(
            alError = { codigo, mensaje ->
                when (codigo) {
                    400 -> mensaje?.let(AuthError::CorreoInvalido) ?: AuthError.CorreoInvalido()
                    429 -> mensaje?.let(AuthError::LimiteExcedido) ?: AuthError.LimiteExcedido()
                    else -> mensaje?.let(AuthError::Desconocido) ?: AuthError.Desconocido() // 502: no salió el correo
                }
            },
        ) { api.pedirCodigo(PedirCodigoDto(email)) }
    }

    override suspend fun verificarCodigo(email: String, codigo: String): SesionUsuario =
        llamar(
            alError = { status, mensaje ->
                when (status) {
                    400 -> mensaje?.let(AuthError::CodigoInvalido) ?: AuthError.CodigoInvalido()
                    429 -> mensaje?.let(AuthError::LimiteExcedido) ?: AuthError.LimiteExcedido()
                    else -> mensaje?.let(AuthError::Desconocido) ?: AuthError.Desconocido()
                }
            },
        ) { api.verificarCodigo(VerificarCodigoDto(email, codigo)).aDominio() }

    override suspend fun cerrarSesion(refreshToken: String) {
        llamar(alError = { _, mensaje -> mensaje?.let(AuthError::Desconocido) ?: AuthError.Desconocido() }) {
            api.cerrarSesion(RefreshDto(refreshToken))
        }
    }

    private suspend fun <T> llamar(alError: (Int, String?) -> AuthError, bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: HttpException) {
            throw alError(e.code(), e.mensajeApi(json))
        } catch (e: IOException) {
            AppLogger.warning("Auth sin conexión: ${e.message}")
            throw AuthError.SinConexion()
        } catch (e: kotlinx.serialization.SerializationException) {
            AppLogger.error(e, "AuthRepository")
            throw AuthError.Desconocido()
        }
}
