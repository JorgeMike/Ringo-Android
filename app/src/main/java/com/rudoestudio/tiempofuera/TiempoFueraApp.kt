package com.rudoestudio.tiempofuera

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.core.notificaciones.NotificacionMisionEnCurso
import com.rudoestudio.tiempofuera.data.bloqueo.VigilanteProteccionWorker
import com.rudoestudio.tiempofuera.data.racha.FinDeDiaWorker
import com.rudoestudio.tiempofuera.data.sincronizacion.ProgramadorSincronizacion
import com.rudoestudio.tiempofuera.domain.bloqueo.AtenderProteccionUseCase
import com.rudoestudio.tiempofuera.domain.servicios.AlcanceApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TiempoFueraApp : Application(), Configuration.Provider {
    @Inject
    lateinit var fabricaWorkers: HiltWorkerFactory

    @Inject
    lateinit var sincronizacion: ProgramadorSincronizacion

    @Inject
    lateinit var atenderProteccion: AtenderProteccionUseCase

    @Inject
    @AlcanceApp
    lateinit var alcance: CoroutineScope

    @Inject
    lateinit var notificacionMision: NotificacionMisionEnCurso

    /** Los workers reciben sus dependencias de Hilt (por eso el manifest apaga el arranque automático). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaWorkers).build()

    override fun onCreate() {
        super.onCreate()
        sincronizacion.programarPeriodica()
        VigilanteProteccionWorker.programar(this)
        FinDeDiaWorker.programar(this)
        FinDeDiaWorker.procesarAhora(this)
        notificacionMision.iniciar()
        // Tras "Forzar detención" no corre nada nuestro hasta que se vuelve a abrir la app.
        alcance.launch { runCatching { atenderProteccion(avisarAtorada = false) }.onFailure { AppLogger.error(it, "TiempoFueraApp") } }
    }
}
