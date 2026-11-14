package com.rudoestudio.tiempofuera.features.misiones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.designsystem.componentes.SeccionRecompensas
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.MisionesEventos
import com.rudoestudio.tiempofuera.domain.misiones.ObtenerMisionesUseCase
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionesMision
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision
import com.rudoestudio.tiempofuera.domain.servicios.HapticService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoMisiones(
    val misiones: List<Mision> = emptyList(),
    /** La recompensa que flota un momento sobre la tarjeta recién completada. */
    val recompensasVisibles: Map<Int, RecompensaMision> = emptyMap(),
    /** Misión de temporizador en curso: las demás quedan bloqueadas. */
    val misionEnCurso: Int? = null,
)

/**
 * Misiones del día en Inicio (M6.1). Al completar una, su recompensa flota medio segundo y
 * la tarjeta se cambia por la de reemplazo. En iOS: `MisionesListViewModel`.
 */
@HiltViewModel
class MisionesListaViewModel @Inject constructor(
    private val obtenerMisiones: ObtenerMisionesUseCase,
    eventos: MisionesEventos,
    sesiones: SesionesMision,
    private val haptics: HapticService,
) : ViewModel() {
    private val datos = MutableStateFlow(EstadoMisiones())

    val estado: StateFlow<EstadoMisiones> = combine(datos, sesiones.sesion) { d, sesion ->
        d.copy(misionEnCurso = sesion?.misionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoMisiones())

    init {
        viewModelScope.launch {
            eventos.completadas.collect { resultado ->
                val completada = resultado.misionCompletada
                haptics.notificacion(HapticService.TipoNotificacion.EXITO)
                datos.update { it.copy(recompensasVisibles = it.recompensasVisibles + (completada.id to resultado.recompensaOtorgada)) }
                delay(500)
                datos.update { d ->
                    val misiones = d.misiones.map { m ->
                        when {
                            m.id != completada.id -> m
                            resultado.misionReemplazo != null -> resultado.misionReemplazo
                            // El tracking se puede repetir: no se marca como completada.
                            completada.tipoMision == TipoMision.TRACKING -> m
                            else -> m.completar()
                        }
                    }
                    d.copy(misiones = misiones, recompensasVisibles = d.recompensasVisibles - completada.id)
                }
            }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                val misiones = obtenerMisiones()
                datos.update { it.copy(misiones = misiones) }
            } catch (e: Exception) {
                AppLogger.error(e, "MisionesListaViewModel")
            }
        }
    }
}

/** En iOS: `MisionesListView`. */
@Composable
fun MisionesLista(alAbrirMision: (Mision) -> Unit, modifier: Modifier = Modifier, viewModel: MisionesListaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.cargar() }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        estado.misiones.forEach { mision ->
            key(mision.id) {
                Box {
                    MisionCard(
                        mision = mision,
                        completada = mision.completada,
                        bloqueada = estado.misionEnCurso != null && estado.misionEnCurso != mision.id,
                        alSeleccionar = { alAbrirMision(mision) },
                    )
                    RecompensaFlotante(estado.recompensasVisibles[mision.id], Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

/** Lo que ganó, flotando un momento sobre la tarjeta. En iOS: `RecompensaFlotanteView`. */
@Composable
private fun RecompensaFlotante(recompensa: RecompensaMision?, modifier: Modifier) {
    AnimatedVisibility(recompensa != null, modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        recompensa?.let { SeccionRecompensas(it.puntosPoder, it.monedas, it.exp) }
    }
}
