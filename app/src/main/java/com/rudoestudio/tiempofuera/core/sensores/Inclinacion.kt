package com.rudoestudio.tiempofuera.core.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Cuánto se inclina la tarjeta de un luchador según cómo sostienes el teléfono (M6.12).
 * En iOS: `DeviceMotionService`, con las mismas cifras: el frente cuenta desde los 45° (así
 * sostenido, la tarjeta se ve derecha) y cada radián son 18°; de lado, la mitad.
 */
object InclinacionTarjeta {
    private const val FUERZA = 18f
    private const val FRENTE_RECTO = (Math.PI / 4).toFloat()
    private const val MAXIMO = 25f

    /**
     * Grados de giro en X (frente) y en Y (lado). [cabeceo] y [alabeo] en radianes como los
     * da iOS: cabeceo 0 acostado y π/2 parado; alabeo positivo con la orilla derecha abajo.
     */
    fun grados(cabeceo: Float, alabeo: Float): Pair<Float, Float> =
        ((cabeceo - FRENTE_RECTO) * FUERZA).coerceIn(-MAXIMO, MAXIMO) to (-alabeo * FUERZA / 2).coerceIn(-MAXIMO, MAXIMO)
}

/**
 * Inclina en 3D lo que lleva, siguiendo al teléfono, con el resorte suave de iOS
 * (`Card3DTiltModifier`). El sensor solo corre mientras esto está en pantalla; sin sensor
 * de rotación se queda derecho.
 */
fun Modifier.inclinacionDelTelefono(): Modifier = composed {
    val contexto = LocalContext.current
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val gestor = contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = gestor.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val rotacion = FloatArray(9)
        val orientacion = FloatArray(3)
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotacion, evento.values)
                SensorManager.getOrientation(rotacion, orientacion)
                // Android: cabeceo negativo al levantar la parte de arriba (iOS al revés).
                val (gx, gy) = InclinacionTarjeta.grados(cabeceo = -orientacion[1], alabeo = orientacion[2])
                x = gx
                y = gy
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        sensor?.let { gestor.registerListener(oyente, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { gestor.unregisterListener(oyente) }
    }
    val resorte = spring<Float>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
    val rx by animateFloatAsState(x, resorte, label = "inclinacionX")
    val ry by animateFloatAsState(y, resorte, label = "inclinacionY")
    graphicsLayer {
        rotationX = rx
        rotationY = ry
        cameraDistance = 10 * density
    }
}
