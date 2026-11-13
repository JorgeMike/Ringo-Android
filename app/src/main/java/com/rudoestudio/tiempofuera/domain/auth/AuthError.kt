package com.rudoestudio.tiempofuera.domain.auth

/**
 * Errores del login (en iOS: `AuthError`). El mensaje viene del API cuando lo manda; si
 * no, el de aquí. El API no distingue código inexistente, vencido o ya usado (a propósito).
 */
sealed class AuthError(mensaje: String) : Exception(mensaje) {
    class CorreoInvalido(mensaje: String = "Revisa tu correo, algo no cuadra.") : AuthError(mensaje)

    class CodigoInvalido(mensaje: String = "Código incorrecto o vencido.") : AuthError(mensaje)

    class LimiteExcedido(mensaje: String = "Muchos intentos. Espera un momento y vuelve a intentar.") : AuthError(mensaje)

    class SinConexion : AuthError("Sin conexión. Revisa tu internet y vuelve a intentar.")

    class Desconocido(mensaje: String = "Algo falló de nuestro lado. Intenta de nuevo.") : AuthError(mensaje)
}
