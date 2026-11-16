package com.rudoestudio.tiempofuera.features.misiones

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.puntos
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision

/** Relleno de la tarjeta: #0C98E8. */
private val CelesteTarjeta = Color(0xFF0C98E8)

/**
 * Polígono de `BotonL` (lienzo 283x66) que se estira sin deformar las esquinas: los puntos
 * de la mitad derecha se recorren con el ancho y los de abajo con el alto. En iOS:
 * `anchoredPath(points:rect:)` de `MisionCardStyle`.
 */
private class FormaAnclada(private val puntos: List<Offset>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val sx = density.density
        val extraX = size.width / sx - LIENZO_ANCHO
        val extraY = size.height / sx - LIENZO_ALTO
        return Outline.Generic(
            Path().apply {
                puntos.forEachIndexed { i, p ->
                    val x = if (p.x < LIENZO_ANCHO / 2) p.x else p.x + extraX
                    val y = when {
                        p.y < 20 -> p.y
                        p.y < 40 -> p.y + extraY / 2
                        else -> p.y + extraY
                    }
                    if (i == 0) moveTo(x * sx, y * sx) else lineTo(x * sx, y * sx)
                }
                close()
            },
        )
    }

    private companion object {
        const val LIENZO_ANCHO = 283f
        const val LIENZO_ALTO = 66f
    }
}

private val BaseTarjeta = FormaAnclada(puntos(9.24437f, 57.5627f, 271.5f, 65.1197f, 282.78f, 29.5f, 276.5f, 8.5f, 271.5f, 8.5f, 9.24437f, 8.5f, 0f, 24.9822f))
private val ContornoTarjeta = FormaAnclada(puntos(10.2796f, 50.6916f, 269.6f, 55.8696f, 279.566f, 29.9793f, 273.5f, 1.5f, 10.2796f, 1.5f, 1.61328f, 24.2835f))
private val CaraTarjeta = FormaAnclada(puntos(8f, 24.5597f, 14.273f, 44.5754f, 264.5f, 48.5f, 272.094f, 29.6618f, 267.498f, 8.5f, 13.9695f, 8.5f))

/** Escala de grises para las tarjetas completadas o bloqueadas (en iOS: `.saturation(0)`). */
private val SinColor = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** El ícono de cada división. */
fun iconoDivision(division: DivisionMision?): Int = when (division) {
    DivisionMision.CONECTAR -> R.drawable.conexion_icon
    DivisionMision.LECTURA -> R.drawable.lectura_icon
    DivisionMision.DEJAR_CELULAR -> R.drawable.dejar_cel_icon
    DivisionMision.JUEGOS_MENTALES -> R.drawable.retos_icon
    DivisionMision.PROGRESO_PERSONAL -> R.drawable.progreso_icon
    DivisionMision.BIENESTAR_MENTAL -> R.drawable.meditacion_icon
    DivisionMision.EJERCICIO, null -> R.drawable.ejercicio_icon
}

/**
 * Tarjeta de una misión del día, con la recompensa sobresaliendo arriba a la derecha. Se
 * hunde al tocarla; completada o bloqueada (hay otra misión en curso) queda gris y sin
 * toque. En iOS: `MisionCardView` + `MisionCardStyle`.
 */
@Composable
fun MisionCard(mision: Mision, completada: Boolean, bloqueada: Boolean, alSeleccionar: () -> Unit, modifier: Modifier = Modifier) {
    val atenuada = completada || bloqueada
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val hundida by animateDpAsState(if (presionada || atenuada) 4.dp else 0.dp, tween(80), label = "tarjeta")
    val recompensa = mision.recompensa

    Box(
        modifier
            .fillMaxWidth()
            .padding(top = 28.dp) // lo que sobresale la recompensa (su fondo mide 57 de alto)
            .clickable(interaccion, indication = null, enabled = !atenuada, role = Role.Button, onClick = alSeleccionar)
            .semantics(mergeDescendants = true) {
                contentDescription = "${mision.titulo}. ${mision.descripcion}. " +
                    "Recompensa: ${recompensa.puntosPoder ?: 0} puntos de poder y ${recompensa.monedas ?: 0} monedas"
                if (completada) stateDescription = "Completada"
            },
    ) {
        Box(Modifier.matchParentSize().background(ColoresTF.Blanco, BaseTarjeta))
        Box(Modifier.offset { IntOffset(0, hundida.roundToPx()) }) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(ColoresTF.AzulNoche, ContornoTarjeta)
                    .border(3.dp, ColoresTF.Blanco, ContornoTarjeta),
            )
            Box(Modifier.matchParentSize().background(if (atenuada) ColoresTF.GrisInactivo else CelesteTarjeta, CaraTarjeta))
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(iconoDivision(mision.division)), null,
                    colorFilter = if (atenuada) SinColor else null,
                    modifier = Modifier.size(44.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextoContorno(
                        mision.titulo,
                        TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        ColoresTF.Blanco, ColoresTF.Negro, 4.dp,
                        maxLineas = 1,
                    )
                    Text(
                        mision.descripcion,
                        fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColoresTF.Blanco,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        InsigniaRecompensa(recompensa, atenuada, Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = (-18).dp))
    }
}

/** En iOS: `MisionCardView.RecompensaBadge`. Con experiencia (misión de liga) solo muestra la EXP. */
@Composable
private fun InsigniaRecompensa(recompensa: RecompensaMision, atenuada: Boolean, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.badge_mision_r), null,
            colorFilter = if (atenuada) SinColor else null,
            // A su tamaño (138×57), centrada detrás, como el `.background` de iOS: estirada al
            // tamaño de los números quedaba corta y el último dígito se salía.
            modifier = Modifier.matchParentSize().wrapContentSize(unbounded = true).offset((-10).dp, 2.dp),
        )
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp).offset(x = (-8).dp).rotate(4f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (recompensa.tieneExp) {
                TextoContorno("${recompensa.exp} EXP", TextStyle(fontFamily = PirataOne, fontSize = 24.sp), Color.Black, ColoresTF.Blanco, 4.dp)
            } else {
                ValorRecompensa(R.drawable.puntos_poder_icon, recompensa.puntosPoder ?: 0, 22, 21)
                ValorRecompensa(R.drawable.moneda_icon, recompensa.monedas ?: 0, 20, 19)
            }
        }
    }
}

@Composable
private fun ValorRecompensa(icono: Int, valor: Int, tamano: Int, fuente: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icono), null, Modifier.size(tamano.dp))
        Text("$valor", fontFamily = PirataOne, fontSize = fuente.sp, color = Color.Black)
    }
}
