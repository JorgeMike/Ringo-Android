package com.rudoestudio.tiempofuera.features.bloqueo

import androidx.lifecycle.ViewModel
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadoPermisos
import com.rudoestudio.tiempofuera.domain.bloqueo.RevisorPermisos
import com.rudoestudio.tiempofuera.servicios.bloqueo.PermisosBloqueo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Permisos del bloqueo (M3.1). Se revisan al volver a la pantalla, porque todos se dan en
 * Ajustes, fuera de la app. En iOS: `ConfPermisosViewModel` (paso 10 del onboarding).
 */
@HiltViewModel
class PermisosBloqueoViewModel @Inject constructor(
    private val revisor: RevisorPermisos,
    private val permisos: PermisosBloqueo,
) : ViewModel() {
    private val _estado = MutableStateFlow(revisor.estado())
    val estado: StateFlow<EstadoPermisos> = _estado.asStateFlow()

    /** Si ya se mostró el diálogo de notificaciones: Android no lo vuelve a mostrar si lo negaron dos veces. */
    var notificacionesPedidas = false
        private set

    fun refrescar() {
        _estado.value = revisor.estado()
    }

    fun abrirAccesibilidad() = permisos.abrirAccesibilidad()

    fun abrirAccesoAlUso() = permisos.abrirAccesoAlUso()

    fun abrirBateria() = permisos.abrirBateria()

    fun abrirNotificaciones() = permisos.abrirNotificaciones()

    fun marcarNotificacionesPedidas() {
        notificacionesPedidas = true
    }
}
