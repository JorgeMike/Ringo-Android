package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.tienda.TiempoExtraApps
import kotlinx.coroutines.flow.first
import java.time.Duration
import javax.inject.Inject

// Casos de uso de Domain/UseCases/Configuracion de iOS.

/** En iOS: `IniciarBloqueoAppsUseCase`. Lo usan las misiones de tiempo y meditación. */
class IniciarBloqueoAppsUseCase @Inject constructor(
    private val bloqueos: AppBlockingRepository,
) {
    suspend operator fun invoke(grupo: TipoGrupoApps, duracion: Duration? = null) = bloqueos.iniciarBloqueo(grupo.clave, duracion)
}

/** En iOS: `DetenerBloqueoAppsUseCase`. */
class DetenerBloqueoAppsUseCase @Inject constructor(
    private val bloqueos: AppBlockingRepository,
) {
    suspend operator fun invoke(grupo: TipoGrupoApps) = bloqueos.detenerBloqueo(grupo.clave)
}

/** En iOS: `GuardarSeleccionAppsUseCase`. */
class GuardarSeleccionAppsUseCase @Inject constructor(
    private val selecciones: AppSelectionRepository,
) {
    suspend operator fun invoke(grupo: TipoGrupoApps, paquetes: Set<String>) =
        selecciones.guardarSeleccion(SeleccionAppsGrupo(grupo.clave, paquetes))
}

/** En iOS: `ObtenerSeleccionAppsUseCase`. */
class ObtenerSeleccionAppsUseCase @Inject constructor(
    private val selecciones: AppSelectionRepository,
) {
    suspend operator fun invoke(grupo: TipoGrupoApps): SeleccionAppsGrupo? = selecciones.obtenerSeleccion(grupo.clave)
}

/**
 * Junta todo lo que el [MotorBloqueo] necesita. Lo lee el servicio de bloqueo (M3.5) en
 * cada evaluación.
 */
class ObtenerEstadoBloqueoUseCase @Inject constructor(
    private val selecciones: AppSelectionRepository,
    private val configuraciones: ConfiguracionGrupoAppsRepository,
    private val bloqueos: AppBlockingRepository,
    private val sueno: ConfiguracionHabitoSuenoRepository,
    private val uso: UsoApps,
) {
    suspend operator fun invoke(): EstadoBloqueo = EstadoBloqueo(
        selecciones = selecciones.selecciones.first(),
        configuraciones = configuraciones.obtenerTodas(),
        misiones = bloqueos.misiones.first(),
        tiemposExtra = bloqueos.tiemposExtra.first(),
        sueno = sueno.obtenerConfiguracionHabitoSueno(),
        usoHoy = uso.usoDeHoy(),
    )
}

/**
 * El tiempo extra de la tienda: quita el bloqueo por límite del grupo durante unos
 * minutos. En iOS lo hacen `AppBlockingRepository.otorgarTiempoExtra` y
 * `ProgresoSeguimientoRepository.otorgarTiempoExtra`.
 */
class TiempoExtraAppsImpl @Inject constructor(
    private val bloqueos: AppBlockingRepository,
    private val progreso: ProgresoSeguimientoRepository,
) : TiempoExtraApps {
    override suspend fun tieneTiempoExtraActivo(grupoId: String): Boolean = bloqueos.tieneTiempoExtraActivo(grupoId)

    override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) {
        progreso.otorgarTiempoExtra(grupoId, segundos)
        bloqueos.otorgarTiempoExtra(grupoId, segundos)
    }
}
