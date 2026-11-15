package com.rudoestudio.tiempofuera.features.tienda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.ProgresoSeguimientoRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.recompensas.FlujoRecompensa
import com.rudoestudio.tiempofuera.domain.tienda.CatalogoTienda
import com.rudoestudio.tiempofuera.domain.tienda.ComprarItemTiendaUseCase
import com.rudoestudio.tiempofuera.domain.tienda.ErrorTienda
import com.rudoestudio.tiempofuera.domain.tienda.PinataTienda
import com.rudoestudio.tiempofuera.domain.tienda.ResultadoCompraTiempoExtra
import com.rudoestudio.tiempofuera.domain.tienda.SobreTienda
import com.rudoestudio.tiempofuera.domain.tienda.TiempoExtra
import com.rudoestudio.tiempofuera.features.recompensas.PresentadorRecompensas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Antes de comprar tiempo extra se avisa qué le pasa a la racha. */
data class ConfirmacionTiempoExtra(val opcion: TiempoExtra, val gastaProtector: Boolean) {
    val titulo: String get() = if (gastaProtector) "Comprar y gastar protector" else "Comprar y perder racha"

    val advertencia: String
        get() = if (gastaProtector) {
            "Se va 1 protector de racha en esta compra. Si traes racha, queda protegida."
        } else {
            "Ojo: comprar tiempo extra te tumba la racha, aunque ya hayas hecho misión hoy."
        }
}

data class EstadoTienda(
    val monedas: Int = 0,
    /** El primer grupo de apps que ya llegó a su límite hoy: solo entonces se vende tiempo extra. */
    val grupoBloqueadoId: String? = null,
    val tiempoExtraActivo: Boolean = false,
    val comprando: Boolean = false,
    val error: String? = null,
    val confirmacion: ConfirmacionTiempoExtra? = null,
    val resultadoTiempoExtra: ResultadoCompraTiempoExtra? = null,
) {
    val sobres: List<SobreTienda> get() = CatalogoTienda.sobres
    val pinata: PinataTienda get() = CatalogoTienda.pinata
    val tiemposExtra: List<TiempoExtra> get() = CatalogoTienda.tiemposExtra
    val muestraTiempoExtra: Boolean get() = grupoBloqueadoId != null

    fun puedeComprar(precio: Int): Boolean = !comprando && monedas >= precio
    fun puedeComprar(opcion: TiempoExtra): Boolean = !tiempoExtraActivo && puedeComprar(opcion.precioEnergia)
}

/**
 * La tiendita (M10.1): sobres por rareza, la piñata de 5 sobres y, si ya llegaste a tu
 * límite, tiempo extra (que cuesta un protector o la racha). Lo comprado se abre con
 * [PresentadorRecompensas]. En iOS: `TiendaViewModel`.
 */
@HiltViewModel
class TiendaViewModel @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val comprar: ComprarItemTiendaUseCase,
    private val progreso: ProgresoSeguimientoRepository,
    private val presentador: PresentadorRecompensas,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoTienda())
    val estado: StateFlow<EstadoTienda> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            perfiles.perfil
                .catch { e ->
                    AppLogger.error(e, "TiendaViewModel")
                    _estado.update { it.copy(error = "No se pudieron cargar tus monedas. Intenta de nuevo.") }
                }
                .collect { p -> _estado.update { it.copy(monedas = p.monedas) } }
        }
        actualizarGrupoBloqueado()
    }

    /** Al volver a la app: pudo llegar al límite o acabarse el tiempo extra. */
    fun actualizarGrupoBloqueado() {
        viewModelScope.launch {
            try {
                val grupo = progreso.gruposConLimiteAlcanzado().firstOrNull()
                val activo = grupo != null && comprar.hayTiempoExtraActivo(grupo)
                _estado.update { it.copy(grupoBloqueadoId = grupo, tiempoExtraActivo = activo) }
            } catch (e: Exception) {
                AppLogger.error(e, "TiendaViewModel")
            }
        }
    }

    fun comprarSobre(sobre: SobreTienda) = compra(sobre.precioEnergia) {
        comprar.comprarSobre(sobre.rareza, sobre.precioEnergia)
        presentador.presentar(FlujoRecompensa.SobrePersonaje(sobre.rareza))
    }

    fun comprarPinata() {
        val pinata = CatalogoTienda.pinata
        compra(pinata.precioEnergia) {
            comprar.comprarPinata(pinata.precioEnergia)
            presentador.presentar(FlujoRecompensa.Pinata(CatalogoTienda.generarRarezasPinata(pinata.numeroSobres)))
        }
    }

    /** Primero se pregunta: la compra le pega a la racha. */
    fun pedirTiempoExtra(opcion: TiempoExtra) {
        viewModelScope.launch {
            try {
                val gasta = perfiles.obtenerPerfil().protectoresRacha > 0
                _estado.update { it.copy(confirmacion = ConfirmacionTiempoExtra(opcion, gasta), error = null) }
            } catch (e: Exception) {
                AppLogger.error(e, "TiendaViewModel")
                _estado.update { it.copy(error = "No se pudo revisar tu racha. Intenta de nuevo.") }
            }
        }
    }

    fun cancelarTiempoExtra() = _estado.update { it.copy(confirmacion = null) }

    fun confirmarTiempoExtra() {
        val e = _estado.value
        val opcion = e.confirmacion?.opcion ?: return
        val grupo = e.grupoBloqueadoId ?: return
        _estado.update { it.copy(confirmacion = null) }
        if (!e.puedeComprar(opcion)) return
        compra(opcion.precioEnergia) {
            val resultado = comprar.comprarTiempoExtra(grupo, opcion.segundos, opcion.precioEnergia)
            _estado.update { it.copy(resultadoTiempoExtra = resultado, tiempoExtraActivo = true) }
        }
    }

    fun cerrarResultado() = _estado.update { it.copy(resultadoTiempoExtra = null) }

    private fun compra(precio: Int, accion: suspend () -> Unit) {
        if (!_estado.value.puedeComprar(precio)) return
        _estado.update { it.copy(comprando = true, error = null) }
        viewModelScope.launch {
            val error = try {
                accion()
                null
            } catch (_: ErrorTienda.MonedasInsuficientes) {
                "No te alcanzan las monedas."
            } catch (_: ErrorTienda.TiempoExtraYaActivo) {
                actualizarGrupoBloqueado()
                "Ya tienes tiempo extra activo."
            } catch (ex: Exception) {
                AppLogger.error(ex, "TiendaViewModel")
                "No se pudo completar la compra. Intenta de nuevo."
            }
            _estado.update { it.copy(comprando = false, error = error) }
        }
    }
}
