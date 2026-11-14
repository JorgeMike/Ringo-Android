package com.rudoestudio.tiempofuera.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.auth.AuthError
import com.rudoestudio.tiempofuera.domain.auth.EnviarCodigoVerificacionUseCase
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import com.rudoestudio.tiempofuera.domain.auth.VerificarCodigoUseCase
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.perfil.SembrarPerfilLocalUseCase
import com.rudoestudio.tiempofuera.domain.perfil.ValidadorEdad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Lo que entrega el paso de datos al coordinador. En iOS: `DatosPersonalesCapturados`. */
data class DatosCapturados(
    val fechaNacimiento: LocalDate,
    val email: String,
    /** La cuenta ya existía y terminó el onboarding en otro teléfono. */
    val perfilExistente: PerfilOnboardingExistente?,
)

/** En iOS: `EstadoDatosView`. */
enum class FaseDatos { INGRESANDO_DATOS, ENVIANDO_CODIGO, INGRESANDO_CODIGO, VERIFICANDO, SINCRONIZANDO }

data class EstadoDatos(
    val fechaNacimiento: LocalDate = LocalDate.now().minusYears(18),
    val email: String = "",
    val codigo: String = "",
    val fase: FaseDatos = FaseDatos.INGRESANDO_DATOS,
    val error: String? = null,
) {
    val ocupado: Boolean get() = fase in setOf(FaseDatos.ENVIANDO_CODIGO, FaseDatos.VERIFICANDO, FaseDatos.SINCRONIZANDO)
}

/**
 * Paso 4: fecha de nacimiento y correo, luego el código que manda el API (así se inicia
 * sesión). Si la cuenta ya había terminado el onboarding, baja su perfil. En iOS:
 * `DatosViewModel`.
 */
@HiltViewModel
class DatosViewModel @Inject constructor(
    private val enviarCodigo: EnviarCodigoVerificacionUseCase,
    private val verificarCodigo: VerificarCodigoUseCase,
    private val sembrarPerfil: SembrarPerfilLocalUseCase,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoDatos())
    val estado: StateFlow<EstadoDatos> = _estado.asStateFlow()

    /** Si ya se verificó y solo falló bajar el perfil, se reintenta solo eso. */
    private var sesionVerificada: SesionUsuario? = null

    fun cambiarFecha(fecha: LocalDate) = _estado.update { it.copy(fechaNacimiento = fecha) }

    fun cambiarEmail(email: String) = _estado.update { it.copy(email = email) }

    fun cambiarCodigo(codigo: String) = _estado.update { it.copy(codigo = codigo.filter(Char::isDigit)) }

    /** El botón hace una cosa u otra según la fase. */
    fun continuar(alTerminar: (DatosCapturados) -> Unit) = when (_estado.value.fase) {
        FaseDatos.INGRESANDO_DATOS -> solicitarCodigo()
        FaseDatos.INGRESANDO_CODIGO -> verificar(alTerminar)
        else -> Unit
    }

    private fun solicitarCodigo() {
        val e = _estado.value
        val email = e.email.trim()
        when {
            !ValidadorEdad.esMayorDeEdad(e.fechaNacimiento) ->
                return _estado.update { it.copy(error = "Debes tener al menos ${ValidadorEdad.EDAD_MINIMA} años para usar Tiempo Fuera.") }
            email.isEmpty() || '@' !in email || '.' !in email ->
                return _estado.update { it.copy(error = "Ese correo no cuadra. Revísalo.") }
        }
        _estado.update { it.copy(error = null, fase = FaseDatos.ENVIANDO_CODIGO) }
        viewModelScope.launch {
            try {
                enviarCodigo(email)
                _estado.update { it.copy(fase = FaseDatos.INGRESANDO_CODIGO) }
            } catch (ex: Exception) {
                AppLogger.error(ex, "DatosViewModel")
                _estado.update { it.copy(error = mensaje(ex), fase = FaseDatos.INGRESANDO_DATOS) }
            }
        }
    }

    private fun verificar(alTerminar: (DatosCapturados) -> Unit) {
        val e = _estado.value
        if (sesionVerificada == null && e.codigo.isEmpty()) {
            _estado.update { it.copy(error = "Pon el código que te mandamos al correo.") }
            return
        }
        viewModelScope.launch {
            val sesion = sesionVerificada ?: try {
                _estado.update { it.copy(error = null, fase = FaseDatos.VERIFICANDO) }
                verificarCodigo(e.email, e.codigo).also { sesionVerificada = it }
            } catch (ex: Exception) {
                AppLogger.error(ex, "DatosViewModel")
                _estado.update { it.copy(error = mensaje(ex), fase = FaseDatos.INGRESANDO_CODIGO) }
                return@launch
            }
            var perfil: PerfilOnboardingExistente? = null
            if (sesion.onboardingCompletado) {
                _estado.update { it.copy(error = null, fase = FaseDatos.SINCRONIZANDO) }
                perfil = try {
                    sembrarPerfil(sesion.email)
                } catch (ex: Exception) {
                    AppLogger.error(ex, "DatosViewModel")
                    _estado.update {
                        it.copy(
                            error = "No pudimos recuperar tu perfil. Revisa tu internet e intenta de nuevo.",
                            fase = FaseDatos.INGRESANDO_CODIGO,
                        )
                    }
                    return@launch
                }
            }
            _estado.update { it.copy(fase = FaseDatos.INGRESANDO_CODIGO) }
            alTerminar(DatosCapturados(e.fechaNacimiento, e.email.trim(), perfil))
        }
    }

    private fun mensaje(error: Exception) =
        if (error is AuthError) error.message ?: GENERICO else GENERICO

    private companion object {
        const val GENERICO = "Algo falló. Intenta de nuevo."
    }
}
