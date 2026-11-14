package com.rudoestudio.tiempofuera.domain.onboarding

import com.rudoestudio.tiempofuera.domain.perfil.UserProfileBorrador
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.sincronizacion.ActualizacionPerfil
import com.rudoestudio.tiempofuera.domain.sincronizacion.UserApiRepository
import javax.inject.Inject

// Casos de uso de Domain/UseCases/Onboarding de iOS. `AvanzarPasoOnboarding` y
// `ObtenerEstadoOnboarding` no se portan: el `OnboardingViewModel` guarda el paso con
// `EstadoOnboardingRepository.guardarProgreso`, junto con el borrador.

/**
 * Manda las respuestas del onboarding al API y las guarda en el perfil local; luego marca
 * el onboarding como terminado. En iOS: `FinalizarOnboardingUseCase`.
 */
class FinalizarOnboardingUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val estado: EstadoOnboardingRepository,
    private val api: UserApiRepository,
) {
    suspend operator fun invoke(borrador: UserProfileBorrador) {
        val remoto = api.actualizarPerfil(
            ActualizacionPerfil(
                proposito = borrador.proposito,
                personalidad = borrador.personalidadId,
                divisionesSeleccionadas = borrador.divisionesSeleccionadas,
                tiempoInicial = borrador.tiempoInicial,
                horaInicioDia = borrador.horaInicioDia,
                horaFinDia = borrador.horaFinDia,
                colchonUso = borrador.colchonInicioFin,
                onboardingCompletado = true,
            ),
        )
        val perfil = perfiles.obtenerPerfil().copy(
            proposito = remoto.proposito,
            personalidadId = remoto.personalidadId,
            divisionesSeleccionadas = remoto.divisionesSeleccionadas,
            email = borrador.email,
            tiempoInicial = remoto.tiempoInicial,
            tiempoMaxUso = borrador.tiempoMaxUso,
            horaInicioDia = remoto.horaInicioDia,
            horaFinDia = remoto.horaFinDia,
            colchonInicioFin = remoto.colchonUso,
        )
        perfiles.guardarPerfil(perfil)
        estado.finalizar()
        // iOS aquí llama a `AsegurarSeguimientoDiarioUseCase`. En Android no hace falta: el
        // servicio de accesibilidad mide y bloquea solo en cuanto tiene permiso y apps.
    }
}
