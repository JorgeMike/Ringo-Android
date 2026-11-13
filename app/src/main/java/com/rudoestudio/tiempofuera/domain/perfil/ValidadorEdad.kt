package com.rudoestudio.tiempofuera.domain.perfil

import java.time.LocalDate
import java.time.Period

/** En iOS: `ValidadorEdad` (Domain/Services). */
object ValidadorEdad {
    const val EDAD_MINIMA = 13

    fun esMayorDeEdad(fechaNacimiento: LocalDate, edadMinima: Int = EDAD_MINIMA, hoy: LocalDate = LocalDate.now()): Boolean =
        Period.between(fechaNacimiento, hoy).years >= edadMinima
}
