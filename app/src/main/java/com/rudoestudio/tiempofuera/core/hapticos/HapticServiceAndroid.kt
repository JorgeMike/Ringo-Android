package com.rudoestudio.tiempofuera.core.hapticos

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.servicios.HapticService
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Vibraciones con el `Vibrator` de Android. Los teléfonos varían mucho: cuando hay
 * primitivas (API 30+) o control de amplitud se usan para imitar a iOS; si no, se cae a
 * efectos predefinidos, que todos tienen.
 */
@Singleton
class HapticServiceAndroid @Inject constructor(
    @ApplicationContext contexto: Context,
) : HapticService {

    private val vibrador: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            contexto.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contexto.getSystemService(Vibrator::class.java)
        }?.takeIf { it.hasVibrator() }

    private val controlAmplitud = vibrador?.hasAmplitudeControl() == true

    override fun impacto(estilo: EstiloImpacto) {
        when (estilo) {
            EstiloImpacto.LIGERO -> primitivaO(PRIMITIVA_TICK, 0.6f) { predefinido(VibrationEffect.EFFECT_TICK) }
            EstiloImpacto.MEDIO -> predefinido(VibrationEffect.EFFECT_CLICK)
            EstiloImpacto.FUERTE -> predefinido(VibrationEffect.EFFECT_HEAVY_CLICK)
            EstiloImpacto.SUAVE -> primitivaO(PRIMITIVA_LOW_TICK, 1f) { pulso(30, 0.35f) }
            EstiloImpacto.RIGIDO -> primitivaO(PRIMITIVA_CLICK, 1f) { predefinido(VibrationEffect.EFFECT_CLICK) }
        }
    }

    override fun notificacion(tipo: TipoNotificacion) {
        when (tipo) {
            TipoNotificacion.EXITO -> predefinido(VibrationEffect.EFFECT_DOUBLE_CLICK)
            // Pausa, pulso, pausa, pulso: dos avisos medianos.
            TipoNotificacion.ADVERTENCIA -> forma(longArrayOf(0, 40, 90, 40), intArrayOf(0, 180, 0, 180))
            // Tres golpes secos y rápidos.
            TipoNotificacion.ERROR -> forma(longArrayOf(0, 30, 50, 30, 50, 45), intArrayOf(0, 255, 0, 255, 0, 255))
        }
    }

    override fun seleccion() = predefinido(VibrationEffect.EFFECT_TICK)

    override fun sacudidaFuerte(duracionSegundos: Double) {
        vibrar(VibrationEffect.createOneShot((duracionSegundos * 1000).toLong().coerceAtLeast(1), 255))
    }

    override fun vibrarRompimientoDeVidrio() {
        if (!controlAmplitud) {
            // Igual que el respaldo de iOS sin motor háptico: golpe seco y uno medio.
            forma(longArrayOf(0, 20, 60, 20), intArrayOf(0, 255, 0, 160))
            return
        }
        // Golpe inicial, fragmentos a 40/80/130/190 ms que se apagan y un retumbo que decae
        // (en iOS: patrón de CoreHaptics con los mismos tiempos e intensidades).
        forma(
            longArrayOf(15, 25, 15, 25, 15, 35, 15, 45, 15, 140),
            intArrayOf(255, 150, 140, 115, 115, 90, 90, 64, 64, 40),
        )
    }

    override fun vibrarTick(intensidad: Float) {
        val i = intensidad.coerceIn(0f, 1f)
        primitivaO(PRIMITIVA_TICK, i) { if (controlAmplitud) pulso(12, i) else predefinido(VibrationEffect.EFFECT_TICK) }
    }

    override fun vibrarRespiracion(intensidad: Float) {
        val i = intensidad.coerceIn(0f, 1f)
        primitivaO(PRIMITIVA_LOW_TICK, i) { if (controlAmplitud) pulso(40, i * 0.6f) else predefinido(VibrationEffect.EFFECT_TICK) }
    }

    // --- Ayudantes ---

    private fun predefinido(efecto: Int) = vibrar(VibrationEffect.createPredefined(efecto))

    private fun pulso(ms: Long, intensidad: Float) =
        vibrar(VibrationEffect.createOneShot(ms, amplitud(intensidad)))

    private fun forma(tiempos: LongArray, amplitudes: IntArray) {
        // Sin control de amplitud, cualquier valor > 0 vibra al máximo: se respeta el ritmo.
        vibrar(VibrationEffect.createWaveform(tiempos, amplitudes, -1))
    }

    /** Usa la primitiva si el teléfono la soporta (API 30+); si no, [respaldo]. */
    private inline fun primitivaO(primitiva: Int, escala: Float, respaldo: () -> Unit) {
        val v = vibrador
        if (Build.VERSION.SDK_INT >= 30 && v != null && v.areAllPrimitivesSupported(primitiva)) {
            vibrar(VibrationEffect.startComposition().addPrimitive(primitiva, escala.coerceIn(0f, 1f)).compose())
        } else {
            respaldo()
        }
    }

    private fun vibrar(efecto: VibrationEffect) {
        val v = vibrador ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                // Como respuesta al tacto: respeta el ajuste de "vibración al tocar".
                v.vibrate(efecto, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                v.vibrate(efecto)
            }
        }.onFailure { AppLogger.error(it, "HapticService") }
    }

    private fun amplitud(intensidad: Float) = (intensidad.coerceIn(0f, 1f) * 254).roundToInt() + 1

    private companion object {
        // Constantes de API 30; solo se usan dentro de primitivaO, que revisa la versión.
        @SuppressLint("InlinedApi")
        const val PRIMITIVA_CLICK = VibrationEffect.Composition.PRIMITIVE_CLICK

        @SuppressLint("InlinedApi")
        const val PRIMITIVA_TICK = VibrationEffect.Composition.PRIMITIVE_TICK

        @SuppressLint("InlinedApi")
        const val PRIMITIVA_LOW_TICK = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
    }
}
