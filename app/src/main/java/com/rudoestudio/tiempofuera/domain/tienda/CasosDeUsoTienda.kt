package com.rudoestudio.tiempofuera.domain.tienda

import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRachaRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed class ErrorTienda(mensaje: String) : Exception(mensaje) {
    class MonedasInsuficientes : ErrorTienda("No tienes suficientes monedas para esta compra.")

    class TiempoExtraYaActivo : ErrorTienda("Ya tienes tiempo extra activo. Espera a que termine.")
}

/**
 * Darle a un grupo de apps minutos sin bloqueo. Lo implementa el bloqueo de apps (M3);
 * en iOS son dos repos: `AppBlockingRepository` y `ProgresoSeguimientoRepository`.
 */
interface TiempoExtraApps {
    suspend fun tieneTiempoExtraActivo(grupoId: String): Boolean

    suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int)
}

/**
 * Compras de la tienda. El tiempo extra tiene castigo: gasta un protector de racha (y el
 * día queda protegido) o, sin protectores, rompe la racha. En iOS: `ComprarItemTiendaUseCase`.
 */
class ComprarItemTiendaUseCase @Inject constructor(
    private val perfiles: UserProfileRepository,
    private val historico: HistoricoRachaRepository,
    private val tiempoExtra: TiempoExtraApps,
    private val reloj: Clock,
) {
    /** El luchador del sobre lo sortea `SorteoLuchadorUseCase`; [rareza] no se usa aquí, como en iOS. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun comprarSobre(rareza: Rareza, precio: Int) = descontarMonedas(precio)

    suspend fun comprarPinata(precio: Int) = descontarMonedas(precio)

    suspend fun comprarTiempoExtra(grupoId: String, segundos: Int, precio: Int): ResultadoCompraTiempoExtra {
        if (tiempoExtra.tieneTiempoExtraActivo(grupoId)) throw ErrorTienda.TiempoExtraYaActivo()
        val ahora = reloj.instant()
        var perfil = perfiles.obtenerPerfil()
        if (perfil.monedas < precio) throw ErrorTienda.MonedasInsuficientes()
        perfil = perfil.gastarMonedas(precio, ahora)

        val protegido = perfil.usarProtectorRacha(ahora)
        val resultado = if (protegido != null) {
            perfil = protegido
            val hoy = LocalDate.now(reloj)
            if (historico.obtenerRegistro(hoy) == null) historico.guardarRegistro(HistoricoRacha.protegido(hoy))
            ResultadoCompraTiempoExtra.PROTECTOR_CONSUMIDO
        } else {
            perfil = perfil.copy(diasRacha = 0)
            ResultadoCompraTiempoExtra.RACHA_PERDIDA
        }
        perfiles.guardarPerfil(perfil)
        tiempoExtra.otorgarTiempoExtra(grupoId, segundos)
        return resultado
    }

    suspend fun hayTiempoExtraActivo(grupoId: String): Boolean = tiempoExtra.tieneTiempoExtraActivo(grupoId)

    private suspend fun descontarMonedas(precio: Int) {
        val perfil = perfiles.obtenerPerfil()
        if (!perfil.tieneMonedasSuficientes(precio)) throw ErrorTienda.MonedasInsuficientes()
        perfiles.guardarPerfil(perfil.gastarMonedas(precio, reloj.instant()))
    }
}
