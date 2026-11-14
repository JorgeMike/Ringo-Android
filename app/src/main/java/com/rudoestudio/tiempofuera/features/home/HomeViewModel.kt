package com.rudoestudio.tiempofuera.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerEstadisticasUsoUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.RevisorPermisos
import com.rudoestudio.tiempofuera.domain.luchadores.CalculadorDeNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.perfil.RachaVigencia
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Qué dice la tarjeta "Tiempo en pantalla" de Inicio. */
sealed interface TiempoPantallaInicio {
    data object Cargando : TiempoPantallaInicio

    /** Falta el acceso al uso (en iOS: "Autoriza Tiempo en pantalla"). */
    data object SinPermiso : TiempoPantallaInicio

    data object SinApps : TiempoPantallaInicio

    /** Uso de hoy de las apps de distracción. */
    data class Uso(val duracion: Duration) : TiempoPantallaInicio
}

data class EstadoInicio(
    val luchador: Luchador? = null,
    val progresoNivel: Float = 0f,
    val diasInvicto: Int = 0,
    val tiempoPantalla: TiempoPantallaInicio = TiempoPantallaInicio.Cargando,
)

/**
 * Tab Inicio (M5.1): luchador equipado, días invicto y tiempo en pantalla de hoy. En iOS:
 * `HomeViewModel`; allá el uso lo dibuja el sistema (`DeviceActivityReport`), aquí se lee
 * con el mismo caso de uso que "Tiempo en pantalla".
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    perfiles: UserProfileRepository,
    private val revisor: RevisorPermisos,
    private val obtenerUso: ObtenerEstadisticasUsoUseCase,
    private val calculador: CalculadorDeNivel,
    private val reloj: Clock,
) : ViewModel() {
    private val tiempoPantalla = MutableStateFlow<TiempoPantallaInicio>(TiempoPantallaInicio.Cargando)

    val estado: StateFlow<EstadoInicio> = combine(perfiles.perfil, tiempoPantalla) { perfil, tiempo ->
        val luchador = perfil.luchadorSeleccionado
        EstadoInicio(
            luchador = luchador,
            progresoNivel = calculador.progreso(luchador.puntosPoder).toFloat(),
            diasInvicto = RachaVigencia.diasVigentes(perfil.diasRacha, perfil.lastActivityDate, reloj.instant(), reloj.zone),
            tiempoPantalla = tiempo,
        )
    }
        .catch { e ->
            AppLogger.error(e, "HomeViewModel")
            emit(EstadoInicio(tiempoPantalla = tiempoPantalla.value))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoInicio())

    /** Al volver a Inicio (de Ajustes, del selector o de otra app) el uso ya cambió. */
    fun refrescarUso() {
        viewModelScope.launch {
            tiempoPantalla.value = try {
                when {
                    !revisor.estado().accesoAlUso -> TiempoPantallaInicio.SinPermiso
                    else -> obtenerUso().let {
                        if (it.hayAppsElegidas) TiempoPantallaInicio.Uso(it.usoHoyDistraccion) else TiempoPantallaInicio.SinApps
                    }
                }
            } catch (e: Exception) {
                AppLogger.error(e, "HomeViewModel")
                TiempoPantallaInicio.SinPermiso
            }
        }
    }
}
