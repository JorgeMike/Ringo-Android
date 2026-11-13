package com.rudoestudio.tiempofuera.domain.servicios

import java.time.Instant
import javax.inject.Qualifier

/**
 * Notificaciones locales. En iOS: `NotificationServiceProtocol`. Se implementa en M12.1;
 * por ahora solo lo usan los casos de uso de racha (cancelar recordatorios).
 */
interface NotificationService {
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
