package com.rudoestudio.tiempofuera.di

import com.rudoestudio.tiempofuera.core.hapticos.HapticServiceAndroid
import com.rudoestudio.tiempofuera.domain.luchadores.CalculadorDeNivel
import com.rudoestudio.tiempofuera.domain.luchadores.NivelPoderCalculator
import com.rudoestudio.tiempofuera.domain.servicios.AlcanceApp
import com.rudoestudio.tiempofuera.domain.servicios.HapticService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton
import kotlin.random.Random

/** Servicios del sistema (en iOS: los `*Service` que arma `AppContainer`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiciosModule {

    @Binds
    abstract fun haptic(impl: HapticServiceAndroid): HapticService

    companion object {
        /** La hora y el día del teléfono. Los tests pasan un `Clock` fijo. */
        @Provides
        fun reloj(): Clock = Clock.systemDefaultZone()

        /** Los sorteos (misiones, luchadores). Los tests pasan una semilla fija. */
        @Provides
        fun azar(): Random = Random.Default

        @Provides
        fun calculadorDeNivel(): CalculadorDeNivel = NivelPoderCalculator()

        @Provides
        @Singleton
        @AlcanceApp
        fun alcanceApp(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
