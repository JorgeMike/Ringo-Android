package com.rudoestudio.tiempofuera.data.bloqueo

import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.domain.bloqueo.AppBlockingRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.AppSelectionRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.BloqueoMision
import com.rudoestudio.tiempofuera.domain.bloqueo.ErrorBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.MotivoBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.PeriodoBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.ProgresoSeguimientoGrupo
import com.rudoestudio.tiempofuera.domain.bloqueo.ProgresoSeguimientoRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.RegistroTiempoBloqueado
import com.rudoestudio.tiempofuera.domain.bloqueo.SeleccionAppsGrupo
import com.rudoestudio.tiempofuera.domain.bloqueo.TiempoBloqueadoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

// Lo que iOS guarda en los UserDefaults del App Group (lo comparte con sus extensiones);
// aquí todo vive en el mismo proceso, en el DataStore del juego. Los nombres de las clases
// `@Serializable` y de las claves no se cambian: romperían lo ya guardado.

/** En iOS: `AppSelectionRepository`. */
@Singleton
class AppSelectionDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : AppSelectionRepository {
    private val serializador = MapSerializer(String.serializer(), SetSerializer(String.serializer()))

    override val selecciones: Flow<Map<String, SeleccionAppsGrupo>> =
        almacen.observar(CLAVE, serializador).map { guardadas ->
            guardadas.orEmpty().mapValues { (grupo, paquetes) -> SeleccionAppsGrupo(grupo, paquetes) }
        }

    override suspend fun obtenerSeleccion(grupoId: String): SeleccionAppsGrupo? = selecciones.first()[grupoId]

    override suspend fun guardarSeleccion(seleccion: SeleccionAppsGrupo) {
        almacen.actualizar(CLAVE, serializador) { it.orEmpty() + (seleccion.grupoId to seleccion.paquetes) }
    }

    private companion object {
        const val CLAVE = "seleccionApps"
    }
}

/** En iOS: `AppBlockingRepository`. */
@Singleton
class AppBlockingDataStore @Inject constructor(
    private val almacen: AlmacenJson,
    private val selecciones: AppSelectionRepository,
    private val reloj: Clock,
) : AppBlockingRepository {
    /** Fin de cada bloqueo de misión en milisegundos; [SIN_FIN] si no tiene. */
    private val serializadorMisiones = MapSerializer(String.serializer(), Long.serializer())
    private val serializadorExtras = MapSerializer(String.serializer(), Long.serializer())

    override val misiones: Flow<Map<String, BloqueoMision>> = almacen.observar(CLAVE_MISIONES, serializadorMisiones).map { guardadas ->
        guardadas.orEmpty().mapValues { (grupo, fin) -> BloqueoMision(grupo, fin.takeIf { it != SIN_FIN }?.let(Instant::ofEpochMilli)) }
    }

    override val tiemposExtra: Flow<Map<String, Instant>> = almacen.observar(CLAVE_EXTRA, serializadorExtras).map { guardados ->
        guardados.orEmpty().mapValues { Instant.ofEpochMilli(it.value) }
    }

    override suspend fun iniciarBloqueo(grupoId: String, duracion: Duration?) {
        if (duracion != null && (duracion.isNegative || duracion.isZero || duracion >= Duration.ofDays(1))) {
            throw ErrorBloqueo.DuracionInvalida()
        }
        if (selecciones.obtenerSeleccion(grupoId)?.estaVacia != false) throw ErrorBloqueo.SinSeleccion()
        // Como en iOS: el inicio se redondea al segundo hacia abajo y el fin hacia arriba.
        val fin = duracion?.let { reloj.instant().truncatedTo(ChronoUnit.SECONDS).plusSeconds(ceilSegundos(it)) }
        almacen.actualizar(CLAVE_MISIONES, serializadorMisiones) { it.orEmpty() + (grupoId to (fin?.toEpochMilli() ?: SIN_FIN)) }
    }

    override suspend fun detenerBloqueo(grupoId: String) {
        almacen.actualizar(CLAVE_MISIONES, serializadorMisiones) { it.orEmpty() - grupoId }
    }

    override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) {
        val fin = reloj.instant().plusSeconds(segundos.toLong())
        almacen.actualizar(CLAVE_EXTRA, serializadorExtras) { it.orEmpty() + (grupoId to fin.toEpochMilli()) }
    }

    override suspend fun tieneTiempoExtraActivo(grupoId: String): Boolean {
        val fin = tiemposExtra.first()[grupoId] ?: return false
        if (fin.isAfter(reloj.instant())) return true
        almacen.actualizar(CLAVE_EXTRA, serializadorExtras) { it.orEmpty() - grupoId }
        return false
    }

    override suspend fun estaBloqueoRigidoActivo(grupoId: String): Boolean {
        val bloqueo = misiones.first()[grupoId] ?: return false
        val fin = bloqueo.fin ?: return true
        if (fin.isAfter(reloj.instant())) return true
        detenerBloqueo(grupoId)
        return false
    }

    private fun ceilSegundos(duracion: Duration): Long = duracion.seconds + if (duracion.nano > 0) 1 else 0

    private companion object {
        const val CLAVE_MISIONES = "bloqueos.misiones"
        const val CLAVE_EXTRA = "bloqueos.tiempoExtraFin"
        const val SIN_FIN = -1L
    }
}

