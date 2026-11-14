package com.rudoestudio.tiempofuera.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadoPermisos
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerSeleccionAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.RevisorPermisos
import com.rudoestudio.tiempofuera.domain.configuracion.CampoConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.onboarding.ReglasOnboarding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModels de los pasos 10 a 12, los que leen o guardan configuración del bloqueo.

data class EstadoConfPermisos(
    val permisos: EstadoPermisos,
    val appsDistraccion: Int = 0,
    val appsEducativas: Int = 0,
    val error: String? = null,
)

/**
 * Paso 10: permisos del bloqueo y apps de cada grupo (M4.11). Se revisa todo al volver a
 * la pantalla, porque los permisos se dan en Ajustes y las apps en el selector. En iOS:
 * `ConfPermisosViewModel` (allá son Screen Time, notificaciones y dos `FamilyActivityPicker`).
 */
@HiltViewModel
class ConfPermisosViewModel @Inject constructor(
    private val revisor: RevisorPermisos,
    private val obtenerSeleccion: ObtenerSeleccionAppsUseCase,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoConfPermisos(revisor.estado()))
    val estado: StateFlow<EstadoConfPermisos> = _estado.asStateFlow()

    fun refrescar() {
        _estado.update { it.copy(permisos = revisor.estado()) }
        viewModelScope.launch {
            val distraccion = obtenerSeleccion(TipoGrupoApps.DISTRACCION)?.paquetes?.size ?: 0
            val educativas = obtenerSeleccion(TipoGrupoApps.EDUCATIVAS)?.paquetes?.size ?: 0
            _estado.update { it.copy(appsDistraccion = distraccion, appsEducativas = educativas) }
        }
    }

    /** Mismas condiciones que iOS, con los permisos de Android en lugar de Screen Time. */
    fun puedeContinuar(): Boolean {
        val e = _estado.value
        val error = when {
            !e.permisos.completos -> "Te faltan permisos. Sin ellos no podemos echarte la mano."
            e.appsDistraccion == 0 -> "Elige al menos una app de distracción."
            e.appsEducativas == 0 -> "Elige al menos una app educativa."
            else -> null
        }
        _estado.update { it.copy(error = error) }
        return error == null
    }
}

data class EstadoConfUsoMax(
    val distraccion: ConfiguracionGrupoApps = ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION),
    val educativas: ConfiguracionGrupoApps = ConfiguracionGrupoApps(TipoGrupoApps.EDUCATIVAS),
    val tiempoInicialMinutos: Int? = null,
    val error: String? = null,
) {
    val limiteSugeridoMinutos: Int? get() = tiempoInicialMinutos?.let(ReglasOnboarding::limiteSugerido)

    fun de(tipo: TipoGrupoApps) = if (tipo == TipoGrupoApps.DISTRACCION) distraccion else educativas
}

/**
 * Paso 11: límite diario y avisos de cada grupo. Sugiere un límite a partir de lo que dijo
 * que usa el celular en el paso 9. En iOS: `ConfUsoMaxViewModel`.
 */
