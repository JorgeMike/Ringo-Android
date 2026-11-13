package com.rudoestudio.tiempofuera.data.bloqueo

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadoProteccion
import com.rudoestudio.tiempofuera.domain.bloqueo.ProteccionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class EstadoProteccionGuardado(
    val conectadaDesdeMs: Long? = null,
    val ultimoLatidoMs: Long? = null,
    val apagadaDesdeMs: Long? = null,
    val consecuenciaAplicada: Boolean = false,
) {
    fun aDominio() = EstadoProteccion(
        conectadaDesdeMs?.let(Instant::ofEpochMilli),
        ultimoLatidoMs?.let(Instant::ofEpochMilli),
        apagadaDesdeMs?.let(Instant::ofEpochMilli),
        consecuenciaAplicada,
    )
}

/** Archivo propio: el latido se escribe seguido y no debe reescribir el del juego. */
@Singleton
class ProteccionDataStore internal constructor(private val almacen: AlmacenJson) : ProteccionRepository {
    @Inject
    constructor(@ApplicationContext contexto: Context) :
        this(AlmacenJson(PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("proteccion") }))

    override suspend fun estado(): EstadoProteccion = (almacen.leer(CLAVE, EstadoProteccionGuardado.serializer()) ?: EstadoProteccionGuardado()).aDominio()

    override suspend fun registrarConexion(fecha: Instant) =
        almacen.escribir(CLAVE, EstadoProteccionGuardado.serializer(), EstadoProteccionGuardado(fecha.toEpochMilli(), fecha.toEpochMilli()))

    override suspend fun registrarLatido(fecha: Instant) = cambiar { it.copy(ultimoLatidoMs = fecha.toEpochMilli()) }

    override suspend fun registrarApagado(fecha: Instant) = cambiar { it.copy(apagadaDesdeMs = it.apagadaDesdeMs ?: fecha.toEpochMilli()) }

    override suspend fun marcarConsecuenciaAplicada() = cambiar { it.copy(consecuenciaAplicada = true) }

    private suspend fun cambiar(cambio: (EstadoProteccionGuardado) -> EstadoProteccionGuardado) =
        almacen.actualizar(CLAVE, EstadoProteccionGuardado.serializer()) { cambio(it ?: EstadoProteccionGuardado()) }

    private companion object {
        const val CLAVE = "estado"
    }
}
