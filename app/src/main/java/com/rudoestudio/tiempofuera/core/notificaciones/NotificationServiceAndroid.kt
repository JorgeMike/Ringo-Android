package com.rudoestudio.tiempofuera.core.notificaciones

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rudoestudio.tiempofuera.MainActivity
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.domain.servicios.CanalNotificacion
import com.rudoestudio.tiempofuera.domain.servicios.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notificaciones locales (M12.1). En iOS: `NotificationService` (UNUserNotificationCenter).
 * Las programadas usan `AlarmManager` sin hora exacta: así no hace falta el permiso de
 * alarmas exactas, que Play revisa; para recordatorios basta con que lleguen cerca de la hora.
 */
@Singleton
class NotificationServiceAndroid @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : NotificationService {

    init {
        crearCanales(contexto)
    }

    override fun mostrar(id: String, titulo: String, cuerpo: String, canal: CanalNotificacion, deepLink: String?) =
        publicar(contexto, id, titulo, cuerpo, canal, deepLink)

    override fun programarNotificacion(id: String, titulo: String, cuerpo: String, fecha: Instant) {
        if (!fecha.isAfter(Instant.now())) return
        val alarma = Intent(contexto, ReceptorNotificacion::class.java)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITULO, titulo)
            .putExtra(EXTRA_CUERPO, cuerpo)
        val pendiente = PendingIntent.getBroadcast(contexto, id.hashCode(), alarma, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        contexto.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fecha.toEpochMilli(), pendiente)
    }

    override fun cancelarNotificacion(id: String) {
        val alarma = Intent(contexto, ReceptorNotificacion::class.java)
        PendingIntent.getBroadcast(contexto, id.hashCode(), alarma, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { contexto.getSystemService(AlarmManager::class.java).cancel(it) }
        NotificationManagerCompat.from(contexto).cancel(id, 0)
    }

    companion object {
        internal const val EXTRA_ID = "id"
        internal const val EXTRA_TITULO = "titulo"
        internal const val EXTRA_CUERPO = "cuerpo"

        private fun idCanal(canal: CanalNotificacion) = canal.name.lowercase()

        private fun crearCanales(contexto: Context) {
            val gestor = contexto.getSystemService(NotificationManager::class.java)
            listOf(
                NotificationChannel(idCanal(CanalNotificacion.SEGUIMIENTO), "Tu tiempo en apps", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Cuánto llevas en tus apps de distracción y educativas." },
                NotificationChannel(idCanal(CanalNotificacion.PROTECCION), "Protección", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Si el bloqueo de apps se apaga o deja de funcionar." },
                NotificationChannel(idCanal(CanalNotificacion.RACHA), "Racha", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Recordatorios para no perder tu racha." },
            ).forEach(gestor::createNotificationChannel)
        }

        internal fun publicar(contexto: Context, id: String, titulo: String, cuerpo: String, canal: CanalNotificacion, deepLink: String?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val abrir = if (deepLink != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), contexto, MainActivity::class.java)
            } else {
                Intent(contexto, MainActivity::class.java)
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val alTocar = PendingIntent.getActivity(contexto, id.hashCode(), abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notificacion = NotificationCompat.Builder(contexto, idCanal(canal))
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setContentIntent(alTocar)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(contexto).notify(id, 0, notificacion)
        }
    }
}

/** Muestra una notificación programada cuando llega su alarma. */
class ReceptorNotificacion : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationServiceAndroid.EXTRA_ID) ?: return
        NotificationServiceAndroid.publicar(
            contexto,
            id,
            intent.getStringExtra(NotificationServiceAndroid.EXTRA_TITULO).orEmpty(),
            intent.getStringExtra(NotificationServiceAndroid.EXTRA_CUERPO).orEmpty(),
            CanalNotificacion.RACHA,
            deepLink = null,
        )
    }
}
