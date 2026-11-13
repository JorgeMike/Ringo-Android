package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF

/**
 * Marco del luchador con los colores de su rareza. En iOS: `FondoLuchadorView(rareza:)`.
 * Recibe los colores sueltos hasta que exista `Rareza` en el dominio (M2.6).
 */
@Composable
fun FondoLuchador(colorRareza: Color, colorRarezaObscuro: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(99f / 103f)) {
        val sx = size.width / 99f
        val sy = size.height / 103f
        fun Path.m(x: Float, y: Float) = moveTo(x * sx, y * sy)
        fun Path.l(x: Float, y: Float) = lineTo(x * sx, y * sy)
        // addCurve(to: fin, control1:, control2:) de iOS = cubicTo(control1, control2, fin)
        fun Path.c(x: Float, y: Float, c1x: Float, c1y: Float, c2x: Float, c2y: Float) =
            cubicTo(c1x * sx, c1y * sy, c2x * sx, c2y * sy, x * sx, y * sy)

        val marco = Path().apply { m(98.8135f, 102.014f); l(3.44824f, 102.014f); l(0f, 12.5146f); l(91.5703f, 0f); close() }
        val interior = Path().apply { m(4.13672f, 15.9866f); l(87.8827f, 4.54102f); l(94.5192f, 98.0135f); l(7.29695f, 98.0135f); close() }
        val adornos = Path().apply {
            // Tres círculos-espiral abajo
            m(23.8828f, 84.541f)
            c(21.3828f, 82.041f, 23.8828f, 83.1603f, 22.7635f, 82.041f)
            c(18.8828f, 84.541f, 20.0021f, 82.041f, 18.8828f, 83.1603f)
            c(21.3828f, 87.041f, 18.8828f, 85.9217f, 20.0021f, 87.041f)
            l(21.3828f, 90.041f)
            c(15.8828f, 84.541f, 18.3452f, 90.041f, 15.8828f, 87.5786f)
            c(21.3828f, 79.041f, 15.8828f, 81.5034f, 18.3452f, 79.041f)
            c(26.8828f, 84.541f, 24.4204f, 79.041f, 26.8828f, 81.5034f)
            c(21.3828f, 90.041f, 26.8828f, 87.5786f, 24.4204f, 90.041f)
            l(21.3828f, 87.041f)
            c(23.8828f, 84.541f, 22.7635f, 87.041f, 23.8828f, 85.9217f)
            close()
            m(38.8028f, 83.501f)
            c(35.3428f, 80.041f, 38.8028f, 81.5901f, 37.2537f, 80.041f)
            c(31.8828f, 83.501f, 33.4319f, 80.0411f, 31.8829f, 81.5901f)
            c(35.3428f, 86.961f, 31.8828f, 85.4119f, 33.4319f, 86.961f)
            l(35.3428f, 89.961f)
            c(28.8828f, 83.501f, 31.775f, 89.961f, 28.8828f, 87.0688f)
            c(35.3428f, 77.041f, 28.8828f, 79.9333f, 31.7751f, 77.041f)
            c(41.8028f, 83.501f, 38.9106f, 77.041f, 41.8028f, 79.9333f)
            c(35.3428f, 89.961f, 41.8028f, 87.0688f, 38.9106f, 89.961f)
            l(35.3428f, 86.961f)
            c(38.8028f, 83.501f, 37.2537f, 86.961f, 38.8028f, 85.4119f)
            close()
            m(87.2324f, 88.8652f)
            c(84.7324f, 86.3652f, 87.2324f, 87.4845f, 86.1131f, 86.3652f)
            c(82.2324f, 88.8652f, 83.3517f, 86.3652f, 82.2324f, 87.4845f)
            c(84.7324f, 91.3652f, 82.2324f, 90.2459f, 83.3517f, 91.3652f)
            l(84.7324f, 94.3652f)
            c(79.2324f, 88.8652f, 81.6949f, 94.3652f, 79.2324f, 91.9028f)
            c(84.7324f, 83.3652f, 79.2324f, 85.8277f, 81.6949f, 83.3652f)
            c(90.2324f, 88.8652f, 87.77f, 83.3652f, 90.2324f, 85.8277f)
            c(84.7324f, 94.3652f, 90.2324f, 91.9028f, 87.77f, 94.3652f)
            l(84.7324f, 91.3652f)
            c(87.2324f, 88.8652f, 86.1131f, 91.3652f, 87.2324f, 90.2459f)
            close()
            // Estrella
            m(59.3803f, 17.3217f)
            c(65.3853f, 17.3217f, 60.2571f, 14.2808f, 64.5085f, 14.2808f)
            l(67.3767f, 24.2282f)
            c(71.8857f, 26.1171f, 67.9378f, 26.174f, 70.1324f, 27.0933f)
            l(78.0756f, 22.6704f)
            c(82.3345f, 26.954f, 80.8089f, 21.1485f, 83.8163f, 24.1732f)
            l(78.9069f, 33.3866f)
            c(80.7678f, 37.9304f, 77.9638f, 35.1564f, 78.8651f, 37.3572f)
            l(87.6463f, 40.003f)
            c(87.6463f, 46.0791f, 90.6283f, 40.9014f, 90.6283f, 45.1806f)
            l(80.7678f, 48.1516f)
            c(78.9069f, 52.6955f, 78.8651f, 48.7248f, 77.9638f, 50.9256f)
            l(82.3345f, 59.128f)
            c(78.0756f, 63.4116f, 83.8163f, 61.9088f, 80.8089f, 64.9335f)
            l(71.8857f, 59.965f)
            c(67.3767f, 61.8539f, 70.1324f, 58.9887f, 67.9378f, 59.9081f)
            l(65.3853f, 68.7604f)
            c(59.3803f, 68.7604f, 64.5085f, 71.8012f, 60.2571f, 71.8012f)
            l(57.3889f, 61.8539f)
            c(52.88f, 59.965f, 56.8279f, 59.9081f, 54.6333f, 58.9887f)
            l(46.69f, 63.4116f)
            c(42.4311f, 59.128f, 43.9567f, 64.9335f, 40.9494f, 61.9088f)
            l(45.8587f, 52.6955f)
            c(43.9978f, 48.1516f, 46.8018f, 50.9256f, 45.9005f, 48.7248f)
            l(37.1193f, 46.0791f)
            c(37.1193f, 40.003f, 34.1373f, 45.1806f, 34.1373f, 40.9014f)
            l(43.9978f, 37.9304f)
            c(45.8587f, 33.3866f, 45.9005f, 37.3572f, 46.8018f, 35.1564f)
            l(42.4311f, 26.954f)
            c(46.69f, 22.6704f, 40.9494f, 24.1732f, 43.9567f, 21.1485f)
            l(52.88f, 26.1171f)
            c(57.3889f, 24.2282f, 54.6333f, 27.0933f, 56.8279f, 26.174f)
            close()
        }
        drawPath(marco, Color.Black)
        drawPath(interior, colorRareza)
        drawPath(adornos, colorRarezaObscuro)
    }
}

