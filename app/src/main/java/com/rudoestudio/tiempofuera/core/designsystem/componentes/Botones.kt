package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF

// Formas de BotonL convertidas 1:1 de los `d=` del SVG (lienzo 283x66, igual que iOS).
private const val L_ANCHO = 283f
private const val L_ALTO = 66f
private val BaseL = FormaPoligono(
    L_ANCHO, L_ALTO,
    puntos(9.24437f, 57.5627f, 271.5f, 65.1197f, 282.78f, 29.5f, 276.5f, 8.5f, 271.5f, 8.5f, 9.24437f, 8.5f, 0f, 24.9822f),
)
private val ContornoL = FormaPoligono(
    L_ANCHO, L_ALTO,
    puntos(10.2796f, 50.6916f, 269.6f, 55.8696f, 279.566f, 29.9793f, 273.5f, 1.5f, 10.2796f, 1.5f, 1.61328f, 24.2835f),
)
private val CaraL = FormaPoligono(
    L_ANCHO, L_ALTO,
    puntos(8f, 24.5597f, 14.273f, 44.5754f, 264.5f, 48.5f, 272.094f, 29.6618f, 267.498f, 8.5f, 13.9695f, 8.5f),
)

// Formas de BotonS (lienzo 63x54, el viewBox del SVG "Activo").
private const val S_ANCHO = 63f
private const val S_ALTO = 54f
private val BaseS = FormaPoligono(
    S_ANCHO, S_ALTO,
    puntos(
        10.288f, 47.8429f, 51.0362f, 52.073f, 60.7988f, 30.9226f, 60.7988f, 25.1572f,
        54.8564f, 7.65723f, 10.288f, 7.65723f, 1.79883f, 21.1572f, 1.79883f, 26.2695f,
    ),
)
private val ContornoS = FormaPoligono(
    S_ANCHO, S_ALTO,
    puntos(10.288f, 41.8429f, 51.0362f, 46.073f, 60.7988f, 24.9226f, 54.8564f, 1.65723f, 10.288f, 1.65723f, 1.79883f, 20.2695f),
)
private val CaraS = FormaPoligono(
    S_ANCHO, S_ALTO,
    puntos(9.0918f, 20.6658f, 15.207f, 36.9201f, 46.1049f, 40.1073f, 53.5075f, 24.8091f, 49.0276f, 7.62402f, 14.9112f, 7.62402f),
)

/**
 * Botón con "tapa" que se hunde al presionarlo o si está deshabilitado. Es la base de
 * [BotonL] y [BotonS]: base blanca fija, y encima contorno + cara + contenido que bajan.
 */
@Composable
private fun BotonConTapa(
    base: FormaPoligono,
    contorno: FormaPoligono,
    cara: FormaPoligono,
    ancho: Dp,
    alto: Dp,
    color: Color,
    colorInactivo: Color,
    desplazamientoPresion: Dp,
    habilitado: Boolean,
    alTocar: () -> Unit,
    modifier: Modifier,
    contenido: @Composable BoxScope.() -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val hundido = presionado || !habilitado
    val desplazamiento by animateDpAsState(
        if (hundido) desplazamientoPresion else 0.dp, tween(80), label = "hundido",
    )
    val relleno by animateColorAsState(if (habilitado) color else colorInactivo, tween(150), label = "relleno")
    Box(
        modifier
            .size(ancho, alto)
            .clickable(interaccion, indication = null, enabled = habilitado, role = Role.Button, onClick = alTocar),
    ) {
        Box(Modifier.matchParentSize().background(ColoresTF.Blanco, base))
        Box(Modifier.matchParentSize().offset(y = desplazamiento)) {
            Box(Modifier.matchParentSize().background(ColoresTF.AzulNoche, contorno).border(3.dp, ColoresTF.Blanco, contorno))
            Box(Modifier.matchParentSize().background(relleno, cara))
            contenido()
        }
    }
}

