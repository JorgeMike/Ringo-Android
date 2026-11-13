package com.rudoestudio.tiempofuera.domain.luchadores

import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto

/**
 * Los luchadores del catálogo con el progreso del usuario encima. En iOS:
 * `LuchadorRepositoryProtocol`.
 */
interface LuchadorRepository {
    suspend fun obtenerTodos(): List<Luchador>

    suspend fun obtenerPorId(id: Int): Luchador?

    /** Los que el usuario ya tiene (cantidad > 0). */
    suspend fun obtenerInventario(): List<Luchador>

    /** Suma uno a la cantidad y fija el nivel; la rareza sale del nivel. */
    suspend fun recolectarLuchador(luchadorId: Int, nuevoNivel: Int)

    /**
     * Fija el nivel; la rareza sale del nivel, como en iOS (allá recibe `nuevaRareza`
     * pero no la usa). Lanza [ErrorSubidaNivel.LuchadorNoEncontrado].
     */
    suspend fun subirNivel(luchadorId: Int, nuevoNivel: Int)

    /** Lanza [ErrorSubidaNivel.LuchadorNoEncontrado]. Puntos ≤ 0 no hacen nada. */
    suspend fun sumarPuntosPoder(luchadorId: Int, puntos: Int)

    /** Sobrescribe el progreso local con el de la nube; ignora ids que no están en el catálogo. */
    suspend fun sembrar(desde: List<LuchadorProgresoRemoto>)

    suspend fun obtenerParaSincronizar(): List<LuchadorProgresoRemoto>
}

sealed class ErrorSubidaNivel(mensaje: String) : Exception(mensaje) {
    data object PuntosInsuficientes : ErrorSubidaNivel("No tienes suficientes puntos de poder para subir de nivel.") {
        private fun readResolve(): Any = PuntosInsuficientes
    }

    data object LuchadorNoEncontrado : ErrorSubidaNivel("No se encontró el luchador.") {
        private fun readResolve(): Any = LuchadorNoEncontrado
    }
}
