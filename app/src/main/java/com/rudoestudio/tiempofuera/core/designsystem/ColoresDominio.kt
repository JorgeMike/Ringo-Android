package com.rudoestudio.tiempofuera.core.designsystem

import androidx.compose.ui.graphics.Color
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad

// Colores de entidades del dominio. En iOS viven dentro de cada enum (`Rareza.color`…);
// aquí se separan para que el dominio no dependa de Compose.

val Rareza.color: Color
    get() = when (this) {
        Rareza.COMUN -> ColoresTF.Celeste
        Rareza.RARO -> ColoresTF.Verde
        Rareza.EPICO -> ColoresTF.Morado
        Rareza.LEGENDARIO -> ColoresTF.Amarillo
    }

val Rareza.colorObscuro: Color
    get() = when (this) {
        Rareza.COMUN -> ColoresTF.Azul
        Rareza.RARO -> ColoresTF.VerdeObscuro
        Rareza.EPICO -> ColoresTF.MoradoObscuro
        Rareza.LEGENDARIO -> ColoresTF.AmarilloObscuro
    }

/** Rayos detrás del luchador al revelarlo. */
val Rareza.colorRayos: Color
    get() = when (this) {
        Rareza.COMUN -> Color(0xFF0069A6)
        Rareza.RARO -> Color(0xFF4D9529)
        Rareza.EPICO -> Color(0xFF6151A9)
        Rareza.LEGENDARIO -> Color(0xFFD6A30D)
    }

val TipoPersonalidad.color: Color
    get() = when (this) {
        TipoPersonalidad.ANALISTA -> ColoresTF.Morado
        TipoPersonalidad.DIPLOMATICO -> ColoresTF.Verde
        TipoPersonalidad.CENTINELA -> ColoresTF.Celeste
        TipoPersonalidad.EXPLORADOR -> ColoresTF.Amarillo
    }
