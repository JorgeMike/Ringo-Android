package com.rudoestudio.tiempofuera.domain.onboarding

/**
 * Baja a la base local los luchadores y ligas que el usuario ya tenía en la nube (al
 * iniciar sesión en un teléfono nuevo). Todo o nada. En iOS: `PerfilLocalSeeder`.
 */
interface PerfilLocalSeeder {
    /** Lanza [LuchadorNoEncontradoEnCatalogo] si la nube trae un luchador que esta versión no conoce. */
    suspend fun sincronizar(luchadores: List<LuchadorProgresoRemoto>, ligas: List<ProgresoLigaRemoto>)
}

class LuchadorNoEncontradoEnCatalogo(val luchadorId: Int) :
    Exception("No se pudo actualizar tu colección con el catálogo disponible. Intenta actualizar la app.")
