package com.rudoestudio.tiempofuera.data.onboarding

import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Provisional hasta M1.7: se pierde al cerrar la app, así que siempre arranca en onboarding. */
@Singleton
class EstadoOnboardingEnMemoria @Inject constructor() : EstadoOnboardingRepository {
    private val estado = MutableStateFlow(false)

    override val finalizado: StateFlow<Boolean> = estado

    override suspend fun finalizar() {
        estado.value = true
    }
}
