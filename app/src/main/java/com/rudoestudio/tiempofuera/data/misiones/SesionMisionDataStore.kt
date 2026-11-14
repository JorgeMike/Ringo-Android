package com.rudoestudio.tiempofuera.data.misiones

import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.SesionTiempoGuardada
import com.rudoestudio.tiempofuera.domain.misiones.SesionMisionRepository
import com.rudoestudio.tiempofuera.domain.misiones.SesionTiempo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** La misión de temporizador en curso, en DataStore para que sobreviva a cerrar la app. */
class SesionMisionDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : SesionMisionRepository {
    override val sesion: Flow<SesionTiempo?> =
        almacen.observar(CLAVE, SesionTiempoGuardada.serializer()).map { it?.aDominio() }

    override suspend fun obtener(): SesionTiempo? = almacen.leer(CLAVE, SesionTiempoGuardada.serializer())?.aDominio()

    override suspend fun guardar(sesion: SesionTiempo) {
        almacen.escribir(CLAVE, SesionTiempoGuardada.serializer(), SesionTiempoGuardada.de(sesion))
    }

    override suspend fun borrar() = almacen.borrar(CLAVE)

    private companion object {
        const val CLAVE = "misiones.sesionTiempo"
    }
}
