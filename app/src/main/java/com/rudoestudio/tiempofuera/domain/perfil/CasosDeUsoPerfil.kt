package com.rudoestudio.tiempofuera.domain.perfil

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoLigaRepository
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.servicios.NotificationService
import com.rudoestudio.tiempofuera.domain.sincronizacion.ProgresoUsuarioRemoto
import com.rudoestudio.tiempofuera.domain.sincronizacion.UserApiRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

// Casos de uso de Domain/UseCases/UserProfile de iOS. `ProgramarRecordatoriosRachaUseCase`
// va con las notificaciones (M12.2).

/** En iOS: `ManejoMonedasUseCase`. */
class ManejoMonedasUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val reloj: Clock,
) {
    suspend fun sumar(monedas: Int): UserProfile = actualizar { it.sumarMonedas(monedas, reloj.instant()) }

    suspend fun restar(monedas: Int): UserProfile = actualizar { it.gastarMonedas(monedas, reloj.instant()) }

    private suspend fun actualizar(cambio: (UserProfile) -> UserProfile): UserProfile =
        cambio(perfiles.obtenerPerfil()).also { perfiles.guardarPerfil(it) }
}

/** En iOS: `ManejoPuntosNivelUseCase`. */
class ManejoPuntosNivelUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val reloj: Clock,
) {
    suspend fun sumar(puntos: Int): UserProfile = actualizar { it.sumarPuntosPoder(puntos, reloj.instant()) }

    suspend fun restar(puntos: Int): UserProfile = actualizar { it.gastarPuntosPoder(puntos, reloj.instant()) }

    private suspend fun actualizar(cambio: (UserProfile) -> UserProfile): UserProfile =
        cambio(perfiles.obtenerPerfil()).also { perfiles.guardarPerfil(it) }
}

/**
 * Activa la racha de hoy con la primera misión. Un día protegido todavía se puede
 * activar; uno ya completado no cambia nada. En iOS: `ActivarRachaUseCase`.
 */
class ActivarRachaUseCase @Inject constructor(
    private val historico: HistoricoRachaRepository,
    private val perfiles: UserProfileRepository,
    private val notificaciones: NotificationService,
    private val reloj: Clock,
) {
    suspend operator fun invoke(): ResultadoActivarRacha {
        val hoy = LocalDate.now(reloj)
        val registroHoy = historico.obtenerRegistro(hoy)
        if (registroHoy != null && !registroHoy.usoProtectorRacha) {
            cancelarRecordatorios(hoy)
            return ResultadoActivarRacha(activada = false, diasDeRacha = perfiles.obtenerPerfil().diasRacha)
        }

        historico.guardarRegistro(HistoricoRacha.completado(hoy))
        val perfil = perfiles.obtenerPerfil()
        val diasHistoricos = contarDiasConsecutivos(hasta = hoy.minusDays(1))
        val diasRemotos = RachaVigencia.diasVigentes(
            perfil.diasRacha,
            perfil.lastActivityDate,
            ahora = hoy.atStartOfDay(reloj.zone).toInstant(),
            zona = reloj.zone,
        )
        // Con la racha en 0 se empieza de nuevo aunque el historial tenga días seguidos.
        val diasPrevios = if (perfil.diasRacha > 0) maxOf(diasHistoricos, diasRemotos) else 0
        val actualizado = perfil.copy(diasRacha = if (diasPrevios > 0) diasPrevios + 1 else 1, lastActivityDate = reloj.instant())
        perfiles.guardarPerfil(actualizado)
        cancelarRecordatorios(hoy)
        return ResultadoActivarRacha(activada = true, diasDeRacha = actualizado.diasRacha)
    }

    private suspend fun contarDiasConsecutivos(hasta: LocalDate): Int {
        var dias = 0
        var fecha = hasta
        while (historico.obtenerRegistro(fecha) != null) {
            dias++
            fecha = fecha.minusDays(1)
        }
        return dias
    }

    private fun cancelarRecordatorios(fecha: LocalDate) {
        RecordatorioRacha.horas.forEach { notificaciones.cancelarNotificacion(RecordatorioRacha.identificador(fecha, it)) }
    }
}

/**
 * Cierra los días que pasaron sin racha: cada día perdido gasta un protector (y queda como
 * protegido) o, sin protectores, la racha vuelve a 0. Pasada la hora de fin del día, sube
 * el progreso al API; si falla, el error sale para reintentar. En iOS:
 * `ProcesarFinDeDiaUseCase` (en Android corre en WorkManager, M12.4).
 */
class ProcesarFinDeDiaUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val historico: HistoricoRachaRepository,
    private val api: UserApiRepository,
    private val sueno: ConfiguracionHabitoSuenoRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(fechaActual: Instant = reloj.instant()): UserProfile {
        val zona = reloj.zone
        val hoy = fechaActual.atZone(zona).toLocalDate()
        val ayer = hoy.minusDays(1)
        var perfil = perfiles.obtenerPerfil()

        if (perfil.diasRacha > 0) {
            val ultimaFechaConocida = historico.obtenerTodosLosRegistros().maxOfOrNull { it.fecha }
                ?: perfil.lastActivityDate?.atZone(zona)?.toLocalDate()
            if (ultimaFechaConocida == null) {
                perfil = perfil.copy(diasRacha = 0)
                perfiles.guardarPerfil(perfil)
            } else {
                var fechaPerdida = ultimaFechaConocida.plusDays(1)
                var cambio = false
                while (!fechaPerdida.isAfter(ayer) && perfil.diasRacha > 0) {
                    val protegido = perfil.usarProtectorRacha(fechaActual)
                    if (protegido != null) {
                        perfil = protegido
                        historico.guardarRegistro(HistoricoRacha.protegido(fechaPerdida))
                    } else {
                        perfil = perfil.copy(diasRacha = 0)
                    }
                    cambio = true
                    fechaPerdida = fechaPerdida.plusDays(1)
                }
                if (cambio) perfiles.guardarPerfil(perfil)
            }
        }

        guardarProgresoSiCorresponde(perfil, fechaActual, hoy)
        return perfil
    }

    private suspend fun guardarProgresoSiCorresponde(perfil: UserProfile, fechaActual: Instant, hoy: LocalDate) {
        val minutosFinDia = sueno.obtenerConfiguracionHabitoSueno()?.horaFinDia
            ?: perfil.horaFinDia.takeIf { it > 0 }
            ?: (23 * 60)
        val finDelDia = hoy.atStartOfDay(reloj.zone).plusMinutes(minutosFinDia.toLong()).toInstant()
        if (fechaActual < finDelDia) return
        api.guardarProgresoUsuario(
            ProgresoUsuarioRemoto(
                luchadorId = perfil.luchadorSeleccionado.id,
                puntosPoder = perfil.puntosPoder,
                monedas = perfil.monedas,
                diasRacha = perfil.diasRacha,
                protectoresRacha = perfil.protectoresRacha,
                lastActivityDate = perfil.lastActivityDate ?: reloj.instant(),
            ),
        )
    }
}

/**
 * Al iniciar sesión con una cuenta que ya existía: baja el perfil, los luchadores y las
 * ligas de la nube al teléfono. Una racha cuya última actividad fue antes de ayer llega
 * en 0. En iOS: `SembrarPerfilLocalUseCase`.
 */
class SembrarPerfilLocalUseCase @Inject constructor(
    private val api: UserApiRepository,
    private val perfiles: UserProfileRepository,
    private val luchadores: LuchadorRepository,
    private val progresoLigas: ProgresoLigaRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(email: String): PerfilOnboardingExistente {
        val remoto = api.obtenerPerfil()
        val luchador = luchadores.obtenerPorId(remoto.luchadorEquipadoId) ?: luchadores.obtenerPorId(ID_RINGO)
        val perfil = perfiles.obtenerPerfil().let {
            it.copy(
                email = email,
                proposito = remoto.proposito,
                personalidadId = remoto.personalidadId,
                divisionesSeleccionadas = remoto.divisionesSeleccionadas,
                tiempoInicial = remoto.tiempoInicial,
                horaInicioDia = remoto.horaInicioDia,
                horaFinDia = remoto.horaFinDia,
                colchonInicioFin = remoto.colchonUso,
                puntosPoder = remoto.puntosPoder,
                monedas = remoto.monedas,
                diasRacha = RachaVigencia.diasVigentes(remoto.diasRacha, remoto.lastActivityDate, reloj.instant(), reloj.zone),
                protectoresRacha = remoto.protectoresRacha,
                lastActivityDate = remoto.lastActivityDate,
                luchadorSeleccionado = luchador ?: it.luchadorSeleccionado,
            )
        }
        perfiles.guardarPerfil(perfil)
        luchadores.sembrar(remoto.luchadores)
        progresoLigas.sembrar(remoto.ligas)
        return remoto
    }

    private companion object {
        const val ID_RINGO = 0
    }
}
