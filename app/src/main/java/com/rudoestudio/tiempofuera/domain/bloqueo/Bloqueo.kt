package com.rudoestudio.tiempofuera.domain.bloqueo

import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// M3: el bloqueo de apps. En iOS lo aplica el sistema (FamilyControls, ManagedSettings,
// DeviceActivity) a partir de "shields" y eventos que la app programa; en Android lo
// aplica nuestro servicio. Por eso aquí el bloqueo es una **función**: dado el estado
// (selecciones, bloqueos de misión, tiempo extra, horario de sueño y uso del día), qué
// apps están bloqueadas ahora y por qué. El servicio la evalúa en cada cambio de ventana
// y cada pocos segundos; no hay alarmas ni eventos que se pierdan.

/**
 * Las apps de un grupo, por nombre de paquete. En iOS: `SeleccionAppsGrupo`, con tokens
 * opacos de `FamilyActivitySelection` (categorías y sitios web no existen en Android).
 */
data class SeleccionAppsGrupo(val grupoId: String, val paquetes: Set<String>) {
    val estaVacia: Boolean get() = paquetes.isEmpty()
}

/** Por qué está bloqueada una app. En iOS cada uno es un `ManagedSettingsStore` distinto. */
enum class MotivoBloqueo(val clave: String) {
    /** Una misión que pide no usar esas apps (store `mision.<grupo>`). */
    MISION("manual"),

    /** El horario de sueño (store `bloqueo`, actividad `bloqueo.distraccion`). */
    SUENO("programado"),

    /** Se acabó el tiempo máximo del día (store `tracking`). */
    LIMITE("limite"),
}

/** Un bloqueo de misión activo. `fin` nulo: hasta que la misión lo quite. */
data class BloqueoMision(val grupoId: String, val fin: Instant?)

/** Todo lo que el motor necesita para decidir. */
data class EstadoBloqueo(
    val selecciones: Map<String, SeleccionAppsGrupo> = emptyMap(),
    val configuraciones: Map<String, ConfiguracionGrupoApps> = emptyMap(),
    val misiones: Map<String, BloqueoMision> = emptyMap(),
    /** Fin del tiempo extra comprado, por grupo. */
    val tiemposExtra: Map<String, Instant> = emptyMap(),
    val sueno: ConfiguracionHabitoSueno? = null,
    /** Uso de hoy por paquete. */
    val usoHoy: Map<String, Duration> = emptyMap(),
)

data class GrupoBloqueado(val grupoId: String, val motivo: MotivoBloqueo)

object MotorBloqueo {

    /** Grupos bloqueados ahora. Si hay varios motivos gana el más fuerte: misión, sueño, límite. */
    fun gruposBloqueados(estado: EstadoBloqueo, ahora: Instant, zona: ZoneId): Map<String, MotivoBloqueo> {
        val hora = ahora.atZone(zona).toLocalTime()
        return estado.selecciones.values.filterNot { it.estaVacia }.mapNotNull { seleccion ->
            val grupo = seleccion.grupoId
            val motivo = when {
                estado.misiones[grupo]?.let { it.fin == null || it.fin.isAfter(ahora) } == true -> MotivoBloqueo.MISION
                grupo == GRUPO_SUENO && estado.sueno?.let { VentanaSueno.contiene(it, hora) } == true -> MotivoBloqueo.SUENO
                limiteAlcanzado(estado, grupo) && !tiempoExtraActivo(estado, grupo, ahora) -> MotivoBloqueo.LIMITE
                else -> null
            }
            motivo?.let { grupo to it }
        }.toMap()
    }

    /** Las apps bloqueadas ahora, con su grupo y motivo. Una app en dos grupos toma el motivo más fuerte. */
    fun paquetesBloqueados(estado: EstadoBloqueo, ahora: Instant, zona: ZoneId): Map<String, GrupoBloqueado> {
        val resultado = mutableMapOf<String, GrupoBloqueado>()
        gruposBloqueados(estado, ahora, zona).entries.sortedBy { it.value.ordinal }.forEach { (grupo, motivo) ->
            estado.selecciones[grupo]?.paquetes?.forEach { resultado.putIfAbsent(it, GrupoBloqueado(grupo, motivo)) }
        }
        return resultado
    }

