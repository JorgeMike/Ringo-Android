package com.rudoestudio.tiempofuera.domain.sincronizacion

import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import java.time.Instant

// En iOS: Configuracion/SincronizacionProgreso.swift. Lo que se manda a
// `PUT /me/sincronizacion` (M1.5).

data class ProgresoUsuarioRemoto(
    val luchadorId: Int,
    val puntosPoder: Int,
    val monedas: Int,
    val diasRacha: Int,
    val protectoresRacha: Int,
    val lastActivityDate: Instant?,
)

data class SincronizacionProgreso(
    val ligas: List<ProgresoLigaRemoto>,
    val luchadores: List<LuchadorProgresoRemoto>,
    val progreso: ProgresoUsuarioRemoto,
)
