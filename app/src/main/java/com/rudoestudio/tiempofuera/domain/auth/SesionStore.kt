package com.rudoestudio.tiempofuera.domain.auth

import kotlinx.coroutines.flow.Flow

/** Dónde vive la sesión entre aperturas de la app (en iOS: `SesionStore`, en Keychain). */
interface SesionStore {
    /** La sesión actual; emite `null` si no hay o si se cerró. */
    val sesion: Flow<SesionUsuario?>

    suspend fun actual(): SesionUsuario?

    suspend fun guardar(sesion: SesionUsuario)

    suspend fun limpiar()
}
