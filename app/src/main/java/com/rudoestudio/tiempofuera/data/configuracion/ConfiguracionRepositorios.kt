package com.rudoestudio.tiempofuera.data.configuracion

import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.ConfiguracionGrupoAppsGuardada
import com.rudoestudio.tiempofuera.data.local.ConfiguracionHabitoSuenoGuardada
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * En iOS: `ConfiguracionGrupoAppsRepository` (UserDefaults del App Group, una llave por
 * grupo más un índice). Aquí todo va en un solo mapa: no hace falta el índice.
 */
@Singleton
class ConfiguracionGrupoAppsDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : ConfiguracionGrupoAppsRepository {
    private val serializador = MapSerializer(String.serializer(), ConfiguracionGrupoAppsGuardada.serializer())

    override suspend fun obtenerConfiguracion(grupoId: String): ConfiguracionGrupoApps? = obtenerTodas()[grupoId]

    override suspend fun obtenerTodas(): Map<String, ConfiguracionGrupoApps> =
        almacen.leer(CLAVE, serializador).orEmpty().mapNotNull { (id, guardada) -> guardada.aDominio()?.let { id to it } }.toMap()

    override suspend fun guardar(configuracion: ConfiguracionGrupoApps, grupoId: String) {
        almacen.actualizar(CLAVE, serializador) { it.orEmpty() + (grupoId to ConfiguracionGrupoAppsGuardada.de(configuracion)) }
    }

    override suspend fun eliminar(grupoId: String) {
        almacen.actualizar(CLAVE, serializador) { it.orEmpty() - grupoId }
    }

    private companion object {
        const val CLAVE = "configuracionGrupoApps"
    }
}

/** En iOS: `ConfiguracionHabitoSuenoRepository` (UserDefaults del App Group). */
@Singleton
class ConfiguracionHabitoSuenoDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : ConfiguracionHabitoSuenoRepository {

    override suspend fun obtenerConfiguracionHabitoSueno(): ConfiguracionHabitoSueno? =
        almacen.leer(CLAVE, ConfiguracionHabitoSuenoGuardada.serializer())?.aDominio()

    override suspend fun guardarConfiguracionHabitoSueno(configuracion: ConfiguracionHabitoSueno) {
        almacen.escribir(CLAVE, ConfiguracionHabitoSuenoGuardada.serializer(), ConfiguracionHabitoSuenoGuardada.de(configuracion))
    }

    override suspend fun eliminarConfiguracionHabitoSueno() {
        almacen.borrar(CLAVE)
    }

    private companion object {
        const val CLAVE = "configuracionHabitoSueno.datos"
    }
}
