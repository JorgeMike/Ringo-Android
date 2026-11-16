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
 * Una misión con reloj en curso: temporizador o meditación. Se guarda en disco: sobrevive a
 * cerrar la app, y el tiempo sale de las fechas, no de un contador. En iOS: el estado de la
 * Live Activity (`LiveActivityService`) más `SesionMisionStore`.
 *
 * Hay dos modos: con tiempo fijo la misión se completa sola al acabarse; en "modo abierto"
 * (`segundosParaRecompensa`) se juntan vueltas hasta que el usuario decide terminar. La
 * meditación además se puede pausar: el tramo corriendo empieza en [inicio] y lo de antes
 * va en [segundosPrevios] (abierto) o [restanteAlPausar] (fijo).
 */
data class SesionTiempo(
    val misionId: Int,
    val inicio: Instant,
    /** Fin del tiempo fijo mientras corre; `null` en modo abierto o en pausa. */
    val fin: Instant?,
    /** Ya se terminó (por tiempo o "terminar y reclamar"): falta reclamar la recompensa. */
    val terminada: Boolean = false,
    /** Segundos acumulados al terminar en modo abierto (el reloj ya no avanza). */
    val segundosAlTerminar: Int? = null,
    val pausada: Boolean = false,
    /** Modo abierto: segundos de los tramos anteriores a la última pausa. */
    val segundosPrevios: Int = 0,
    /** Tiempo fijo en pausa: lo que faltaba al pausar. */
    val restanteAlPausar: Int? = null,
)

interface SesionMisionRepository {
    val sesion: Flow<SesionTiempo?>

    suspend fun obtener(): SesionTiempo?

    suspend fun guardar(sesion: SesionTiempo)

    suspend fun borrar()
}

/**
 * La notificación fija de la misión en curso (M13.1). El reloj lo cuenta Android solo: con
 * tiempo fijo hacia atrás hasta [cuentaRegresivaHasta]; en modo abierto hacia adelante desde
 * [cuentaDesde]. En pausa no hay reloj, solo el texto. En iOS: `TiempoLiveActivity`.
 */
data class AvisoMision(
    val misionId: Int,
    val titulo: String,
    val texto: String,
    val cuentaRegresivaHasta: Instant? = null,
    val cuentaDesde: Instant? = null,
)

/** Cuentas del reloj de la misión. En iOS: las propiedades calculadas de `TiempoViewModel`. */
object RelojMision {
    /** Duración de la misión de tiempo fijo (30 s si la semilla no la trae, como iOS). */
    fun duracion(mision: Mision): Int = mision.configuracion.duracionSegundos ?: 30

    fun esModoAbierto(mision: Mision): Boolean = mision.configuracion.segundosParaRecompensa != null

    fun restante(sesion: SesionTiempo, ahora: Instant): Int {
        if (sesion.terminada) return 0
        if (sesion.pausada) return sesion.restanteAlPausar ?: 0
        val fin = sesion.fin ?: return 0
        val ms = Duration.between(ahora, fin).toMillis()
        return if (ms <= 0) 0 else ((ms + 999) / 1000).toInt() // redondeo hacia arriba, como iOS
    }

    fun transcurrido(sesion: SesionTiempo, ahora: Instant): Int = when {
        sesion.segundosAlTerminar != null -> sesion.segundosAlTerminar
        sesion.pausada -> sesion.segundosPrevios
        else -> sesion.segundosPrevios + Duration.between(sesion.inicio, ahora).seconds.toInt().coerceAtLeast(0)
    }

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
        sesion.terminada || (!esModoAbierto(mision) && !sesion.pausada && restante(sesion, ahora) == 0)

    /** Corriendo: ni en pausa ni acabada. */
    fun corriendo(mision: Mision, sesion: SesionTiempo?, ahora: Instant): Boolean =
        sesion != null && !sesion.pausada && !seAcabo(mision, sesion, ahora)

