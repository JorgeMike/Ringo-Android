package com.rudoestudio.tiempofuera.domain.luchadores

import kotlin.random.Random

// En iOS: Domain/Entities/Luchadores. Los colores de Rareza viven en el design system
// (core/designsystem/ColoresDominio.kt): el dominio no conoce la UI.

/** Rareza de un luchador o sobre, de menor a mayor (el orden del enum es el de iOS). */
enum class Rareza(val clave: String, val nombreVisible: String, val imagenSobre: String) {
    COMUN("comun", "Común", "PackComun"),
    RARO("raro", "Raro", "PackRaro"),
    EPICO("epico", "Épico", "PackEpico"),
    LEGENDARIO("legendario", "Legendario", "PackLegendario"),
    ;

    companion object {
        fun desdeClave(clave: String): Rareza? = entries.firstOrNull { it.clave == clave }

        /**
         * Rareza del sobre que da un nivel de liga (1–100). Los hitos son fijos; los demás
         * niveles sortean. En iOS: `Rareza.calcularRareza(paraNivel:)`.
         */
        fun calcularRareza(paraNivel: Int, azar: Random = Random.Default): Rareza {
            val nivel = paraNivel.coerceIn(1, 100)
            when (nivel) {
                10 -> return COMUN
                25 -> return RARO
                50, 75 -> return EPICO
                100 -> return LEGENDARIO
            }
            val tirada = azar.nextInt(1, 101)
            return when (nivel) {
                in 1..24 -> COMUN
                in 26..49 -> if (tirada <= 30) RARO else COMUN // 30% raro
                in 51..74 -> if (tirada <= 30) EPICO else RARO // 30% épico
                in 76..99 -> when { // 5% legendario, 65% épico, 30% raro
                    tirada <= 5 -> LEGENDARIO
                    tirada <= 70 -> EPICO
                    else -> RARO
                }
                else -> COMUN
            }
        }

        /** Rareza de un luchador según su nivel. En iOS: `Rareza.paraNivel(_:)`. */
        fun paraNivel(nivel: Int): Rareza = when {
            nivel < 10 -> COMUN
            nivel <= 49 -> RARO
            nivel <= 249 -> EPICO
            else -> LEGENDARIO
        }
    }
}

enum class TipoLuchador(val clave: String) {
    PIEDRA("Piedra"),
    PAPEL("Papel"),
    TIJERA("Tijera"),
    VERSATIL("Versátil"),
    ;

    /** Piedra pierde contra papel, papel contra tijera y tijera contra piedra. */
    fun estaEnDesventaja(contra: TipoLuchador): Boolean = when (this) {
        VERSATIL -> false
        PIEDRA -> contra == PAPEL
        PAPEL -> contra == TIJERA
        TIJERA -> contra == PIEDRA
    }

    companion object {
        fun desdeClave(clave: String): TipoLuchador? = entries.firstOrNull { it.clave == clave }
    }
}

/**
 * En iOS: `Luchador`. Un legendario siempre es versátil: allá lo fuerza un `didSet`;
 * aquí [tipo] se calcula a partir de [tipoBase], así cualquier `copy` lo respeta.
 */
data class Luchador(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val nivel: Int,
    val puntosPoder: Int,
    val frase: String,
    val descripcion: String,
    val rareza: Rareza,
    val tipoBase: TipoLuchador,
    /** Skin elegida por el usuario. */
    val skinEquipada: String = SKIN_DEFAULT,
) {
    val tipo: TipoLuchador get() = if (rareza == Rareza.LEGENDARIO) TipoLuchador.VERSATIL else tipoBase

    /** Nombre del asset (de iOS) de la imagen: el nombre, o `nombre_skin`. */
    val imagen: String get() = if (skinEquipada == SKIN_DEFAULT) nombre else "${nombre}_$skinEquipada"

    fun sumarPuntosPoder(puntos: Int): Luchador = if (puntos <= 0) this else copy(puntosPoder = puntosPoder + puntos)

    companion object {
        const val SKIN_DEFAULT = "default"
    }
}

enum class EstadoNivel { ACTUAL, RECOMPENSA_RECOLECTADA, RECOMPENSA_PENDIENTE, BARRERA_PENDIENTE, BARRERA_DESBLOQUEADA, BLOQUEADO }

