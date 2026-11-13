package com.rudoestudio.tiempofuera.servicios.bloqueo

/** Un rectángulo en pixeles de pantalla (como `android.graphics.Rect`, pero probable en JVM). */
data class Zona(val izquierda: Int, val arriba: Int, val derecha: Int, val abajo: Int) {
    val ancho: Int get() = derecha - izquierda
    val alto: Int get() = abajo - arriba
    val area: Long get() = ancho.toLong() * alto
}

/**
 * Qué zona tapar de la ventana de una app bloqueada. Lo aprendido en el spike (ver
 * `spike-bloqueo-android/README.md`):
 *
 * - **No perseguir animaciones:** el overlay va ~100 ms atrás y se veía un cuadro
 *   persiguiendo la ventana. Si la ventana **crece** (se abre), se tapa toda la pantalla
 *   al instante; si se **encoge** (se cierra), se quita; si está **quieta**, se tapa justo
 *   su zona (p. ej. una mitad en pantalla dividida).
 * - **Ventana flotante:** quieta 300 ms y más chica que la pantalla en ambos sentidos. Se
 *   deja libre la barra de arriba, que es la única forma de cerrarla.
 * - **Arrastrarla** (mismo tamaño en otro lugar) no es animación: el overlay la sigue.
 */
class SeguidorVentanas(private val barraFlotantePx: Int) {
    private class Seguimiento(val limites: Zona, val desdeMs: Long, val creciendo: Boolean)

    private enum class Estado { ESTABLE, CRECIENDO, ENCOGIENDO }

    private val seguimiento = HashMap<String, Seguimiento>()

    /**
     * @param zona `null`: quitar el bloqueo de esa app (se está cerrando).
     * @param revisarEnMs si no es `null`, volver a evaluar después de ese tiempo: al
     *   terminar una animación Android deja de mandar eventos.
     */
    data class Decision(val zona: Zona?, val flotante: Boolean, val revisarEnMs: Long?)

    fun decidir(paquete: String, limites: Zona, pantalla: Zona, ahoraMs: Long): Decision {
        val previo = seguimiento[paquete]
        var revisar: Long? = null
        if (previo != null && previo.limites != limites && previo.limites.ancho == limites.ancho && previo.limites.alto == limites.alto) {
            // La están arrastrando: se conserva desde cuándo tiene ese tamaño.
            seguimiento[paquete] = Seguimiento(limites, previo.desdeMs, previo.creciendo)
        } else if (previo == null || previo.limites != limites) {
            val creciendo = previo == null || limites.area > previo.limites.area
            seguimiento[paquete] = Seguimiento(limites, ahoraMs, creciendo)
            revisar = ESPERA_ESTABLE_MS + 50
        }
        val s = seguimiento.getValue(paquete)
        val estado = when {
            ocupaAnchoOAlto(limites, pantalla) || ahoraMs - s.desdeMs >= ESPERA_ESTABLE_MS -> Estado.ESTABLE
            s.creciendo -> Estado.CRECIENDO
            else -> Estado.ENCOGIENDO
        }
        val flotante = estado == Estado.ESTABLE && !ocupaAnchoOAlto(limites, pantalla)
        val zona = when {
            flotante -> limites.copy(arriba = limites.arriba + barraFlotantePx)
            estado == Estado.ESTABLE -> limites
            estado == Estado.CRECIENDO -> pantalla
            else -> null
        }
        return Decision(zona, flotante, revisar)
    }

    /** Olvida las ventanas de apps que ya no están bloqueadas. */
    fun conservarSolo(paquetes: Set<String>) {
        seguimiento.keys.retainAll(paquetes)
    }

    /** Pantalla completa o una mitad de pantalla dividida. Si no, es flotante o va animándose. */
    private fun ocupaAnchoOAlto(r: Zona, pantalla: Zona): Boolean =
        r.ancho >= pantalla.ancho * 95 / 100 || r.alto >= pantalla.alto * 95 / 100

    companion object {
        const val ESPERA_ESTABLE_MS = 300L
    }
}
