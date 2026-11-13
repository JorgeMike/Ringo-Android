package com.rudoestudio.tiempofuera.domain.misiones

import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.perfil.ResultadoActivarRacha
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad

// En iOS: Domain/Entities/Misiones.

enum class TipoMision(val clave: String, val titulo: String) {
    TIEMPO("tiempo", "Temporizador"),
    MEDITACION("meditacion", "Meditación"),
    ACCIONES("acciones", "Contador de Acciones"),
    LECTURAS("lecturas", "Comprensión Lectora"),
    SECUENCIA("secuencia", "Memorizar Secuencia"),
    ESCENA("escena", "Memorizar Escena"),
    TOMAR_FOTO("tomarFoto", "Tomar Foto por Color"),
    LAGARTIJAS("lagartijas", "Lagartijas (Sensor Proximidad)"),
    TRACKING("tracking", "Seguimiento de Apps"),
    CARRERA("carrera", "Carrera"),
    ;

    companion object {
        fun desdeClave(clave: String): TipoMision? = entries.firstOrNull { it.clave == clave }
    }
}

enum class DivisionMision(val clave: String, val titulo: String, val descripcion: String, val nombreIcono: String) {
    EJERCICIO("ejercicio", "Ejercicio", "Realizar ejercicios físicos", "EjercicioIcon"),
    BIENESTAR_MENTAL("bienestarMental", "Bienestar mental", "Practica técnicas de relajación", "MeditacionIcon"),
    CONECTAR("conectar", "Conectar", "Conectar con las demás personas", "ConexionIcon"),
    LECTURA("lectura", "Lectura", "Fomentar la lectura, aprendizaje, etc", "LecturaIcon"),
    DEJAR_CELULAR("dejarCelular", "Dejar el celular", "Cosas para usar menos el teléfono", "DejarCelIcon"),
    JUEGOS_MENTALES("juegosMentales", "Juegos mentales", "Juegos pequeños para ayudar a la mente", "RetosIcon"),
    PROGRESO_PERSONAL("progresoPersonal", "Progreso personal", "Avanzar en tus proyectos personales", "ProgresoIcon"),
    ;

    companion object {
        fun desdeClave(clave: String): DivisionMision? = entries.firstOrNull { it.clave == clave }
    }
}

/** Parámetros de cada tipo de misión; solo se llenan los del tipo que aplica. */
data class ConfiguracionMision(
    // .tiempo
    val duracionSegundos: Int? = null,
    val segundosParaRecompensa: Int? = null,
    // .acciones
    val cantidadObjetivo: Int? = null,
    // .lecturas (en iOS marcados "DEPRECADO, POR ELIMINAR"; se portan mientras la semilla los use)
    val textoLectura: String? = null,
    val preguntas: List<PreguntaLectura>? = null,
    // .secuencia
    val numSecuencias: Int? = null,
    val numTambores: Int? = null,
    val secuencias: List<Secuencia>? = null, // deprecado en iOS
    // .escena
    val elementosEscena: List<ElementoEnEscena>? = null,
    val tiempoVisualizacionSegundos: Int? = null, // deprecado en iOS
    // .tomarFoto
    val colorObjetivo: String? = null,
    // .lagartijas
    val repeticionesObjetivo: Int? = null,
    val umbralCercaniaCM: Double? = null,
    // .carrera
    val isTracking: Boolean? = null,
    val distanciaObjetivoMetros: Double? = null,
)

data class PreguntaLectura(val pregunta: String, val opciones: List<String>, val respuestaCorrectaIndex: Int)

data class Secuencia(val secuencia: List<String>, val numeroTambores: Int, val tiempoPorSecuencia: Int)

data class ElementoEnEscena(val idElemento: Int, val imagen: String, val posicionX: Double, val posicionY: Double)

data class RecompensaMision(
    val exp: Int? = null,
    val puntosPoder: Int? = null,
    val monedas: Int? = null,
    val protectorRacha: Int? = null,
) {
    val tieneExp: Boolean get() = (exp ?: 0) > 0
    val tienePuntosPoder: Boolean get() = (puntosPoder ?: 0) > 0
    val tieneMonedas: Boolean get() = (monedas ?: 0) > 0
    val tieneProtectorRacha: Boolean get() = (protectorRacha ?: 0) > 0

    /** Para misiones por "vueltas": 0 vueltas no regala nada; el protector no se multiplica. */
    fun multiplicada(por: Int): RecompensaMision {
        val factor = maxOf(por, 0)
        return RecompensaMision(
            exp = exp?.times(factor),
            puntosPoder = puntosPoder?.times(factor),
            monedas = monedas?.times(factor),
            protectorRacha = if (factor > 0) protectorRacha else null,
        )
    }
}

/** En iOS dos misiones son iguales si tienen el mismo id, sin importar lo demás. */
data class Mision(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val recompensa: RecompensaMision,
    /** La personalidad a la que va dirigida, con la clave de [TipoPersonalidad]. */
    val categoria: String,
    val configuracion: ConfiguracionMision,
    val tipoMision: TipoMision? = null,
    val division: DivisionMision? = null,
    val ligaId: Int? = null,
    val completada: Boolean = false,
    /** Solo para `.tracking`: el grupo de apps que mide. */
    val identificadorGrupoApps: TipoGrupoApps? = null,
) {
    val tipoPersonalidad: TipoPersonalidad? get() = TipoPersonalidad.desdeClave(categoria)

    fun completar(): Mision = copy(completada = true)

    override fun equals(other: Any?): Boolean = other is Mision && other.id == id

    override fun hashCode(): Int = id
}

data class ResultadoCompletarMision(
    val misionCompletada: Mision,
    val misionReemplazo: Mision?,
    val racha: ResultadoActivarRacha,
    val recompensaOtorgada: RecompensaMision,
    val progresoLigaActualizado: ProgresoUsuarioLiga? = null,
)

data class RecompensaMisionPendiente(val racha: ResultadoActivarRacha, val recompensaMision: RecompensaMision)
