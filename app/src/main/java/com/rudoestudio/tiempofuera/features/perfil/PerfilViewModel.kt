package com.rudoestudio.tiempofuera.features.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerSeleccionAppsUseCase
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import com.rudoestudio.tiempofuera.domain.sincronizacion.SincronizarProgresoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Los números que en debug se pueden cambiar a mano (como los campos de iOS). */
data class EstadisticasEditables(val puntosPoder: Int, val diasRacha: Int, val monedas: Int, val protectoresRacha: Int)

data class EstadoPerfil(
    val perfil: UserProfile? = null,
    val sesion: SesionUsuario? = null,
    /** Cuántas apps tiene elegidas cada grupo. */
    val appsElegidas: Map<TipoGrupoApps, Int> = emptyMap(),
    val configuraciones: Map<TipoGrupoApps, ConfiguracionGrupoApps> = emptyMap(),
    val sincronizando: Boolean = false,
    val mensajeSincronizacion: String? = null,
    val error: String? = null,
) {
    /** Los grupos que tienen algo que configurar (salud no). */
    val gruposConfigurables: List<TipoGrupoApps> get() = TipoGrupoApps.entries.filter { it.camposConfigurables.isNotEmpty() }
}

/**
 * Tab Perfil (M11.1 y M11.2): tus números, tu personalidad, tus apps y su seguimiento, y
 * sincronizar con la nube. En iOS: `UserProfileViewModel`.
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val sesiones: SesionStore,
    private val seleccion: ObtenerSeleccionAppsUseCase,
    private val configuraciones: ConfiguracionGrupoAppsRepository,
    private val sincronizarProgreso: SincronizarProgresoUseCase,
    private val reloj: Clock,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoPerfil())
    val estado: StateFlow<EstadoPerfil> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            perfiles.perfil
                .catch { e -> falla(e, "No se pudo cargar tu perfil. Intenta de nuevo.") }
                .collect { p -> _estado.update { it.copy(perfil = p) } }
        }
        viewModelScope.launch {
            sesiones.sesion.collect { s -> _estado.update { it.copy(sesion = s) } }
        }
    }

    /** Al volver del selector de apps o de editar un grupo. */
    fun refrescar() {
        viewModelScope.launch {
            try {
                val todas = configuraciones.obtenerTodas()
                val grupos = TipoGrupoApps.entries.filter { it != TipoGrupoApps.SALUD }
                _estado.update { e ->
                    e.copy(
                        appsElegidas = grupos.associateWith { seleccion(it)?.paquetes?.size ?: 0 },
                        configuraciones = TipoGrupoApps.entries.mapNotNull { g -> todas[g.clave]?.let { g to it } }.toMap(),
                    )
                }
            } catch (ex: Exception) {
                falla(ex, "No se pudieron leer tus grupos de apps.")
            }
        }
    }

    fun cambiarPersonalidad(personalidad: TipoPersonalidad) = guardar("No se pudo cambiar tu personalidad.") {
        it.copy(personalidadId = personalidad)
    }

    /** Solo en debug: en iOS es un formulario abierto, aquí no se regalan monedas en release. */
    fun guardarEstadisticas(e: EstadisticasEditables) = guardar("No se pudieron guardar los números.") {
        it.copy(
            puntosPoder = e.puntosPoder.coerceAtLeast(0),
            diasRacha = e.diasRacha.coerceAtLeast(0),
            monedas = e.monedas.coerceAtLeast(0),
            protectoresRacha = e.protectoresRacha.coerceIn(0, UserProfile.MAX_PROTECTORES_RACHA),
            lastActivityDate = reloj.instant(),
        )
    }

    fun sincronizar() {
        if (_estado.value.sincronizando) return
        _estado.update { it.copy(sincronizando = true, mensajeSincronizacion = null) }
        viewModelScope.launch {
            val mensaje = try {
                sincronizarProgreso()
                "¡Listo! Tu progreso ya está en la nube."
            } catch (e: ErrorApi) {
                e.message
            } catch (e: Exception) {
                AppLogger.error(e, "PerfilViewModel")
                "No se pudo sincronizar. Revisa tu conexión e intenta de nuevo."
            }
            _estado.update { it.copy(sincronizando = false, mensajeSincronizacion = mensaje) }
        }
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    private fun guardar(mensajeError: String, cambio: (UserProfile) -> UserProfile) {
        viewModelScope.launch {
            try {
                perfiles.guardarPerfil(cambio(perfiles.obtenerPerfil()))
                _estado.update { it.copy(error = null) }
            } catch (e: Exception) {
                falla(e, mensajeError)
            }
        }
    }

    private fun falla(e: Throwable, mensaje: String) {
        AppLogger.error(e, "PerfilViewModel")
        _estado.update { it.copy(error = mensaje) }
    }
}
