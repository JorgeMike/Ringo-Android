package com.rudoestudio.tiempofuera.domain.sincronizacion

import com.rudoestudio.tiempofuera.domain.ligas.ProgresoLigaRepository
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilLocalSeeder
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.perfil.RachaVigencia
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import java.time.Clock
import javax.inject.Inject

// Casos de uso de Domain/UseCases/API de iOS que tocan datos locales. Los que solo
// pasan la llamada al API (`ActualizarPerfilUsuario`, `GuardarProgresoUsuario`…) no se
// portan: se llama directo a `UserApiRepository`.

/**
 * Junta y aplica el progreso local que se sincroniza con la nube. En iOS:
 * `ProgresoLocalProvider` (Data/Repositories/API).
 */
class ProgresoLocal @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val luchadores: LuchadorRepository,
    private val progresoLigas: ProgresoLigaRepository,
    private val reloj: Clock,
) {
    suspend fun leer(): SincronizacionProgreso {
        val perfil = perfiles.obtenerPerfil()
        return SincronizacionProgreso(
            ligas = progresoLigas.obtenerParaSincronizar(),
            luchadores = luchadores.obtenerParaSincronizar().filter { it.cantidad > 0 },
            progreso = ProgresoUsuarioRemoto(
                luchadorId = perfil.luchadorSeleccionado.id,
                puntosPoder = perfil.puntosPoder,
                monedas = perfil.monedas,
                diasRacha = RachaVigencia.diasVigentes(perfil.diasRacha, perfil.lastActivityDate, reloj.instant(), reloj.zone),
                protectoresRacha = perfil.protectoresRacha,
                lastActivityDate = perfil.lastActivityDate,
            ),
        )
    }

    suspend fun aplicar(sincronizacion: SincronizacionProgreso) {
        // Primero luchadores y ligas, para leer al luchador equipado ya actualizado.
        luchadores.sembrar(sincronizacion.luchadores)
        progresoLigas.sembrar(sincronizacion.ligas)

        val remoto = sincronizacion.progreso
        val perfil = perfiles.obtenerPerfil()
        val ultimaActividad = remoto.lastActivityDate ?: perfil.lastActivityDate
        perfiles.guardarPerfil(
            perfil.copy(
                puntosPoder = remoto.puntosPoder,
                monedas = remoto.monedas,
                diasRacha = RachaVigencia.diasVigentes(remoto.diasRacha, ultimaActividad, reloj.instant(), reloj.zone),
                protectoresRacha = remoto.protectoresRacha,
                lastActivityDate = ultimaActividad,
                luchadorSeleccionado = luchadores.obtenerPorId(remoto.luchadorId) ?: perfil.luchadorSeleccionado,
            ),
        )
    }
}

/** Manda lo local y aplica lo que regresa la nube. En iOS: `SincronizarProgresoUseCase` (M1.5). */
class SincronizarProgresoUseCase @Inject constructor(
    private val local: ProgresoLocal,
    private val api: UserApiRepository,
) {
    suspend operator fun invoke(): SincronizacionProgreso = api.sincronizar(local.leer()).also { local.aplicar(it) }
}

/** Baja el perfil de la nube y siembra luchadores y ligas. En iOS: `ObtenerPerfilUsuarioUseCase`. */
class ObtenerPerfilUsuarioUseCase @Inject constructor(
    private val api: UserApiRepository,
    private val seeder: PerfilLocalSeeder,
) {
    suspend operator fun invoke(): PerfilOnboardingExistente =
        api.obtenerPerfil().also { seeder.sincronizar(it.luchadores, it.ligas) }
}
