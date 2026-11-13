package com.rudoestudio.tiempofuera.domain.auth

/**
 * Sesión con tiempo-fuera-api (en iOS: `SesionUsuario`). El access token dura poco (1 h);
 * el refresh token, 60 días y se rota en cada renovación.
 */
data class SesionUsuario(
    val accessToken: String,
    val refreshToken: String,
    val expiraEnSegundos: Int,
    val usuarioId: String,
    val email: String,
    val onboardingCompletado: Boolean,
    /** Cuándo se obtuvo el access token, en milisegundos desde epoch. */
    val creadaEnMs: Long,
) {
    val expiraEnMs: Long get() = creadaEnMs + expiraEnSegundos * 1000L

    /**
     * Con [margenMs] de anticipación, para no mandar un token que caduca en el camino.
     * Si el reloj del teléfono está mal, el API responde 401 y se renueva igual.
     */
    fun estaExpirada(ahoraMs: Long = System.currentTimeMillis(), margenMs: Long = 30_000): Boolean =
        ahoraMs >= expiraEnMs - margenMs
}
