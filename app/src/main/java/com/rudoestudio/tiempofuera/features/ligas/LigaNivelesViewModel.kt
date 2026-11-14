package com.rudoestudio.tiempofuera.features.ligas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.ligas.EvaluadorProgresoLiga
import com.rudoestudio.tiempofuera.domain.ligas.GuardarProgresoLigaUseCase
import com.rudoestudio.tiempofuera.domain.ligas.MotorMinijuego
import com.rudoestudio.tiempofuera.domain.ligas.Liga
import com.rudoestudio.tiempofuera.domain.ligas.NivelLiga
import com.rudoestudio.tiempofuera.domain.ligas.ObtenerLigasUseCase
import com.rudoestudio.tiempofuera.domain.ligas.ObtenerProgresoLigaUseCase
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.ligas.RecolectarRecompensaLigaUseCase
import com.rudoestudio.tiempofuera.domain.ligas.TipoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.EstadoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.MisionesEventos
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiarioRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.recompensas.FlujoRecompensa
import com.rudoestudio.tiempofuera.domain.recompensas.PantallaRecompensa
import com.rudoestudio.tiempofuera.domain.servicios.AudioService
import com.rudoestudio.tiempofuera.features.recompensas.PresentadorRecompensas
import com.rudoestudio.tiempofuera.navigation.LigaNiveles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class EstadoLiga(
    val cargando: Boolean = true,
    val liga: Liga? = null,
    val progreso: ProgresoUsuarioLiga? = null,
    /** Los 100 niveles (los sobres al azar se sortean una vez al abrir). */
    val niveles: List<NivelLiga> = emptyList(),
    val luchador: Luchador? = null,
    /** Ids de las misiones de la liga que ya se hicieron hoy. */
    val misionesHechasHoy: Set<Int> = emptySet(),
    val error: String? = null,
) {
    val paleta: PaletaLiga get() = PaletaLiga.para(liga?.id ?: 1)

    val visibles: List<NivelLiga>
        get() = progreso?.let { EvaluadorProgresoLiga.nivelesVisibles(niveles, it) }.orEmpty()

    fun estado(nivel: NivelLiga): EstadoNivel =
        progreso?.let { EvaluadorProgresoLiga.calcularEstado(nivel, it) } ?: EstadoNivel.BLOQUEADO

    val expTotal: Int get() = progreso?.totalExp?.toInt() ?: 0

    /** `null`: ya no hay más niveles. */
    val expSiguiente: Double? get() = progreso?.let { EvaluadorProgresoLiga.expSiguienteNivel(niveles, it) }

    val avance: Float
        get() {
            val siguiente = expSiguiente ?: return 1f
            if (siguiente <= 0) return 1f
            return ((progreso?.totalExp ?: 0.0) / siguiente).toFloat().coerceIn(0f, 1f)
        }

    val misiones: List<Mision> get() = liga?.misiones.orEmpty().map { it.copy(completada = it.id in misionesHechasHoy) }
}

/**
 * El camino de una liga (M9.2): avanza con la experiencia de sus misiones, se cobran los
 * niveles y las barreras se pelean. En iOS: `LigaNivelesViewModel`.
 */
