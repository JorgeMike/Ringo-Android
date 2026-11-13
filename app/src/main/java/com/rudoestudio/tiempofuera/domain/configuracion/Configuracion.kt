package com.rudoestudio.tiempofuera.domain.configuracion

// En iOS: Domain/Entities/Configuracion. Lo que allá depende de FamilyControls
// (SeleccionAppsGrupo, ManagedSettingsStoreNaming) y el seguimiento diario
// (ProgresoSeguimientoGrupo) se porta con M3, con su equivalente en Android.

enum class TipoGrupoApps(val clave: String, val nombreVisible: String) {
    DISTRACCION("distraccion", "Distracción"),

    /** Clave "lectura" por compatibilidad: así lo guarda iOS. */
    EDUCATIVAS("lectura", "Educativas"),

    /** No mide apps: usa datos de salud (HealthKit en iOS, Health Connect en Android). */
    SALUD("salud", "Salud"),
    PRODUCTIVIDAD("productividad", "Productividad"),
    ;

    val camposConfigurables: List<CampoConfiguracionGrupoApps>
        get() = when (this) {
            DISTRACCION -> listOf(CampoConfiguracionGrupoApps.TIEMPO_MAXIMO_DIARIO, CampoConfiguracionGrupoApps.INTERVALO_HITO)
            EDUCATIVAS, PRODUCTIVIDAD -> listOf(CampoConfiguracionGrupoApps.INTERVALO_HITO)
            SALUD -> emptyList()
        }

    companion object {
        fun desdeClave(clave: String): TipoGrupoApps? = entries.firstOrNull { it.clave == clave }
    }
}

/** Tiempos en segundos (en iOS, `TimeInterval`). */
data class ConfiguracionGrupoApps(
    val tipo: TipoGrupoApps,
    /** Al alcanzarlo se bloquean las apps del grupo el resto del día. */
    val tiempoMaximoDiario: Long? = null,
    /** Cada cuánto uso acumulado se manda notificación (y gratificación, salvo en distracción). */
    val intervaloHito: Long? = null,
    /** Tiempo continuo sin abrir estas apps para ganar recompensa. */
    val tiempoRachaSinUsoParaRecompensa: Long? = null,
) {
    fun valor(campo: CampoConfiguracionGrupoApps): Long? = when (campo) {
        CampoConfiguracionGrupoApps.TIEMPO_MAXIMO_DIARIO -> tiempoMaximoDiario
        CampoConfiguracionGrupoApps.INTERVALO_HITO -> intervaloHito
        CampoConfiguracionGrupoApps.TIEMPO_RACHA_SIN_USO -> tiempoRachaSinUsoParaRecompensa
    }

    /** En iOS esto es un `WritableKeyPath`. */
    fun con(campo: CampoConfiguracionGrupoApps, segundos: Long?): ConfiguracionGrupoApps = when (campo) {
        CampoConfiguracionGrupoApps.TIEMPO_MAXIMO_DIARIO -> copy(tiempoMaximoDiario = segundos)
        CampoConfiguracionGrupoApps.INTERVALO_HITO -> copy(intervaloHito = segundos)
        CampoConfiguracionGrupoApps.TIEMPO_RACHA_SIN_USO -> copy(tiempoRachaSinUsoParaRecompensa = segundos)
    }

    companion object {
        const val MINUTOS_MAXIMOS_DIARIOS = 24 * 60
    }
}

enum class CampoConfiguracionGrupoApps(val titulo: String, val descripcion: String) {
    TIEMPO_MAXIMO_DIARIO("Tiempo máximo diario", "Al alcanzarlo, se bloquean las apps del grupo por el resto del día."),
    INTERVALO_HITO("Notificame cuando lo use...", "Se manda una notificación cada que se cumple este intervalo de uso acumulado."),
    TIEMPO_RACHA_SIN_USO("Racha sin uso para recompensa", "Tiempo continuo sin abrir estas apps para ganar una recompensa."),
}

/** Horario del hábito de sueño, en minutos desde medianoche. */
data class ConfiguracionHabitoSueno(val horaInicioDia: Int, val horaFinDia: Int, val colchonUso: Int)
