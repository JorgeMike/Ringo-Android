package com.rudoestudio.tiempofuera.domain.luchadores

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.servicios.AlcanceApp
import com.rudoestudio.tiempofuera.domain.sincronizacion.UserApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import kotlin.random.Random

// Casos de uso de Domain/UseCases/Luchadores de iOS.

class SinLuchadoresDisponibles : Exception("No hay luchadores disponibles para esta recompensa. Intenta de nuevo.")

/**
 * Qué luchador sale de un sobre. El primer sobre (el del onboarding) siempre da a Ringo.
 * Los legendarios solo salen de sobres legendarios. En iOS: `SorteoLuchadorUseCase`.
 */
class SorteoLuchadorUseCase @Inject constructor(
    private val luchadores: LuchadorRepository,
    private val azar: Random,
) {
    suspend operator fun invoke(rareza: Rareza): Luchador {
        val todos = luchadores.obtenerTodos()
        todos.firstOrNull { it.id == ID_RINGO && it.cantidad == 0 }?.let { return it }

        val pool = if (rareza == Rareza.LEGENDARIO) todos else todos.filter { it.rareza != Rareza.LEGENDARIO }
        // Sin luchadores de esa rareza, cualquiera del pool.
        return pool.filter { it.rareza == rareza }.randomOrNull(azar)
            ?: pool.randomOrNull(azar)
            ?: throw SinLuchadoresDisponibles()
    }

    private companion object {
        const val ID_RINGO = 0
    }
}

/**
 * Guarda el luchador que salió de un sobre. La primera vez solo se desbloquea, con los
 * puntos de su nivel; repetido, suma los puntos de [IncrementoNivelDuplicado] y puede
 * subir de nivel (nunca bajar). Después lo manda al API sin esperar: si la red falla, la
 * recolección cuenta igual. En iOS: `RecolectarLuchadorUseCase`.
 */
class RecolectarLuchadorUseCase @Inject constructor(
    private val luchadores: LuchadorRepository,
    private val api: UserApiRepository,
    private val calculador: CalculadorDeNivel,
    @AlcanceApp private val alcance: CoroutineScope,
) {
    suspend operator fun invoke(luchadorId: Int): ResultadoRecoleccion =
        recolectarLocalmente(luchadorId).also { sincronizarConApi(luchadorId) }

    private suspend fun recolectarLocalmente(luchadorId: Int): ResultadoRecoleccion {
        val actual = luchadores.obtenerPorId(luchadorId) ?: throw ErrorSubidaNivel.LuchadorNoEncontrado
        val puntosBase = calculador.puntosRequeridos(actual.nivel)

        if (actual.cantidad == 0) {
            luchadores.recolectarLuchador(luchadorId, actual.nivel)
            // Arranca con los puntos que corresponden a su nivel.
            luchadores.sumarPuntosPoder(luchadorId, puntosBase)
            return ResultadoRecoleccion(
                luchadorId = luchadorId, nombre = actual.nombre, esDuplicado = false,
                puntosPoderAnterior = puntosBase, puntosPoderNuevo = puntosBase,
                nivelAnterior = actual.nivel, nivelNuevo = actual.nivel,
                rarezaAnterior = actual.rareza, rarezaNueva = actual.rareza,
                cantidadNueva = actual.cantidad + 1,
            )
        }

        val puntosASumar = calculador.puntosRequeridos(IncrementoNivelDuplicado.incremento(actual.rareza))
        luchadores.sumarPuntosPoder(luchadorId, puntosASumar)
        val puntosNuevos = actual.puntosPoder + puntosASumar
        val nivelNuevo = maxOf(actual.nivel, calculador.nivelActual(puntosNuevos))
        val rarezaNueva = calculador.rareza(nivelNuevo)
        luchadores.recolectarLuchador(luchadorId, nivelNuevo)
        if (rarezaNueva != actual.rareza) luchadores.subirNivel(luchadorId, nivelNuevo)
        return ResultadoRecoleccion(
            luchadorId = luchadorId, nombre = actual.nombre, esDuplicado = true,
            // Como en iOS: lo "anterior" son los puntos base del nivel, no los acumulados.
            puntosPoderAnterior = puntosBase, puntosPoderNuevo = puntosNuevos,
            nivelAnterior = actual.nivel, nivelNuevo = nivelNuevo,
            rarezaAnterior = actual.rareza, rarezaNueva = rarezaNueva,
            cantidadNueva = actual.cantidad + 1,
        )
    }

    /** Manda el estado ya guardado. Pendiente: reintentar sin conexión (lo resuelve la sincronización). */
    private fun sincronizarConApi(luchadorId: Int) {
        alcance.launch {
            try {
                luchadores.obtenerPorId(luchadorId)?.let { api.actualizarLuchador(it) }
            } catch (e: Exception) {
                AppLogger.error(e, "RecolectarLuchadorUseCase")
            }
        }
    }
}

/**
 * Sube un nivel gastando energía. Los puntos gastados se le suman al luchador (son su
 * poder acumulado). En iOS: `SubirDeNivelLuchadorUseCase`.
 */
class SubirDeNivelLuchadorUseCase @Inject constructor(
    private val luchadores: LuchadorRepository,
    private val calculador: CalculadorDeNivel,
) {
    suspend operator fun invoke(luchador: Luchador, puntosPoderDisponibles: Int): ResultadoSubidaNivel {
        val nivelObjetivo = luchador.nivel + 1
        val costo = calculador.costoParaSubirA(nivelObjetivo)
        if (costo > puntosPoderDisponibles) throw ErrorSubidaNivel.PuntosInsuficientes
        luchadores.sumarPuntosPoder(luchador.id, costo)
        val rarezaNueva = calculador.rareza(nivelObjetivo)
        luchadores.subirNivel(luchador.id, nivelObjetivo)
        return ResultadoSubidaNivel(
            luchadorId = luchador.id,
            puntosPoderAnterior = luchador.puntosPoder,
            puntosPoderNuevo = luchador.puntosPoder + costo,
            nivelAnterior = luchador.nivel,
            nivelNuevo = nivelObjetivo,
            rarezaAnterior = luchador.rareza,
            rarezaNueva = rarezaNueva,
            puntosGastados = costo,
        )
    }
}

/**
 * Sube un nivel al luchador con los puntos de poder del perfil y se los descuenta. En iOS
 * esto lo hace `LuchadoresViewModel.subirNivel` (el caso de uso más `gastarPuntosPoder`).
 */
class ComprarSubidaNivelUseCase @Inject constructor(
    private val subirDeNivel: SubirDeNivelLuchadorUseCase,
    private val perfiles: UserProfileRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(luchador: Luchador): ResultadoSubidaNivel {
        val perfil = perfiles.obtenerPerfil()
        val resultado = subirDeNivel(luchador, perfil.puntosPoder)
        perfiles.guardarPerfil(perfil.gastarPuntosPoder(resultado.puntosGastados, reloj.instant()))
        return resultado
    }
}

/** Cambia el luchador del perfil; solo los que ya tiene. En iOS: `LuchadoresViewModel.seleccionarLuchador`. */
class EquiparLuchadorUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
) {
    suspend operator fun invoke(luchador: Luchador) {
        if (luchador.cantidad <= 0) return
        val perfil = perfiles.obtenerPerfil()
        if (perfil.luchadorSeleccionado.id == luchador.id) return
        perfiles.guardarPerfil(perfil.copy(luchadorSeleccionado = luchador))
    }
}
