package com.rudoestudio.tiempofuera.domain.recompensas

import com.rudoestudio.tiempofuera.domain.ligas.RecompensaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoRecoleccion
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoSubidaNivel
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision

/**
 * Lo que dispara una celebración. En iOS: `RecompensaFlowType`. La lucha ganada no se pide
 * sola: sale dentro de [NivelLiga].
 */
sealed interface FlujoRecompensa {
    /** Abrir un sobre (tienda, onboarding). */
    data class SobrePersonaje(val rareza: Rareza) : FlujoRecompensa

    /** La piñata de la tienda: se rompe y luego se abren sus sobres, de menor a mayor rareza. */
    data class Pinata(val rarezas: List<Rareza>) : FlujoRecompensa

    /** Primera misión del día: racha y luego la misión completada. */
    data class RachaActivada(val diasDeRacha: Int, val recompensaMision: RecompensaMision) : FlujoRecompensa

    data class MisionCompletada(val recompensaMision: RecompensaMision) : FlujoRecompensa

    /** Subida de nivel comprada con puntos de poder (colección de luchadores). */
    data class SubidaNivel(val resultado: ResultadoSubidaNivel, val luchador: Luchador) : FlujoRecompensa

    /**
     * Se cobró un nivel de liga: si fue una barrera, primero "¡Lucha ganada!" ([lucha]);
     * luego la subida del luchador (si subió con el poder del nivel) y al final sus sobres,
     * con piñata si son varios. En iOS: `.luchaGanada`, `.subidaNivel(..., recompensaPendiente:)`
     * y `RecompensaFlowType.paraRecompensa`.
     */
    data class NivelLiga(
        val sobres: List<Rareza>,
        val subida: ResultadoSubidaNivel?,
        val luchador: Luchador,
        val lucha: PantallaRecompensa.LuchaGanada? = null,
    ) : FlujoRecompensa

    companion object {
        /** Al reclamar una misión. En iOS: `MainTabView.onChange(of: misionActiva)`. */
        fun alCompletarMision(rachaActivada: Boolean, diasDeRacha: Int, recompensa: RecompensaMision): FlujoRecompensa =
            if (rachaActivada) RachaActivada(diasDeRacha, recompensa) else MisionCompletada(recompensa)

        /**
         * Al cobrar un nivel; `null` si no hay nada que festejar. Con un solo sobre su rareza
         * se sortea por nivel, como iOS (`Rareza.calcularRareza(paraNivel:)`).
         */
        fun alCobrarNivel(
            sobres: List<Rareza>,
            nivelId: Int,
            subida: ResultadoSubidaNivel?,
            luchador: Luchador,
            lucha: PantallaRecompensa.LuchaGanada? = null,
        ): FlujoRecompensa? {
            val rarezas = if (sobres.size == 1) listOf(Rareza.calcularRareza(nivelId)) else sobres.sorted()
            val subio = subida?.takeIf { it.subioDeNivel }
            return if (lucha == null && subio == null && rarezas.isEmpty()) null else NivelLiga(rarezas, subio, luchador, lucha)
        }
    }
}

/** Cada pantalla del flujo. En iOS: las rutas de `RecompensaFlowCoordinator`. */
sealed interface PantallaRecompensa {
    data class Sobre(val rareza: Rareza) : PantallaRecompensa

    data class RachaActivada(val diasDeRacha: Int, val recompensaMision: RecompensaMision) : PantallaRecompensa

    data class MisionCompletada(val recompensaMision: RecompensaMision) : PantallaRecompensa

    /** Un sobre trajo un luchador repetido y subió de nivel (o de rareza). */
    data class SubidaNivel(val resultado: ResultadoSubidaNivel, val luchador: Luchador?) : PantallaRecompensa

    /** Varios sobres: se rompe la piñata y luego se abren uno por uno. En iOS: `PinataView` (M7.3). */
    data class Pinata(val rarezas: List<Rareza>) : PantallaRecompensa

    /** Se ganó la pelea de una barrera. En iOS: `LuchaGanadaView` (M7.6). */
    data class LuchaGanada(val recompensa: RecompensaNivel, val luchador: Luchador, val rival: Luchador?) : PantallaRecompensa
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
    /** Lo que va antes de abrir los sobres (subida de nivel, piñata). */
    val antesDeSobres: List<PantallaRecompensa> = emptyList(),
    val paso: Int = 0,
) {
    val terminado: Boolean get() = pantalla == null

    /** El sobre no se pudo abrir: se sigue como si no hubiera sido repetido. */
    fun sobreConError(): EstadoRecompensas = siguienteSobre()

    /** Se recolectó el luchador; si era repetido, primero se festeja la subida de nivel. */
    fun sobreRecolectado(luchador: Luchador, resultado: ResultadoRecoleccion): EstadoRecompensas =
        if (!resultado.esDuplicado) siguienteSobre() else ir(PantallaRecompensa.SubidaNivel(resultado.comoSubidaNivel(), luchador))

    /** "Continuar" en racha, misión completada, subida de nivel o lucha ganada; o la piñata ya rota. */
    fun continuar(): EstadoRecompensas = when (val p = pantalla) {
        is PantallaRecompensa.RachaActivada -> ir(PantallaRecompensa.MisionCompletada(p.recompensaMision))
        is PantallaRecompensa.SubidaNivel, is PantallaRecompensa.LuchaGanada, is PantallaRecompensa.Pinata -> siguienteSobre()
        is PantallaRecompensa.MisionCompletada, is PantallaRecompensa.Sobre, null -> ir(null)
    }

    private fun siguienteSobre(): EstadoRecompensas = when {
        antesDeSobres.isNotEmpty() -> copy(antesDeSobres = antesDeSobres.drop(1)).ir(antesDeSobres.first())
        sobresPendientes.isEmpty() -> ir(null)
        else -> copy(sobresPendientes = sobresPendientes.drop(1)).ir(PantallaRecompensa.Sobre(sobresPendientes.first()))
    }

    private fun ir(siguiente: PantallaRecompensa?) = copy(pantalla = siguiente, paso = paso + 1)

    companion object {
        fun iniciar(flujo: FlujoRecompensa): EstadoRecompensas = when (flujo) {
            is FlujoRecompensa.SobrePersonaje -> EstadoRecompensas(PantallaRecompensa.Sobre(flujo.rareza))
            is FlujoRecompensa.Pinata -> flujo.rarezas.sorted().let { EstadoRecompensas(PantallaRecompensa.Pinata(it), sobresPendientes = it) }
            is FlujoRecompensa.RachaActivada -> EstadoRecompensas(PantallaRecompensa.RachaActivada(flujo.diasDeRacha, flujo.recompensaMision))
            is FlujoRecompensa.MisionCompletada -> EstadoRecompensas(PantallaRecompensa.MisionCompletada(flujo.recompensaMision))
            is FlujoRecompensa.SubidaNivel -> EstadoRecompensas(PantallaRecompensa.SubidaNivel(flujo.resultado, flujo.luchador))
            // Todo lo del nivel va en fila: lucha, subida, piñata y los sobres.
            is FlujoRecompensa.NivelLiga -> EstadoRecompensas(
                pantalla = null,
                sobresPendientes = flujo.sobres,
                antesDeSobres = listOfNotNull(
                    flujo.lucha,
                    flujo.subida?.let { PantallaRecompensa.SubidaNivel(it, flujo.luchador) },
                    PantallaRecompensa.Pinata(flujo.sobres).takeIf { flujo.sobres.size > 1 },
                ),
            ).siguienteSobre().copy(paso = 0)
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
