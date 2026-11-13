package com.rudoestudio.tiempofuera.domain.ligas

import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto

/** En iOS: `LigaRepositoryProtocol`. */
interface LigaRepository {
    /** Con sus luchadores ya resueltos (con el progreso del usuario). */
    suspend fun obtenerLigas(): List<Liga>

    suspend fun obtenerMisionesLiga(ligaId: Int): List<Int>
}

/**
 * Experiencia y niveles de cada liga. No guarda `progresoMisionesDiarias`: eso es de
 * [ProgresoMisionLigaRepository]. En iOS: `ProgresoLigaRepositoryProtocol`.
 */
interface ProgresoLigaRepository {
    suspend fun obtenerProgreso(ligaId: Int): ProgresoUsuarioLiga?

    suspend fun guardarProgreso(progreso: ProgresoUsuarioLiga)

    /** Deja exactamente las ligas que manda la nube: borra las que no vienen. */
    suspend fun sembrar(desde: List<ProgresoLigaRemoto>)

    /** Sin progreso local manda la liga 1 en ceros, como iOS. */
    suspend fun obtenerParaSincronizar(): List<ProgresoLigaRemoto>
}

/**
 * Avance de hoy en cada misión de liga (las que dan experiencia por vueltas). En iOS:
 * `ProgresoMisionLigaRepositoryProtocol`.
 */
interface ProgresoMisionLigaRepository {
    suspend fun obtenerProgresoMision(misionId: Int): ProgresoMisionDiaria?

    suspend fun obtenerTodosLosProgresos(): List<ProgresoMisionDiaria>

    suspend fun guardarProgresoMision(progreso: ProgresoMisionDiaria)

    suspend fun eliminarProgresoMision(misionId: Int)

    suspend fun eliminarTodoElProgreso()
}
