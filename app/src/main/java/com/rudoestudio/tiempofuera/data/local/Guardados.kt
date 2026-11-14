package com.rudoestudio.tiempofuera.data.local

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoMisionDiaria
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionTiempo
import com.rudoestudio.tiempofuera.domain.onboarding.PasoOnboarding
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoOnboarding
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.RegistroCarrera
import com.rudoestudio.tiempofuera.domain.perfil.RegistroLagartijas
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileBorrador
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// Cómo se guardan en JSON (DataStore y columnas de Room) los tipos del dominio, que no
// conocen kotlinx.serialization. Las fechas van en milisegundos o "yyyy-MM-dd".
// Cambiar un nombre aquí rompe lo que ya está guardado en los teléfonos.

@Serializable
data class ProgresoMisionGuardado(val misionId: Int, val fechaMs: Long, val rawProgreso: Double, val expObtenidaHoy: Double) {
    fun aDominio() = ProgresoMisionDiaria(misionId, Instant.ofEpochMilli(fechaMs), rawProgreso, expObtenidaHoy)

    companion object {
        fun de(p: ProgresoMisionDiaria) = ProgresoMisionGuardado(p.misionId, p.fecha.toEpochMilli(), p.rawProgreso, p.expObtenidaHoy)
    }
}

@Serializable
data class ProgresoLigaGuardado(
    val ligaId: Int,
    val totalExp: Double,
    val nivelActualId: Int,
    val nivelesDesbloqueados: Set<Int>,
    val progresoMisionesDiarias: List<ProgresoMisionGuardado> = emptyList(),
) {
    fun aDominio() = ProgresoUsuarioLiga(ligaId, totalExp, nivelActualId, nivelesDesbloqueados, progresoMisionesDiarias.map { it.aDominio() })

    companion object {
        fun de(p: ProgresoUsuarioLiga) =
            ProgresoLigaGuardado(p.ligaId, p.totalExp, p.nivelActualId, p.nivelesDesbloqueados, p.progresoMisionesDiarias.map(ProgresoMisionGuardado::de))
    }
}

/** En iOS: `SeedDiario` de `InUserDefaultsMisionesDiariasRepository`. */
@Serializable
data class MisionesAsignadasGuardadas(val fecha: String, val misionesIds: List<Int>) {
    val dia: LocalDate get() = LocalDate.parse(fecha)
}

@Serializable
data class ConfiguracionGrupoAppsGuardada(
    val tipo: String,
    val tiempoMaximoDiario: Long? = null,
    val intervaloHito: Long? = null,
    val tiempoRachaSinUsoParaRecompensa: Long? = null,
) {
    /** `null` si el tipo ya no existe en esta versión. */
    fun aDominio(): ConfiguracionGrupoApps? = TipoGrupoApps.desdeClave(tipo)?.let {
        ConfiguracionGrupoApps(it, tiempoMaximoDiario, intervaloHito, tiempoRachaSinUsoParaRecompensa)
    }

    companion object {
        fun de(c: ConfiguracionGrupoApps) =
            ConfiguracionGrupoAppsGuardada(c.tipo.clave, c.tiempoMaximoDiario, c.intervaloHito, c.tiempoRachaSinUsoParaRecompensa)
    }
}

@Serializable
data class ConfiguracionHabitoSuenoGuardada(val horaInicioDia: Int, val horaFinDia: Int, val colchonUso: Int) {
    fun aDominio() = ConfiguracionHabitoSueno(horaInicioDia, horaFinDia, colchonUso)

    companion object {
        fun de(c: ConfiguracionHabitoSueno) = ConfiguracionHabitoSuenoGuardada(c.horaInicioDia, c.horaFinDia, c.colchonUso)
    }
}

@Serializable
data class RegistroCarreraGuardado(val id: String, val fechaMs: Long, val distanciaMetros: Double, val velocidadPromedioKmh: Double) {
    fun aDominio() = RegistroCarrera(UUID.fromString(id), Instant.ofEpochMilli(fechaMs), distanciaMetros, velocidadPromedioKmh)