@HiltViewModel
class LigaNivelesViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val obtenerLigas: ObtenerLigasUseCase,
    private val obtenerProgreso: ObtenerProgresoLigaUseCase,
    private val guardarProgreso: GuardarProgresoLigaUseCase,
    private val recolectarRecompensa: RecolectarRecompensaLigaUseCase,
    private val perfiles: UserProfileRepository,
    private val progresoDiario: ProgresoDiarioRepository,
    private val presentador: PresentadorRecompensas,
    private val audio: AudioService,
    eventos: MisionesEventos,
    private val reloj: Clock,
) : ViewModel() {
    private val ligaId = estadoGuardado.toRoute<LigaNiveles>().ligaId

    private val _estado = MutableStateFlow(EstadoLiga())
    val estado: StateFlow<EstadoLiga> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val liga = obtenerLigas().first { it.id == ligaId }
                _estado.update {
                    it.copy(cargando = false, liga = liga, niveles = liga.niveles().sortedBy { n -> n.id }, progreso = obtenerProgreso(ligaId))
                }
                evaluarYGuardar()
                refrescarMisiones()
            } catch (e: Exception) {
                AppLogger.error(e, "LigaNivelesViewModel")
                _estado.update { it.copy(cargando = false, error = "No se pudo abrir la liga. Intenta de nuevo.") }
            }
        }
        viewModelScope.launch {
            perfiles.perfil.collect { p -> _estado.update { it.copy(luchador = p.luchadorSeleccionado) } }
        }
        // Una misión de esta liga da experiencia: el camino avanza sin recargar.
        viewModelScope.launch {
            eventos.completadas.collect { resultado ->
                val nuevo = resultado.progresoLigaActualizado?.takeIf { it.ligaId == ligaId } ?: return@collect
                _estado.update { it.copy(progreso = nuevo.copy(nivelesDesbloqueados = nuevo.nivelesDesbloqueados + it.progreso?.nivelesDesbloqueados.orEmpty())) }
                evaluarYGuardar()
                refrescarMisiones()
            }
        }
    }

    fun refrescarMisiones() {
        viewModelScope.launch {
            val hechas = progresoDiario.obtenerProgreso(LocalDate.now(reloj))?.misiones.orEmpty().toSet()
            _estado.update { it.copy(misionesHechasHoy = hechas) }
        }
    }

    /** Cobra un nivel y festeja: subida del luchador y sobres. En iOS: `recolectarRecompensa` + `mostrarRecompensaPendiente`. */
    fun cobrar(nivel: NivelLiga) = cobrar(nivel, ganoLucha = false)

    /** El rival de una barrera; si no trae, el luchador estrella de la liga (como iOS). */
    fun rival(nivel: NivelLiga): Luchador? = nivel.luchadorBarrera ?: _estado.value.liga?.luchadorEstrella

    private fun cobrar(nivel: NivelLiga, ganoLucha: Boolean) {
        viewModelScope.launch {
            try {
                val antes = _estado.value.luchador
                val subida = recolectarRecompensa(ligaId, nivel)
                desbloquear(nivel)
                antes?.let {
                    val lucha = if (ganoLucha) PantallaRecompensa.LuchaGanada(nivel.recompensa, it, rival(nivel)) else null
                    FlujoRecompensa.alCobrarNivel(nivel.recompensa.sobres, nivel.id, subida, it, lucha)
                }?.let(presentador::presentar)
            } catch (e: Exception) {
                AppLogger.error(e, "LigaNivelesViewModel")
                _estado.update { it.copy(error = "No se pudo cobrar el nivel. Intenta de nuevo.") }
            }
        }
    }

    /** Ganó la pelea de la barrera: se cobra y festeja con "¡Lucha ganada!". En iOS: `completarBarrera`. */
    fun ganarBarrera(nivel: NivelLiga) {
        if (nivel.tipo != TipoNivel.BARRERA) return
        cobrar(nivel, ganoLucha = true)
    }

    fun sonar(sonido: MotorMinijuego.Sonido) = when (sonido) {
        MotorMinijuego.Sonido.GOLPE -> audio.reproducirGolpeBarrera()
        MotorMinijuego.Sonido.FALLO -> audio.reproducirFalloBarrera()
    }

    /** Herramienta de prueba (solo debug), como la `TestingBarView` de iOS. */
    fun sumarExpDePrueba(exp: Double) {
        _estado.update { it.copy(progreso = it.progreso?.copy(totalExp = it.progreso.totalExp + exp)) }
        viewModelScope.launch {
            _estado.value.progreso?.let { guardarProgreso(it) }
            evaluarYGuardar()
        }
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    private suspend fun desbloquear(nivel: NivelLiga) {
        _estado.update { it.copy(progreso = it.progreso?.let { p -> p.copy(nivelesDesbloqueados = p.nivelesDesbloqueados + nivel.id) }) }
        evaluarYGuardar()
    }

    private suspend fun evaluarYGuardar() {
        val e = _estado.value
        val progreso = e.progreso ?: return
        val avanzado = EvaluadorProgresoLiga.evaluarAvance(progreso, e.niveles) ?: return
        _estado.update { it.copy(progreso = avanzado) }
        try {
            guardarProgreso(avanzado)
        } catch (ex: Exception) {
            AppLogger.error(ex, "LigaNivelesViewModel")
            _estado.update { it.copy(error = "No se pudo guardar tu avance. Intenta de nuevo.") }
        }
    }
}
