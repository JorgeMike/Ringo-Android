package com.rudoestudio.tiempofuera.domain.onboarding

import kotlinx.coroutines.flow.Flow

/**
 * Si el usuario ya terminó el onboarding. Decide si `RootScreen` muestra el onboarding o
 * las tabs (en iOS: `OnboardingCoordinator.haFinalizado`). Se persiste en M1.7.
 */
interface EstadoOnboardingRepository {
    val finalizado: Flow<Boolean>

    suspend fun finalizar()
}