    companion object {
        fun de(r: RegistroCarrera) = RegistroCarreraGuardado(r.id.toString(), r.fecha.toEpochMilli(), r.distanciaMetros, r.velocidadPromedioKmh)
    }
}

@Serializable
data class RegistroLagartijasGuardado(val id: String, val fechaMs: Long, val cantidad: Int) {
    fun aDominio() = RegistroLagartijas(UUID.fromString(id), Instant.ofEpochMilli(fechaMs), cantidad)

    companion object {
        fun de(r: RegistroLagartijas) = RegistroLagartijasGuardado(r.id.toString(), r.fecha.toEpochMilli(), r.cantidad)
    }
}

/**
 * En iOS: `UserProfileBorrador` en UserDefaults (`onboarding.borrador.v1`) más el paso.
 * Los enums van por su clave (la del API); una clave que ya no exista toma el valor por
 * defecto del borrador.
 */
@Serializable
data class ProgresoOnboardingGuardado(
    val paso: String,
    val fechaNacimiento: String? = null,
    val email: String = "",
    val proposito: String? = null,
    val personalidad: String? = null,
    val divisiones: List<String> = emptyList(),
    val tiempoInicial: Int = 0,
    val tiempoMaxUso: Int = 0,
    val intervaloHitoUso: Int = 0,
    val horaInicioDia: Int = 0,
    val horaFinDia: Int = 0,
    val colchonInicioFin: Int = 0,
) {
    /** `null` si el paso ya no existe en esta versión. */
    fun aDominio(): ProgresoOnboarding? {
        val paso = PasoOnboarding.entries.firstOrNull { it.name == paso } ?: return null
        val defecto = UserProfileBorrador()
        val borrador = UserProfileBorrador(
            fechaNacimiento = fechaNacimiento?.let(LocalDate::parse),
            email = email,
            proposito = proposito?.let(PropositoUsuario::desdeClave) ?: defecto.proposito,
            personalidadId = personalidad?.let(TipoPersonalidad::desdeClave) ?: defecto.personalidadId,
            divisionesSeleccionadas = divisiones.mapNotNull(DivisionMision::desdeClave).toSet(),
            tiempoInicial = tiempoInicial,
            tiempoMaxUso = tiempoMaxUso,
            intervaloHitoUso = intervaloHitoUso,
            horaInicioDia = horaInicioDia,
            horaFinDia = horaFinDia,
            colchonInicioFin = colchonInicioFin,
        )
        return ProgresoOnboarding(paso, borrador)
    }

    companion object {
        fun de(p: ProgresoOnboarding) = with(p.borrador) {
            ProgresoOnboardingGuardado(
                paso = p.paso.name,
                fechaNacimiento = fechaNacimiento?.toString(),
                email = email,
                proposito = proposito.clave,
                personalidad = personalidadId.clave,
                divisiones = divisionesSeleccionadas.map { it.clave },
                tiempoInicial = tiempoInicial,
                tiempoMaxUso = tiempoMaxUso,
                intervaloHitoUso = intervaloHitoUso,
                horaInicioDia = horaInicioDia,
                horaFinDia = horaFinDia,
                colchonInicioFin = colchonInicioFin,
            )
        }
    }
}

/** En iOS: el estado de la Live Activity del temporizador (`LiveActivityService`). */
@Serializable
data class SesionTiempoGuardada(
    val misionId: Int,
    val inicioMs: Long,
    val finMs: Long? = null,
    val terminada: Boolean = false,
    val segundosAlTerminar: Int? = null,
) {
    fun aDominio() = SesionTiempo(misionId, Instant.ofEpochMilli(inicioMs), finMs?.let(Instant::ofEpochMilli), terminada, segundosAlTerminar)

    companion object {
        fun de(s: SesionTiempo) = SesionTiempoGuardada(s.misionId, s.inicio.toEpochMilli(), s.fin?.toEpochMilli(), s.terminada, s.segundosAlTerminar)
    }
}