/** Botón grande con texto (283x66). En iOS: `BotonL` (BotonPrimario.swift). */
@Composable
fun BotonL(
    color: Color,
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    desplazamientoPresion: Dp = 4.dp,
    colorInactivo: Color = ColoresTF.GrisInactivo,
) {
    BotonConTapa(
        BaseL, ContornoL, CaraL, L_ANCHO.dp, L_ALTO.dp, color, colorInactivo,
        desplazamientoPresion, habilitado, alTocar, modifier,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleLarge,
            color = ColoresTF.Blanco,
            modifier = Modifier.align(Alignment.Center).offset(y = (-6).dp),
        )
    }
}

/** Íconos de [BotonS]. Arriba/abajo reusan "Derecha" rotada, como en iOS. */
enum class IconoBotonS(@DrawableRes val imagen: Int?, val rotacion: Float, val descripcion: String) {
    CHECK(R.drawable.check, 0f, "Listo"),
    COMPARTIR(R.drawable.compartir, 0f, "Compartir"),
    REGRESAR(R.drawable.izquierda, 0f, "Regresar"),
    CONTINUAR(R.drawable.derecha, 0f, "Continuar"),
    ARRIBA(R.drawable.derecha, -90f, "Arriba"),
    ABAJO(R.drawable.derecha, 90f, "Abajo"),
    VACIO(null, 0f, ""),
}

/** Botón chico con ícono (63x54 por [escala]). En iOS: `BotonS` (BotonSecundario.swift). */
@Composable
fun BotonS(
    color: Color,
    icono: IconoBotonS,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    escala: Float = 1f,
    habilitado: Boolean = true,
    desplazamientoPresion: Dp = 6.dp,
    colorInactivo: Color = ColoresTF.GrisInactivo,
) {
    BotonConTapa(
        BaseS, ContornoS, CaraS, (S_ANCHO * escala).dp, (S_ALTO * escala).dp, color, colorInactivo,
        desplazamientoPresion * escala, habilitado, alTocar,
        modifier.semantics { contentDescription = icono.descripcion },
    ) {
        icono.imagen?.let {
            Image(
                painterResource(it),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-6 * escala).dp)
                    .rotate(icono.rotacion)
                    .scale(escala),
            )
        }
    }
}

/** Botón de regresar. En iOS: `BackButton`. */
@Composable
fun BotonRegresar(alTocar: () -> Unit, modifier: Modifier = Modifier) {
    BotonS(color = ColoresTF.Celeste, icono = IconoBotonS.REGRESAR, alTocar = alTocar, modifier = modifier)
}

/**
 * Botón "3D" con sombra sólida abajo que desaparece al presionarlo. En iOS:
 * `ThreeDButtonStyle`. El tamaño lo define [contenido] (agregarle su propio padding).
 */
@Composable
fun Boton3D(
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ColoresTF.Verde,
    colorSombra: Color = ColoresTF.VerdeObscuro,
    radio: Dp = 14.dp,
    profundidad: Dp = 5.dp,
    forzarPresionado: Boolean = false,
    habilitado: Boolean = true,
    contenido: @Composable BoxScope.() -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado = interaccion.collectIsPressedAsState().value || forzarPresionado
    val resorte = spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
    val desplazamiento by animateDpAsState(if (presionado) profundidad else 0.dp, resorte, label = "boton3d")
    val opacidadSombra by animateFloatAsState(if (presionado) 0f else 1f, label = "sombra3d")
    val forma = RoundedCornerShape(radio)
    Box(
        modifier.clickable(interaccion, indication = null, enabled = habilitado, role = Role.Button, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().offset(y = profundidad).alpha(opacidadSombra).background(colorSombra, forma))
        Box(Modifier.matchParentSize().offset(y = desplazamiento).background(color, forma).border(3.dp, colorSombra, forma))
        Box(Modifier.offset(y = desplazamiento), contentAlignment = Alignment.Center, content = contenido)
    }
}