@HiltViewModel
class ConfUsoMaxViewModel @Inject constructor(
    private val repositorio: ConfiguracionGrupoAppsRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoConfUsoMax())
    val estado: StateFlow<EstadoConfUsoMax> = _estado.asStateFlow()

    /** La sugerencia espera a que se cargue lo guardado, para no pisarlo. */
    private var sugerenciaPendiente: Int? = null
    private var cargado = false

    init {
        viewModelScope.launch {
            val distraccion = repositorio.obtenerConfiguracion(TipoGrupoApps.DISTRACCION.clave)
            val educativas = repositorio.obtenerConfiguracion(TipoGrupoApps.EDUCATIVAS.clave)
            _estado.update {
                it.copy(
                    distraccion = distraccion ?: it.distraccion,
                    educativas = educativas ?: it.educativas,
                )
            }
            cargado = true
            sugerenciaPendiente?.let(::aplicarSugerencia)
        }
    }

    /** Solo cambia el límite si no lo había tocado (vacío o igual a la sugerencia anterior). */
    fun aplicarSugerencia(tiempoInicialMinutos: Int) {
        if (tiempoInicialMinutos <= 0 || tiempoInicialMinutos > ConfiguracionGrupoApps.MINUTOS_MAXIMOS_DIARIOS) return
        if (!cargado) {
            sugerenciaPendiente = tiempoInicialMinutos
            return
        }
        _estado.update { e ->
            val anterior = e.limiteSugeridoMinutos?.let { it * 60L }
            val actual = e.distraccion.tiempoMaximoDiario
            val nuevo = e.copy(tiempoInicialMinutos = tiempoInicialMinutos)
            if (actual == null || actual == anterior) {
                nuevo.copy(distraccion = nuevo.distraccion.copy(tiempoMaximoDiario = nuevo.limiteSugeridoMinutos!! * 60L))
            } else {
                nuevo
            }
        }
    }

    fun cambiar(tipo: TipoGrupoApps, campo: CampoConfiguracionGrupoApps, minutos: Int) = _estado.update { e ->
        val segundos = minutos * 60L
        if (tipo == TipoGrupoApps.DISTRACCION) e.copy(distraccion = e.distraccion.con(campo, segundos))
        else e.copy(educativas = e.educativas.con(campo, segundos))
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    fun guardar(alTerminar: (ConfiguracionGrupoApps) -> Unit) {
        viewModelScope.launch {
            val e = _estado.value
            try {
                repositorio.guardar(e.distraccion, TipoGrupoApps.DISTRACCION.clave)
                repositorio.guardar(e.educativas, TipoGrupoApps.EDUCATIVAS.clave)
                alTerminar(e.distraccion)
            } catch (ex: Exception) {
                AppLogger.error(ex, "ConfUsoMaxViewModel")
                _estado.update { it.copy(error = "No se pudo guardar la configuración de uso. Intenta de nuevo.") }
            }
        }
    }
}

data class EstadoConfHabito(
    /** Minutos desde medianoche. */
    val horaInicioDia: Int = 7 * 60,
    val horaFinDia: Int = 23 * 60,
    val colchonUso: Int = 30,
    val error: String? = null,
)

/** Paso 12: a qué hora despierta y se duerme, y el colchón. En iOS: `ConfHabitoViewModel`. */
@HiltViewModel
class ConfHabitoViewModel @Inject constructor(
    private val repositorio: ConfiguracionHabitoSuenoRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoConfHabito())
    val estado: StateFlow<EstadoConfHabito> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repositorio.obtenerConfiguracionHabitoSueno()?.let { c ->
                    _estado.update {
                        it.copy(
                            horaInicioDia = c.horaInicioDia,
                            horaFinDia = c.horaFinDia,
                            colchonUso = c.colchonUso.coerceIn(ReglasOnboarding.COLCHON_MINIMO, ReglasOnboarding.COLCHON_MAXIMO),
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.error(e, "ConfHabitoViewModel")
                _estado.update { it.copy(error = "No se pudo leer el horario guardado. Revisa los valores antes de continuar.") }
            }
        }
    }

    fun cambiarInicio(minutos: Int) = _estado.update { it.copy(horaInicioDia = minutos) }

    fun cambiarFin(minutos: Int) = _estado.update { it.copy(horaFinDia = minutos) }

    fun cambiarColchon(delta: Int) = _estado.update {
        it.copy(colchonUso = (it.colchonUso + delta).coerceIn(ReglasOnboarding.COLCHON_MINIMO, ReglasOnboarding.COLCHON_MAXIMO))
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    fun guardar(alTerminar: (ConfiguracionHabitoSueno) -> Unit) {
        viewModelScope.launch {
            val e = _estado.value
            val configuracion = ConfiguracionHabitoSueno(e.horaInicioDia, e.horaFinDia, e.colchonUso)
            try {
                repositorio.guardarConfiguracionHabitoSueno(configuracion)
                alTerminar(configuracion)
            } catch (ex: Exception) {
                AppLogger.error(ex, "ConfHabitoViewModel")
                _estado.update { it.copy(error = "No se pudo guardar el horario de sueño. Intenta de nuevo.") }
            }
        }
    }
}