/** Puntos de poder que da un luchador repetido, según su rareza. */
object IncrementoNivelDuplicado {
    fun incremento(paraRareza: Rareza): Int = when (paraRareza) {
        Rareza.COMUN -> 2
        Rareza.RARO -> 10
        Rareza.EPICO -> 50
        Rareza.LEGENDARIO -> 250
    }
}

data class ResultadoRecoleccion(
    val luchadorId: Int,
    val nombre: String,
    val esDuplicado: Boolean,
    val puntosPoderAnterior: Int,
    val puntosPoderNuevo: Int,
    val nivelAnterior: Int,
    val nivelNuevo: Int,
    val rarezaAnterior: Rareza,
    val rarezaNueva: Rareza,
    val cantidadNueva: Int,
) {
    val nivelesGanados: Int get() = nivelNuevo - nivelAnterior
    val subioDeNivel: Boolean get() = nivelNuevo > nivelAnterior
    val subioDeRareza: Boolean get() = rarezaAnterior != rarezaNueva
}

data class ResultadoSubidaNivel(
    val luchadorId: Int,
    val puntosPoderAnterior: Int,
    val puntosPoderNuevo: Int,
    val nivelAnterior: Int,
    val nivelNuevo: Int,
    val rarezaAnterior: Rareza,
    val rarezaNueva: Rareza,
    val puntosGastados: Int,
) {
    val subioDeNivel: Boolean get() = nivelNuevo > nivelAnterior
    val subioDeRareza: Boolean get() = rarezaAnterior != rarezaNueva
}

/**
 * Curva de nivel de los luchadores. En iOS: `CalculadorDeNivel` / `NivelPoderCalculator`
 * (Domain/Services). Curva super-lineal suave: `puntosBase · (nivel−1)^exponente`.
 */
interface CalculadorDeNivel {
    fun puntosRequeridos(paraNivel: Int): Int
    fun nivelActual(paraPuntos: Int): Int
    fun rareza(paraNivel: Int): Rareza
    fun puntosParaSiguienteNivel(puntosActuales: Int): Int
    fun costoParaSubirA(nivel: Int): Int
    fun progreso(puntosActuales: Int): Double
}

class NivelPoderCalculator(
    /** Puntos para pasar del nivel 1 al 2. */
    private val puntosBase: Double = 25.0,
    /** 1.0 = lineal; 1.15–1.4 = cada nivel cuesta un poco más; 2.0+ ya se siente exponencial. */
    private val exponente: Double = 1.25,
) : CalculadorDeNivel {

    override fun puntosRequeridos(paraNivel: Int): Int {
        if (paraNivel <= 1) return 0
        return Math.round(puntosBase * Math.pow((paraNivel - 1).toDouble(), exponente)).toInt()
    }

    override fun nivelActual(paraPuntos: Int): Int {
        if (paraPuntos <= 0) return 1
        var nivel = 1
        // La curva es estrictamente creciente: basta avanzar mientras alcance.
        while (puntosRequeridos(nivel + 1) <= paraPuntos) nivel++
        return nivel
    }

    override fun rareza(paraNivel: Int): Rareza = when {
        paraNivel <= 9 -> Rareza.COMUN
        paraNivel <= 49 -> Rareza.RARO
        paraNivel <= 249 -> Rareza.EPICO
        else -> Rareza.LEGENDARIO
    }

    override fun puntosParaSiguienteNivel(puntosActuales: Int): Int {
        val siguiente = puntosRequeridos(nivelActual(puntosActuales) + 1)
        return maxOf(siguiente - puntosActuales, 0)
    }

    override fun costoParaSubirA(nivel: Int): Int =
        if (nivel <= 1) 0 else puntosRequeridos(nivel) - puntosRequeridos(nivel - 1)

    override fun progreso(puntosActuales: Int): Double {
        val nivel = nivelActual(puntosActuales)
        val actual = puntosRequeridos(nivel)
        val rango = (puntosRequeridos(nivel + 1) - actual).toDouble()
        if (rango <= 0) return 1.0
        return ((puntosActuales - actual) / rango).coerceIn(0.0, 1.0)
    }
}
