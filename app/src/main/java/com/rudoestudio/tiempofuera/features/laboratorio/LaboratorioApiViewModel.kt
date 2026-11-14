package com.rudoestudio.tiempofuera.features.laboratorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.BuildConfig
import com.rudoestudio.tiempofuera.data.red.ApiTiempoFuera
import com.rudoestudio.tiempofuera.domain.auth.CerrarSesionUseCase
import com.rudoestudio.tiempofuera.domain.auth.EnviarCodigoVerificacionUseCase
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import com.rudoestudio.tiempofuera.domain.auth.VerificarCodigoUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.DetenerBloqueoAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.IniciarBloqueoAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.ObtenerSeleccionAppsUseCase
import com.rudoestudio.tiempofuera.domain.bloqueo.UsoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.servicios.bloqueo.PermisosBloqueo
import java.time.Duration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Herramienta de desarrollo para probar M1 contra el API real (staging) y el bloqueo de
 * M3 en el teléfono. Usa [ApiTiempoFuera] directo para `/health` y `/me`, saltándose el
 * dominio: no copiar este patrón en pantallas de la app.
 */
@HiltViewModel
class LaboratorioApiViewModel @Inject constructor(
    private val enviarCodigo: EnviarCodigoVerificacionUseCase,
    private val verificarCodigo: VerificarCodigoUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val sesionStore: SesionStore,
    private val api: ApiTiempoFuera,
    private val permisos: PermisosBloqueo,
    private val obtenerSeleccion: ObtenerSeleccionAppsUseCase,
    private val iniciarBloqueo: IniciarBloqueoAppsUseCase,
    private val detenerBloqueo: DetenerBloqueoAppsUseCase,
    private val configuraciones: ConfiguracionGrupoAppsRepository,
    private val uso: UsoApps,
    private val estadoOnboarding: EstadoOnboardingRepository,
) : ViewModel() {

    val urlApi: String = BuildConfig.API_URL

    val sesion: StateFlow<SesionUsuario?> =
        sesionStore.sesion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _bitacora = MutableStateFlow<List<String>>(emptyList())
    val bitacora: StateFlow<List<String>> = _bitacora.asStateFlow()

    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    fun probarConexion() = ejecutar("GET /health") {
        val salud = api.salud()
        "ok=${salud.ok}"
    }

    fun mandarCodigo(email: String) = ejecutar("Mandar código a $email") {
        enviarCodigo(email)
        "Listo: revisa tu correo"
    }

    fun verificar(email: String, codigo: String) = ejecutar("Verificar código") {
        val s = verificarCodigo(email, codigo)
        "Sesión iniciada como ${s.email} (onboarding en el API: ${s.onboardingCompletado})"
    }

    fun leerPerfil() = ejecutar("GET /me") {
        val p = api.perfilCompleto()
        "poder=${p.puntosPoder} monedas=${p.monedas} racha=${p.diasRacha} " +
            "luchadores=${p.luchadores.size} ligas=${p.ligas.size} onboarding=${p.onboardingCompletado}"
    }

    /** Marca el access token como caducado para probar la renovación en la siguiente llamada. */
    fun caducarToken() = ejecutar("Caducar token") {
        val s = sesionStore.actual() ?: return@ejecutar "No hay sesión"
        sesionStore.guardar(s.copy(creadaEnMs = 0))
        "Caducado: la próxima llamada a /me debe renovarlo"
    }

    // region Bloqueo (M3): todo sobre el grupo de distracción

    fun revisarPermisos() = ejecutar("Permisos del bloqueo") {
        permisos.estado().toString()
    }

    fun bloquearPorMision(minutos: Long) = ejecutar("Misión: bloquear distracción $minutos min") {
        iniciarBloqueo(TipoGrupoApps.DISTRACCION, Duration.ofMinutes(minutos))
        "Listo: abre una de tus apps de distracción"
    }

    fun ponerLimite(minutos: Long) = ejecutar("Límite de $minutos min a distracción") {
        configuraciones.guardar(ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION, tiempoMaximoDiario = minutos * 60), TipoGrupoApps.DISTRACCION.clave)
        val paquetes = obtenerSeleccion(TipoGrupoApps.DISTRACCION)?.paquetes.orEmpty()
        val usado = uso.usoDeHoy().filterKeys { it in paquetes }.values.sumOf { it.toMinutes() }
        "Llevas $usado min hoy en tus ${paquetes.size} apps de distracción"
    }

    /**
     * Como iOS, solo se avisan los primeros 10 hitos del día: si ya se usó la app, un
     * intervalo de 1 min ya quedó atrás. Se elige uno para que el siguiente hito caiga un
     * minuto después del uso de hoy.
     */
    fun avisarEnUnMinuto() = ejecutar("Aviso de uso en 1 min de distracción") {
        val clave = TipoGrupoApps.DISTRACCION.clave
        val paquetes = obtenerSeleccion(TipoGrupoApps.DISTRACCION)?.paquetes.orEmpty()
        val usado = uso.usoDeHoy().filterKeys { it in paquetes }.values.sumOf { it.toMinutes() }
        val actual = configuraciones.obtenerConfiguracion(clave) ?: ConfiguracionGrupoApps(TipoGrupoApps.DISTRACCION)
        configuraciones.guardar(actual.copy(intervaloHito = (usado + 1) * 60), clave)
        "Llevas $usado min: usa una app de distracción 1 min más y llega el aviso"
    }

    fun quitarBloqueos() = ejecutar("Quitar bloqueos") {
        detenerBloqueo(TipoGrupoApps.DISTRACCION)
        configuraciones.eliminar(TipoGrupoApps.DISTRACCION.clave)
        "Sin misión ni límite en distracción"
    }

    // endregion

    /** Al terminar, `RootScreen` cambia solo al onboarding. */
    fun repetirOnboarding() = ejecutar("Repetir onboarding") {
        estadoOnboarding.reiniciar()
        "Onboarding desde el paso 0"
    }

    fun salir() = ejecutar("Cerrar sesión") {
        cerrarSesion()
        "Sesión cerrada"
    }

    private fun ejecutar(accion: String, bloque: suspend () -> String) {
        viewModelScope.launch {
            _ocupado.value = true
            val resultado = runCatching { bloque() }
                .fold({ "✅ $it" }, { "❌ ${it.javaClass.simpleName}: ${it.message}" })
            _bitacora.value = listOf("$accion → $resultado") + _bitacora.value
            _ocupado.value = false
        }
    }
}
