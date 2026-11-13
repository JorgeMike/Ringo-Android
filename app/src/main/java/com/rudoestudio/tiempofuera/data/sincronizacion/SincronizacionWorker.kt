package com.rudoestudio.tiempofuera.data.sincronizacion

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import com.rudoestudio.tiempofuera.domain.sincronizacion.SincronizarProgresoUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sube el progreso local a la nube con reintentos (M1.5). En iOS solo se sincroniza con el
 * botón del perfil; aquí además corre sola cada 12 horas con internet.
 *
 * La sincronización **sobrescribe** la nube con lo local, así que solo corre con sesión y
 * con el onboarding terminado: un teléfono recién instalado todavía no bajó el progreso
 * de la cuenta (`SembrarPerfilLocalUseCase`) y lo borraría.
 */
@HiltWorker
class SincronizacionWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val sincronizar: SincronizarProgresoUseCase,
    private val sesion: SesionStore,
    private val onboarding: EstadoOnboardingRepository,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        if (sesion.actual() == null || !onboarding.finalizado.first()) return Result.success()
        return try {
            sincronizar()
            Result.success()
        } catch (e: ErrorApi.NoAutenticado) {
            Result.failure() // sin sesión no hay a quién subirle nada; reintentar no sirve
        } catch (e: ErrorApi.SinConexion) {
            Result.retry()
        } catch (e: Exception) {
            AppLogger.error(e, "SincronizacionWorker")
            if (runAttemptCount < MAX_REINTENTOS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val MAX_REINTENTOS = 3
    }
}

/** Programa la sincronización en WorkManager. */
@Singleton
class ProgramadorSincronizacion @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {
    private val conInternet = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Cada 12 horas. Si ya estaba programada, se queda como está. */
    fun programarPeriodica() {
        val trabajo = PeriodicWorkRequestBuilder<SincronizacionWorker>(12, TimeUnit.HOURS)
            .setConstraints(conInternet)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(PERIODICA, ExistingPeriodicWorkPolicy.KEEP, trabajo)
    }

    /** En cuanto haya internet (p. ej. al terminar el día o con el botón del perfil). */
    fun sincronizarAhora() {
        val trabajo = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(conInternet)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(AHORA, ExistingWorkPolicy.REPLACE, trabajo)
    }

    private companion object {
        const val PERIODICA = "sincronizacion.periodica"
        const val AHORA = "sincronizacion.ahora"
    }
}
