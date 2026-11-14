package com.rudoestudio.tiempofuera.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.perfil.EstadisticasRacha
import com.rudoestudio.tiempofuera.domain.perfil.ObtenerEstadisticasRachaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Días invicto" (M5.2). En iOS: `RachaStatsViewModel`. */
@HiltViewModel
class RachaStatsViewModel @Inject constructor(
    private val obtener: ObtenerEstadisticasRachaUseCase,
) : ViewModel() {
    private val _estadisticas = MutableStateFlow<EstadisticasRacha?>(null)
    val estadisticas: StateFlow<EstadisticasRacha?> = _estadisticas.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _estadisticas.value = obtener()
            } catch (e: Exception) {
                AppLogger.error(e, "RachaStatsViewModel")
                _error.value = "No se pudieron cargar tus días invicto. Intenta de nuevo."
            }
        }
    }
}
