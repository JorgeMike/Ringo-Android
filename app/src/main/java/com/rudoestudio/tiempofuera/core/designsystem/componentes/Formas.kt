package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Polígono definido en un lienzo de referencia ([ancho] x [alto], el viewBox del SVG
 * original) y escalado al tamaño real. Así se portan 1:1 las formas de iOS que usan
 * `buildPath(points:rect:)` (BotonL, BotonS, barra de nivel…).
 */
class FormaPoligono(
    private val ancho: Float,
    private val alto: Float,
    private val puntos: List<Offset>,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(trazo(size))

    fun trazo(size: Size): Path {
        val sx = size.width / ancho
        val sy = size.height / alto
        return Path().apply {
            puntos.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.x * sx, p.y * sy) else lineTo(p.x * sx, p.y * sy)
            }
            close()
        }
    }
}

/** `puntos(x1, y1, x2, y2, …)` → lista de [Offset]. */
fun puntos(vararg coordenadas: Float): List<Offset> =
    coordenadas.toList().chunked(2) { Offset(it[0], it[1]) }

/** En iOS: `ParalelogramoShape`. Arriba se recorre a la derecha, abajo a la izquierda. */
class ParalelogramoShape(private val inclinacion: Dp = 24.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val i = with(density) { inclinacion.toPx() }
        return Outline.Generic(
            Path().apply {
                moveTo(i, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - i, size.height)
                lineTo(0f, size.height)
                close()
            },
        )
    }
}

/**
 * En iOS: `DiagonalShape`. El borde de arriba se desplaza [inclinacion] a la derecha
 * (negativo = a la izquierda) y puede salirse del área, igual que en iOS.
 */
class DiagonalShape(private val inclinacion: Dp = 18.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { inclinacion.toPx() }
        return Outline.Generic(
            Path().apply {
                moveTo(s, 0f)
                lineTo(size.width + s, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
        )
    }
}

/**
 * Coloca el centro del elemento en ([x], [y]) del padre, como `.position(x:y:)` de
 * SwiftUI. El elemento debe estar alineado arriba a la izquierda dentro de un `Box`.
 */
fun Modifier.centrarEn(x: Dp, y: Dp): Modifier = layout { medible, restricciones ->
    val colocable = medible.measure(restricciones.copy(minWidth = 0, minHeight = 0))
    layout(colocable.width, colocable.height) {
        colocable.place(
            IntOffset(x.roundToPx() - colocable.width / 2, y.roundToPx() - colocable.height / 2),
        )
    }
}
