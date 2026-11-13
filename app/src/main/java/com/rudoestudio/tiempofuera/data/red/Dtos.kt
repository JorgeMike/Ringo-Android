package com.rudoestudio.tiempofuera.data.red

import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import kotlinx.serialization.Serializable

// Contratos de tiempo-fuera-api (ver openapi.json de ese repo). En iOS: Models/DTO.

@Serializable
data class ErrorDto(val error: String)

@Serializable
data class OkDto(val ok: Boolean)

@Serializable
data class SaludDto(val ok: Boolean, val version: String? = null)

@Serializable
data class PedirCodigoDto(val email: String)

@Serializable
data class VerificarCodigoDto(val email: String, val codigo: String)

@Serializable
data class RefreshDto(val refreshToken: String)

@Serializable
data class UsuarioDto(val id: String, val email: String, val onboardingCompletado: Boolean)

/** Respuesta de `/auth/verificar` y `/auth/refresh`. */
@Serializable
data class SesionDto(
    val accessToken: String,
    val refreshToken: String,
    val expiraEnSegundos: Int,
    val usuario: UsuarioDto,
) {
    fun aDominio(ahoraMs: Long = System.currentTimeMillis()) = SesionUsuario(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiraEnSegundos = expiraEnSegundos,
        usuarioId = usuario.id,
        email = usuario.email,
        onboardingCompletado = usuario.onboardingCompletado,
        creadaEnMs = ahoraMs,
    )
}

/** `GET /me`. Se mapea al `UserProfile` del dominio en M2. */
@Serializable
data class PerfilCompletoDto(
    val id: String,
    val email: String,
    val onboardingCompletado: Boolean,
    val proposito: String? = null,
    val personalidad: String? = null,
    val divisionesSeleccionadas: List<String> = emptyList(),
    val tiempoInicial: Int? = null,
    val horaInicioDia: Int? = null,
    val horaFinDia: Int? = null,
    val colchonUso: Int? = null,
    val puntosPoder: Int,
    val monedas: Int,
    val diasRacha: Int,
    val protectoresRacha: Int,
    val lastActivityDate: String? = null,
    val luchadorId: Int? = null,
    val luchadores: List<LuchadorDelUsuarioDto> = emptyList(),
    val ligas: List<ProgresoDeLigaDto> = emptyList(),
)

@Serializable
data class LuchadorDelUsuarioDto(
    val luchadorId: Int,
    val cantidad: Int,
    val nivel: Int,
    val puntosPoder: Int,
    val rareza: String,
    val skinEquipada: String,
    val desbloqueadoEn: String,
)

@Serializable
data class ProgresoDeLigaDto(
    val ligaId: Int,
    val totalExp: Double,
    val nivelActualId: Int,
    val nivelesDesbloqueados: List<Int>,
)

/**
 * `PATCH /me`: elecciones del onboarding. Los campos nulos no se mandan (el `Json` del
 * cliente usa `explicitNulls = false`); el API valida rangos (p. ej. colchón 20–120).
 */
@Serializable
data class PerfilRequestDto(
    val proposito: String? = null,
    val personalidad: String? = null,
    val divisionesSeleccionadas: List<String>? = null,
    val tiempoInicial: Int? = null,
    val horaInicioDia: Int? = null,
    val horaFinDia: Int? = null,
    val colchonUso: Int? = null,
    val onboardingCompletado: Boolean? = null,
)

/** `PATCH /me` responde solo con los campos del onboarding. */
@Serializable
data class PerfilDto(
    val id: String,
    val email: String,
    val onboardingCompletado: Boolean,
    val proposito: String? = null,
    val personalidad: String? = null,
    val divisionesSeleccionadas: List<String> = emptyList(),
    val tiempoInicial: Int? = null,
    val horaInicioDia: Int? = null,
    val horaFinDia: Int? = null,
    val colchonUso: Int? = null,
)
