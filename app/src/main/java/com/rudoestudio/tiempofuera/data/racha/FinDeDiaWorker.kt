package com.rudoestudio.tiempofuera.data.racha

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.domain.perfil.ProcesarFinDeDiaUseCase
import com.rudoestudio.tiempofuera.domain.perfil.ProgramarRecordatoriosRachaUseCase
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Cierra el día (M12.4) y vuelve a programar los recordatorios de racha (M12.2). En iOS
 * los dos corren cada vez que la app pasa a primer plano; aquí además cada hora, así que
 * la racha se cierra aunque no abras la app, y los recordatorios que Android borra al
 * reiniciar el teléfono regresan solos (WorkManager sí sobrevive al reinicio).
 *
 * Solo con el onboarding terminado: antes no hay perfil que cerrar.
 */
@HiltWorker
class FinDeDiaWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val finDeDia: ProcesarFinDeDiaUseCase,
    private val recordatorios: ProgramarRecordatoriosRachaUseCase,
    private val onboarding: EstadoOnboardingRepository,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        if (!onboarding.finalizado.first()) return Result.success()
        try {
            recordatorios()
        } catch (e: Exception) {
            AppLogger.error(e, "FinDeDiaWorker.recordatorios")
        }
        // La racha se cierra en el teléfono antes de llamar al API: si falla la subida, lo
        // local ya quedó, y reintentar no gasta otro protector.
        return try {
            finDeDia()
            Result.success()
        } catch (e: ErrorApi.NoAutenticado) {
            Result.success() // sin sesión no se sube nada, pero el día sí se cerró
        } catch (e: ErrorApi.SinConexion) {
            Result.retry()
        } catch (e: Exception) {
            AppLogger.error(e, "FinDeDiaWorker")
            if (runAttemptCount < MAX_REINTENTOS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MAX_REINTENTOS = 3
        private const val PERIODICO = "racha.finDeDia"
        private const val AHORA = "racha.finDeDia.ahora"

        /** Cada hora. Si ya estaba programado, se queda como está. */
        fun programar(contexto: Context) {
            val trabajo = PeriodicWorkRequestBuilder<FinDeDiaWorker>(1, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(PERIODICO, ExistingPeriodicWorkPolicy.KEEP, trabajo)
        }

        /** Al abrir la app, como iOS al pasar a primer plano. */
        fun procesarAhora(contexto: Context) {
            val trabajo = OneTimeWorkRequestBuilder<FinDeDiaWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(contexto).enqueueUniqueWork(AHORA, ExistingWorkPolicy.KEEP, trabajo)
        }
    }
}
