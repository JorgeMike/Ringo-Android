package com.rudoestudio.tiempofuera.core.notificaciones

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.misiones.AvisoMision
import com.rudoestudio.tiempofuera.domain.misiones.MisionRepository
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionMisionRepository
import com.rudoestudio.tiempofuera.domain.servicios.AlcanceApp
import com.rudoestudio.tiempofuera.domain.servicios.CanalNotificacion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La misión en curso fuera de la app (M13.1): una notificación fija con el reloj, que se ve
 * en la barra y en la pantalla de bloqueo. Al tocarla regresa a la misión. En iOS:
 * `LiveActivityService` + `TiempoLiveActivity`.
 *
 * No la llama nadie: sigue a la sesión guardada, así que empezar, pausar, reanudar, rendirse
 * o terminar la actualizan solos, y al reabrir la app (o tras reiniciar el teléfono, cuando
 * WorkManager la despierta) regresa si la misión sigue corriendo.
 */
@Singleton
class NotificacionMisionEnCurso @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val sesiones: SesionMisionRepository,
    private val misiones: MisionRepository,
    private val reloj: Clock,
    @AlcanceApp private val alcance: CoroutineScope,
) {
    private var iniciada = false

    fun iniciar() {
        if (iniciada) return
        iniciada = true
        alcance.launch {
            sesiones.sesion.collectLatest { sesion ->
                val mision = sesion?.let { runCatching { misiones.obtenerPorId(it.misionId) }.getOrNull() }
                val aviso = if (sesion != null && mision != null) RelojMision.aviso(mision, sesion, reloj.instant()) else null
                if (aviso == null) {
                    quitar()
                    return@collectLatest
                }
                mostrar(aviso)
                // Al acabarse el tiempo fijo se quita sola; avisa la de "¡Sesión completada!".
                aviso.cuentaRegresivaHasta?.let {
                    delay(Duration.between(reloj.instant(), it).toMillis().coerceAtLeast(0))
                    quitar()
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // lo revisa puedePublicar
    private fun mostrar(aviso: AvisoMision) {
        if (!NotificationServiceAndroid.puedePublicar(contexto)) return
        val constructor = NotificationCompat.Builder(contexto, NotificationServiceAndroid.idCanal(CanalNotificacion.MISION))
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ColoresTF.Amarillo.toArgb())
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.texto))
            .setContentIntent(NotificationServiceAndroid.alTocar(contexto, ID, "ringo://id-mision/${aviso.misionId}"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        when {
            aviso.cuentaRegresivaHasta != null -> constructor
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(aviso.cuentaRegresivaHasta.toEpochMilli())
                .setTimeoutAfter(Duration.between(reloj.instant(), aviso.cuentaRegresivaHasta).toMillis().coerceAtLeast(1))
            aviso.cuentaDesde != null -> constructor
                .setUsesChronometer(true)
                .setWhen(aviso.cuentaDesde.toEpochMilli())
            else -> constructor.setShowWhen(false)
        }
        try {
            NotificationManagerCompat.from(contexto).notify(ID, 0, constructor.build())
        } catch (e: SecurityException) {
            AppLogger.error(e, "NotificacionMisionEnCurso")
        }
    }

    private fun quitar() = NotificationManagerCompat.from(contexto).cancel(ID, 0)

    private companion object {
        const val ID = "mision.enCurso"
    }
}
