package com.rudoestudio.tiempofuera.di

import com.rudoestudio.tiempofuera.data.onboarding.EstadoOnboardingEnMemoria
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Qué implementación usa cada repositorio del dominio (en iOS: `AppContainer`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {

    @Binds
    abstract fun estadoOnboarding(impl: EstadoOnboardingEnMemoria): EstadoOnboardingRepository
}
