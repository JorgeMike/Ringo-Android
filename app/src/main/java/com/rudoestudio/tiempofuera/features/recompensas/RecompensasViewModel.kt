package com.rudoestudio.tiempofuera.features.recompensas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.luchadores.CalculadorDeNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.RecolectarLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoRecoleccion
import com.rudoestudio.tiempofuera.domain.luchadores.SorteoLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.recompensas.FlujoRecompensa
import com.rudoestudio.tiempofuera.domain.servicios.AudioService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La celebración que hay que mostrar encima de todo, de cualquier pantalla (p. ej. al
 * reclamar una misión). `MainScaffold` la dibuja mientras no sea `null`. En iOS: el
 * `recompensaFlowActivo` de `MainTabView` con `.recompensaFlow`.
 */
@Singleton
class PresentadorRecompensas @Inject constructor() {
    private val _flujo = MutableStateFlow<FlujoRecompensa?>(null)
    val flujo: StateFlow<FlujoRecompensa?> = _flujo.asStateFlow()

    fun presentar(flujo: FlujoRecompensa) {
        _flujo.value = flujo
    }

    fun cerrar() {
        _flujo.value = null
    }
}

/**
 * Lo que necesitan las pantallas de recompensa: sortear y recolectar el luchador del sobre,
 * la curva de nivel y la campana. En iOS: `SobrePersonajeViewModel` más las fábricas de
 * `RecompensaFlowCoordinatorFactory+Environment`.
 */
@HiltViewModel
class RecompensasViewModel @Inject constructor(
    private val presentador: PresentadorRecompensas,
    private val sorteo: SorteoLuchadorUseCase,
    private val recolectarLuchador: RecolectarLuchadorUseCase,
    private val calculador: CalculadorDeNivel,
    private val audio: AudioService,
    perfiles: UserProfileRepository,
) : ViewModel() {
    val presentado: StateFlow<FlujoRecompensa?> = presentador.flujo

    /** Imagen del luchador equipado, para racha y misión completada. */
    val luchadorEquipado: StateFlow<Int?> = perfiles.perfil
        .map { p -> p.luchadorSeleccionado.let { AssetsIos.imagen(it.imagen) ?: AssetsIos.imagen(it.nombre) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `null` si no hay luchadores que sortear (en iOS: "No se pudo abrir el sobre"). */
    suspend fun sortear(rareza: Rareza): Luchador? = try {
        sorteo(rareza)
    } catch (e: Exception) {
        AppLogger.error(e, "RecompensasViewModel")
        null
    }

    suspend fun recolectar(luchador: Luchador): Result<ResultadoRecoleccion> =
        runCatching { recolectarLuchador(luchador.id) }.onFailure { AppLogger.error(it, "RecompensasViewModel") }

    fun nivel(puntos: Int): Int = calculador.nivelActual(puntos)

    fun progreso(puntos: Int): Float = calculador.progreso(puntos).toFloat()

    fun campana() = audio.reproducirCampanaRingo()

    fun cerrar() = presentador.cerrar()
}
