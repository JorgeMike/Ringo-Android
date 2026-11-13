package com.rudoestudio.tiempofuera.data.onboarding

import androidx.room.withTransaction
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.data.ligas.aEntidad
import com.rudoestudio.tiempofuera.data.local.BaseDatosTiempoFuera
import com.rudoestudio.tiempofuera.data.luchadores.aEntidad
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorNoEncontradoEnCatalogo
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilLocalSeeder
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * En iOS: `SwiftDataPerfilLocalSeeder`. Una transacción de Room: si falta un luchador en
 * el catálogo no se escribe nada. A diferencia de `ProgresoLigaRepository.sembrar`, no
 * borra ligas locales que la nube no traiga (igual que iOS).
 */
@Singleton
class PerfilLocalSeederRoom @Inject constructor(
    private val db: BaseDatosTiempoFuera,
) : PerfilLocalSeeder {
    private val idsCatalogo = CATALOGO_LUCHADORES.map { it.id }.toSet()

    override suspend fun sincronizar(luchadores: List<LuchadorProgresoRemoto>, ligas: List<ProgresoLigaRemoto>) {
        luchadores.firstOrNull { it.luchadorId !in idsCatalogo }?.let { throw LuchadorNoEncontradoEnCatalogo(it.luchadorId) }
        db.withTransaction {
            db.luchadores().guardarTodos(luchadores.map { it.aEntidad() })
            db.progresoLigas().guardarTodos(ligas.map { it.aEntidad() })
        }
    }
}
