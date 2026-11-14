package com.rudoestudio.tiempofuera.features.ligas

import androidx.compose.ui.graphics.Color

/** Colores de cada liga. En iOS: `PaletaLiga`. */
data class PaletaLiga(
    /** Fondo general, encabezado y franja de arriba. */
    val fondoOscuro: Color,
    /** Fondo claro detrás del camino. */
    val fondoScroll: Color,
    val camino: Color,
    /** Barra de experiencia. */
    val acento: Color,
) {
    companion object {
        private val Azul = PaletaLiga(Color(0xFF001A29), Color(0xFF1586C8), Color(0xFF0079BE), Color(0xFF0079BE))
        private val Verde = PaletaLiga(Color(0xFF376B1E), Color(0xFF99E65F), Color(0xFF376A1D), Color(0xFF99E65F))
        private val Morado = PaletaLiga(Color(0xFF3C3269), Color(0xFF6B59AE), Color(0xFF3C3269), Color(0xFFA38EF0))
        private val Amarillo = PaletaLiga(Color(0xFF2B2100), Color(0xFFD3A10C), Color(0xFF8E6A00), Color(0xFFE0A800))

        fun para(ligaId: Int) = when (ligaId) {
            2 -> Verde // Fuera de las cuerdas
            3 -> Morado // Alianza del Ring
            4 -> Amarillo // Reyes de la lona
            else -> Azul // La llave del Saber
        }
    }
}
