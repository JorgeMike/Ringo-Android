package com.rudoestudio.tiempofuera.domain.recompensas

import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoRecoleccion
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoSubidaNivel
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision

/**
 * Lo que dispara una celebración. En iOS: `RecompensaFlowType`. Faltan `piñata` y
 * `luchaGanada`, que solo salen al ganar en ligas (M7.3 y M7.6, con M9).
 */
sealed interface FlujoRecompensa {
    /** Abrir un sobre (tienda, onboarding). */
    data class SobrePersonaje(val rareza: Rareza) : FlujoRecompensa

    /** Primera misión del día: racha y luego la misión completada. */
    data class RachaActivada(val diasDeRacha: Int, val recompensaMision: RecompensaMision) : FlujoRecompensa

    data class MisionCompletada(val recompensaMision: RecompensaMision) : FlujoRecompensa

    /** Subida de nivel comprada con puntos de poder (colección de luchadores). */
    data class SubidaNivel(val resultado: ResultadoSubidaNivel, val luchador: Luchador) : FlujoRecompensa

    companion object {
        /** Al reclamar una misión. En iOS: `MainTabView.onChange(of: misionActiva)`. */
        fun alCompletarMision(rachaActivada: Boolean, diasDeRacha: Int, recompensa: RecompensaMision): FlujoRecompensa =
            if (rachaActivada) RachaActivada(diasDeRacha, recompensa) else MisionCompletada(recompensa)
    }
}

/** Cada pantalla del flujo. En iOS: las rutas de `RecompensaFlowCoordinator`. */
sealed interface PantallaRecompensa {
    data class Sobre(val rareza: Rareza) : PantallaRecompensa

    data class RachaActivada(val diasDeRacha: Int, val recompensaMision: RecompensaMision) : PantallaRecompensa

    data class MisionCompletada(val recompensaMision: RecompensaMision) : PantallaRecompensa

    /** Un sobre trajo un luchador repetido y subió de nivel (o de rareza). */
    data class SubidaNivel(val resultado: ResultadoSubidaNivel, val luchador: Luchador?) : PantallaRecompensa
}

/**
 * En qué pantalla va la celebración y qué sigue. Inmutable: cada evento regresa el estado
 * nuevo, y [pantalla] `null` es que ya terminó. [paso] cambia en cada pantalla (aunque se
 * repita el tipo, p. ej. dos sobres seguidos) para reiniciar sus animaciones. En iOS:
 * `RecompensaFlowCoordinator`.
 */
data class EstadoRecompensas(
    val pantalla: PantallaRecompensa?,
    val sobresPendientes: List<Rareza> = emptyList(),
    val paso: Int = 0,
) {
    val terminado: Boolean get() = pantalla == null

    /** El sobre no se pudo abrir: se sigue como si no hubiera sido repetido. */
    fun sobreConError(): EstadoRecompensas = siguienteSobre()

    /** Se recolectó el luchador; si era repetido, primero se festeja la subida de nivel. */
    fun sobreRecolectado(luchador: Luchador, resultado: ResultadoRecoleccion): EstadoRecompensas =
        if (!resultado.esDuplicado) siguienteSobre() else ir(PantallaRecompensa.SubidaNivel(resultado.comoSubidaNivel(), luchador))

    /** "Continuar" en racha, misión completada o subida de nivel. */
    fun continuar(): EstadoRecompensas = when (val p = pantalla) {
        is PantallaRecompensa.RachaActivada -> ir(PantallaRecompensa.MisionCompletada(p.recompensaMision))
        is PantallaRecompensa.SubidaNivel -> siguienteSobre()
        is PantallaRecompensa.MisionCompletada, is PantallaRecompensa.Sobre, null -> ir(null)
    }

    private fun siguienteSobre(): EstadoRecompensas =
        if (sobresPendientes.isEmpty()) ir(null)
        else copy(sobresPendientes = sobresPendientes.drop(1)).ir(PantallaRecompensa.Sobre(sobresPendientes.first()))

    private fun ir(siguiente: PantallaRecompensa?) = copy(pantalla = siguiente, paso = paso + 1)

    companion object {
        fun iniciar(flujo: FlujoRecompensa): EstadoRecompensas = when (flujo) {
            is FlujoRecompensa.SobrePersonaje -> EstadoRecompensas(PantallaRecompensa.Sobre(flujo.rareza))
            is FlujoRecompensa.RachaActivada -> EstadoRecompensas(PantallaRecompensa.RachaActivada(flujo.diasDeRacha, flujo.recompensaMision))
            is FlujoRecompensa.MisionCompletada -> EstadoRecompensas(PantallaRecompensa.MisionCompletada(flujo.recompensaMision))
            is FlujoRecompensa.SubidaNivel -> EstadoRecompensas(PantallaRecompensa.SubidaNivel(flujo.resultado, flujo.luchador))
        }
    }
}

/** Un sobre no gasta puntos: se reporta 0. En iOS: `ResultadoRecoleccion.comoResultadoSubidaNivel()`. */
fun ResultadoRecoleccion.comoSubidaNivel() = ResultadoSubidaNivel(
    luchadorId = luchadorId,
    puntosPoderAnterior = puntosPoderAnterior,
    puntosPoderNuevo = puntosPoderNuevo,
    nivelAnterior = nivelAnterior,
    nivelNuevo = nivelNuevo,
    rarezaAnterior = rarezaAnterior,
    rarezaNueva = rarezaNueva,
    puntosGastados = 0,
)
