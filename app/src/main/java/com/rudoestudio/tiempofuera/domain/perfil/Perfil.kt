package com.rudoestudio.tiempofuera.domain.perfil

import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

// En iOS: Domain/Entities/UserProfile. Los métodos `mutating` de iOS aquí regresan una
// copia nueva; `ahora` se recibe como parámetro para poder probar sin depender del reloj.

enum class PropositoUsuario(val clave: String, val titulo: String) {
    DEJAR_CELULAR("dejarCelular", "Dejar el celular"),
    MEJORAR_MENTAL("mejorarMental", "Mejorar mi salud mental"),
    CUIDAR_CUERPO("cuidarCuerpo", "Cuidar mi cuerpo"),
    RECONECTAR_GENTE("reconectarGente", "Reconectar con la gente"),
    ;

    companion object {
        fun desdeClave(clave: String): PropositoUsuario? = entries.firstOrNull { it.clave == clave }
    }
}

/** El "bando" del onboarding. La clave es la que usa el API. */
enum class TipoPersonalidad(val clave: String, val nombreVisible: String, val descripcion: String) {
    ANALISTA(
        "Analíticos", "Lxs Analístas",
        "Te mueves por la lógica, los retos mentales y la eficiencia. Buscas optimizar tu tiempo y resolver problemas estratégicos.",
    ),
    DIPLOMATICO(
        "Diplomáticos", "Lxs Diplomáticos",
        "Te mueves por la empatía, la conexión y el propósito. Buscas introspección, autenticidad y relaciones significativas.",
    ),
    CENTINELA(
        "Centinelas", "Lxs Centinelas",
        "Te mueves por la estructura, el orden y los buenos hábitos. Valoras la constancia, la disciplina y el cumplimiento de tus metas.",
    ),
    EXPLORADOR(
        "Exploradores", "Lxs Exploradores",
        "Te mueves por la acción, la espontaneidad y los estímulos reales. Prefieres retos físicos, dinamismo y reaccionar al momento.",
    ),
    ;

    companion object {
        fun desdeClave(clave: String): TipoPersonalidad? = entries.firstOrNull { it.clave == clave }
    }
}

data class UserProfile(
    val email: String,
    val personalidadId: TipoPersonalidad,
    val puntosPoder: Int,
    val monedas: Int,
    val diasRacha: Int,
    val protectoresRacha: Int = 0,
    val lastActivityDate: Instant? = null,
    val luchadorSeleccionado: Luchador,
    val progresoLigas: List<ProgresoUsuarioLiga>,
    val proposito: PropositoUsuario,
    val divisionesSeleccionadas: Set<DivisionMision>,
    val tiempoInicial: Int,
    val tiempoMaxUso: Int,
    val horaInicioDia: Int,
    val horaFinDia: Int,
    val colchonInicioFin: Int,
) {
    val puedeAgregarProtector: Boolean get() = protectoresRacha < MAX_PROTECTORES_RACHA

    /** Suma protectores sin pasar del máximo (3). */
    fun sumarProtectorRacha(cantidad: Int = 1, ahora: Instant = Instant.now()): UserProfile =
        if (cantidad <= 0) this
        else copy(protectoresRacha = minOf(MAX_PROTECTORES_RACHA, protectoresRacha + cantidad), lastActivityDate = ahora)

    fun gastarProtectorRacha(cantidad: Int = 1): UserProfile =
        if (cantidad <= 0) this else copy(protectoresRacha = maxOf(0, protectoresRacha - cantidad))

    /** Usa un protector; `null` si no había (en iOS regresa `false`). */
    fun usarProtectorRacha(ahora: Instant = Instant.now()): UserProfile? =
        if (protectoresRacha <= 0) null else copy(protectoresRacha = protectoresRacha - 1, lastActivityDate = ahora)

    fun sumarPuntosPoder(puntos: Int, ahora: Instant = Instant.now()): UserProfile =
        if (puntos <= 0) this else copy(puntosPoder = puntosPoder + puntos, lastActivityDate = ahora)

    fun sumarPuntosPoderALuchadorSeleccionado(puntos: Int, ahora: Instant = Instant.now()): UserProfile =
        if (puntos <= 0) this
        else copy(luchadorSeleccionado = luchadorSeleccionado.sumarPuntosPoder(puntos), lastActivityDate = ahora)

    fun gastarPuntosPoder(puntos: Int, ahora: Instant = Instant.now()): UserProfile =
        if (puntos <= 0) this else copy(puntosPoder = maxOf(0, puntosPoder - puntos), lastActivityDate = ahora)

    fun sumarMonedas(cantidad: Int, ahora: Instant = Instant.now()): UserProfile =
        if (cantidad <= 0) this else copy(monedas = monedas + cantidad, lastActivityDate = ahora)

    fun gastarMonedas(cantidad: Int, ahora: Instant = Instant.now()): UserProfile =
        if (cantidad <= 0) this else copy(monedas = maxOf(0, monedas - cantidad), lastActivityDate = ahora)

    fun tieneMonedasSuficientes(cantidad: Int): Boolean = monedas >= cantidad

    /** Protector, poder y monedas de la recompensa (la experiencia va a la liga, no aquí). */
    fun agregarRecompensa(recompensa: RecompensaMision, ahora: Instant = Instant.now()): UserProfile {
        var perfil = this
        if (recompensa.tieneProtectorRacha) perfil = perfil.sumarProtectorRacha(recompensa.protectorRacha ?: 0, ahora)
        if (recompensa.tienePuntosPoder) perfil = perfil.sumarPuntosPoder(recompensa.puntosPoder ?: 0, ahora)
        if (recompensa.tieneMonedas) perfil = perfil.sumarMonedas(recompensa.monedas ?: 0, ahora)
        return perfil.copy(lastActivityDate = ahora)
    }

    fun sumarRacha(ahora: Instant = Instant.now()): UserProfile = copy(diasRacha = diasRacha + 1, lastActivityDate = ahora)

    /** Se perdió un día: un protector salva la racha; sin protector, vuelve a 0. */
    fun rachaPerdida(ahora: Instant = Instant.now()): UserProfile = usarProtectorRacha(ahora) ?: copy(diasRacha = 0)

    companion object {
        const val MAX_PROTECTORES_RACHA = 3
    }
}

