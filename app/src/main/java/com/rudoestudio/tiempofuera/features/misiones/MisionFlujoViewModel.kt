package com.rudoestudio.tiempofuera.features.misiones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.ErrorBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerSeleccionAppsUseCase
import com.rudoestudio.tiempofuera.domain.misiones.CompletarMisionUseCase
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.ObtenerMisionPorIdUseCase
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionTiempo
import com.rudoestudio.tiempofuera.domain.misiones.SesionesMision
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.navigation.MisionFlujo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class EstadoFlujo(
    val cargando: Boolean = true,
    val mision: Mision? = null,
    /** Imagen del luchador equipado (para el tutorial y las misiones). */
    val luchador: Int? = null,
    val tutorialVisto: Boolean = false,
    /** La sesión de temporizador o meditación, si es de esta misión. */
    val sesion: SesionTiempo? = null,
    /** Otra misión con reloj está en curso: esta no se puede empezar. */
    val otraEnCurso: Boolean = false,
    val hayAppsDistraccion: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    /** Ya se reclamó: la pantalla se cierra. */
    val reclamada: Boolean = false,
)

/**
 * Una misión de principio a fin: tutorial, la misión y reclamar la recompensa. En iOS:
 * `MisionFlowContainerView` + `MisionPresenter` + `GestorCompletarMision`, y la parte de
 * `TiempoViewModel` que no es pantalla.
 */
@HiltViewModel
class MisionFlujoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val obtenerMision: ObtenerMisionPorIdUseCase,
    private val perfiles: UserProfileRepository,
    private val sesiones: SesionesMision,
    private val completarMision: CompletarMisionUseCase,
    private val obtenerSeleccion: ObtenerSeleccionAppsUseCase,
) : ViewModel() {
    private val misionId = estadoGuardado.toRoute<MisionFlujo>().misionId

    private val _estado = MutableStateFlow(EstadoFlujo())
    val estado: StateFlow<EstadoFlujo> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val mision = obtenerMision(misionId)
            val luchador = runCatching { perfiles.obtenerPerfil().luchadorSeleccionado }.getOrNull()
            val sesion = sesiones.actual()
            _estado.update {
                it.copy(
                    cargando = false,
                    mision = mision,
                    luchador = luchador?.let { l -> AssetsIos.imagen(l.imagen) ?: AssetsIos.imagen(l.nombre) },
                    // Si ya está corriendo, se salta el tutorial (como iOS con la Live Activity).
                    tutorialVisto = sesion?.misionId == misionId,
                )
            }
            sesiones.sesion.collect { s ->
                _estado.update { it.copy(sesion = s?.takeIf { s.misionId == misionId }, otraEnCurso = s != null && s.misionId != misionId) }
            }
        }
    }

    fun comenzar() = _estado.update { it.copy(tutorialVisto = true) }

    /** Al volver del selector de apps. */
    fun revisarApps() {
        viewModelScope.launch {
            val paquetes = obtenerSeleccion(SesionesMision.GRUPO)?.paquetes.orEmpty()
            _estado.update { it.copy(hayAppsDistraccion = paquetes.isNotEmpty()) }
        }
    }

    fun iniciarTiempo() = conMision { mision ->
        try {
            sesiones.iniciar(mision)
        } catch (e: ErrorBloqueo) {
            _estado.update { it.copy(error = "No se pudieron bloquear tus apps: ${e.message}") }
        }
    }

    fun rendirse() = conMision { sesiones.rendirse(it) }

    /** Meditación: pausar suelta las apps; reanudar las vuelve a bloquear. */
    fun pausar() = conMision { sesiones.pausar(it) }

    fun reanudar() = conMision { mision ->
        try {
            sesiones.reanudar(mision)
        } catch (e: ErrorBloqueo) {
            _estado.update { it.copy(error = "No se pudieron bloquear tus apps: ${e.message}") }
        }
    }

    /** Modo abierto: "terminar y reclamar". El tiempo fijo se termina solo con [revisarFin]. */
    fun terminarTiempo() = conMision { sesiones.terminar(it) }

    /** La pantalla lo llama cada segundo: si se acabó el tiempo fijo, se marca terminada. */
    fun revisarFin(ahora: Instant) {
        val e = _estado.value
        val mision = e.mision ?: return
        val sesion = e.sesion ?: return
        if (!sesion.terminada && RelojMision.seAcabo(mision, sesion, ahora)) {
            conMision { sesiones.terminar(it) }
        }
    }

    /** [unidades]: vueltas en modo abierto, 1 en lo demás. */
    fun reclamar(unidades: Int = 1) = conMision { mision ->
        if (_estado.value.guardando) return@conMision
        _estado.update { it.copy(guardando = true, error = null) }
        try {
            completarMision(mision, unidades)
            if (_estado.value.sesion != null) sesiones.cerrar()
            _estado.update { it.copy(guardando = false, reclamada = true) }
        } catch (e: Exception) {
            AppLogger.error(e, "MisionFlujoViewModel")
            _estado.update { it.copy(guardando = false, error = "No se pudo guardar la misión. Intenta reclamar la recompensa de nuevo.") }
        }
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    private fun conMision(bloque: suspend (Mision) -> Unit) {
        val mision = _estado.value.mision ?: return
        viewModelScope.launch { bloque(mision) }
    }
}
