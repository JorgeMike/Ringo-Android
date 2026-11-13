package com.rudoestudio.tiempofuera.domain.auth

/**
 * Login por código al correo (en iOS: `AuthRepository`). Lanza [AuthError].
 * La renovación del access token no está aquí: la hace sola la capa HTTP al recibir un 401.
 */
interface AuthRepository {
    suspend fun enviarCodigoVerificacion(email: String)

    suspend fun verificarCodigo(email: String, codigo: String): SesionUsuario

    /** Revoca el refresh token en el API. Si no hay conexión, la sesión local se borra igual. */
    suspend fun cerrarSesion(refreshToken: String)
}
