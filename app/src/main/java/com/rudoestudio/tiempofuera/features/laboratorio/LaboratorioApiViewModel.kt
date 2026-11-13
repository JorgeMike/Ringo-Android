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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Herramienta de desarrollo para probar M1 contra el API real (staging). Usa
 * [ApiTiempoFuera] directo para `/health` y `/me`, saltándose el dominio: no copiar
 * este patrón en pantallas de la app.
 */
@HiltViewModel
class LaboratorioApiViewModel @Inject constructor(
    private val enviarCodigo: EnviarCodigoVerificacionUseCase,
    private val verificarCodigo: VerificarCodigoUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val sesionStore: SesionStore,
    private val api: ApiTiempoFuera,
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