    /**
     * La notificación de la misión, o `null` si ya no va: acabada (con tiempo fijo avisa la
     * notificación de "¡Sesión completada!") o terminada para reclamar.
     */
    fun aviso(mision: Mision, sesion: SesionTiempo, ahora: Instant): AvisoMision? {
        if (sesion.misionId != mision.id || seAcabo(mision, sesion, ahora)) return null
        return when {
            sesion.pausada -> AvisoMision(mision.id, mision.titulo, "En pausa · ${formato(segundosEnPantalla(mision, sesion, ahora))}. Tus apps quedaron libres.")
            esModoAbierto(mision) -> AvisoMision(
                mision.id, mision.titulo, "Suma vueltas hasta que decidas terminar. Tus apps de distracción siguen bloqueadas.",
                cuentaDesde = sesion.inicio.minusSeconds(sesion.segundosPrevios.toLong()),
            )
            else -> AvisoMision(
                mision.id, mision.titulo, "Aguanta el round: tus apps de distracción siguen bloqueadas.",
                cuentaRegresivaHasta = sesion.fin,
            )
        }
    }

    /** Lo que dice el aviso de arriba (en iOS: `SesionMisionStore.textoEstado`). */
    fun textoEstado(mision: Mision, sesion: SesionTiempo, ahora: Instant): String {
        val tiempo = formato(segundosEnPantalla(mision, sesion, ahora))
        return when {
            seAcabo(mision, sesion, ahora) -> "Reclamar recompensa"
            sesion.pausada -> "En pausa · $tiempo"
            else -> tiempo
        }
    }
}

/**
 * Empezar, pausar, rendirse y terminar una misión con reloj (temporizador o meditación):
 * bloquea las apps de distracción mientras corre y avisa al acabar. En iOS esto lo hacen
 * `TiempoViewModel` y `MeditacionViewModel` directo con los casos de uso de bloqueo y la
 * Live Activity.
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
    suspend fun iniciar(mision: Mision) = correr(mision, SesionTiempo(mision.id, reloj.instant(), fin = null))

    /** Solo la meditación: suelta las apps y congela el reloj. */
    suspend fun pausar(mision: Mision) {
        val sesion = repositorio.obtener()?.takeIf { it.misionId == mision.id && !it.pausada && !it.terminada } ?: return
        val ahora = reloj.instant()
        liberar(mision.id)
        repositorio.guardar(
            sesion.copy(
                pausada = true,
                fin = null,
                segundosPrevios = RelojMision.transcurrido(sesion, ahora),
                restanteAlPausar = if (RelojMision.esModoAbierto(mision)) null else RelojMision.restante(sesion, ahora),
            ),
        )
    }

    suspend fun reanudar(mision: Mision) {
        val sesion = repositorio.obtener()?.takeIf { it.misionId == mision.id && it.pausada } ?: return
        correr(mision, sesion.copy(inicio = reloj.instant(), pausada = false))
    }

    /** Bloquea y guarda el tramo que empieza; el fijo dura lo que le queda y avisa al final. */
    private suspend fun correr(mision: Mision, sesion: SesionTiempo) {
        if (RelojMision.esModoAbierto(mision)) {
            iniciarBloqueo(GRUPO)
            repositorio.guardar(sesion.copy(fin = null))
            return
        }
        val segundos = sesion.restanteAlPausar ?: RelojMision.duracion(mision)
        val duracion = Duration.ofSeconds(segundos.toLong())
        val fin = sesion.inicio + duracion
        iniciarBloqueo(GRUPO, duracion)
        repositorio.guardar(sesion.copy(fin = fin, restanteAlPausar = null))
        notificaciones.programarNotificacion(
            idNotificacion(mision.id),
            "¡Sesión completada!",
            "${mision.titulo}: ya puedes reclamar tu recompensa.",
            fin,
        )
    }

    /** Se acabó el tiempo fijo, o "terminar y reclamar" en modo abierto. */
    suspend fun terminar(mision: Mision) {
        val sesion = repositorio.obtener()?.takeIf { it.misionId == mision.id && !it.terminada } ?: return
        val segundos = RelojMision.transcurrido(sesion, reloj.instant())
        if (!sesion.pausada) liberar(mision.id)
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

        fun idNotificacion(misionId: Int) = "mision.reloj.$misionId"
    }
}
