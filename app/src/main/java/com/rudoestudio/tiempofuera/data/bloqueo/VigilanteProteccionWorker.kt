package com.rudoestudio.tiempofuera.data.bloqueo

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.AtenderProteccionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Revisa cada 15 min (lo mínimo de WorkManager) que la protección siga prendida y no
 * esté atorada (M3.12). "Forzar detención" también cancela este trabajo; ese caso se
 * detecta al volver a abrir la app.
 */
@HiltWorker
class VigilanteProteccionWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val atender: AtenderProteccionUseCase,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        try {
            atender()
        } catch (e: Exception) {
            AppLogger.error(e, "VigilanteProteccionWorker")
        }
        return Result.success()
    }

    companion object {
        private const val NOMBRE = "proteccion.vigilante"

        fun programar(contexto: Context) {
            val trabajo = PeriodicWorkRequestBuilder<VigilanteProteccionWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.KEEP, trabajo)
        }
    }
}
