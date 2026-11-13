package com.rudoestudio.tiempofuera.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rudoestudio.tiempofuera.R

/** Arvo: casi todo el texto de la app (en iOS, `Arvo-Bold` de 11 a 24 pt). */
val Arvo = FontFamily(
    Font(R.font.arvo_regular, FontWeight.Normal),
    Font(R.font.arvo_bold, FontWeight.Bold),
    Font(R.font.arvo_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.arvo_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

/** Pirata One: títulos grandes (en iOS, de 16 a 40 pt, sobre todo en el onboarding). */
val PirataOne = FontFamily(Font(R.font.pirata_one_regular, FontWeight.Normal))

private fun arvo(tamano: Int, peso: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Arvo, fontWeight = peso, fontSize = tamano.sp)

private fun pirata(tamano: Int) = TextStyle(fontFamily = PirataOne, fontSize = tamano.sp)

/**
 * Escala tipográfica tomada de los tamaños que más usa iOS. En iOS casi todo es Arvo
 * Bold; aquí `body*` queda en Regular para textos largos y el resto en Bold.
 */
val TipografiaTF = Typography(
    displayLarge = pirata(40),
    displayMedium = pirata(36),
    displaySmall = pirata(24),
    headlineLarge = arvo(24),
    headlineMedium = arvo(20),
    headlineSmall = arvo(18),
    titleLarge = arvo(18),
    titleMedium = arvo(16),
    titleSmall = arvo(14),
    bodyLarge = arvo(16, FontWeight.Normal),
    bodyMedium = arvo(14, FontWeight.Normal),
    bodySmall = arvo(12, FontWeight.Normal),
    labelLarge = arvo(14),
    labelMedium = arvo(12),
    labelSmall = arvo(11),
)