// Barra de nivel: lienzo 187x55, igual que iOS.
private const val N_ANCHO = 187f
private const val N_ALTO = 55f
private val Contenedor = puntos(49.1598f, 46.6876f, 176.875f, 49.9823f, 185.051f, 33.5091f, 180.074f, 15.3887f, 49.1598f, 15.3887f, 42.0508f, 29.885f)
private val Pista = puntos(48.1562f, 30.1929f, 53.2772f, 42.8527f, 172.349f, 45.335f, 178.548f, 33.4199f, 174.797f, 20.0352f, 53.0295f, 20.0352f)
private val BadgeExterior = puntos(
    2.05078f, 37.1713f, 9.87687f, 52.3887f, 22.9203f, 49.78f, 24.6595f, 51.5191f, 48.1377f, 49.78f,
    54.2247f, 24.1278f, 45.529f, 2.38867f, 32.4856f, 5.86693f, 33.3551f, 4.56258f, 4.65948f, 12.8235f,
)
private val BadgeInterior = puntos(
    5.52734f, 36.7178f, 11.6143f, 48.8307f, 22.9186f, 46.5169f, 25.9621f, 48.8307f, 46.31f, 47.2882f,
    51.7592f, 24.9976f, 43.7882f, 5.86719f, 31.9042f, 8.95225f, 32.6578f, 7.23581f, 7.78821f, 14.5628f,
)