@Serializable
data class ProgresoSeguimientoGuardado(
    val fecha: String,
    val segundosAcumulados: Long = 0,
    val segundosExtraOtorgados: Long = 0,
    val limiteMaximoAlcanzado: Boolean = false,
    val ultimoUsoEnMs: Long? = null,
    val ultimaRachaRecompensadaEnMs: Long? = null,
    val unidadesTrackingReclamadas: Map<String, Int> = emptyMap(),
    val hitosNotificados: Long = 0,
) {
    fun aDominio() = ProgresoSeguimientoGrupo(
        fecha = LocalDate.parse(fecha),
        segundosAcumulados = segundosAcumulados,
        segundosExtraOtorgados = segundosExtraOtorgados,
        limiteMaximoAlcanzado = limiteMaximoAlcanzado,
        ultimoUsoEn = ultimoUsoEnMs?.let(Instant::ofEpochMilli),
        ultimaRachaRecompensadaEn = ultimaRachaRecompensadaEnMs?.let(Instant::ofEpochMilli),
        unidadesTrackingReclamadas = unidadesTrackingReclamadas.mapKeys { it.key.toInt() },
        hitosNotificados = hitosNotificados,
    )

    companion object {
        fun de(p: ProgresoSeguimientoGrupo) = ProgresoSeguimientoGuardado(
            fecha = p.fecha.toString(),
            segundosAcumulados = p.segundosAcumulados,
            segundosExtraOtorgados = p.segundosExtraOtorgados,
            limiteMaximoAlcanzado = p.limiteMaximoAlcanzado,
            ultimoUsoEnMs = p.ultimoUsoEn?.toEpochMilli(),
            ultimaRachaRecompensadaEnMs = p.ultimaRachaRecompensadaEn?.toEpochMilli(),
            unidadesTrackingReclamadas = p.unidadesTrackingReclamadas.mapKeys { it.key.toString() },
            hitosNotificados = p.hitosNotificados,
        )
    }
}

/** En iOS: `ProgresoSeguimientoRepository`. Un progreso de otro día se descarta al leerlo. */
@Singleton
class ProgresoSeguimientoDataStore @Inject constructor(
    private val almacen: AlmacenJson,
    private val reloj: Clock,
) : ProgresoSeguimientoRepository {
    private val serializador = MapSerializer(String.serializer(), ProgresoSeguimientoGuardado.serializer())

    override suspend fun obtener(grupoId: String): ProgresoSeguimientoGrupo? = todos()[grupoId]

    override suspend fun registrarTiempoAcumulado(grupoId: String, segundosTotales: Long) =
        cambiar(grupoId) { it.copy(segundosAcumulados = maxOf(it.segundosAcumulados, segundosTotales)) }

    override suspend fun marcarLimiteAlcanzado(grupoId: String) = cambiar(grupoId) { it.copy(limiteMaximoAlcanzado = true) }

    override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) =
        cambiar(grupoId) { it.copy(segundosExtraOtorgados = it.segundosExtraOtorgados + segundos) }

    override suspend fun registrarUltimoUso(grupoId: String, fecha: Instant) = cambiar(grupoId) { it.copy(ultimoUsoEn = fecha) }

    override suspend fun marcarRachaRecompensada(grupoId: String, fecha: Instant) =
        cambiar(grupoId) { it.copy(ultimaRachaRecompensadaEn = fecha) }

    override suspend fun unidadesTrackingReclamadas(grupoId: String, misionId: Int): Int =
        obtener(grupoId)?.unidadesTrackingReclamadas?.get(misionId) ?: 0

    override suspend fun registrarUnidadesTrackingReclamadas(grupoId: String, misionId: Int, unidades: Int) {
        if (unidades <= 0) return
        cambiar(grupoId) {
            it.copy(unidadesTrackingReclamadas = it.unidadesTrackingReclamadas + (misionId to (it.unidadesTrackingReclamadas[misionId] ?: 0) + unidades))
        }
    }

    override suspend fun gruposConLimiteAlcanzado(): List<String> = todos().filterValues { it.limiteMaximoAlcanzado }.keys.toList()

    override suspend fun registrarHitosNotificados(grupoId: String, hitos: Long) =
        cambiar(grupoId) { it.copy(hitosNotificados = maxOf(it.hitosNotificados, hitos)) }

    private fun hoy(): LocalDate = LocalDate.now(reloj)

    private suspend fun todos(): Map<String, ProgresoSeguimientoGrupo> =
        almacen.leer(CLAVE, serializador).orEmpty().mapValues { it.value.aDominio() }.filterValues { it.fecha == hoy() }

    private suspend fun cambiar(grupoId: String, cambio: (ProgresoSeguimientoGrupo) -> ProgresoSeguimientoGrupo) {
        val hoy = hoy()
        almacen.actualizar(CLAVE, serializador) { guardados ->
            val actual = guardados?.get(grupoId)?.aDominio()?.takeIf { it.fecha == hoy } ?: ProgresoSeguimientoGrupo(hoy)
            // Lo de días anteriores ya no sirve: se tira al escribir.
            guardados.orEmpty().filterValues { it.fecha == hoy.toString() } + (grupoId to ProgresoSeguimientoGuardado.de(cambio(actual)))
        }
    }

    private companion object {
        const val CLAVE = "progresoSeguimiento"
    }
}

