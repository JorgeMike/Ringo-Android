package com.rudoestudio.tiempofuera.domain.servicios

import java.time.Instant
import javax.inject.Qualifier

/** Canales de notificación: el usuario puede apagar cada uno por separado en Ajustes. */
enum class CanalNotificacion { SEGUIMIENTO, PROTECCION, RACHA }

/**
 * Notificaciones locales. En iOS: `NotificationServiceProtocol`. Sin permiso de
 * notificaciones (Android 13+) no se muestra nada y no truena.
 */
interface NotificationService {
    /** Al momento. Una con el mismo [id] reemplaza a la anterior. [deepLink]: `ringo://…` al tocarla. */
    fun mostrar(id: String, titulo: String, cuerpo: String, canal: CanalNotificacion, deepLink: String? = null)

    /** A la hora [fecha] (aproximada: Android la puede retrasar unos minutos para ahorrar batería). */
    fun programarNotificacion(id: String, titulo: String, cuerpo: String, fecha: Instant)

    fun cancelarNotificacion(id: String)
}

/**
 * Alcance de corrutinas que vive lo que la app: para trabajo que no debe frenar a quien
 * lo pide ni cancelarse si cierra la pantalla (en iOS, un `Task { }` suelto).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlcanceApp
