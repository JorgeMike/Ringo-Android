package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRachaRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.servicios.CanalNotificacion
import com.rudoestudio.tiempofuera.domain.servicios.NotificationService
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

// M3.12 y M3.13. En iOS el bloqueo lo aplica el sistema y no se puede apagar sin quitar
// Screen Time; en Android basta con apagar la accesibilidad (21 s en el spike) o tocar
// "Forzar detención", que además borra el permiso. No se puede impedir, pero sí detectar.

/** Lo que se sabe del servicio de bloqueo. */
data class EstadoProteccion(
    /** Última vez que el servicio se conectó. `null`: nunca ha protegido. */
    val conectadaDesde: Instant? = null,
    /** El servicio escribe un latido mientras corre. */
    val ultimoLatido: Instant? = null,
    /** Desde cuándo está apagada; `null` si no se ha detectado apagada. */
    val apagadaDesde: Instant? = null,
    /** Si ya se cobró la consecuencia de este apagado (solo una vez por apagado). */
    val consecuenciaAplicada: Boolean = false,
)

enum class DiagnosticoProteccion {
    /** Todavía no hay apps elegidas o nunca se prendió: no hay nada que proteger. */
    SIN_CONFIGURAR,
    ACTIVA,

    /** La apagaron: accesibilidad apagada o "Forzar detención". */
    APAGADA,

    /**
     * Prendida en Ajustes pero sin latido: Android la dejó atorada (le pasó al spike tras
     * reinstalar). No es culpa del usuario: solo se le avisa.
     */
    ATORADA,
}

interface ProteccionRepository {
    suspend fun estado(): EstadoProteccion

    /** Se conectó el servicio: empieza una protección nueva. */
    suspend fun registrarConexion(fecha: Instant)

    suspend fun registrarLatido(fecha: Instant)

    /** Conserva la primera fecha si ya estaba apagada. */
    suspend fun registrarApagado(fecha: Instant)

    suspend fun marcarConsecuenciaAplicada()
}

object VigilanteProteccion {
    /** Sin latido por más de esto con la accesibilidad prendida, se considera atorada. */
    val SIN_LATIDO_MAXIMO: Duration = Duration.ofMinutes(15)

    fun diagnosticar(estado: EstadoProteccion, accesibilidadActiva: Boolean, hayAppsElegidas: Boolean, ahora: Instant): DiagnosticoProteccion {
        if (!hayAppsElegidas) return DiagnosticoProteccion.SIN_CONFIGURAR
        val algunaVezProtegio = estado.conectadaDesde != null || estado.apagadaDesde != null
        if (!accesibilidadActiva) return if (algunaVezProtegio) DiagnosticoProteccion.APAGADA else DiagnosticoProteccion.SIN_CONFIGURAR
        val latido = estado.ultimoLatido ?: return DiagnosticoProteccion.ATORADA
        return if (Duration.between(latido, ahora) > SIN_LATIDO_MAXIMO) DiagnosticoProteccion.ATORADA else DiagnosticoProteccion.ACTIVA
    }
}

enum class ResultadoPenalizacion { PROTECTOR_GASTADO, RACHA_PERDIDA, NADA_QUE_PERDER }

/**
 * La consecuencia de apagar la protección (M3.13, decidido 13/11/2026): igual que
 * comprar tiempo extra, un protector salva la racha (y el día queda protegido); sin
 * protector, la racha vuelve a 0. Sin racha no se gasta nada.
 */
class PenalizarProteccionApagadaUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val historico: HistoricoRachaRepository,
    private val reloj: Clock,
) {
    suspend operator fun invoke(): ResultadoPenalizacion {
        val perfil = perfiles.obtenerPerfil()
        if (perfil.diasRacha <= 0) return ResultadoPenalizacion.NADA_QUE_PERDER
        val protegido = perfil.usarProtectorRacha(reloj.instant())
        if (protegido != null) {
            perfiles.guardarPerfil(protegido)
            val hoy = LocalDate.now(reloj)
            if (historico.obtenerRegistro(hoy) == null) historico.guardarRegistro(HistoricoRacha.protegido(hoy))
            return ResultadoPenalizacion.PROTECTOR_GASTADO
        }
        perfiles.guardarPerfil(perfil.copy(diasRacha = 0))
        return ResultadoPenalizacion.RACHA_PERDIDA
    }
}

/**
 * Revisa la protección y actúa: si la apagaron cobra la consecuencia (una vez) y avisa;
 * si está atorada, avisa. Lo llaman el servicio al desconectarse, el vigilante cada
 * 15 min y la app al abrirse (después de "Forzar detención" no corre nada hasta entonces).
 */
class AtenderProteccionUseCase @Inject constructor(
    private val permisos: RevisorPermisos,
    private val proteccion: ProteccionRepository,
    private val selecciones: AppSelectionRepository,
    private val penalizar: PenalizarProteccionApagadaUseCase,
    private val notificaciones: NotificationService,
    private val reloj: Clock,
) {
    /**
     * @param avisarAtorada `false` al abrir la app: justo después de prender el teléfono el
     *   servicio tarda unos segundos en conectarse y no se debe avisar de más.
     */
    suspend operator fun invoke(avisarAtorada: Boolean = true): DiagnosticoProteccion {
        val ahora = reloj.instant()
        val hayApps = selecciones.selecciones.first().values.any { !it.estaVacia }
        val diagnostico = VigilanteProteccion.diagnosticar(proteccion.estado(), permisos.estado().accesibilidad, hayApps, ahora)
        when (diagnostico) {
            DiagnosticoProteccion.APAGADA -> alApagarse(ahora)
            DiagnosticoProteccion.ATORADA -> if (avisarAtorada) notificaciones.mostrar(
                ID_NOTIFICACION,
                "Tu protección se trabó 🤔",
                "Android dice que está prendida pero no responde. Apágala y vuélvela a prender en Ajustes → Accesibilidad.",
                CanalNotificacion.PROTECCION,
            )
            else -> Unit
        }
        return diagnostico
    }

    private suspend fun alApagarse(ahora: Instant) {
        proteccion.registrarApagado(ahora)
        if (proteccion.estado().consecuenciaAplicada) return
        val resultado = penalizar()
        proteccion.marcarConsecuenciaAplicada()
        AppLogger.warning("Protección apagada: $resultado")
        val (titulo, cuerpo) = when (resultado) {
            ResultadoPenalizacion.PROTECTOR_GASTADO ->
                "Se apagó tu protección 🛡️" to "Gastaste un protector para salvar tu racha. Vuelve a prenderla en Tiempo Fuera."
            ResultadoPenalizacion.RACHA_PERDIDA ->
                "¡Te quedaste sin esquina! 💥" to "Se apagó la protección y tu racha se fue a la lona. Vuelve a prenderla en Tiempo Fuera."
            ResultadoPenalizacion.NADA_QUE_PERDER ->
                "Se apagó tu protección" to "Tus apps ya no se bloquean. Vuelve a prenderla en Tiempo Fuera."
        }
        notificaciones.mostrar(ID_NOTIFICACION, titulo, cuerpo, CanalNotificacion.PROTECCION)
    }

    private companion object {
        const val ID_NOTIFICACION = "proteccion"
    }
}
