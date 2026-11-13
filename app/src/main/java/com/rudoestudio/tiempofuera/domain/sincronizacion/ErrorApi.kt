package com.rudoestudio.tiempofuera.domain.sincronizacion

/**
 * Errores de las rutas `/me/…`. En iOS: `APIError`. Todos son excepciones: quien llama
 * decide si reintenta (la sincronización en segundo plano reintenta [SinConexion]).
 */
sealed class ErrorApi(mensaje: String) : Exception(mensaje) {
    class SinConexion : ErrorApi("Sin conexión. Tu progreso se guarda en el teléfono y se sube después.")

    class NoAutenticado : ErrorApi("Tu sesión expiró. Inicia sesión de nuevo.")

    /** `PATCH /me` con el onboarding marcado como completo pero respuestas faltantes. */
    class PerfilIncompleto(mensaje: String = "Tu perfil quedó incompleto.") : ErrorApi(mensaje)

    /** El API mandó un valor que esta versión de la app no conoce (p. ej. un propósito nuevo). */
    class RespuestaDesconocida(detalle: String) : ErrorApi("Actualiza la app para cargar tu perfil ($detalle).")

    class Desconocido(mensaje: String = "Algo falló de nuestro lado. Intenta de nuevo.") : ErrorApi(mensaje)
}
