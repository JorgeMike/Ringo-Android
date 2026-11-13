package com.rudoestudio.tiempofuera.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EstadoRaiz {
    data object Cargando : EstadoRaiz
    data object Onboarding : EstadoRaiz
    data object Principal : EstadoRaiz
    data class Error(val mensaje: String) : EstadoRaiz
}

/** Decide qué ve el usuario al abrir la app (en iOS: `RootView`). */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val estadoOnboarding: EstadoOnboardingRepository,
) : ViewModel() {

    /** Cada cambio vuelve a leer el estado desde cero (botón "Reintentar"). */
    private val intentos = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<EstadoRaiz> = intentos
        .flatMapLatest {
            estadoOnboarding.finalizado
                .map<Boolean, EstadoRaiz> { finalizado ->
                    if (finalizado) EstadoRaiz.Principal else EstadoRaiz.Onboarding
                }
                .catch { error ->
                    AppLogger.error(error, "RootViewModel")
                    emit(EstadoRaiz.Error("No se pudo leer tu perfil guardado. Intenta de nuevo."))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoRaiz.Cargando)

    fun reintentar() {
        intentos.value++
    }

    fun terminarOnboarding() {
        viewModelScope.launch { estadoOnboarding.finalizar() }
    }
}
