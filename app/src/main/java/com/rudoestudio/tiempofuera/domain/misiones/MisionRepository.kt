package com.rudoestudio.tiempofuera.domain.misiones

import java.time.LocalDate

/** El catálogo de misiones (diarias y de liga). En iOS: `MisionRepositoryProtocol`. */
interface MisionRepository {
    suspend fun obtenerMisiones(): List<Mision>

    suspend fun obtenerPorId(id: Int): Mision?

    /** Las que reparte el [MotorDeMisiones] (ids ≥ 1000). */
    suspend fun obtenerMisionesDiarias(): List<Mision>

    /** Por personalidad, con la clave de `TipoPersonalidad` ("Analíticos"…). */
    suspend fun misionesPorPerfil(categoria: String): List<Mision>

    suspend fun obtenerMisionesPendientes(idsCompletados: Set<Int>): List<Mision>
}

/**
 * Las misiones que le tocaron al usuario hoy, para que no cambien al reabrir la app.
 * Solo guarda un día: pedir otra fecha regresa `null`. En iOS:
 * `MisionesDiariasRepositoryProtocol`.
 */
interface MisionesDiariasRepository {
    suspend fun obtenerMisionesAsignadas(fecha: LocalDate): List<Int>?

    /** Pone [idMisionNueva] en el lugar de [idMisionCompletada]; `null` si no estaba asignada. */
    suspend fun sustituirMision(idMisionCompletada: Int, idMisionNueva: Int, fecha: LocalDate): List<Int>?

    suspend fun guardarMisionesAsignadas(ids: List<Int>, fecha: LocalDate)
}
