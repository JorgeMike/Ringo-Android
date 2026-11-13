package com.rudoestudio.tiempofuera.features.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadisticasUso
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerEstadisticasUsoUseCase
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Tiempo en pantalla y su configuración (M3.10). En iOS: `ScreenTimeStatsViewModel`. Los
 * cambios no necesitan "reprogramar" nada como en iOS: el motor de bloqueo los lee solo.
 */
@HiltViewModel
class EstadisticasUsoViewModel @Inject constructor(
    private val obtener: ObtenerEstadisticasUsoUseCase,
    private val configuraciones: ConfiguracionGrupoAppsRepository,
    private val sueno: ConfiguracionHabitoSuenoRepository,
) : ViewModel() {
    private val _estadisticas = MutableStateFlow<EstadisticasUso?>(null)
    val estadisticas: StateFlow<EstadisticasUso?> = _estadisticas.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargar() = ejecutar("No se pudieron leer tus estadísticas. Intenta de nuevo.") {}

    /** De 5 en 5 minutos, entre "sin límite" y 24 h. */
    fun ajustarLimite(minutos: Int) = ejecutar("No se pudo guardar el límite diario.") {
        val clave = TipoGrupoApps.DISTRACCION.clave
        val previo = configuraciones.obtenerConfiguracion(clave) ?: ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION)
        val ajustado = ((previo.tiempoMaximoDiario ?: 0) + minutos * 60L).coerceIn(0, MINUTOS_MAXIMOS_DIARIOS * 60L)
        configuraciones.guardar(previo.copy(tiempoMaximoDiario = ajustado.takeIf { it > 0 }), clave)
    }

    fun cambiarHoraDespertar(hora: LocalTime) = guardarSueno { it.copy(horaInicioDia = hora.hour * 60 + hora.minute) }

    fun cambiarHoraDormir(hora: LocalTime) = guardarSueno { it.copy(horaFinDia = hora.hour * 60 + hora.minute) }

    /** Entre 20 min y 2 h, como iOS. */
    fun ajustarColchon(minutos: Int) = guardarSueno { it.copy(colchonUso = (it.colchonUso + minutos).coerceIn(COLCHON_MINIMO, COLCHON_MAXIMO)) }

    private fun guardarSueno(cambio: (ConfiguracionHabitoSueno) -> ConfiguracionHabitoSueno) =
        ejecutar("No se pudo guardar el horario de sueño.") {
            val actual = _estadisticas.value?.sueno ?: return@ejecutar
            sueno.guardarConfiguracionHabitoSueno(cambio(actual))
        }

    /** Hace el cambio y vuelve a leer todo. */
    private fun ejecutar(mensajeError: String, cambio: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                cambio()
                _estadisticas.value = obtener()
                _error.value = null
            } catch (e: Exception) {
                AppLogger.error(e, "EstadisticasUsoViewModel")
                _error.value = mensajeError
            }
        }
    }

    companion object {
        const val MINUTOS_MAXIMOS_DIARIOS = 24 * 60
        const val COLCHON_MINIMO = 20
        const val COLCHON_MAXIMO = 120
    }
}
