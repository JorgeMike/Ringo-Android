package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.servicios.CanalNotificacion
import com.rudoestudio.tiempofuera.domain.servicios.NotificationService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Corre el [MotorBloqueo] y lleva las cuentas: el tiempo bloqueado, el uso acumulado de
 * cada grupo, su último uso, los avisos de hitos y si ya se alcanzó el límite. Es lo que en iOS hace la extensión `monitor`
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
    private val historial: HistorialUsoRepository,
    private val notificaciones: NotificationService,
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
            val anterior = progreso.obtener(grupoId)
            if (segundos > (anterior?.segundosAcumulados ?: 0)) {
                progreso.registrarTiempoAcumulado(grupoId, segundos)
                historial.registrar(LocalDate.now(reloj), grupoId, segundos)
                // En iOS es el "heartbeat" (cada 15 min de uso); aquí se sabe en cada vuelta.
                progreso.registrarUltimoUso(grupoId, ahora)
            }
            if (MotorBloqueo.limiteAlcanzado(estado, grupoId)) progreso.marcarLimiteAlcanzado(grupoId)
            avisarHito(estado, grupoId, segundos, anterior?.segundosUltimoHito ?: 0)
        }
        MotorBloqueo.paquetesBloqueados(estado, ahora, reloj.zone)
    }

    /** Un aviso por hito nuevo; si se juntaron varios (p. ej. tras reiniciar), solo el último. */
    private suspend fun avisarHito(estado: EstadoBloqueo, grupoId: String, segundos: Long, ultimoAvisado: Long) {
        val config = estado.configuraciones[grupoId] ?: return
        val intervalo = HitosUso.intervalo(config) ?: return
        val hitos = minOf(segundos / intervalo, HitosUso.tope(config.tipo))
        val segundosHito = hitos * intervalo
        if (hitos <= 0 || segundosHito <= ultimoAvisado) return
        // Los minutos reales: si se juntaron hitos (o se llegó al tope), "hitos × intervalo" se queda corto.
        val minutos = segundos / 60
        notificaciones.mostrar(
            id = "tracking.$grupoId.notificacion.$hitos.${LocalDate.now(reloj)}",
            titulo = HitosUso.titulo(config.tipo),
            cuerpo = HitosUso.cuerpo(config.tipo, minutos),
            canal = CanalNotificacion.SEGUIMIENTO,
            deepLink = HitosUso.DEEP_LINK_EDUCATIVAS.takeIf { config.tipo == TipoGrupoApps.EDUCATIVAS },
        )
        progreso.registrarHitoAvisado(grupoId, segundosHito)
    }
}
