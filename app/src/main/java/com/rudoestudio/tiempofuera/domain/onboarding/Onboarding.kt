package com.rudoestudio.tiempofuera.domain.onboarding

import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
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
