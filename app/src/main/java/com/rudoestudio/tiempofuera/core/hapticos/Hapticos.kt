package com.rudoestudio.tiempofuera.core.hapticos

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.rudoestudio.tiempofuera.domain.servicios.HapticService
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion

/**
 * El [HapticService] para usar desde Compose. Lo provee `MainActivity`; en ViewModels
 * se inyecta con Hilt.
 */
val LocalHaptics = staticCompositionLocalOf<HapticService> {
    error("LocalHaptics sin proveer: envolver la UI con CompositionLocalProvider en MainActivity")
}

/**
 * Vibra al tocar, sin quitarle el toque a `clickable` ni a otros gestos. En iOS:
 * `.hapticOnTap(_:)` (View+Haptics).
 */
fun Modifier.hapticAlTocar(estilo: EstiloImpacto = EstiloImpacto.MEDIO): Modifier =
    alTerminarToque { impacto(estilo) }

/** Vibración de notificación al tocar. En iOS: `.hapticNotification(_:)`. */
fun Modifier.hapticNotificacionAlTocar(tipo: TipoNotificacion): Modifier =
    alTerminarToque { notificacion(tipo) }

private fun Modifier.alTerminarToque(accion: HapticService.() -> Unit): Modifier = composed {
    val haptics = LocalHaptics.current
    pointerInput(haptics) {
        awaitEachGesture {
            // Pase inicial: vemos el toque antes que clickable lo consuma.
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (waitForUpOrCancellation(PointerEventPass.Initial) != null) haptics.accion()
        }
    }
}
