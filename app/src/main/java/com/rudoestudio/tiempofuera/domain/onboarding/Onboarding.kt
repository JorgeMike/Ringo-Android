package com.rudoestudio.tiempofuera.domain.onboarding

import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileBorrador
import java.time.Instant

// En iOS: Domain/Entities/Onboarding. `OnboardingBand` lleva colores: se porta con su
// pantalla (M4.7).

/** Los 16 pasos del onboarding, en orden (en iOS, `rawValue` 0…15). */
enum class PasoOnboarding {
    /** Registrarse o iniciar sesión. */
    BIENVENIDA,
    ESTADISTICA,
    PROBLEMA,
    AYUDA,

    /** Fecha de nacimiento y correo. */
    DATOS,
    META,

    /** Personalidad. */
    BANDO,
    DIVISIONES,

    /** Pantalla de descanso. */
    TRANQUILIZANTE,
    CONF_USO_INICIAL,
    CONF_PERMISOS,
    CONF_USO_MAX,

    /** Hora de inicio, fin y colchón de uso. */
    CONF_HABITO,

    /** El usuario firma su compromiso. */
    FIRMA,
    RECOMPENSA,

    /** Lleva a la primera liga. */
    HABITO_INICIAL,
    ;

    val siguiente: PasoOnboarding? get() = entries.getOrNull(ordinal + 1)
    val anterior: PasoOnboarding? get() = entries.getOrNull(ordinal - 1)

    val puedeRegresar: Boolean
        get() = this !in setOf(BIENVENIDA, META, CONF_USO_INICIAL, RECOMPENSA, HABITO_INICIAL)
}

data class EstadoOnboarding(val pasoActual: PasoOnboarding, val completado: Boolean)

/** Por dónde va el onboarding y lo que el usuario ya contestó. */
data class ProgresoOnboarding(val paso: PasoOnboarding, val borrador: UserProfileBorrador)

/** Reglas de los pasos del onboarding. En iOS viven sueltas en cada ViewModel. */
object ReglasOnboarding {
    /** Horas de uso al día que se pueden contestar (`ConfUsoInicialViewModel.rangoValido`). */
    val RANGO_HORAS_USO = 1..18
    const val HORAS_USO_INICIAL = 8

    /** Divisiones de misiones que hay que elegir (`DivisionesViewModel.cantidadRequerida`). */
    const val DIVISIONES_REQUERIDAS = 3

    /** Colchón antes y después del sueño, en minutos (`ConfHabitoViewModel`). */
    const val COLCHON_MINIMO = 20
    const val COLCHON_MAXIMO = 120

    /** Paso de los contadores de minutos (+/- 5). */
    const val PASO_MINUTOS = 5

    /** Límite diario sugerido: 30 min menos de lo que dijo que usa, mínimo 5 (`ConfUsoMaxViewModel`). */
    fun limiteSugerido(tiempoInicialMinutos: Int): Int = maxOf(tiempoInicialMinutos - 30, 5)

    /** Marca o desmarca [division]; no deja pasar de [DIVISIONES_REQUERIDAS]. */
    fun alternarDivision(seleccion: Set<DivisionMision>, division: DivisionMision): Set<DivisionMision> = when {
        division in seleccion -> seleccion - division
        seleccion.size < DIVISIONES_REQUERIDAS -> seleccion + division
        else -> seleccion
    }
}

/** Progreso de un luchador tal como lo guarda el API. */
data class LuchadorProgresoRemoto(
    val luchadorId: Int,
    val cantidad: Int,
    val nivel: Int,
    val puntosPoder: Int,
    val rareza: String,
    val skinEquipada: String,
    val desbloqueadoEn: Instant?,
)

/** Progreso de una liga tal como lo guarda el API. */
data class ProgresoLigaRemoto(
    val ligaId: Int,
    val totalExp: Double,
    val nivelActualId: Int,
    val nivelesDesbloqueados: Set<Int>,
)

/** Perfil que ya existía en el API al iniciar sesión (para precargar el onboarding). */
data class PerfilOnboardingExistente(
    val puntosPoder: Int,
    val monedas: Int,
    val diasRacha: Int,
    val protectoresRacha: Int,
    val lastActivityDate: Instant?,
    val luchadorEquipadoId: Int,
    val proposito: PropositoUsuario,
    val personalidadId: TipoPersonalidad,
    val divisionesSeleccionadas: Set<DivisionMision>,
    val tiempoInicial: Int,
    val horaInicioDia: Int,
    val horaFinDia: Int,
    val colchonUso: Int,
    val luchadores: List<LuchadorProgresoRemoto>,
    val ligas: List<ProgresoLigaRemoto>,
)
