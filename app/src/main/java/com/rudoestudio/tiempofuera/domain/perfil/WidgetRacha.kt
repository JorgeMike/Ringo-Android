package com.rudoestudio.tiempofuera.domain.perfil

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Lo que muestra el widget de la pantalla de inicio (M13.2): tu racha, tu luchador y si ya
 * hiciste la misión de hoy. En iOS el widget (`Ringo_Widget`) todavía es la plantilla de
 * Xcode; este es nuestro.
 */
data class EstadoWidgetRacha(
    val diasRacha: Int,
    val activadaHoy: Boolean,
    val protectores: Int,
    /** Nombre del asset de iOS de la imagen (ver `AssetsIos`). */
    val luchadorImagen: String,
    val luchadorNombre: String,
) {
    val mensaje: String
        get() = when {
            activadaHoy -> "¡Racha activa hoy!"
            diasRacha > 0 -> "Haz una misión o la pierdes"
            else -> "Haz una misión y arranca tu racha"
        }
}

class ObtenerEstadoWidgetRachaUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val historico: HistoricoRachaRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(): EstadoWidgetRacha {
        val perfil = perfiles.obtenerPerfil()
        val hoy = LocalDate.now(reloj)
        // Un día protegido no cuenta como activado: todavía hay que hacer la misión.
        val activada = historico.obtenerRegistro(hoy)?.let { !it.usoProtectorRacha } ?: false
        return EstadoWidgetRacha(
            diasRacha = RachaVigencia.diasVigentes(perfil.diasRacha, perfil.lastActivityDate, reloj.instant(), reloj.zone),
            activadaHoy = activada,
            protectores = perfil.protectoresRacha,
            luchadorImagen = perfil.luchadorSeleccionado.imagen,
            luchadorNombre = perfil.luchadorSeleccionado.nombre,
        )
    }
}