object RachaVigencia {
    /**
     * La racha sigue viva si la última actividad fue hoy o ayer; si no, vale 0.
     * En iOS: `RachaVigencia.diasVigentes`.
     */
    fun diasVigentes(
        dias: Int,
        ultimaActividad: Instant?,
        ahora: Instant = Instant.now(),
        zona: ZoneId = ZoneId.systemDefault(),
    ): Int {
        if (dias <= 0 || ultimaActividad == null) return 0
        val transcurridos = ChronoUnit.DAYS.between(ultimaActividad.atZone(zona).toLocalDate(), ahora.atZone(zona).toLocalDate())
        return if (transcurridos in 0..1) dias else 0
    }
}

data class ResultadoActivarRacha(
    /** `true` solo si hoy se activó por primera vez (dispara la pantalla RachaActivada). */
    val activada: Boolean,
    val diasDeRacha: Int,
)

/** Misiones completadas en un día. */
data class ProgresoDiario(val dia: LocalDate, val misiones: List<Int>) {
    val misionesCompletadasCount: Int get() = misiones.size

    fun estaCompletada(misionId: Int): Boolean = misionId in misiones
}

/** Un día del histórico de racha. Solo se crea con [completado] o [protegido]. */
data class HistoricoRacha private constructor(val fecha: LocalDate, val usoProtectorRacha: Boolean) {
    companion object {
        fun completado(fecha: LocalDate) = HistoricoRacha(fecha, usoProtectorRacha = false)

        fun protegido(fecha: LocalDate) = HistoricoRacha(fecha, usoProtectorRacha = true)
    }
}

data class PerfilActualizado(
    val proposito: PropositoUsuario,
    val personalidadId: TipoPersonalidad,
    val divisionesSeleccionadas: Set<DivisionMision>,
    val tiempoInicial: Int,
    val horaInicioDia: Int,
    val horaFinDia: Int,
    val colchonUso: Int,
    val onboardingCompletado: Boolean,
)

/** Lo que se va llenando durante el onboarding, antes de crear el perfil. */
data class UserProfileBorrador(
    val fechaNacimiento: LocalDate? = null,
    val email: String = "",
    val proposito: PropositoUsuario = PropositoUsuario.DEJAR_CELULAR,
    val personalidadId: TipoPersonalidad = TipoPersonalidad.DIPLOMATICO,
    val divisionesSeleccionadas: Set<DivisionMision> = emptySet(),
    val tiempoInicial: Int = 0,
    val tiempoMaxUso: Int = 0,
    val intervaloHitoUso: Int = 0,
    val horaInicioDia: Int = 0,
    val horaFinDia: Int = 0,
    val colchonInicioFin: Int = 0,
) {
    /** Rellena lo que ya existe en la nube para que el usuario vea sus elecciones previas. */
    fun precargar(desde: PerfilOnboardingExistente) = copy(
        proposito = desde.proposito,
        personalidadId = desde.personalidadId,
        divisionesSeleccionadas = desde.divisionesSeleccionadas,
        tiempoInicial = desde.tiempoInicial,
        horaInicioDia = desde.horaInicioDia,
        horaFinDia = desde.horaFinDia,
        colchonInicioFin = desde.colchonUso,
    )
}

data class RegistroCarrera(
    val id: UUID = UUID.randomUUID(),
    val fecha: Instant,
    val distanciaMetros: Double,
    val velocidadPromedioKmh: Double,
)

data class RegistroLagartijas(val id: UUID = UUID.randomUUID(), val fecha: Instant, val cantidad: Int)

/** Recordatorios de racha: tres al día, una semana adelante (en iOS, por el límite de notificaciones). */
object RecordatorioRacha {
    val horas = listOf(10, 14, 18)
    const val DIAS_PROGRAMADOS = 7

    fun identificador(fecha: LocalDate, hora: Int): String =
        "racha.recordatorio.%04d%02d%02d.%02d".format(fecha.year, fecha.monthValue, fecha.dayOfMonth, hora)

    fun fecha(dia: LocalDate, hora: Int): LocalDateTime = dia.atTime(hora, 0)

    /** En iOS: "Activa tu racha de hoy". */
    const val TITULO = "Activa tu racha de hoy"

    /** En iOS siempre es "Completa una misión para mantener tu racha."; aquí sube el tono con la hora. */
    fun cuerpo(hora: Int): String = when {
        hora < 12 -> "Échate una misión y arranca el día con todo."
        hora < 17 -> "Una misión y tu racha sigue viva. Tú puedes, campeón."
        else -> "¡Se te va el día! Completa una misión para no perder tu racha."
    }
}
