package com.rudoestudio.tiempofuera.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta de marca, igual a los colorsets `*-RG` de iOS (Assets.xcassets). En iOS no tienen
 * versión oscura: la app usa siempre esta paleta.
 */
object ColoresTF {
    val Amarillo = Color(0xFFF5B611)
    val AmarilloObscuro = Color(0xFF755506)
    val Azul = Color(0xFF002D48)
    val Negro = Color(0xFF1F1F1F)
    val Celeste = Color(0xFF1485C7)
    val Morado = Color(0xFF7566B8)
    val MoradoObscuro = Color(0xFF3B3169)
    val Naranja = Color(0xFFF76622)
    val NaranjaObscuro = Color(0xFF9F3100)
    val Rojo = Color(0xFFD31F1F)
    val Verde = Color(0xFF66AD42)
    val VerdeObscuro = Color(0xFF376A1D)

    /** `fondo-Misiones` en iOS. */
    val FondoMisiones = Color(0xFF003D60)

    /** `stroke-rojo` en iOS: contorno de textos sobre rojo. */
    val ContornoRojo = Color(0xFF8A0101)

    val Blanco = Color(0xFFFFFFFF)

    // Colores que iOS escribe en el código (no son colorsets) y se repiten en componentes.

    /** #001A29: contornos de botones, sombras de títulos, badges del temporizador. */
    val AzulNoche = Color(0xFF001A29)

    /** #002E49: pista de la barra de nivel del luchador. */
    val AzulPista = Color(0xFF002E49)

    /** Relleno de botones deshabilitados. */
    val GrisInactivo = Color(0xFFB5B8BD)

    /** #D9D9D9: sombra del subtítulo en `Titulo`. */
    val GrisClaro = Color(0xFFD9D9D9)

    /** Fondo por defecto de `TextoEnParalelogramo`. */
    val AzulParalelogramo = Color(0xFF17385A)

    /** Fondo de `BannerError`. */
    val RojoError = Color(0xFFCC2E33)
}
