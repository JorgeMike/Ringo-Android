package com.rudoestudio.tiempofuera.features.luchadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.luchadores.CalculadorDeNivel
import com.rudoestudio.tiempofuera.domain.luchadores.ComprarSubidaNivelUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.EquiparLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.recompensas.FlujoRecompensa
import com.rudoestudio.tiempofuera.features.recompensas.PresentadorRecompensas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Un luchador de la colección, con lo que la pantalla necesita ya calculado. */
data class LuchadorEnColeccion(
    val luchador: Luchador,
    /** De 0 a 1, hacia el siguiente nivel. */
    val progreso: Float,
    val puedeSubir: Boolean,
)

data class EstadoLuchadores(
    val cargando: Boolean = true,
    val puntosPoder: Int = 0,
    val equipado: LuchadorEnColeccion? = null,
    /** Los que ya tiene: de mayor a menor nivel. */
    val desbloqueados: List<LuchadorEnColeccion> = emptyList(),
    val error: String? = null,
)

/**
 * Tab Luchadores (M8.1): el luchador equipado, subirle de nivel con puntos de poder y
 * cambiarlo por otro de la colección. En iOS: `LuchadoresViewModel`.
 */
@HiltViewModel
class LuchadoresViewModel @Inject constructor(
    perfiles: UserProfileRepository,
    private val luchadores: LuchadorRepository,
    private val calculador: CalculadorDeNivel,
    private val equipar: EquiparLuchadorUseCase,
    private val comprarSubida: ComprarSubidaNivelUseCase,
    private val presentador: PresentadorRecompensas,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)

    // El perfil se vuelve a emitir cuando cambia cualquier luchador: así la colección
    // también se refresca al subir de nivel o al sacar uno de un sobre.
    val estado: StateFlow<EstadoLuchadores> = combine(
        perfiles.perfil.map { perfil -> perfil to luchadores.obtenerTodos() },
        error,
    ) { (perfil, todos), mensaje ->
        fun enColeccion(l: Luchador) = LuchadorEnColeccion(
            l, calculador.progreso(l.puntosPoder).toFloat(), perfil.puntosPoder >= costo(l),
        )
        val equipado = todos.firstOrNull { it.id == perfil.luchadorSeleccionado.id } ?: perfil.luchadorSeleccionado
        EstadoLuchadores(
            cargando = false,
            puntosPoder = perfil.puntosPoder,
            equipado = enColeccion(equipado),
            desbloqueados = todos.filter { it.cantidad > 0 }.sortedWith(compareByDescending<Luchador> { it.nivel }.thenBy { it.id }).map(::enColeccion),
            error = mensaje,
        )
    }
        .catch { e ->
            AppLogger.error(e, "LuchadoresViewModel")
            emit(EstadoLuchadores(cargando = false, error = "No se pudieron cargar tus luchadores. Intenta de nuevo."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoLuchadores())

    fun costo(luchador: Luchador): Int = calculador.costoParaSubirA(luchador.nivel + 1)

    fun equipar(luchador: Luchador) {
        viewModelScope.launch {
            try {
                equipar.invoke(luchador)
                error.value = null
            } catch (e: Exception) {
                AppLogger.error(e, "LuchadoresViewModel")
                error.value = "No se pudo cambiar tu luchador. Intenta de nuevo."
            }
        }
    }

    /** Si sube, festeja con la subida de nivel (en iOS: `flowRecompensa = .subidaNivel`). */
    fun subirNivel(luchador: Luchador) {
        viewModelScope.launch {
            try {
                val resultado = comprarSubida(luchador)
                error.value = null
                presentador.presentar(FlujoRecompensa.SubidaNivel(resultado, luchador))
            } catch (e: Exception) {
                AppLogger.error(e, "LuchadoresViewModel")
                error.value = e.message ?: "No se pudo subir de nivel. Intenta de nuevo."
            }
        }
    }
}
