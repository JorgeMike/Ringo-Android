package com.rudoestudio.tiempofuera.domain.onboarding

import kotlinx.coroutines.flow.Flow

/**
 * Si el usuario ya terminó el onboarding, y por dónde iba si no. `finalizado` decide si
 * `RootScreen` muestra el onboarding o las tabs (en iOS: `OnboardingCoordinator.haFinalizado`).
 */
interface EstadoOnboardingRepository {
    val finalizado: Flow<Boolean>

    /** Marca el onboarding como terminado y borra el progreso guardado. */
    suspend fun finalizar()

    /**
     * Paso y respuestas para retomar el onboarding si se cerró la app. En iOS: el paso en
     * `UserProfileRepository.guardarPasoOnboarding` y el borrador en UserDefaults
     * (`onboarding.borrador.v1`).
     */
    suspend fun obtenerProgreso(): ProgresoOnboarding?

    suspend fun guardarProgreso(progreso: ProgresoOnboarding)

    /** Vuelve a mostrar el onboarding desde el principio (Laboratorio; no existe en iOS). */
    suspend fun reiniciar()
}
