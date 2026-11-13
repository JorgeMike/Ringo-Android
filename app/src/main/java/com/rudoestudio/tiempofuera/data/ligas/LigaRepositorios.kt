package com.rudoestudio.tiempofuera.data.ligas

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LIGAS
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.ProgresoLigaDao
import com.rudoestudio.tiempofuera.data.local.ProgresoLigaEntity
import com.rudoestudio.tiempofuera.data.local.ProgresoMisionGuardado
import com.rudoestudio.tiempofuera.domain.ligas.Liga
import com.rudoestudio.tiempofuera.domain.ligas.LigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoLigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoMisionDiaria
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoMisionLigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.misiones.MisionRepository
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

/** Las ligas del catálogo con sus luchadores al día. En iOS: `InMemoryLigaRepository`. */
@Singleton
class LigaRepositoryCatalogo @Inject constructor(
    private val luchadores: LuchadorRepository,
    private val misiones: MisionRepository,
) : LigaRepository {

    override suspend fun obtenerLigas(): List<Liga> {
        val luchadoresPorId = luchadores.obtenerTodos().associateBy { it.id }
        val misionesPorId = misiones.obtenerMisiones().associateBy { it.id }
        return CATALOGO_LIGAS.sortedBy { it.id }.mapNotNull { liga ->
            // Como en iOS: una liga cuya estrella no existe no se muestra.
            val estrella = luchadoresPorId[liga.luchadorEstrellaId] ?: return@mapNotNull null
            Liga(
                id = liga.id,
                nombre = liga.nombre,
                luchadorEstrella = estrella,
                misiones = liga.misionIds.mapNotNull(misionesPorId::get),
                luchadoresLiga = liga.luchadoresLigaIds.mapNotNull(luchadoresPorId::get),
            )
        }
    }

    override suspend fun obtenerMisionesLiga(ligaId: Int): List<Int> =
        CATALOGO_LIGAS.firstOrNull { it.id == ligaId }?.misionIds.orEmpty()
}

/** En iOS: `InMemoryProgresoLigaRepository` (SwiftData). */
@Singleton
class ProgresoLigaRepositoryRoom @Inject constructor(
    private val dao: ProgresoLigaDao,
) : ProgresoLigaRepository {

    override suspend fun obtenerProgreso(ligaId: Int): ProgresoUsuarioLiga? = dao.porLiga(ligaId)?.aDominio()

    override suspend fun guardarProgreso(progreso: ProgresoUsuarioLiga) {
        dao.guardar(ProgresoLigaEntity(progreso.ligaId, progreso.totalExp, progreso.nivelActualId, progreso.nivelesDesbloqueados))
    }

    override suspend fun sembrar(desde: List<ProgresoLigaRemoto>) {
        dao.borrarExcepto(desde.map { it.ligaId })
        dao.guardarTodos(desde.map { it.aEntidad() })
    }

    override suspend fun obtenerParaSincronizar(): List<ProgresoLigaRemoto> =
        dao.todos().map { ProgresoLigaRemoto(it.ligaId, it.totalExp, it.nivelActualId, it.nivelesDesbloqueados) }
            .ifEmpty { listOf(ProgresoLigaRemoto(ligaId = 1, totalExp = 0.0, nivelActualId = 1, nivelesDesbloqueados = setOf(0))) }
}

internal fun ProgresoLigaRemoto.aEntidad() = ProgresoLigaEntity(ligaId, totalExp, nivelActualId, nivelesDesbloqueados)

private fun ProgresoLigaEntity.aDominio() = ProgresoUsuarioLiga(ligaId, totalExp, nivelActualId, nivelesDesbloqueados)

/** En iOS: `InUserDefaultsProgresoMisionLigaRepository`. */
@Singleton
class ProgresoMisionLigaDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : ProgresoMisionLigaRepository {
    private val serializador = MapSerializer(String.serializer(), ProgresoMisionGuardado.serializer())

    override suspend fun obtenerProgresoMision(misionId: Int): ProgresoMisionDiaria? = leer()[misionId.toString()]?.aDominio()

    override suspend fun obtenerTodosLosProgresos(): List<ProgresoMisionDiaria> = leer().values.map { it.aDominio() }

    override suspend fun guardarProgresoMision(progreso: ProgresoMisionDiaria) {
        almacen.actualizar(CLAVE, serializador) { it.orEmpty() + (progreso.misionId.toString() to ProgresoMisionGuardado.de(progreso)) }
    }

    override suspend fun eliminarProgresoMision(misionId: Int) {
        almacen.actualizar(CLAVE, serializador) { it.orEmpty() - misionId.toString() }
    }

    override suspend fun eliminarTodoElProgreso() {
        almacen.borrar(CLAVE)
    }

    private suspend fun leer(): Map<String, ProgresoMisionGuardado> = almacen.leer(CLAVE, serializador).orEmpty()

    private companion object {
        const val CLAVE = "progresoMisiones.ligas"
    }
}
