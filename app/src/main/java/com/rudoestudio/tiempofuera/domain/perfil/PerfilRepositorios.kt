package com.rudoestudio.tiempofuera.domain.perfil

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * El perfil de juego del usuario (uno por teléfono). En iOS:
 * `UserProfileRepositoryProtocol`. El paso del onboarding y si ya terminó no viven aquí
 * sino en `EstadoOnboardingRepository`, para no guardarlo en dos lados.
 */
interface UserProfileRepository {
    /**
     * Emite el perfil cada vez que cambia, también si sube de nivel su luchador. Sustituye
     * a `UserProfileEventPublisher` de iOS (M1.9).
     */
    val perfil: Flow<UserProfile>

    /** Si no existe, lo crea vacío con Ringo de luchador. */
    suspend fun obtenerPerfil(): UserProfile

    suspend fun guardarPerfil(perfil: UserProfile)
}

/** Misiones completadas por día. En iOS: `ProgresoDiarioRepositoryProtocol`. */
interface ProgresoDiarioRepository {
    suspend fun obtenerProgreso(fecha: LocalDate): ProgresoDiario?

    /** Del día más viejo al más nuevo. */
    suspend fun obtenerHistorico(): List<ProgresoDiario>

    suspend fun guardarProgreso(progreso: ProgresoDiario)
}

/** Días con racha o protector. En iOS: `HistoricoRachaRepositoryProtocol`. */
interface HistoricoRachaRepository {
    /** `null`: ese día no hubo racha ni protector. */
    suspend fun obtenerRegistro(fecha: LocalDate): HistoricoRacha?

    suspend fun guardarRegistro(registro: HistoricoRacha)

    /** Del día más viejo al más nuevo. */
    suspend fun obtenerTodosLosRegistros(): List<HistoricoRacha>
}

/** Las 3 mejores marcas de carrera y de lagartijas. En iOS: `EstadisticasEjercicioRepositoryProtocol`. */
interface EstadisticasEjercicioRepository {
    suspend fun registrarCarrera(registro: RegistroCarrera)

    suspend fun registrarLagartijas(registro: RegistroLagartijas)

    /** Más distancia primero; empate: más velocidad, luego la más reciente. */
    suspend fun obtenerTopCarreras(): List<RegistroCarrera>

    /** Más lagartijas primero; empate: la más reciente. */
    suspend fun obtenerTopLagartijas(): List<RegistroLagartijas>

    companion object {
        const val TOP = 3
    }
}
