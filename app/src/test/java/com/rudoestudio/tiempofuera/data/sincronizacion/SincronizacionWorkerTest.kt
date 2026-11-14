package com.rudoestudio.tiempofuera.data.sincronizacion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.rudoestudio.tiempofuera.domain.ApiFake
import com.rudoestudio.tiempofuera.domain.LuchadoresFake
import com.rudoestudio.tiempofuera.domain.PerfilesFake
import com.rudoestudio.tiempofuera.domain.ProgresoLigasFake
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoOnboarding
import com.rudoestudio.tiempofuera.domain.reloj
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import com.rudoestudio.tiempofuera.domain.sincronizacion.ProgresoLocal
import com.rudoestudio.tiempofuera.domain.sincronizacion.SincronizarProgresoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SincronizacionWorkerTest {
    private val api = ApiFake()
    private val perfiles = PerfilesFake()
    private val sesion = SesionFake(SesionUsuario("a", "r", 3600, "u-1", "a@b.c", true, System.currentTimeMillis()))
    private val onboarding = OnboardingFake(finalizadoInicial = true)

    private fun correr(): ListenableWorker.Result = runBlocking {
        val sincronizar = SincronizarProgresoUseCase(ProgresoLocal(perfiles, LuchadoresFake(), ProgresoLigasFake(), reloj()), api)
        val fabrica = object : WorkerFactory() {
            override fun createWorker(contexto: Context, clase: String, parametros: WorkerParameters) =
                SincronizacionWorker(contexto, parametros, sincronizar, sesion, onboarding)
        }
        TestListenableWorkerBuilder<SincronizacionWorker>(ApplicationProvider.getApplicationContext())
            .setWorkerFactory(fabrica)
            .build()
            .doWork()
    }

    @Test
    fun `sincroniza y aplica lo que regresa la nube`() {
        perfiles.actual = perfiles.actual.copy(monedas = 42)
        assertEquals(ListenableWorker.Result.success(), correr())
        assertEquals(42, perfiles.actual.monedas)
    }

    @Test
    fun `sin sesion o sin onboarding no toca la nube`() {
        api.falla = IllegalStateException("no debió llamarse")
        onboarding.estado.value = false
        assertEquals(ListenableWorker.Result.success(), correr())
        onboarding.estado.value = true
        sesion.estado.value = null
        assertEquals(ListenableWorker.Result.success(), correr())
    }

    @Test
    fun `sin internet reintenta y sin sesion valida se rinde`() {
        api.falla = ErrorApi.SinConexion()
        assertTrue(correr() is ListenableWorker.Result.Retry)
        api.falla = ErrorApi.NoAutenticado()
        assertTrue(correr() is ListenableWorker.Result.Failure)
    }

    private class SesionFake(inicial: SesionUsuario?) : SesionStore {
        val estado = MutableStateFlow(inicial)
        override val sesion: StateFlow<SesionUsuario?> = estado
        override suspend fun actual() = estado.value
        override suspend fun guardar(sesion: SesionUsuario) { estado.value = sesion }
        override suspend fun limpiar() { estado.value = null }
    }

    private class OnboardingFake(finalizadoInicial: Boolean) : EstadoOnboardingRepository {
        val estado = MutableStateFlow(finalizadoInicial)
        override val finalizado = estado
        override suspend fun finalizar() { estado.value = true }
        override suspend fun obtenerProgreso(): ProgresoOnboarding? = null
        override suspend fun guardarProgreso(progreso: ProgresoOnboarding) = Unit
        override suspend fun reiniciar() { estado.value = false }
    }
}