/** Relleno de la barra: la orilla derecha avanza de la izquierda a la derecha de la pista. */
private fun puntosProgreso(p: Float) = puntos(
    48.1562f, 30.1929f,
    53.2772f, 42.8527f,
    53.2772f + p * (172.349f - 53.2772f), 43.8162f,
    48.1562f + p * (178.548f - 48.1562f), 33.4199f,
    53.0295f + p * (174.797f - 53.0295f), 20.0352f,
    53.0295f, 20.0352f,
)

private fun DrawScope.poligono(p: List<Offset>, color: Color, trazo: Float? = null) {
    val path = FormaPoligono(N_ANCHO, N_ALTO, p).trazo(size)
    drawPath(path, color, style = trazo?.let { Stroke(it) } ?: Fill)
}

/**
 * Nombre, nivel y barra de progreso del luchador. En iOS: `IndicadorNivelLuchadorView`.
 * Por ahora recibe los datos ya calculados; la versión que recibe `Luchador` y usa
 * `CalculadorDeNivel` llega con M2.3/M8.
 */
@Composable
fun IndicadorNivelLuchador(
    nivel: Int,
    progreso: Float,
    colorRareza: Color,
    colorRarezaObscuro: Color,
    modifier: Modifier = Modifier,
    nombre: String = "Ringo",
    muestraNombre: Boolean = true,
    muestraIndicadorSubirNivel: Boolean = false,
) {
    val avance by animateFloatAsState(progreso.coerceIn(0f, 1f), label = "nivel")
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy((-12).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (muestraNombre) {
            TextoContorno(
                nombre,
                TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                ColoresTF.Blanco, Color.Black, 4.dp,
                Modifier.widthIn(max = N_ANCHO.dp).padding(bottom = 2.dp).zIndex(2f),
                maxLineas = 1,
            )
        }
        Box(Modifier.size(N_ANCHO.dp, N_ALTO.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val s = size.width / N_ANCHO
                poligono(Contenedor, ColoresTF.AzulNoche)
                poligono(Contenedor, ColoresTF.Blanco, trazo = 3.3f * s)
                poligono(Pista, ColoresTF.AzulPista)
                if (avance > 0f) poligono(puntosProgreso(avance), colorRareza)
                if (!muestraIndicadorSubirNivel) {
                    poligono(BadgeExterior, colorRareza)
                    poligono(BadgeExterior, ColoresTF.Blanco, trazo = 4f * s)
                    poligono(BadgeInterior, colorRareza)
                    poligono(BadgeInterior, ColoresTF.AzulNoche, trazo = 4f * s)
                }
            }
            if (muestraIndicadorSubirNivel) {
                Image(
                    painterResource(R.drawable.sube_nivel_indicador),
                    contentDescription = "Sube de nivel",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(53.dp, 64.dp).centrarEn((N_ANCHO * 0.25f).dp, (N_ALTO * 0.53f).dp),
                )
            } else {
                TextoContorno(
                    "$nivel",
                    TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 24.sp),
                    ColoresTF.Blanco, colorRarezaObscuro, 4.dp,
                    Modifier.centrarEn((N_ANCHO * 0.15f).dp, (N_ALTO * 0.53f).dp),
                )
            }
        }
    }
}
