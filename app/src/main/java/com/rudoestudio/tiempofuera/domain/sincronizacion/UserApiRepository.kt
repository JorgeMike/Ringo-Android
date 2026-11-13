package com.rudoestudio.tiempofuera.domain.sincronizacion

import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.perfil.PerfilActualizado
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad

/**
 * El estado del usuario en `tiempo-fuera-api` (`/me/…`). En iOS:
 * `UserAPIRepositoryProtocol`; allá recibe DTOs, aquí tipos del dominio y los DTOs se
 * quedan en `data/red`. Se implementa en la parte 4 de M2 (M1.4). Solo están los métodos
 * que usan los casos de uso; `actualizarProgresoEnLiga` y `marcarDiaCumplido` no los usa
 * nadie en iOS.
 */
interface UserApiRepository {
    /** `GET /me` con luchadores y ligas. */
    suspend fun obtenerPerfil(): PerfilOnboardingExistente

    /** `PATCH /me`. */
    suspend fun actualizarPerfil(cambios: ActualizacionPerfil): PerfilActualizado

    /** `PUT /me/luchadores/{id}` con nivel, puntos, skin, cantidad y rareza. */
    suspend fun actualizarLuchador(luchador: Luchador)

    /** `PATCH /me/progreso`. */
    suspend fun guardarProgresoUsuario(progreso: ProgresoUsuarioRemoto)

    /** `PUT /me/sincronizacion`: manda lo local y regresa lo que quedó en la nube. */
    suspend fun sincronizar(sincronizacion: SincronizacionProgreso): SincronizacionProgreso
}

/** En iOS: `ActualizarPerfilRequestDTO`. */
data class ActualizacionPerfil(
    val proposito: PropositoUsuario,
    val personalidad: TipoPersonalidad,
    val divisionesSeleccionadas: Set<DivisionMision>,
    val tiempoInicial: Int,
    val horaInicioDia: Int,
    val horaFinDia: Int,
    val colchonUso: Int,
    val onboardingCompletado: Boolean,
)
