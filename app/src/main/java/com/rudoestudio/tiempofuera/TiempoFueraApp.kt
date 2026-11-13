package com.rudoestudio.tiempofuera

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rudoestudio.tiempofuera.data.sincronizacion.ProgramadorSincronizacion
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TiempoFueraApp : Application(), Configuration.Provider {
    @Inject
    lateinit var fabricaWorkers: HiltWorkerFactory

    @Inject
    lateinit var sincronizacion: ProgramadorSincronizacion

    /** Los workers reciben sus dependencias de Hilt (por eso el manifest apaga el arranque automático). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaWorkers).build()

    override fun onCreate() {
        super.onCreate()
        sincronizacion.programarPeriodica()
    }
}