    /** Uso de hoy sumando las apps del grupo. */
    fun usoDelGrupo(estado: EstadoBloqueo, grupoId: String): Duration =
        estado.selecciones[grupoId]?.paquetes.orEmpty().fold(Duration.ZERO) { total, paquete ->
            total + (estado.usoHoy[paquete] ?: Duration.ZERO)
        }

    fun limiteAlcanzado(estado: EstadoBloqueo, grupoId: String): Boolean {
        val maximo = estado.configuraciones[grupoId]?.tiempoMaximoDiario ?: return false
        return usoDelGrupo(estado, grupoId).seconds >= maximo
    }

    fun tiempoExtraActivo(estado: EstadoBloqueo, grupoId: String, ahora: Instant): Boolean =
        estado.tiemposExtra[grupoId]?.isAfter(ahora) == true

    /**
     * Cuántos hitos de uso lleva el grupo (un hito cada `intervaloHito`). En iOS son los
     * eventos `tracking.<grupo>.notificacion.<seg>.<i>` de DeviceActivity, que avisan con
     * una notificación; las educativas usan 5 min fijos.
     */
    fun hitosAlcanzados(uso: Duration, intervaloSegundos: Long): Long =
        if (intervaloSegundos <= 0) 0 else uso.seconds / intervaloSegundos

    /** Solo el grupo de distracción se bloquea de noche (en iOS: `SeguimientoSuenoService`). */
    val GRUPO_SUENO = TipoGrupoApps.DISTRACCION.clave
}

/**
 * El bloqueo nocturno empieza `colchonUso` minutos antes de dormir y termina
 * `colchonUso` minutos después de despertar. Las horas vienen en minutos desde la
 * medianoche. En iOS: `SeguimientoSuenoService.calcularVentanaBloqueo`.
 */
object VentanaSueno {
    private const val MINUTOS_DIA = 24 * 60

    fun inicio(config: ConfiguracionHabitoSueno): Int = normalizar(config.horaFinDia - config.colchonUso)

    fun fin(config: ConfiguracionHabitoSueno): Int = normalizar(config.horaInicioDia + config.colchonUso)

    fun contiene(config: ConfiguracionHabitoSueno, hora: LocalTime): Boolean {
        val minuto = hora.hour * 60 + hora.minute
        val inicio = inicio(config)
        val fin = fin(config)
        return when {
            inicio == fin -> false
            inicio < fin -> minuto in inicio until fin
            else -> minuto >= inicio || minuto < fin // cruza la medianoche
        }
    }

    private fun normalizar(minutos: Int) = ((minutos % MINUTOS_DIA) + MINUTOS_DIA) % MINUTOS_DIA
}

/**
 * Lo que va de hoy en un grupo: para las notificaciones de hitos, las misiones de
 * tracking y la tienda. En iOS: `ProgresoSeguimientoGrupo`. Un progreso de otro día no
 * cuenta.
 */
data class ProgresoSeguimientoGrupo(
    val fecha: LocalDate,
    val segundosAcumulados: Long = 0,
    val segundosExtraOtorgados: Long = 0,
    val limiteMaximoAlcanzado: Boolean = false,
    val ultimoUsoEn: Instant? = null,
    val ultimaRachaRecompensadaEn: Instant? = null,
    /** Vueltas ya cobradas por misión de tracking (id de misión → vueltas). */
    val unidadesTrackingReclamadas: Map<Int, Int> = emptyMap(),
    /**
     * En qué segundo de uso cayó el último hito avisado hoy. Se guarda el segundo y no
     * cuántos hitos, para que cambiar el intervalo no deje avisos sin mandar ni los repita.
     */
    val segundosUltimoHito: Long = 0,
)

/** Un rato con apps bloqueadas. */
data class PeriodoBloqueo(val inicio: Instant, val fin: Instant)

/**
 * Cuánto tiempo hubo apps bloqueadas, sin contar doble cuando se juntan varios motivos
 * (p. ej. límite y sueño a la vez). Se cuenta desde que se activa el primer motivo hasta
 * que se quita el último, y se parte en la medianoche. Guarda 30 días. En iOS:
 * `TiempoBloqueadoRepository` (allá la lógica vive en el repositorio).
 */
