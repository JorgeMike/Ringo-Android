package com.rudoestudio.tiempofuera.di

import com.rudoestudio.tiempofuera.data.auth.AuthRepositoryImpl
import com.rudoestudio.tiempofuera.data.auth.SesionStoreCifrada
import com.rudoestudio.tiempofuera.data.onboarding.EstadoOnboardingDataStore
import com.rudoestudio.tiempofuera.domain.auth.AuthRepository
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
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
    abstract fun estadoOnboarding(impl: EstadoOnboardingDataStore): EstadoOnboardingRepository

    @Binds
    abstract fun sesionStore(impl: SesionStoreCifrada): SesionStore

    @Binds
    abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
}
