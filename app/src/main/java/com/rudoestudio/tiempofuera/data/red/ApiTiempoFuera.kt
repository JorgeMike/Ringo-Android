package com.rudoestudio.tiempofuera.data.red

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * Endpoints de tiempo-fuera-api (en iOS: `AuthEndpoint`). Las rutas bajo `/me/` que
 * guardan ligas, luchadores, días, racha y progreso se agregan con M1.5/M2, cuando
 * existan sus entidades. Qué rutas llevan token lo decide [InterceptorAuth].
 */
interface ApiTiempoFuera {
    @GET("health")
    suspend fun salud(): SaludDto

    @POST("auth/codigo")
    suspend fun pedirCodigo(@Body cuerpo: PedirCodigoDto): OkDto

    @POST("auth/verificar")
    suspend fun verificarCodigo(@Body cuerpo: VerificarCodigoDto): SesionDto

    @POST("auth/logout")
    suspend fun cerrarSesion(@Body cuerpo: RefreshDto): OkDto

    @GET("me")
    suspend fun perfilCompleto(): PerfilCompletoDto

    @PATCH("me")
    suspend fun actualizarPerfil(@Body cuerpo: PerfilRequestDto): PerfilDto
}

/** Rutas que no llevan `Authorization` (el resto sí). */
internal val RUTAS_PUBLICAS = setOf("/health", "/auth/codigo", "/auth/verificar", "/auth/refresh")
