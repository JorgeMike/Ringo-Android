package com.rudoestudio.tiempofuera.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val esquemaTF = lightColorScheme(
    primary = ColoresTF.Celeste,
    onPrimary = ColoresTF.Blanco,
    primaryContainer = ColoresTF.Azul,
    onPrimaryContainer = ColoresTF.Blanco,
    secondary = ColoresTF.Amarillo,
    onSecondary = ColoresTF.Negro,
    secondaryContainer = ColoresTF.AmarilloObscuro,
    onSecondaryContainer = ColoresTF.Blanco,
    tertiary = ColoresTF.Verde,
    onTertiary = ColoresTF.Blanco,
    tertiaryContainer = ColoresTF.VerdeObscuro,
    onTertiaryContainer = ColoresTF.Blanco,
    error = ColoresTF.Rojo,
    onError = ColoresTF.Blanco,
    background = ColoresTF.Blanco,
    onBackground = ColoresTF.Negro,
    surface = ColoresTF.Blanco,
    onSurface = ColoresTF.Negro,
)

/**
 * Tema de la app. Siempre claro, como iOS: la paleta de marca no tiene versión oscura.
 * Para colores de marca que Material no cubre, usar [ColoresTF] directo.
 */
@Composable
fun TiempoFueraTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = esquemaTF,
        typography = TipografiaTF,
        content = contenido,
    )
}
