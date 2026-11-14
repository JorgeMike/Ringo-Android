package com.rudoestudio.tiempofuera.domain.misiones

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.random.Random

/**
 * "Memorizar secuencia" (M6.6): los tambores se encienden en orden y hay que repetirlo
 * contra el reloj. En iOS: la parte que no es pantalla de `SecuenciaViewModel`.
 */
object JuegoSecuencia {
    /** Longitud de la primera ronda; cada ronda agrega un paso. */
    const val LONGITUD_BASE = 3
    const val INCREMENTO_POR_RONDA = 1

    /** Cuánto dura encendido un tambor y cada cuánto se enciende el siguiente (ms). */
    const val ENCENDIDO_MS = 450L
    const val ENTRE_PASOS_MS = 650L

    private const val SEGUNDOS_POR_PASO_BASE = 2.0
    private const val REDUCCION_POR_RONDA = 0.3
    private const val SEGUNDOS_POR_PASO_MINIMO = 0.8
    private const val MARGEN_SEGUNDOS = 2.0

    /**
     * Las rondas de la misión, al azar: cada una es una lista de índices de tambor (desde 0).
     * La semilla de iOS ya no trae las secuencias; solo cuántas y cuántos tambores.
     */
    fun generar(mision: Mision, azar: Random = Random.Default): List<List<Int>> {
        val rondas = mision.configuracion.numSecuencias ?: 2
        val tambores = (mision.configuracion.numTambores ?: 4).coerceAtLeast(1)
        return List(rondas) { ronda -> List(LONGITUD_BASE + ronda * INCREMENTO_POR_RONDA) { azar.nextInt(tambores) } }
    }

    fun numeroTambores(mision: Mision): Int = (mision.configuracion.numTambores ?: 4).coerceAtLeast(1)

    /** Segundos para repetir una ronda: cada ronda hay menos tiempo por paso, hasta un mínimo. */
    fun tiempoLimite(pasos: Int, ronda: Int): Double {
        val porPaso = maxOf(SEGUNDOS_POR_PASO_MINIMO, SEGUNDOS_POR_PASO_BASE - ronda * REDUCCION_POR_RONDA)
        return pasos * porPaso + MARGEN_SEGUNDOS
    }
}

/** Un punto de la escena, de 0 a 1 en cada eje. */
data class PuntoEscena(val x: Double, val y: Double) {
    fun distancia(otro: PuntoEscena): Double = hypot(x - otro.x, y - otro.y)
}

/**
 * "Memorizar escena" (M6.7): se ven unos stickers, desaparecen y hay que regresarlos a su
 * lugar contra el reloj. En iOS: la parte que no es pantalla de `EscenaViewModel`.
 */
object JuegoEscena {
    /** Dentro del marco inclinado de la escena (viewBox 453x613), para no salirse. */
    const val X_MIN = 70.0742 / 453.0
    const val X_MAX = 358.35 / 453.0
    const val Y_MIN = 28.0791 / 613.0
    const val Y_MAX = 462.971 / 613.0

    /** Qué tan cerca hay que dejar cada sticker para que cuente. */
    const val RADIO_ACIERTO = 0.12

    const val TOTAL_STICKERS = 9

    private const val DISTANCIA_MINIMA = 0.16
    private const val INTENTOS = 40

    /** Posiciones al azar, separadas entre sí lo más que se pueda (como iOS, 40 intentos). */
    fun posiciones(cantidad: Int, azar: Random = Random.Default): List<PuntoEscena> {
        val usadas = mutableListOf<PuntoEscena>()
        repeat(cantidad) {
            var mejor = aleatoria(azar)
            var mejorDistancia = distanciaMinima(mejor, usadas)
            repeat(INTENTOS) {
                if (mejorDistancia >= DISTANCIA_MINIMA) return@repeat
                val candidata = aleatoria(azar)
                val d = distanciaMinima(candidata, usadas)
                if (d > mejorDistancia) {
                    mejor = candidata
                    mejorDistancia = d
                }
            }
            usadas += mejor
        }
        return usadas
    }

    /** Números de sticker (1 a 9) sin repetir mientras alcancen. */
    fun stickers(cantidad: Int, azar: Random = Random.Default): List<Int> {
        val todos = (1..TOTAL_STICKERS).toList()
        var disponibles = todos.shuffled(azar).toMutableList()
        return List(cantidad) {
            if (disponibles.isEmpty()) disponibles = todos.shuffled(azar).toMutableList()
            disponibles.removeAt(0)
        }
    }

    /** Segundos para memorizar: 2 + medio segundo por sticker, máximo 7. */
    fun tiempoMemorizar(cantidad: Int): Int = minOf(7, ceil(2.0 + cantidad * 0.5).toInt())

    /** Segundos para acomodar: 4 + 2 por sticker, máximo 25. */
    fun tiempoColocar(cantidad: Int): Int = minOf(25, ceil(4.0 + cantidad * 2.0).toInt())

    /** Lo que suelta el usuario se queda dentro del marco. */
    fun acotar(punto: PuntoEscena) = PuntoEscena(punto.x.coerceIn(X_MIN, X_MAX), punto.y.coerceIn(Y_MIN, Y_MAX))

    fun esAcierto(colocado: PuntoEscena?, original: PuntoEscena): Boolean =
        colocado != null && colocado.distancia(original) <= RADIO_ACIERTO

    private fun aleatoria(azar: Random) =
        PuntoEscena(X_MIN + azar.nextDouble() * (X_MAX - X_MIN), Y_MIN + azar.nextDouble() * (Y_MAX - Y_MIN))

    private fun distanciaMinima(punto: PuntoEscena, usadas: List<PuntoEscena>): Double =
        usadas.minOfOrNull { it.distancia(punto) } ?: Double.MAX_VALUE
}