@Serializable
data class RegistroTiempoBloqueadoGuardado(
    val periodosPorDia: Map<String, List<List<Long>>> = emptyMap(),
    val fuentesActivas: Set<String> = emptySet(),
    val inicioActivoMs: Long? = null,
) {
    fun aDominio() = RegistroTiempoBloqueado(
        periodosPorDia = periodosPorDia.entries.associate { (dia, periodos) ->
            LocalDate.parse(dia) to periodos.map { PeriodoBloqueo(Instant.ofEpochMilli(it[0]), Instant.ofEpochMilli(it[1])) }
        },
        fuentesActivas = fuentesActivas,
        inicioActivo = inicioActivoMs?.let(Instant::ofEpochMilli),
    )

    companion object {
        fun de(r: RegistroTiempoBloqueado) = RegistroTiempoBloqueadoGuardado(
            periodosPorDia = r.periodosPorDia.entries.associate { (dia, periodos) ->
                dia.toString() to periodos.map { listOf(it.inicio.toEpochMilli(), it.fin.toEpochMilli()) }
            },
            fuentesActivas = r.fuentesActivas,
            inicioActivoMs = r.inicioActivo?.toEpochMilli(),
        )
    }
}

/** En iOS: `TiempoBloqueadoRepository`; la lógica vive en [RegistroTiempoBloqueado]. */
@Singleton
class TiempoBloqueadoDataStore @Inject constructor(
    private val almacen: AlmacenJson,
    private val reloj: Clock,
) : TiempoBloqueadoRepository {
    private val serializador = RegistroTiempoBloqueadoGuardado.serializer()

    override suspend fun iniciarBloqueo(grupoId: String, motivo: MotivoBloqueo, fecha: Instant) =
        cambiar { it.iniciar(grupoId, motivo.clave, fecha) }

    override suspend fun finalizarBloqueo(grupoId: String, motivo: MotivoBloqueo, fecha: Instant) =
        cambiar { it.finalizar(grupoId, motivo.clave, fecha, reloj.zone) }

    override suspend fun sincronizar(grupos: Map<String, MotivoBloqueo>, fecha: Instant) =
        cambiar { it.sincronizar(grupos, fecha, reloj.zone) }

    override suspend fun tiempoBloqueadoHoy(fecha: Instant, excluidos: List<PeriodoBloqueo>): Long =
        (almacen.leer(CLAVE, serializador)?.aDominio() ?: RegistroTiempoBloqueado()).segundosBloqueados(fecha, reloj.zone, excluidos)

    private suspend fun cambiar(cambio: (RegistroTiempoBloqueado) -> RegistroTiempoBloqueado) {
        almacen.actualizar(CLAVE, serializador) { RegistroTiempoBloqueadoGuardado.de(cambio(it?.aDominio() ?: RegistroTiempoBloqueado())) }
    }

    private companion object {
        const val CLAVE = "tiempoBloqueado.configuredApps"
    }
}
