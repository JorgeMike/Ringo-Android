package com.rudoestudio.tiempofuera.data.bloqueo

import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.domain.bloqueo.HistorialUsoRepository
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Fecha "yyyy-MM-dd" → grupo → segundos. Guarda 30 días. */
@Singleton
class HistorialUsoDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : HistorialUsoRepository {
    private val serializador = MapSerializer(String.serializer(), MapSerializer(String.serializer(), Long.serializer()))

    override suspend fun registrar(fecha: LocalDate, grupoId: String, segundos: Long) {
        val desde = fecha.minusDays(HistorialUsoRepository.DIAS_GUARDADOS).toString()
        almacen.actualizar(CLAVE, serializador) { guardado ->
            val dia = guardado?.get(fecha.toString()).orEmpty()
            val nuevo = dia + (grupoId to maxOf(dia[grupoId] ?: 0, segundos))
            // Las claves "yyyy-MM-dd" se ordenan igual que las fechas.
            guardado.orEmpty().filterKeys { it >= desde } + (fecha.toString() to nuevo)
        }
    }

    override suspend fun ultimosDias(hasta: LocalDate, dias: Int): Map<LocalDate, Map<String, Long>> {
        val desde = hasta.minusDays(dias - 1L)
        return almacen.leer(CLAVE, serializador).orEmpty()
            .mapKeys { LocalDate.parse(it.key) }
            .filterKeys { !it.isBefore(desde) && !it.isAfter(hasta) }
    }

    private companion object {
        const val CLAVE = "historialUso"
    }
}
