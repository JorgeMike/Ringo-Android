package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps

/**
 * Cada cuánto se avisa del uso de un grupo. En iOS: los eventos que arma
 * `SeguimientoDiarioService` (distracción y productividad: `intervaloHito`, máximo 10 al
 * día; educativas: cada 5 min sin tope, rearmando lotes de 20).
 */
object HitosUso {
    const val INTERVALO_EDUCATIVAS_SEGUNDOS = 300L
    const val MAXIMO_HITOS = 10L

    /** Deep link de la misión de liga "Rastreo educativo", como en iOS. */
    const val DEEP_LINK_EDUCATIVAS = "ringo://id-mision/102"

    /** Segundos entre hitos, o `null` si el grupo no avisa. */
    fun intervalo(config: ConfiguracionGrupoApps): Long? = when (config.tipo) {
        TipoGrupoApps.SALUD -> null
        TipoGrupoApps.EDUCATIVAS -> INTERVALO_EDUCATIVAS_SEGUNDOS
        else -> config.intervaloHito?.takeIf { it > 0 }
    }

    fun tope(tipo: TipoGrupoApps): Long = if (tipo == TipoGrupoApps.EDUCATIVAS) Long.MAX_VALUE else MAXIMO_HITOS

    /** En iOS: `NotificacionSeguimientoTextoProvider.titulo`. */
    fun titulo(tipo: TipoGrupoApps): String = when (tipo) {
        TipoGrupoApps.DISTRACCION -> "Aguas 👀"
        TipoGrupoApps.EDUCATIVAS -> "¡Recompensa educativa lista! 📚"
        TipoGrupoApps.PRODUCTIVIDAD -> "¡Sigue así! 💪"
        else -> "Progreso"
    }

    /** En iOS: `NotificacionSeguimientoTextoProvider.cuerpo` (allá dice "Abre Ringo"). */
    fun cuerpo(tipo: TipoGrupoApps, minutos: Long): String = when (tipo) {
        TipoGrupoApps.DISTRACCION -> "Llevas ${minutos}m en el cel bro, déjalo tantito 😅"
        TipoGrupoApps.EDUCATIVAS -> "Acumulaste ${minutos}m en tus apps educativas. Abre Tiempo Fuera para recoger tu recompensa."
        TipoGrupoApps.PRODUCTIVIDAD -> "Ya acumulaste ${minutos}m de productividad hoy"
        else -> "Llevas ${minutos}m de progreso hoy"
    }
}
