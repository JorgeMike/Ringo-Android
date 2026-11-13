package com.rudoestudio.tiempofuera.features.bloqueo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.AppInstalada
import com.rudoestudio.tiempofuera.domain.bloqueo.AppsInstaladas
import com.rudoestudio.tiempofuera.domain.bloqueo.BuscadorApps
import com.rudoestudio.tiempofuera.domain.bloqueo.GuardarSeleccionAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerSeleccionAppsUseCase
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.navigation.SelectorApps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoSelectorApps(
    val cargando: Boolean = true,
    /** Todas, con las elegidas al principio (el orden se fija al abrir, para que no salten al tocar). */
    val apps: List<AppInstalada> = emptyList(),
    val elegidas: Set<String> = emptySet(),
    val consulta: String = "",
    val error: String? = null,
) {
    val visibles: List<AppInstalada> get() = BuscadorApps.filtrar(apps, consulta)
}

/**
 * Elegir las apps de un grupo (M3.2). En iOS: `SelectorAppsViewModel` +
 * `FamilyActivityPicker`, que es del sistema; aquí la lista la armamos nosotros.
 */
@HiltViewModel
class SelectorAppsViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val appsInstaladas: AppsInstaladas,
    private val obtenerSeleccion: ObtenerSeleccionAppsUseCase,
    private val guardarSeleccion: GuardarSeleccionAppsUseCase,
) : ViewModel() {
    val grupo: TipoGrupoApps = TipoGrupoApps.desdeClave(estadoGuardado.toRoute<SelectorApps>().grupo) ?: TipoGrupoApps.DISTRACCION

    private val _estado = MutableStateFlow(EstadoSelectorApps())
    val estado: StateFlow<EstadoSelectorApps> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val elegidas = obtenerSeleccion(grupo)?.paquetes.orEmpty()
            val apps = appsInstaladas.lanzables()
            _estado.update { it.copy(cargando = false, apps = BuscadorApps.ordenar(apps, elegidas), elegidas = elegidas) }
        }
    }

    fun buscar(consulta: String) = _estado.update { it.copy(consulta = consulta) }

    fun alternar(paquete: String) = _estado.update {
        it.copy(elegidas = if (paquete in it.elegidas) it.elegidas - paquete else it.elegidas + paquete)
    }

    fun guardar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            try {
                guardarSeleccion(grupo, _estado.value.elegidas)
                alTerminar()
            } catch (e: Exception) {
                AppLogger.error(e, "SelectorAppsViewModel")
                _estado.update { it.copy(error = "No se pudo guardar tu selección. Intenta de nuevo.") }
            }
        }
    }
}
