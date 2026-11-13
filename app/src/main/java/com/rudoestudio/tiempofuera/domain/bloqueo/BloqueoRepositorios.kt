package com.rudoestudio.tiempofuera.domain.bloqueo

import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

/** Qué apps hay en cada grupo. En iOS: `AppSelectionRepositoryProtocol`. */
interface AppSelectionRepository {
    val selecciones: Flow<Map<String, SeleccionAppsGrupo>>

    suspend fun obtenerSeleccion(grupoId: String): SeleccionAppsGrupo?

    suspend fun guardarSeleccion(seleccion: SeleccionAppsGrupo)
}

/**
 * Los bloqueos que pide la app: los de misión y el tiempo extra. El de sueño y el de
 * límite no se guardan aquí: se calculan (ver [MotorBloqueo]). En iOS:
 * `AppBlockingRepositoryProtocol`.
 */
interface AppBlockingRepository {
    val misiones: Flow<Map<String, BloqueoMision>>

    val tiemposExtra: Flow<Map<String, Instant>>

    /**
     * Bloquea el grupo por [duracion] (entre 0 y 24 h) o, sin duración, hasta que se llame
     * a [detenerBloqueo]. Lanza [ErrorBloqueo.SinSeleccion] o [ErrorBloqueo.DuracionInvalida].
     */
    suspend fun iniciarBloqueo(grupoId: String, duracion: Duration? = null)

    suspend fun detenerBloqueo(grupoId: String)

    suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int)

    suspend fun tieneTiempoExtraActivo(grupoId: String): Boolean

    /** Si hay un bloqueo de misión vigente; uno vencido se limpia aquí, como en iOS. */
    suspend fun estaBloqueoRigidoActivo(grupoId: String): Boolean
}

sealed class ErrorBloqueo(mensaje: String) : Exception(mensaje) {
    class SinSeleccion : ErrorBloqueo("No hay apps seleccionadas para bloquear en este grupo.")

    class DuracionInvalida : ErrorBloqueo("La duración del bloqueo debe ser mayor que cero y menor que un día.")
}

/** En iOS: `ProgresoSeguimientoRepositoryProtocol`. Todo es del día de hoy. */
interface ProgresoSeguimientoRepository {
    suspend fun obtener(grupoId: String): ProgresoSeguimientoGrupo?

    /**
     * En Android el uso se mide continuo, así que se guarda el total medido (no se suma
     * por hitos como en iOS). Nunca baja.
     */
    suspend fun registrarTiempoAcumulado(grupoId: String, segundosTotales: Long)

    suspend fun marcarLimiteAlcanzado(grupoId: String)

    suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int)

    suspend fun registrarUltimoUso(grupoId: String, fecha: Instant)

    suspend fun marcarRachaRecompensada(grupoId: String, fecha: Instant)

    suspend fun unidadesTrackingReclamadas(grupoId: String, misionId: Int): Int

    suspend fun registrarUnidadesTrackingReclamadas(grupoId: String, misionId: Int, unidades: Int)

    suspend fun gruposConLimiteAlcanzado(): List<String>

    suspend fun registrarHitoAvisado(grupoId: String, segundos: Long)
}

/** En iOS: `TiempoBloqueadoRepositoryProtocol`. */
interface TiempoBloqueadoRepository {
    suspend fun iniciarBloqueo(grupoId: String, motivo: MotivoBloqueo, fecha: Instant)

    suspend fun finalizarBloqueo(grupoId: String, motivo: MotivoBloqueo, fecha: Instant)

    /** Deja activos exactamente estos bloqueos (grupo → motivo). Ver [RegistroTiempoBloqueado.sincronizar]. */
    suspend fun sincronizar(grupos: Map<String, MotivoBloqueo>, fecha: Instant)

    /** Segundos con apps bloqueadas en el día de [fecha], sin contar los periodos [excluidos] (p. ej. el sueño). */
    suspend fun tiempoBloqueadoHoy(fecha: Instant, excluidos: List<PeriodoBloqueo> = emptyList()): Long
}

/**
 * Cuánto se usó cada app hoy. En iOS lo mide DeviceActivity por dentro; aquí sale de
 * `UsageStatsManager` más un contador propio que sobrevive a reinicios (ver el spike).
 */
interface UsoApps {
    /** Uso de hoy por paquete. Nunca baja durante el día. */
    suspend fun usoDeHoy(): Map<String, Duration>

    /** Apps con una actividad en primer plano ahora (en pantalla dividida pueden ser varias). */
    suspend fun enPrimerPlano(): Set<String>
}
