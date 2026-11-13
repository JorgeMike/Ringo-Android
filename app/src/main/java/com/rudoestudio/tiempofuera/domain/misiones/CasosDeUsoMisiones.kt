package com.rudoestudio.tiempofuera.domain.misiones

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoLigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.perfil.ActivarRachaUseCase
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiario
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiarioRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// Casos de uso de Domain/UseCases/Misiones de iOS y `CompletarMisionUseCase`.
// `CompletarMisionLigaUseCase` no se porta: en iOS nadie lo usa (las misiones de liga
// pasan por `CompletarMisionUseCase`).

/**
 * Avisa cuando se completa una misión, para que Inicio y Ligas se refresquen. En iOS:
 * `MisionesEventPublisher` (M1.9).
 */
@Singleton
class MisionesEventos @Inject constructor() {
    private val _completadas = MutableSharedFlow<ResultadoCompletarMision>(extraBufferCapacity = 8)
    val completadas: SharedFlow<ResultadoCompletarMision> = _completadas

    fun publicar(resultado: ResultadoCompletarMision) {
        _completadas.tryEmit(resultado)
    }
}

class ObtenerMisionPorIdUseCase @Inject constructor(
    private val repositorio: MisionRepository,
) {
    suspend operator fun invoke(id: Int): Mision? = repositorio.obtenerPorId(id)
}

/**
 * Las misiones de hoy: las mismas todo el día (se guardan al sortearlas) con su estado
 * de completada. Son 5 variables más una de tracking si cabe. En iOS: `ObtenerMisionesUseCase`.
 */
class ObtenerMisionesUseCase @Inject constructor(
    private val misiones: MisionRepository,
    private val progresoDiario: ProgresoDiarioRepository,
    private val misionesDiarias: MisionesDiariasRepository,
    private val perfiles: UserProfileRepository,
    private val reloj: Clock,
    private val azar: Random,
) {
    suspend operator fun invoke(): List<Mision> {
        val catalogo = misiones.obtenerMisiones().filter { it.id >= 1000 }
        val hoy = LocalDate.now(reloj)
        val completadasHoy = progresoDiario.obtenerProgreso(hoy)?.misiones.orEmpty().toSet()

        misionesDiarias.obtenerMisionesAsignadas(hoy)?.let { asignadas ->
            // Como en iOS, salen en el orden del catálogo, no en el de la asignación.
            val guardadas = catalogo.filter { it.id in asignadas }
            if (guardadas.size == asignadas.size && MotorDeMisiones.cumpleLimiteDeDivision(guardadas)) {
                return guardadas.map { it.copy(completada = it.id in completadasHoy) }
            }
        }

        val frecuencia = MotorDeMisiones.calcularFrecuenciaPorDivision(progresoDiario.obtenerHistorico(), catalogo)
        val pesos = MotorDeMisiones.calcularPesos(perfiles.obtenerPerfil(), frecuencia)
        val disponibles = catalogo.filter { it.id !in completadasHoy }
        val seleccionadas = MotorDeMisiones.seleccionarMisionesDiarias(
            pool = disponibles.filter { it.tipoMision != TipoMision.TRACKING },
            pesos = pesos,
            cantidad = CANTIDAD_MISIONES_VARIABLES,
            excluir = completadasHoy,
            azar = azar,
        ).toMutableList()
        val ids = seleccionadas.map { it.id }.toSet()
        disponibles.firstOrNull {
            it.tipoMision == TipoMision.TRACKING && it.id !in ids && MotorDeMisiones.puedeAgregar(it, seleccionadas)
        }?.let { seleccionadas += it }

        misionesDiarias.guardarMisionesAsignadas(seleccionadas.map { it.id }, hoy)
        return seleccionadas.map { it.copy(completada = false) }
    }

    companion object {
        const val CANTIDAD_MISIONES_VARIABLES = 5
    }
}

/**
 * Registra la misión de hoy, activa la racha y entrega la recompensa: la experiencia va a
 * la liga si la misión es de liga; si no, poder y monedas van al perfil y la misión se
 * sustituye por otra de una división distinta. En iOS: `CompletarMisionUseCase`.
 */
class CompletarMisionUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val progresoDiario: ProgresoDiarioRepository,
    private val progresoLigas: ProgresoLigaRepository,
    private val misiones: MisionRepository,
    private val misionesDiarias: MisionesDiariasRepository,
    private val activarRacha: ActivarRachaUseCase,
    private val eventos: MisionesEventos,
    private val reloj: Clock,
    private val azar: Random,
) {
    /** [unidades]: vueltas completadas en misiones que se repiten (1 en las de una sola vez). */
    suspend operator fun invoke(mision: Mision, unidades: Int): ResultadoCompletarMision {
        val hoy = LocalDate.now(reloj)
        var progresoHoy = progresoDiario.obtenerProgreso(hoy) ?: ProgresoDiario(hoy, emptyList())
        if (mision.tipoMision != TipoMision.TRACKING && !progresoHoy.estaCompletada(mision.id)) {
            progresoHoy = progresoHoy.copy(misiones = progresoHoy.misiones + mision.id)
            progresoDiario.guardarProgreso(progresoHoy)
        }

        val racha = activarRacha()
        val recompensa = mision.recompensa.multiplicada(unidades)

        // De liga: la experiencia va a la liga y la misión no se sustituye.
        mision.ligaId?.let { ligaId ->
            var progresoLiga = progresoLigas.obtenerProgreso(ligaId) ?: ProgresoUsuarioLiga(ligaId)
            val exp = recompensa.exp
            if (recompensa.tieneExp && exp != null) {
                progresoLiga = progresoLiga.copy(totalExp = progresoLiga.totalExp + exp)
                progresoLigas.guardarProgreso(progresoLiga)
            }
            return ResultadoCompletarMision(mision, null, racha, recompensa, progresoLiga).also(eventos::publicar)
        }

        val perfil = perfiles.obtenerPerfil().agregarRecompensa(recompensa, reloj.instant())
        perfiles.guardarPerfil(perfil)

        val reemplazo = if (mision.tipoMision == TipoMision.TRACKING) null else elegirReemplazo(mision, perfil, progresoHoy, hoy)
        if (reemplazo != null) {
            misionesDiarias.sustituirMision(mision.id, reemplazo.id, hoy)
        } else if (mision.tipoMision != TipoMision.TRACKING) {
            AppLogger.warning("No se encontró ninguna misión válida para sustituir la ${mision.id}")
        }
        return ResultadoCompletarMision(mision, reemplazo, racha, recompensa).also(eventos::publicar)
    }

    private suspend fun elegirReemplazo(
        mision: Mision,
        perfil: UserProfile,
        progresoHoy: ProgresoDiario,
        hoy: LocalDate,
    ): Mision? {
        val catalogo = misiones.obtenerMisionesDiarias()
        val enPantalla = misionesDiarias.obtenerMisionesAsignadas(hoy).orEmpty().toSet()
        val excluidas = progresoHoy.misiones.toSet() + enPantalla + mision.id
        val restantes = catalogo.filter { it.id in enPantalla && it.id != mision.id }

        val frecuencia = MotorDeMisiones.calcularFrecuenciaPorDivision(progresoDiario.obtenerHistorico(), catalogo)
        val pesos = MotorDeMisiones.calcularPesos(perfil, frecuencia)
        // Evita repetir la división que se acaba de completar.
        val division = MotorDeMisiones.sortearDivisiones(pesos - setOfNotNull(mision.division), 1, azar).firstOrNull()
            ?: MotorDeMisiones.sortearDivisiones(pesos, 1, azar).firstOrNull()
        val candidatas = catalogo.filter { it.id !in excluidas && MotorDeMisiones.puedeAgregar(it, restantes) }
        val respaldo = { candidatas.filter { it.tipoMision != TipoMision.TRACKING }.shuffled(azar).firstOrNull() }
        return division?.let { MotorDeMisiones.seleccionarMision(it, candidatas, excluidas, azar = azar) } ?: respaldo()
    }
}
