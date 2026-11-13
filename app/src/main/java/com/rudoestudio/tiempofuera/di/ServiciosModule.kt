package com.rudoestudio.tiempofuera.di

import com.rudoestudio.tiempofuera.core.hapticos.HapticServiceAndroid
import com.rudoestudio.tiempofuera.domain.servicios.HapticService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Servicios del sistema (en iOS: los `*Service` que arma `AppContainer`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiciosModule {

    @Binds
    abstract fun haptic(impl: HapticServiceAndroid): HapticService
}
