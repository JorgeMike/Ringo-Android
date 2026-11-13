package com.rudoestudio.tiempofuera.domain.tienda

import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import kotlin.math.roundToInt
import kotlin.random.Random

// En iOS: Domain/Entities/Tienda y TiempoExtra (Configuracion). Los precios son en
// "energía" = puntos de poder.

data class SobreTienda(
    val rareza: Rareza,
    val precioEnergia: Int,
    val nombre: String,
    val descripcion: String,
    val imagen: String,
)

data class PinataTienda(
    val nombre: String,
    val descripcion: String,
    val precioEnergia: Int,
    val numeroSobres: Int,
    val imagen: String,
)

data class TiempoExtra(val nombre: String, val descripcion: String, val segundos: Int, val precioEnergia: Int)

enum class ResultadoCompraTiempoExtra { PROTECTOR_CONSUMIDO, RACHA_PERDIDA }

object CatalogoTienda {
    val precioSobrePorRareza: Map<Rareza, Int> = mapOf(
        Rareza.COMUN to 120,
        Rareza.RARO to 500,
        Rareza.EPICO to 800,
        Rareza.LEGENDARIO to 2200,
    )

    val sobres: List<SobreTienda> = Rareza.entries.map { r ->
        SobreTienda(
            rareza = r,
            precioEnergia = precioSobrePorRareza.getValue(r),
            // Igual que iOS, que usa el rawValue en minúsculas.
            nombre = "Sobre ${r.clave}",
            descripcion = "Contiene un personaje de rareza ${r.clave}.",
            imagen = "sobre_${r.clave}",
        )
    }

    /** Cuántos sobres trae una piñata. */
    const val NUMERO_SOBRES_PINATA = 5

    /** La piñata cuesta bastante más que esos sobres por separado: es lo "premium". */
    private const val MULTIPLICADOR_PREMIUM_PINATA = 2.2

    val pinata: PinataTienda = run {
        // División entera como en iOS: (120+500+800+2200)/4 = 905.
        val costoPromedioSobre = precioSobrePorRareza.values.sum() / precioSobrePorRareza.size
        val costoBase = costoPromedioSobre * NUMERO_SOBRES_PINATA
        PinataTienda(
            nombre = "Piñata Sorpresa",
            descripcion = "Rompe la piñata y recibe $NUMERO_SOBRES_PINATA sobres aleatorios de rarezas variadas.",
            precioEnergia = (costoBase * MULTIPLICADOR_PREMIUM_PINATA).roundToInt(),
            numeroSobres = NUMERO_SOBRES_PINATA,
            imagen = "pinata",
        )
    }

    /** Probabilidad de cada rareza en los sobres de la piñata. */
    val pesosRarezaPinata: Map<Rareza, Double> = mapOf(
        Rareza.COMUN to 50.0,
        Rareza.RARO to 30.0,
        Rareza.EPICO to 15.0,
        Rareza.LEGENDARIO to 5.0,
    )

    /** Rarezas de los sobres de una piñata, ordenadas de menor a mayor. */
    fun generarRarezasPinata(cantidad: Int = NUMERO_SOBRES_PINATA, azar: Random = Random.Default): List<Rareza> =
        List(cantidad) { rarezaAleatoriaPonderada(azar) }.sorted()

    private fun rarezaAleatoriaPonderada(azar: Random): Rareza {
        var valor = azar.nextDouble(pesosRarezaPinata.values.sum())
        for (rareza in Rareza.entries) {
            val peso = pesosRarezaPinata[rareza] ?: 0.0
            if (valor < peso) return rareza
            valor -= peso
        }
        return Rareza.COMUN // no debería alcanzarse
    }

    val tiemposExtra: List<TiempoExtra> = listOf(
        TiempoExtra("15 min extra", "Para terminar lo que estabas haciendo.", 15 * 60, 30),
        TiempoExtra("30 min extra", "Media hora más de pantalla.", 30 * 60, 55),
        TiempoExtra("1 hora extra", "Una hora completa adicional.", 60 * 60, 100),
    )
}
