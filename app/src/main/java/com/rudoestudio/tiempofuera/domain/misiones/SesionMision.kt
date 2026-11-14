package com.rudoestudio.tiempofuera.domain.misiones

import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.bloqueo.DetenerBloqueoAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.IniciarBloqueoAppsUseCase
import com.rudoestudio.tiempofuera.domain.servicios.NotificationService
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Una misión de temporizador en curso. Se guarda en disco: sobrevive a cerrar la app, y el
 * tiempo sale de las fechas, no de un contador. En iOS: el estado de la Live Activity
 * (`LiveActivityService`) más `SesionMisionStore`.
 *
 * Hay dos modos: con [fin] la misión dura un tiempo fijo y se completa sola; sin [fin]
 * ("modo abierto") se juntan vueltas de `segundosParaRecompensa` hasta que el usuario
 * decide terminar.
 */
data class SesionTiempo(
    val misionId: Int,
    val inicio: Instant,
    val fin: Instant?,
    /** Ya se terminó (por tiempo o "terminar y reclamar"): falta reclamar la recompensa. */
    val terminada: Boolean = false,
    /** Segundos acumulados al terminar en modo abierto (el reloj ya no avanza). */
    val segundosAlTerminar: Int? = null,
)

interface SesionMisionRepository {
    val sesion: Flow<SesionTiempo?>

    suspend fun obtener(): SesionTiempo?

    suspend fun guardar(sesion: SesionTiempo)

    suspend fun borrar()
}

/** Cuentas del reloj de la misión. En iOS: las propiedades calculadas de `TiempoViewModel`. */
object RelojMision {
    /** Duración de la misión de tiempo fijo (30 s si la semilla no la trae, como iOS). */
    fun duracion(mision: Mision): Int = mision.configuracion.duracionSegundos ?: 30

    fun esModoAbierto(mision: Mision): Boolean = mision.configuracion.segundosParaRecompensa != null

    fun restante(sesion: SesionTiempo, ahora: Instant): Int {
        val fin = sesion.fin ?: return 0
        if (sesion.terminada) return 0
        val ms = Duration.between(ahora, fin).toMillis()
        return if (ms <= 0) 0 else ((ms + 999) / 1000).toInt() // redondeo hacia arriba, como iOS
    }

    fun transcurrido(sesion: SesionTiempo, ahora: Instant): Int =
        sesion.segundosAlTerminar ?: Duration.between(sesion.inicio, ahora).seconds.toInt().coerceAtLeast(0)

    /** Vueltas completas en modo abierto. */
    fun vueltas(mision: Mision, segundos: Int): Int {
        val unidad = mision.configuracion.segundosParaRecompensa ?: return 0
        return if (unidad > 0) segundos / unidad else 0
    }

    /** 0 a 1: avance de la vuelta actual (abierto) o del tiempo total (fijo). */
    fun progreso(mision: Mision, sesion: SesionTiempo?, ahora: Instant): Float {
        if (sesion == null) return 0f
        if (esModoAbierto(mision)) {
            val unidad = mision.configuracion.segundosParaRecompensa ?: return 0f
            if (unidad <= 0) return 0f
            return (transcurrido(sesion, ahora) % unidad) / unidad.toFloat()
        }
        val total = duracion(mision)
        if (total <= 0) return 0f
        return 1f - restante(sesion, ahora) / total.toFloat()
    }

    /** Lo que se ve en el reloj: lo que falta (fijo) o lo que lleva (abierto). */
    fun segundosEnPantalla(mision: Mision, sesion: SesionTiempo?, ahora: Instant): Int = when {
        sesion == null -> if (esModoAbierto(mision)) 0 else duracion(mision)
        esModoAbierto(mision) -> transcurrido(sesion, ahora)
        else -> restante(sesion, ahora)
    }

    /** "05:30". */
    fun formato(segundos: Int): String = "%02d:%02d".format(segundos / 60, segundos % 60)

    /** La de tiempo fijo ya se acabó, aunque nadie la haya marcado como terminada. */
    fun seAcabo(mision: Mision, sesion: SesionTiempo, ahora: Instant): Boolean =
        sesion.terminada || (!esModoAbierto(mision) && restante(sesion, ahora) == 0)
}

/**
 * Empezar, rendirse y terminar una misión de temporizador: bloquea las apps de distracción
 * mientras dura y avisa al acabar. En iOS esto lo hace `TiempoViewModel` directo con los
 * casos de uso de bloqueo y la Live Activity.
 */
class SesionesMision @Inject constructor(
    private val repositorio: SesionMisionRepository,
    private val iniciarBloqueo: IniciarBloqueoAppsUseCase,
    private val detenerBloqueo: DetenerBloqueoAppsUseCase,
    private val notificaciones: NotificationService,
    private val reloj: Clock,
) {
    val sesion: Flow<SesionTiempo?> = repositorio.sesion

    suspend fun actual(): SesionTiempo? = repositorio.obtener()

    /** Lanza la excepción del bloqueo si no se pudo (en iOS: "No se pudo bloquear las apps"). */
    suspend fun iniciar(mision: Mision) {
        val ahora = reloj.instant()
        if (RelojMision.esModoAbierto(mision)) {
            iniciarBloqueo(GRUPO)
            repositorio.guardar(SesionTiempo(mision.id, ahora, fin = null))
        } else {
            val duracion = Duration.ofSeconds(RelojMision.duracion(mision).toLong())
            val fin = ahora + duracion
            iniciarBloqueo(GRUPO, duracion)
            repositorio.guardar(SesionTiempo(mision.id, ahora, fin))
            notificaciones.programarNotificacion(
                idNotificacion(mision.id),
                "¡Sesión completada!",
                "${mision.titulo}: ya puedes reclamar tu recompensa.",
                fin,
            )
        }
    }

    /** Se acabó el tiempo fijo, o "terminar y reclamar" en modo abierto. */
    suspend fun terminar(mision: Mision) {
        val sesion = repositorio.obtener()?.takeIf { it.misionId == mision.id && !it.terminada } ?: return
        val segundos = RelojMision.transcurrido(sesion, reloj.instant())
        liberar(mision.id)
        repositorio.guardar(
            sesion.copy(terminada = true, segundosAlTerminar = if (RelojMision.esModoAbierto(mision)) segundos else null),
        )
    }

    /** "Me rindo": se pierde lo que llevaba. */
    suspend fun rendirse(mision: Mision) {
        liberar(mision.id)
        repositorio.borrar()
    }

    /** Ya se reclamó la recompensa. */
    suspend fun cerrar() = repositorio.borrar()

    private suspend fun liberar(misionId: Int) {
        notificaciones.cancelarNotificacion(idNotificacion(misionId))
        detenerBloqueo(GRUPO)
    }

    companion object {
        /** En iOS `TiempoViewModel` recibe `.distraccion` por defecto y nadie lo cambia. */
        val GRUPO = TipoGrupoApps.DISTRACCION

        fun idNotificacion(misionId: Int) = "tiempo.mision.$misionId"
    }
}