data class RegistroTiempoBloqueado(
    val periodosPorDia: Map<LocalDate, List<PeriodoBloqueo>> = emptyMap(),
    /** Motivos activos como "grupo.fuente". */
    val fuentesActivas: Set<String> = emptySet(),
    val inicioActivo: Instant? = null,
) {
    fun iniciar(grupoId: String, fuente: String, fecha: Instant): RegistroTiempoBloqueado {
        val nombre = "$grupoId.$fuente"
        if (nombre in fuentesActivas) return this
        return copy(fuentesActivas = fuentesActivas + nombre, inicioActivo = if (fuentesActivas.isEmpty()) fecha else inicioActivo)
    }

    fun finalizar(grupoId: String, fuente: String, fecha: Instant, zona: ZoneId): RegistroTiempoBloqueado {
        val nombre = "$grupoId.$fuente"
        if (nombre !in fuentesActivas) return this
        val restantes = fuentesActivas - nombre
        val inicio = inicioActivo
        if (restantes.isNotEmpty() || inicio == null) return copy(fuentesActivas = restantes)
        return copy(fuentesActivas = restantes, inicioActivo = null, periodosPorDia = agregarPeriodo(inicio, fecha, zona))
    }

    /**
     * Deja activos exactamente los motivos de [grupos] (grupo → motivo): termina los que ya
     * no están y empieza los nuevos. Así el registro se corrige solo aunque el servicio
     * haya estado muerto un rato.
     */
    fun sincronizar(grupos: Map<String, MotivoBloqueo>, fecha: Instant, zona: ZoneId): RegistroTiempoBloqueado {
        val activas = grupos.map { (grupo, motivo) -> "$grupo.${motivo.clave}" }.toSet()
        var registro = this
        (fuentesActivas - activas).forEach { nombre ->
            val (grupo, fuente) = nombre.split('.', limit = 2)
            registro = registro.finalizar(grupo, fuente, fecha, zona)
        }
        (activas - fuentesActivas).forEach { nombre ->
            val (grupo, fuente) = nombre.split('.', limit = 2)
            registro = registro.iniciar(grupo, fuente, fecha)
        }
        return registro
    }

    /** Segundos bloqueados en el día de [fecha], contando un bloqueo que siga activo y sin los [excluidos]. */
    fun segundosBloqueados(fecha: Instant, zona: ZoneId, excluidos: List<PeriodoBloqueo> = emptyList()): Long {
        val dia = fecha.atZone(zona).toLocalDate()
        val inicioDia = dia.atStartOfDay(zona).toInstant()
        val finDia = dia.plusDays(1).atStartOfDay(zona).toInstant()
        val periodos = periodosPorDia[dia].orEmpty() + listOfNotNull(inicioActivo?.let { PeriodoBloqueo(it, fecha) })
        return periodos.sumOf { contar(maxOf(it.inicio, inicioDia), minOf(it.fin, finDia), excluidos) }
    }

    private fun contar(inicio: Instant, fin: Instant, excluidos: List<PeriodoBloqueo>): Long {
        if (!fin.isAfter(inicio)) return 0
        var total = 0L
        var cursor = inicio
        excluidos
            .map { PeriodoBloqueo(maxOf(it.inicio, inicio), minOf(it.fin, fin)) }
            .filter { it.fin.isAfter(it.inicio) }
            .sortedBy { it.inicio }
            .forEach {
                if (it.inicio.isAfter(cursor)) total += Duration.between(cursor, it.inicio).seconds
                cursor = maxOf(cursor, it.fin)
            }
        if (fin.isAfter(cursor)) total += Duration.between(cursor, fin).seconds
        return total
    }

    private fun agregarPeriodo(inicio: Instant, fin: Instant, zona: ZoneId): Map<LocalDate, List<PeriodoBloqueo>> {
        if (!fin.isAfter(inicio)) return periodosPorDia
        val resultado = periodosPorDia.toMutableMap()
        var cursor = inicio
        while (cursor.isBefore(fin)) {
            val dia = cursor.atZone(zona).toLocalDate()
            val finSegmento = minOf(fin, dia.plusDays(1).atStartOfDay(zona).toInstant())
            resultado[dia] = resultado[dia].orEmpty() + PeriodoBloqueo(cursor, finSegmento)
            cursor = finSegmento
        }
        val retenerDesde = fin.atZone(zona).toLocalDate().minusDays(DIAS_RETENIDOS)
        return resultado.filterKeys { !it.isBefore(retenerDesde) }
    }

    companion object {
        const val DIAS_RETENIDOS = 30L
    }
}
