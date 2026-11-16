package com.rudoestudio.tiempofuera.domain.misiones

import kotlin.math.abs
import kotlin.math.acos

// M6.9: la misión de lagartijas. El teléfono va en el piso, boca arriba, y la cámara de
// enfrente ve tu cara: al bajar se ve grande, al subir chica. Cada "bajé y subí" es una.
// En iOS: `LagartijasViewModel` + `SensorRepeticionesFacialService`, con las mismas cifras.

/** Que el teléfono esté acostado boca arriba. En iOS: la parte de `iniciarDeteccionSuperficie`. */
object SuperficieLagartijas {
    /** Hasta cuántos grados de chueco cuenta como plano. */
    const val TOLERANCIA_GRADOS = 12.0

    /**
     * [gravedadZ] en m/s² del sensor de gravedad de Android: +9.8 boca arriba, -9.8 boca
     * abajo (en iOS el signo es al revés). Boca abajo no sirve: la cámara mira al piso.
     */
    fun esPlana(gravedadZ: Float): Boolean {
        val z = (gravedadZ / 9.81f).coerceIn(-1f, 1f)
        val grados = Math.toDegrees(acos(abs(z).toDouble()))
        return z > 0 && grados <= TOLERANCIA_GRADOS
    }
}

/**
 * Cuenta repeticiones con el ancho de la cara en la cámara (de 0 a 1 del ancho de la
 * imagen). Para "cerca" hay que pasar de [umbralCerca]; para volver a "lejos", bajar de
 * [umbralLejos] (así un valor en la orilla no cuenta de más). Cuenta al subir, y no dos en
 * menos de [antirreboteSeg].
 */
class ContadorRepeticiones(
    private val umbralCerca: Float = 0.45f,
    private val umbralLejos: Float = 0.30f,
    private val antirreboteSeg: Double = 0.35,
) {
    init {
        require(umbralLejos < umbralCerca) { "El umbral de lejos debe ser menor al de cerca" }
    }

    var cerca = false
        private set
    private var ultima = Double.NEGATIVE_INFINITY

    fun reiniciar() {
        cerca = false
    }

    /** `true` si con esta imagen se completó una repetición. Sin cara, [ancho] es 0. */
    fun evaluar(ancho: Float, ahoraSeg: Double): Boolean {
        val antes = cerca
        cerca = if (antes) ancho >= umbralLejos else ancho >= umbralCerca
        if (!antes || cerca || ahoraSeg - ultima < antirreboteSeg) return false
        ultima = ahoraSeg
        return true
    }
}

/**
 * La sesión de lagartijas: con meta de repeticiones (sin tiempo) o con tiempo fijo (las que
 * salgan). Inmutable: cada evento regresa la sesión nueva. En iOS: el estado de
 * `LagartijasViewModel`.
 */
data class SesionLagartijas(
    val modo: Modo,
    val estado: Estado = Estado.VERIFICANDO_SUPERFICIE,
    val repeticiones: Int = 0,
    /** Solo con tiempo: lo que falta, en segundos. */
    val restante: Int? = (modo as? Modo.Duracion)?.segundos,
) {
    sealed interface Modo {
        data class Objetivo(val repeticiones: Int) : Modo

        data class Duracion(val segundos: Int) : Modo
    }

    enum class Estado { VERIFICANDO_SUPERFICIE, SUPERFICIE_INVALIDA, LISTO, EN_PROGRESO, COMPLETADO }

    val enCurso: Boolean get() = estado == Estado.EN_PROGRESO
    val completado: Boolean get() = estado == Estado.COMPLETADO
    val objetivo: Int? get() = (modo as? Modo.Objetivo)?.repeticiones
    val conTiempo: Boolean get() = modo is Modo.Duracion

    /** Con tiempo, cada lagartija paga (como las vueltas); con meta, se paga una vez. */
    val unidadesRecompensa: Int get() = if (conTiempo) repeticiones else 1

    /** Antes de empezar, si el teléfono está acostado. Ya empezada, no importa. */
    fun superficie(esPlana: Boolean): SesionLagartijas =
        if (enCurso || completado) this else copy(estado = if (esPlana) Estado.LISTO else Estado.SUPERFICIE_INVALIDA)

    fun iniciar(): SesionLagartijas =
        if (estado != Estado.LISTO) this
        else copy(estado = Estado.EN_PROGRESO, repeticiones = 0, restante = (modo as? Modo.Duracion)?.segundos)

    fun repeticion(): SesionLagartijas {
        if (!enCurso) return this
        val nuevas = repeticiones + 1
        val meta = objetivo
        return copy(repeticiones = nuevas, estado = if (meta != null && nuevas >= meta) Estado.COMPLETADO else estado)
    }

    /** Un segundo menos (solo con tiempo); en cero se completa. */
    fun segundo(): SesionLagartijas {
        val falta = restante ?: return this
        if (!enCurso) return this
        val nuevo = (falta - 1).coerceAtLeast(0)
        return copy(restante = nuevo, estado = if (nuevo == 0) Estado.COMPLETADO else estado)
    }

    val mensaje: String?
        get() = when (estado) {
            Estado.VERIFICANDO_SUPERFICIE -> "Buscando una superficie plana..."
            Estado.SUPERFICIE_INVALIDA -> "Pon tu celular boca arriba sobre el piso o una superficie plana"
            Estado.LISTO -> "Todo listo, ¡dale cuando quieras!"
            Estado.EN_PROGRESO -> if (conTiempo) "¡Haz todas las que puedas antes de que se acabe el tiempo!" else "¡Vamos, tú puedes!"
            Estado.COMPLETADO -> null
        }

    val mensajeCompletado: String
        get() = if (conTiempo) "¡Tiempo! Hiciste $repeticiones repeticiones 💪" else "¡Meta alcanzada! 💪"

    companion object {
        /** Meta de repeticiones si la trae; si no, tiempo fijo. En iOS: el `init` del ViewModel. */
        fun para(mision: Mision): SesionLagartijas {
            val c = mision.configuracion
            val modo = when {
                (c.repeticionesObjetivo ?: 0) > 0 -> Modo.Objetivo(c.repeticionesObjetivo!!)
                (c.duracionSegundos ?: 0) > 0 -> Modo.Duracion(c.duracionSegundos!!)
                else -> Modo.Objetivo(0)
            }
            return SesionLagartijas(modo)
        }
    }
}
