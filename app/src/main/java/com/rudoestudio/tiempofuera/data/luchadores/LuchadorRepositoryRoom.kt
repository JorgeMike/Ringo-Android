package com.rudoestudio.tiempofuera.data.luchadores

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.data.local.LuchadorDao
import com.rudoestudio.tiempofuera.data.local.LuchadorProgresoEntity
import com.rudoestudio.tiempofuera.domain.luchadores.ErrorSubidaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto
import javax.inject.Inject
import javax.inject.Singleton

/** En iOS: `InMemoryLuchadorRepository` (SwiftData). */
@Singleton
class LuchadorRepositoryRoom @Inject constructor(
    private val dao: LuchadorDao,
) : LuchadorRepository {

    override suspend fun obtenerTodos(): List<Luchador> = combinarConCatalogo(dao.todos())

    override suspend fun obtenerPorId(id: Int): Luchador? = catalogo[id]?.con(dao.porId(id))

    override suspend fun obtenerInventario(): List<Luchador> = obtenerTodos().filter { it.cantidad > 0 }

    override suspend fun recolectarLuchador(luchadorId: Int, nuevoNivel: Int) {
        val actual = obtenerPorId(luchadorId) ?: return
        dao.guardar(actual.copy(cantidad = actual.cantidad + 1, nivel = nuevoNivel, rareza = Rareza.paraNivel(nuevoNivel)).aEntidad())
    }

    override suspend fun subirNivel(luchadorId: Int, nuevoNivel: Int) {
        val actual = obtenerPorId(luchadorId) ?: throw ErrorSubidaNivel.LuchadorNoEncontrado
        dao.guardar(actual.copy(nivel = nuevoNivel, rareza = Rareza.paraNivel(nuevoNivel)).aEntidad())
    }

    override suspend fun sumarPuntosPoder(luchadorId: Int, puntos: Int) {
        if (puntos <= 0) return
        val actual = obtenerPorId(luchadorId) ?: throw ErrorSubidaNivel.LuchadorNoEncontrado
        dao.guardar(actual.sumarPuntosPoder(puntos).aEntidad())
    }

    override suspend fun sembrar(desde: List<LuchadorProgresoRemoto>) {
        val (conocidos, faltantes) = desde.distinctBy { it.luchadorId }.partition { it.luchadorId in catalogo }
        if (faltantes.isNotEmpty()) {
            AppLogger.warning("Seed: luchadores remotos sin catálogo local: ${faltantes.map { it.luchadorId }}")
        }
        dao.guardarTodos(conocidos.map { it.aEntidad() })
    }

    override suspend fun obtenerParaSincronizar(): List<LuchadorProgresoRemoto> = obtenerInventario().map {
        LuchadorProgresoRemoto(
            luchadorId = it.id,
            cantidad = it.cantidad,
            nivel = it.nivel,
            puntosPoder = it.puntosPoder,
            rareza = it.rareza.clave,
            skinEquipada = it.skinEquipada,
            desbloqueadoEn = null,
        )
    }

    companion object {
        private val catalogo: Map<Int, Luchador> = CATALOGO_LUCHADORES.associateBy { it.id }

        /** Catálogo ordenado por id con el progreso guardado encima. */
        fun combinarConCatalogo(progreso: List<LuchadorProgresoEntity>): List<Luchador> {
            val porId = progreso.associateBy { it.id }
            return CATALOGO_LUCHADORES.sortedBy { it.id }.map { it.con(porId[it.id]) }
        }
    }
}

private fun Luchador.con(progreso: LuchadorProgresoEntity?): Luchador = if (progreso == null) {
    this
} else {
    copy(
        cantidad = progreso.cantidad,
        nivel = progreso.nivel,
        puntosPoder = progreso.puntosPoder,
        rareza = Rareza.desdeClave(progreso.rareza) ?: Rareza.COMUN,
        skinEquipada = progreso.skinEquipada,
    )
}

private fun Luchador.aEntidad() = LuchadorProgresoEntity(id, cantidad, nivel, puntosPoder, rareza.clave, skinEquipada)

/** Una rareza desconocida se deduce del nivel, como en iOS. */
internal fun LuchadorProgresoRemoto.aEntidad() = LuchadorProgresoEntity(
    id = luchadorId,
    cantidad = cantidad,
    nivel = nivel,
    puntosPoder = puntosPoder,
    rareza = (Rareza.desdeClave(rareza) ?: Rareza.paraNivel(nivel)).clave,
    skinEquipada = skinEquipada,
)
