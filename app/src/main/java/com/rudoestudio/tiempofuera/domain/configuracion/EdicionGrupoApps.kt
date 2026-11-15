package com.rudoestudio.tiempofuera.domain.configuracion

/**
 * Un campo de un grupo de apps mientras se edita: prendido o apagado y cuántos minutos.
 * En iOS: `ConfiguracionGrupoAppsEditView.EstadoCampo`.
 */
data class CampoEditable(val campo: CampoConfiguracionGrupoApps, val habilitado: Boolean, val minutos: Int) {
    /** El límite diario llega hasta 24 h; los demás, hasta 10 h (como el `Stepper` de iOS). */
    val maximo: Int
        get() = if (campo == CampoConfiguracionGrupoApps.TIEMPO_MAXIMO_DIARIO) ConfiguracionGrupoApps.MINUTOS_MAXIMOS_DIARIOS else 600

    fun ajustar(deltaMinutos: Int): CampoEditable = copy(minutos = (minutos + deltaMinutos).coerceIn(1, maximo))
}

/** Pasar la configuración de un grupo a campos editables y de regreso (M11.3). */
object EdicionGrupoApps {
    private const val MINUTOS_POR_DEFECTO = 60

    fun campos(grupo: TipoGrupoApps, actual: ConfiguracionGrupoApps?): List<CampoEditable> =
        grupo.camposConfigurables.map { campo ->
            val segundos = actual?.valor(campo)
            CampoEditable(campo, segundos != null, segundos?.let { maxOf(1, (it / 60).toInt()) } ?: MINUTOS_POR_DEFECTO)
        }

    /** Lo apagado se guarda como `null`; lo que no se editó se queda como estaba. */
    fun aplicar(grupo: TipoGrupoApps, actual: ConfiguracionGrupoApps?, campos: List<CampoEditable>): ConfiguracionGrupoApps =
        campos.fold(actual ?: ConfiguracionGrupoApps(grupo)) { config, c ->
            config.con(c.campo, if (c.habilitado) c.minutos * 60L else null)
        }

    /** "Tiempo máximo diario: 90min · …", o "Sin configurar". En iOS: `UserProfileEditView.resumen(for:)`. */
    fun resumen(grupo: TipoGrupoApps, actual: ConfiguracionGrupoApps?): String {
        val partes = grupo.camposConfigurables.mapNotNull { campo -> actual?.valor(campo)?.let { "${campo.titulo}: ${it / 60}min" } }
        return partes.joinToString(" · ").ifEmpty { "Sin configurar" }
    }
}
