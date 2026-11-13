package com.rudoestudio.tiempofuera.domain.auth

/**
 * Login por código al correo (en iOS: `AuthRepository`). Lanza [AuthError].
 * La renovación del access token no está aquí: la hace sola la capa HTTP al recibir un 401.
 */
interface AuthRepository {
    suspend fun enviarCodigoVerificacion(email: String)

    suspend fun verificarCodigo(email: String, codigo: String): SesionUsuario

    /** Revoca en el API el refresh token de la sesión actual (no borra la sesión local). */
    suspend fun cerrarSesion()
}
