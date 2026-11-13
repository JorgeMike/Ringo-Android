package com.rudoestudio.tiempofuera.domain.ligas

import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import java.time.Instant
import kotlin.random.Random

// En iOS: Domain/Entities/Ligas y Domain/Factories/NivelLigaFactory.

/** En iOS dos ligas son iguales si tienen el mismo id. */
class Liga(
    val id: Int,
    val nombre: String,
    val luchadorEstrella: Luchador,
    val misiones: List<Mision>,
    val luchadoresLiga: List<Luchador>,
) {
    /** Los 100 niveles; los sobres de algunos niveles se sortean (ver [NivelLigaFactory]). */
    fun niveles(azar: Random = Random.Default): List<NivelLiga> = NivelLigaFactory.generar(this, azar)

    override fun equals(other: Any?): Boolean = other is Liga && other.id == id

    override fun hashCode(): Int = id

    override fun toString(): String = "Liga($id, $nombre)"
}

enum class TipoNivel { NORMAL, BARRERA }

data class RecompensaNivel(
    val puntosPoder: Int = 0,
    val monedas: Int = 0,
    val sobres: List<Rareza>,
    val protectorRacha: Boolean = false,
)

data class NivelLiga(
    val id: Int,
    val expRequerida: Double,
    val tipo: TipoNivel,
    val luchadorBarrera: Luchador?,
    val recompensa: RecompensaNivel,
    val recompensaRecolectada: Boolean,
) {
    /** Tipo del rival en cada barrera: 10 y 75 tijera, 25 papel, 50 piedra, 100 versátil. */
    val tipoLuchadorBarrera: TipoLuchador?
        get() = if (tipo != TipoNivel.BARRERA) null else when (id) {
            10, 75 -> TipoLuchador.TIJERA
            25 -> TipoLuchador.PAPEL
            50 -> TipoLuchador.PIEDRA
            else -> TipoLuchador.VERSATIL
        }
}

data class ProgresoUsuarioLiga(
    val ligaId: Int,
    val totalExp: Double = 0.0,
    val nivelActualId: Int = 1,
    val nivelesDesbloqueados: Set<Int> = emptySet(),
    val progresoMisionesDiarias: List<ProgresoMisionDiaria> = emptyList(),
)

data class ProgresoMisionDiaria(
    val misionId: Int,
    val fecha: Instant,
    val rawProgreso: Double,
    val expObtenidaHoy: Double,
)

object NivelLigaFactory {
    const val TOTAL_NIVELES = 100
    const val NIVELES_POR_BARRERA = 25

    /** Barreras: niveles 10, 25, 50, 75 y 100. */
    fun esNivelBarrera(nivelId: Int): Boolean = nivelId == 10 || nivelId % NIVELES_POR_BARRERA == 0

    fun generar(liga: Liga, azar: Random = Random.Default): List<NivelLiga> = (1..TOTAL_NIVELES).map { id ->
        val barrera = esNivelBarrera(id)
        NivelLiga(
            id = id,
            expRequerida = expRequerida(id),
            tipo = if (barrera) TipoNivel.BARRERA else TipoNivel.NORMAL,
            // El luchador estrella de la liga aparece en todas las barreras.
            luchadorBarrera = if (barrera) liga.luchadorEstrella else null,
            recompensa = recompensa(id, barrera, azar),
            recompensaRecolectada = false,
        )
    }

    /** Curva ligera: 0.28·n² + 3.2·n con n = nivel − 1. */
    fun expRequerida(nivelId: Int): Double {
        if (nivelId <= 1) return 0.0
        val n = (nivelId - 1).toDouble()
        return Math.round(0.28 * n * n + 3.2 * n).toDouble()
    }

    fun recompensa(nivelId: Int, esBarrera: Boolean, azar: Random = Random.Default): RecompensaNivel {
        val sobres = when (nivelId) {
            10 -> listOf(Rareza.COMUN)
            25 -> listOf(Rareza.RARO, Rareza.COMUN, Rareza.COMUN)
            50, 75 -> listOf(Rareza.EPICO, Rareza.RARO, Rareza.COMUN)
            100 -> listOf(Rareza.LEGENDARIO, Rareza.EPICO, Rareza.EPICO)
            else -> if (nivelId % 5 == 0) listOf(Rareza.calcularRareza(nivelId, azar)) else emptyList()
        }
        if (esBarrera) {
            val multiplicador = if (nivelId == 10) 1 else nivelId / NIVELES_POR_BARRERA + 1
            return RecompensaNivel(
                puntosPoder = 80 + multiplicador * 30,
                monedas = 150 * multiplicador,
                sobres = sobres,
                protectorRacha = true,
            )
        }
        return RecompensaNivel(
            puntosPoder = 10 + nivelId / 5,
            monedas = 15 + nivelId / 2,
            sobres = sobres,
            protectorRacha = nivelId % 10 == 0,
        )
    }
}
