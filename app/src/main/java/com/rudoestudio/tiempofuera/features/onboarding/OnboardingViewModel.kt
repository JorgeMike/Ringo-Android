package com.rudoestudio.tiempofuera.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.domain.onboarding.FinalizarOnboardingUseCase
import com.rudoestudio.tiempofuera.domain.onboarding.PasoOnboarding
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoOnboarding
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileBorrador
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoCoordinador(
    val cargando: Boolean = true,
    val paso: PasoOnboarding = PasoOnboarding.BIENVENIDA,
    val borrador: UserProfileBorrador = UserProfileBorrador(),
    val finalizando: Boolean = false,
    val error: String? = null,
)

/**
 * Lleva el paso del onboarding y las respuestas (el borrador), y las guarda en cada paso
 * para retomar si se cierra la app. Al terminar manda el perfil al API; `RootScreen` pasa a
 * las tabs solo, porque observa `EstadoOnboardingRepository.finalizado`. En iOS:
 * `OnboardingCoordinator`.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val estadoOnboarding: EstadoOnboardingRepository,
    private val finalizarOnboarding: FinalizarOnboardingUseCase,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoCoordinador())
    val estado: StateFlow<EstadoCoordinador> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val guardado = try {
                estadoOnboarding.obtenerProgreso()
            } catch (e: Exception) {
                AppLogger.error(e, "OnboardingViewModel")
                null
            }
            _estado.update {
                it.copy(
                    cargando = false,
                    paso = guardado?.paso ?: PasoOnboarding.BIENVENIDA,
                    borrador = guardado?.borrador ?: UserProfileBorrador(),
                )
            }
        }
    }

    fun siguiente() {
        val siguiente = _estado.value.paso.siguiente
        if (siguiente == null) finalizar() else ir(siguiente)
    }

    fun anterior() {
        val paso = _estado.value.paso
        if (!paso.puedeRegresar) return
        paso.anterior?.let(::ir)
    }

    /** "Ya tenía mi perfil": salta al paso de datos, donde se inicia sesión. */
    fun yaTengoPerfil() = ir(PasoOnboarding.DATOS)

    /** Un paso guarda su respuesta en el borrador y se avanza. En iOS: `continuar(_:en:)`. */
    fun continuar(actualizar: (UserProfileBorrador) -> UserProfileBorrador) {
        _estado.update { it.copy(borrador = actualizar(it.borrador)) }
        siguiente()
    }

    fun datosCompletados(datos: DatosCapturados) = continuar { borrador ->
        val base = datos.perfilExistente?.let(borrador::precargar) ?: borrador
        base.copy(fechaNacimiento = datos.fechaNacimiento, email = datos.email)
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    /**
     * Cambia de paso en pantalla y lo guarda. A diferencia de iOS, si guardar falla el paso
     * ya cambió: solo se avisa (lo peor es repetir pasos si se cierra la app).
     */
    private fun ir(paso: PasoOnboarding) {
        _estado.update { it.copy(paso = paso) }
        val progreso = ProgresoOnboarding(paso, _estado.value.borrador)
        viewModelScope.launch {
            try {
                estadoOnboarding.guardarProgreso(progreso)
            } catch (e: Exception) {
                AppLogger.error(e, "OnboardingViewModel")
                _estado.update { it.copy(error = "No se pudieron guardar tus respuestas. Si cierras la app, tendrás que repetir este paso.") }
            }
        }
    }

    private fun finalizar() {
        if (_estado.value.finalizando) return
        _estado.update { it.copy(finalizando = true, error = null) }
        viewModelScope.launch {
            try {
                finalizarOnboarding(_estado.value.borrador)
            } catch (e: ErrorApi.PerfilIncompleto) {
                _estado.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                AppLogger.error(e, "OnboardingViewModel")
                _estado.update { it.copy(error = "No se pudo guardar tu perfil. Revisa tu internet e intenta de nuevo.") }
            } finally {
                _estado.update { it.copy(finalizando = false) }
            }
        }
    }
}
