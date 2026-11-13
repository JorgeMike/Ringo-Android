package com.rudoestudio.tiempofuera.data.red

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Endpoints de tiempo-fuera-api (en iOS: `AuthEndpoint`). Qué rutas llevan token lo decide
 * [InterceptorAuth]. `PUT /me/ligas`, `/me/dias/{dia}` y `/me/racha/{fecha}` no están:
 * iOS no los usa (las ligas viajan en la sincronización).
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

    @PUT("me/luchadores/{luchadorId}")
    suspend fun actualizarLuchador(@Path("luchadorId") luchadorId: Int, @Body cuerpo: LuchadorRequestDto): LuchadorResponseDto

    @PATCH("me/progreso")
    suspend fun guardarProgreso(@Body cuerpo: ProgresoUsuarioDto): ProgresoUsuarioDto

    @PUT("me/sincronizacion")
    suspend fun sincronizar(@Body cuerpo: SincronizacionDto): SincronizacionResponseDto
}

/** Rutas que no llevan `Authorization` (el resto sí). */
internal val RUTAS_PUBLICAS = setOf("/health", "/auth/codigo", "/auth/verificar", "/auth/refresh")
