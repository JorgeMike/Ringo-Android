package com.rudoestudio.tiempofuera.domain.configuracion

/** Límites por grupo de apps. En iOS: `ConfiguracionGrupoAppsRepositoryProtocol`. */
interface ConfiguracionGrupoAppsRepository {
    suspend fun obtenerConfiguracion(grupoId: String): ConfiguracionGrupoApps?

    suspend fun obtenerTodas(): Map<String, ConfiguracionGrupoApps>

    suspend fun guardar(configuracion: ConfiguracionGrupoApps, grupoId: String)

    suspend fun eliminar(grupoId: String)
}

/**
 * Horario de sueño. En iOS `obtener…` lanza `datosNoEncontrados`
 * ("Configura primero tu horario de sueño."); aquí regresa `null`.
 */
interface ConfiguracionHabitoSuenoRepository {
    suspend fun obtenerConfiguracionHabitoSueno(): ConfiguracionHabitoSueno?

    suspend fun guardarConfiguracionHabitoSueno(configuracion: ConfiguracionHabitoSueno)

    suspend fun eliminarConfiguracionHabitoSueno()
}
