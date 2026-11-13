package com.rudoestudio.tiempofuera.data.misiones

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_DIARIAS
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_LIGA
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.MisionesAsignadasGuardadas
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.MisionRepository
import com.rudoestudio.tiempofuera.domain.misiones.MisionesDiariasRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** El catálogo vive en código. En iOS: `InMemoryMisionRepository` (SwiftData). */
@Singleton
class MisionRepositoryCatalogo @Inject constructor() : MisionRepository {
    private val todas = (CATALOGO_MISIONES_LIGA + CATALOGO_MISIONES_DIARIAS).sortedBy { it.id }
    private val porId = todas.associateBy { it.id }

    override suspend fun obtenerMisiones(): List<Mision> = todas

    override suspend fun obtenerPorId(id: Int): Mision? = porId[id]

    override suspend fun obtenerMisionesDiarias(): List<Mision> = todas.filter { it.id >= PRIMER_ID_DIARIA }

    override suspend fun misionesPorPerfil(categoria: String): List<Mision> = todas.filter { it.categoria == categoria }

    override suspend fun obtenerMisionesPendientes(idsCompletados: Set<Int>): List<Mision> = todas.filter { it.id !in idsCompletados }

    companion object {
        const val PRIMER_ID_DIARIA = 1000
    }
}

/** En iOS: `InUserDefaultsMisionesDiariasRepository`. */
@Singleton
class MisionesDiariasDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : MisionesDiariasRepository {

    override suspend fun obtenerMisionesAsignadas(fecha: LocalDate): List<Int>? =
        almacen.leer(CLAVE, MisionesAsignadasGuardadas.serializer())?.takeIf { it.dia == fecha }?.misionesIds

    override suspend fun sustituirMision(idMisionCompletada: Int, idMisionNueva: Int, fecha: LocalDate): List<Int>? {
        val actuales = obtenerMisionesAsignadas(fecha) ?: return null
        val indice = actuales.indexOf(idMisionCompletada).takeIf { it >= 0 } ?: return null
        // Mismo lugar exacto: la tarjeta nueva aparece donde estaba la completada.
        val nuevas = actuales.toMutableList().apply { this[indice] = idMisionNueva }
        guardarMisionesAsignadas(nuevas, fecha)
        return nuevas
    }

    override suspend fun guardarMisionesAsignadas(ids: List<Int>, fecha: LocalDate) {
        almacen.escribir(CLAVE, MisionesAsignadasGuardadas.serializer(), MisionesAsignadasGuardadas(fecha.toString(), ids))
    }

    private companion object {
        const val CLAVE = "misionesDiarias.seed"
    }
}
