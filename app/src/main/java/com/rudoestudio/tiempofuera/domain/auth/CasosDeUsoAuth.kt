package com.rudoestudio.tiempofuera.domain.auth

import javax.inject.Inject

// Casos de uso de Domain/UseCases/API/Auth de iOS. `RefrescarSesionUseCase` no se porta:
// en iOS nadie lo usa, la renovación la hace el cliente HTTP.

class EnviarCodigoVerificacionUseCase @Inject constructor(
    private val repositorio: AuthRepository,
) {
    suspend operator fun invoke(email: String) = repositorio.enviarCodigoVerificacion(email.trim())
}

class VerificarCodigoUseCase @Inject constructor(
    private val repositorio: AuthRepository,
    private val sesionStore: SesionStore,
) {
    suspend operator fun invoke(email: String, codigo: String): SesionUsuario {
        val sesion = repositorio.verificarCodigo(email.trim(), codigo.trim())
        sesionStore.guardar(sesion)
        return sesion
    }
}

/** No existe en iOS todavía: lo pide el API (`/auth/logout`) y lo usará el perfil (M11). */
class CerrarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepository,
    private val sesionStore: SesionStore,
) {
    suspend operator fun invoke() {
        if (sesionStore.actual() == null) return
        // La sesión local se borra aunque el API no conteste: el usuario quiere salir.
        runCatching { repositorio.cerrarSesion() }
        sesionStore.limpiar()
    }
}
