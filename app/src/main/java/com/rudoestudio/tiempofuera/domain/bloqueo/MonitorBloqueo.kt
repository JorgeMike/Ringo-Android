package com.rudoestudio.tiempofuera.domain.bloqueo

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Corre el [MotorBloqueo] y lleva las cuentas: el tiempo bloqueado, el uso acumulado de
 * cada grupo y si ya se alcanzó el límite. Es lo que en iOS hace la extensión `monitor`
 * cuando el sistema le avisa de un evento. Lo llama el servicio de bloqueo cada pocos
 * segundos y cuando cambia un bloqueo.
 *
 * No guarda estado en memoria: el tiempo bloqueado se **sincroniza** con lo que está
 * bloqueado ahora, así que si el servicio muere y regresa, nada se queda "abierto".
 */
@Singleton
class MonitorBloqueo @Inject constructor(
    private val obtenerEstado: ObtenerEstadoBloqueoUseCase,
    private val tiempoBloqueado: TiempoBloqueadoRepository,
    private val progreso: ProgresoSeguimientoRepository,
    private val reloj: Clock,
) {
    private val candado = Mutex()

    /** Las apps bloqueadas ahora, por paquete. */
    suspend fun evaluar(): Map<String, GrupoBloqueado> = candado.withLock {
        val estado = obtenerEstado()
        val ahora = reloj.instant()
        val grupos = MotorBloqueo.gruposBloqueados(estado, ahora, reloj.zone)
        tiempoBloqueado.sincronizar(grupos, ahora)

        for (grupoId in estado.selecciones.keys) {
            val segundos = MotorBloqueo.usoDelGrupo(estado, grupoId).seconds
            if (segundos > 0) progreso.registrarTiempoAcumulado(grupoId, segundos)
            if (MotorBloqueo.limiteAlcanzado(estado, grupoId)) progreso.marcarLimiteAlcanzado(grupoId)
        }
        MotorBloqueo.paquetesBloqueados(estado, ahora, reloj.zone)
    }
}
