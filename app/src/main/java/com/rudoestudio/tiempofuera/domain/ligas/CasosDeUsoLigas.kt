package com.rudoestudio.tiempofuera.domain.ligas

import com.rudoestudio.tiempofuera.domain.luchadores.CalculadorDeNivel
import com.rudoestudio.tiempofuera.domain.luchadores.EstadoNivel
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoSubidaNivel
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import java.time.Clock
import javax.inject.Inject

// Casos de uso de Domain/UseCases/Ligas de iOS.

class ObtenerLigasUseCase @Inject constructor(
    private val repositorio: LigaRepository,
) {
    suspend operator fun invoke(): List<Liga> = repositorio.obtenerLigas()
}

class GuardarProgresoLigaUseCase @Inject constructor(
    private val repositorio: ProgresoLigaRepository,
) {
    suspend operator fun invoke(progreso: ProgresoUsuarioLiga) = repositorio.guardarProgreso(progreso)
}

/** El progreso de la liga junto con el avance de hoy en sus misiones. En iOS: `ObtenerProgresoLigaUseCase`. */
class ObtenerProgresoLigaUseCase @Inject constructor(
    private val ligas: LigaRepository,
    private val progresoLigas: ProgresoLigaRepository,
    private val progresoMisiones: ProgresoMisionLigaRepository,
) {
    suspend operator fun invoke(ligaId: Int): ProgresoUsuarioLiga {
        val progresoMisionesHoy = ligas.obtenerMisionesLiga(ligaId).mapNotNull { progresoMisiones.obtenerProgresoMision(it) }
        val base = progresoLigas.obtenerProgreso(ligaId) ?: ProgresoUsuarioLiga(ligaId)
        return base.copy(progresoMisionesDiarias = progresoMisionesHoy)
    }
}

/** Reglas del camino de la liga. En iOS: `EvaluadorProgresoLigaUseCase`. */
object EvaluadorProgresoLiga {
    fun calcularEstado(nivel: NivelLiga, progreso: ProgresoUsuarioLiga): EstadoNivel {
        val desbloqueado = nivel.id in progreso.nivelesDesbloqueados
        return when {
            nivel.id == progreso.nivelActualId -> when {
                nivel.tipo != TipoNivel.BARRERA -> EstadoNivel.ACTUAL
                desbloqueado -> EstadoNivel.BARRERA_DESBLOQUEADA
                else -> EstadoNivel.BARRERA_PENDIENTE
            }
            nivel.id < progreso.nivelActualId -> if (desbloqueado) EstadoNivel.RECOMPENSA_RECOLECTADA else EstadoNivel.RECOMPENSA_PENDIENTE
            else -> EstadoNivel.BLOQUEADO
        }
    }

    /**
     * Avanza mientras la experiencia alcance el siguiente nivel y no haya una barrera sin
     * vencer. Regresa el progreso nuevo, o `null` si no avanzó (en iOS: `inout` + `Bool`).
     */
    fun evaluarAvance(progreso: ProgresoUsuarioLiga, niveles: List<NivelLiga>): ProgresoUsuarioLiga? {
        val ordenados = niveles.sortedBy { it.id }
        var actualId = progreso.nivelActualId
        while (true) {
            val actual = ordenados.firstOrNull { it.id == actualId } ?: break
            val siguiente = ordenados.firstOrNull { it.id > actual.id } ?: break
            if (progreso.totalExp < siguiente.expRequerida) break
            if (actual.tipo == TipoNivel.BARRERA && actual.id !in progreso.nivelesDesbloqueados) break
            actualId = siguiente.id
        }
        return if (actualId != progreso.nivelActualId) progreso.copy(nivelActualId = actualId) else null
    }

    /** Los niveles hasta la siguiente barrera sin vencer (en iOS: `LigaNivelesViewModel.nivelesVisibles`). */
    fun nivelesVisibles(niveles: List<NivelLiga>, progreso: ProgresoUsuarioLiga): List<NivelLiga> {
        val ordenados = niveles.sortedBy { it.id }
        val barrera = ordenados.firstOrNull { it.tipo == TipoNivel.BARRERA && it.id !in progreso.nivelesDesbloqueados }
            ?: return ordenados
        return ordenados.filter { it.id <= barrera.id }
    }

    /** Experiencia del siguiente nivel; `null` si ya no hay más. */
    fun expSiguienteNivel(niveles: List<NivelLiga>, progreso: ProgresoUsuarioLiga): Double? =
        niveles.filter { it.id > progreso.nivelActualId }.minByOrNull { it.id }?.expRequerida
}

/**
 * Cobra la recompensa de un nivel: lo marca desbloqueado y da poder al luchador equipado
 * (que puede subir de nivel), monedas y protector. En iOS: `RecolectarRecompensaLigaUseCase`.
 */
class RecolectarRecompensaLigaUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val progresoLigas: ProgresoLigaRepository,
    private val luchadores: LuchadorRepository,
    private val calculador: CalculadorDeNivel,
    private val reloj: Clock,
) {
    /** Regresa la subida del luchador si el nivel daba poder. */
    suspend operator fun invoke(ligaId: Int, nivel: NivelLiga): ResultadoSubidaNivel? {
        val progreso = progresoLigas.obtenerProgreso(ligaId)
            ?: ProgresoUsuarioLiga(ligaId, nivelActualId = nivel.id + 1, nivelesDesbloqueados = setOf(nivel.id))
        progresoLigas.guardarProgreso(progreso.copy(nivelesDesbloqueados = progreso.nivelesDesbloqueados + nivel.id))

        val recompensa = nivel.recompensa
        val ahora = reloj.instant()
        var perfil = perfiles.obtenerPerfil()
        var subida: ResultadoSubidaNivel? = null

        if (recompensa.puntosPoder > 0) {
            val antes = perfil.luchadorSeleccionado
            luchadores.sumarPuntosPoder(antes.id, recompensa.puntosPoder)
            var luchador = antes.sumarPuntosPoder(recompensa.puntosPoder)
            val nivelPorPuntos = calculador.nivelActual(luchador.puntosPoder)
            if (nivelPorPuntos > antes.nivel) {
                luchadores.subirNivel(luchador.id, nivelPorPuntos)
                luchador = luchador.copy(nivel = nivelPorPuntos, rareza = calculador.rareza(nivelPorPuntos))
            }
            perfil = perfil.copy(luchadorSeleccionado = luchador)
            subida = ResultadoSubidaNivel(
                luchadorId = luchador.id,
                puntosPoderAnterior = antes.puntosPoder,
                puntosPoderNuevo = luchador.puntosPoder,
                nivelAnterior = antes.nivel,
                nivelNuevo = luchador.nivel,
                rarezaAnterior = antes.rareza,
                rarezaNueva = luchador.rareza,
                puntosGastados = 0,
            )
        }
        if (recompensa.monedas > 0) perfil = perfil.sumarMonedas(recompensa.monedas, ahora)
        if (recompensa.protectorRacha) perfil = perfil.sumarProtectorRacha(ahora = ahora)
        perfiles.guardarPerfil(perfil)
        return subida
    }
}
