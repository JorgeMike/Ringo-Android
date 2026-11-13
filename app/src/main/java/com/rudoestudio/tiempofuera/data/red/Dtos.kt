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

/** `GET /me`. Se mapea a `PerfilOnboardingExistente` en `UserApiRepositoryImpl`. */
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

/** `PUT /me/luchadores/{id}`. `cantidad` y `rareza` se mandan siempre (en el API son opcionales). */
@Serializable
data class LuchadorRequestDto(
    val nivel: Int,
    val puntosPoder: Int,
    val skinEquipada: String,
    val cantidad: Int,
    val rareza: String,
)

/** Respuesta de `PUT /me/luchadores/{id}` y cada luchador de `PUT /me/sincronizacion`. */
@Serializable
data class LuchadorResponseDto(
    val luchadorId: Int,
    val nivel: Int,
    val puntosPoder: Int,
    val skinEquipada: String,
    val cantidad: Int,
    val rareza: String,
    val desbloqueadoEn: String,
)

/** Un luchador dentro de `PUT /me/sincronizacion`. */
@Serializable
data class LuchadorDeSincronizacionDto(
    val luchadorId: Int,
    val nivel: Int,
    val puntosPoder: Int,
    val skinEquipada: String,
    val cantidad: Int,
    val rareza: String,
)

/** `PATCH /me/progreso` y el bloque `progreso` de la sincronización. `lastActivityDate` es un instante ISO 8601. */
@Serializable
data class ProgresoUsuarioDto(
    val luchadorId: Int? = null,
    val puntosPoder: Int,
    val monedas: Int,
    val diasRacha: Int,
    val protectoresRacha: Int,
    val lastActivityDate: String? = null,
)

/** `PUT /me/sincronizacion`. Cada bloque es opcional: el API escribe solo lo que viene. */
@Serializable
data class SincronizacionDto(
    val ligas: List<ProgresoDeLigaDto>? = null,
    val luchadores: List<LuchadorDeSincronizacionDto>? = null,
    val progreso: ProgresoUsuarioDto? = null,
)

/** Respuesta de `PUT /me/sincronizacion`: solo trae los bloques que se mandaron. */
@Serializable
data class SincronizacionResponseDto(
    val ligas: List<ProgresoDeLigaDto>? = null,
    val luchadores: List<LuchadorResponseDto>? = null,
    val progreso: ProgresoUsuarioDto? = null,
)
