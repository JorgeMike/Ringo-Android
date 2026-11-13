package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoLigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.luchadores.ErrorSubidaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.misiones.MisionesDiariasRepository
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRachaRepository
import com.rudoestudio.tiempofuera.domain.perfil.PerfilActualizado
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiario
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiarioRepository
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.servicios.CanalNotificacion
import com.rudoestudio.tiempofuera.domain.servicios.NotificationService
import com.rudoestudio.tiempofuera.domain.sincronizacion.ActualizacionPerfil
import com.rudoestudio.tiempofuera.domain.sincronizacion.ProgresoUsuarioRemoto
import com.rudoestudio.tiempofuera.domain.sincronizacion.SincronizacionProgreso
import com.rudoestudio.tiempofuera.domain.sincronizacion.UserApiRepository
import com.rudoestudio.tiempofuera.domain.tienda.TiempoExtraApps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Repositorios en memoria para probar los casos de uso sin Room ni red. Equivalen al
// `AppContainer.createPreviewContainer()` y a los mocks de Ringo-iOSTests.

val ZONA: ZoneId = ZoneId.of("America/Mexico_City")

/** Un reloj parado en [fecha] a la [hora] local. */
fun reloj(fecha: LocalDate = LocalDate.of(2026, 11, 13), hora: Int = 12, minuto: Int = 0): Clock =
    Clock.fixed(LocalDateTime.of(fecha, LocalTime.of(hora, minuto)).atZone(ZONA).toInstant(), ZONA)

fun perfilBase(luchador: Luchador = CATALOGO_LUCHADORES.first { it.id == 0 }) = UserProfile(
    email = "", personalidadId = TipoPersonalidad.DIPLOMATICO, puntosPoder = 0, monedas = 0, diasRacha = 0,
    luchadorSeleccionado = luchador, progresoLigas = emptyList(), proposito = PropositoUsuario.DEJAR_CELULAR,
    divisionesSeleccionadas = emptySet(), tiempoInicial = 0, tiempoMaxUso = 0, horaInicioDia = 0, horaFinDia = 0, colchonInicioFin = 0,
)

class PerfilesFake(inicial: UserProfile = perfilBase()) : UserProfileRepository {
    private val estado = MutableStateFlow(inicial)
    var actual: UserProfile
        get() = estado.value
        set(valor) { estado.value = valor }

    override val perfil: Flow<UserProfile> = estado

    override suspend fun obtenerPerfil(): UserProfile = estado.value

    override suspend fun guardarPerfil(perfil: UserProfile) {
        estado.value = perfil
    }
}

class HistoricoFake : HistoricoRachaRepository {
    val registros = mutableMapOf<LocalDate, HistoricoRacha>()

    override suspend fun obtenerRegistro(fecha: LocalDate) = registros[fecha]

    override suspend fun guardarRegistro(registro: HistoricoRacha) {
        registros[registro.fecha] = registro
    }

    override suspend fun obtenerTodosLosRegistros() = registros.values.sortedBy { it.fecha }
}

class NotificacionesFake : NotificationService {
    val canceladas = mutableListOf<String>()
    val mostradas = mutableListOf<Pair<String, String>>() // id, cuerpo

    override fun mostrar(id: String, titulo: String, cuerpo: String, canal: CanalNotificacion, deepLink: String?) {
        mostradas += id to cuerpo
    }

    override fun programarNotificacion(id: String, titulo: String, cuerpo: String, fecha: Instant) = Unit

    override fun cancelarNotificacion(id: String) {
        canceladas += id
    }
}

class SuenoFake(var configuracion: ConfiguracionHabitoSueno? = null) : ConfiguracionHabitoSuenoRepository {
    override suspend fun obtenerConfiguracionHabitoSueno() = configuracion

    override suspend fun guardarConfiguracionHabitoSueno(configuracion: ConfiguracionHabitoSueno) {
        this.configuracion = configuracion
    }

    override suspend fun eliminarConfiguracionHabitoSueno() {
        configuracion = null
    }
}

class ApiFake(var perfilRemoto: PerfilOnboardingExistente? = null) : UserApiRepository {
    var falla: Exception? = null
    val progresosGuardados = mutableListOf<ProgresoUsuarioRemoto>()
    val luchadoresActualizados = mutableListOf<Luchador>()

    private fun revisar() = falla?.let { throw it }

    override suspend fun obtenerPerfil(): PerfilOnboardingExistente {
        revisar()
        return perfilRemoto ?: throw IOException("sin perfil")
    }

    override suspend fun actualizarPerfil(cambios: ActualizacionPerfil): PerfilActualizado {
        revisar()
        return PerfilActualizado(
            cambios.proposito, cambios.personalidad, cambios.divisionesSeleccionadas, cambios.tiempoInicial,
            cambios.horaInicioDia, cambios.horaFinDia, cambios.colchonUso, cambios.onboardingCompletado,
        )
    }

    override suspend fun actualizarLuchador(luchador: Luchador) {
        revisar()
        luchadoresActualizados += luchador
    }

    override suspend fun guardarProgresoUsuario(progreso: ProgresoUsuarioRemoto) {
        revisar()
        progresosGuardados += progreso
    }

    override suspend fun sincronizar(sincronizacion: SincronizacionProgreso): SincronizacionProgreso {
        revisar()
        return sincronizacion
    }
}

/** Mismas reglas que `LuchadorRepositoryRoom`, sobre un mapa. */
class LuchadoresFake : LuchadorRepository {
    val luchadores = CATALOGO_LUCHADORES.associateBy { it.id }.toMutableMap()

    override suspend fun obtenerTodos() = luchadores.values.sortedBy { it.id }

    override suspend fun obtenerPorId(id: Int) = luchadores[id]

    override suspend fun obtenerInventario() = obtenerTodos().filter { it.cantidad > 0 }

    override suspend fun recolectarLuchador(luchadorId: Int, nuevoNivel: Int) {
        val l = luchadores[luchadorId] ?: return
        luchadores[luchadorId] = l.copy(cantidad = l.cantidad + 1, nivel = nuevoNivel, rareza = Rareza.paraNivel(nuevoNivel))
    }

    override suspend fun subirNivel(luchadorId: Int, nuevoNivel: Int) {
        val l = luchadores[luchadorId] ?: throw ErrorSubidaNivel.LuchadorNoEncontrado
        luchadores[luchadorId] = l.copy(nivel = nuevoNivel, rareza = Rareza.paraNivel(nuevoNivel))
    }

    override suspend fun sumarPuntosPoder(luchadorId: Int, puntos: Int) {
        if (puntos <= 0) return
        val l = luchadores[luchadorId] ?: throw ErrorSubidaNivel.LuchadorNoEncontrado
        luchadores[luchadorId] = l.sumarPuntosPoder(puntos)
    }

    override suspend fun sembrar(desde: List<LuchadorProgresoRemoto>) {
        desde.forEach { r ->
            val l = luchadores[r.luchadorId] ?: return@forEach
            luchadores[r.luchadorId] = l.copy(
                cantidad = r.cantidad, nivel = r.nivel, puntosPoder = r.puntosPoder,
                rareza = Rareza.desdeClave(r.rareza) ?: Rareza.paraNivel(r.nivel), skinEquipada = r.skinEquipada,
            )
        }
    }

    override suspend fun obtenerParaSincronizar() = obtenerInventario().map {
        LuchadorProgresoRemoto(it.id, it.cantidad, it.nivel, it.puntosPoder, it.rareza.clave, it.skinEquipada, null)
    }
}

class ProgresoLigasFake : ProgresoLigaRepository {
    val progresos = mutableMapOf<Int, ProgresoUsuarioLiga>()

    override suspend fun obtenerProgreso(ligaId: Int) = progresos[ligaId]

    override suspend fun guardarProgreso(progreso: ProgresoUsuarioLiga) {
        progresos[progreso.ligaId] = progreso.copy(progresoMisionesDiarias = emptyList())
    }

    override suspend fun sembrar(desde: List<ProgresoLigaRemoto>) {
        progresos.clear()
        desde.forEach { progresos[it.ligaId] = ProgresoUsuarioLiga(it.ligaId, it.totalExp, it.nivelActualId, it.nivelesDesbloqueados) }
    }

    override suspend fun obtenerParaSincronizar() =
        progresos.values.map { ProgresoLigaRemoto(it.ligaId, it.totalExp, it.nivelActualId, it.nivelesDesbloqueados) }
}

class ProgresoDiarioFake : ProgresoDiarioRepository {
    val dias = mutableMapOf<LocalDate, ProgresoDiario>()

    override suspend fun obtenerProgreso(fecha: LocalDate) = dias[fecha]

    override suspend fun obtenerHistorico() = dias.values.sortedBy { it.dia }

    override suspend fun guardarProgreso(progreso: ProgresoDiario) {
        dias[progreso.dia] = progreso
    }
}

class MisionesDiariasFake : MisionesDiariasRepository {
    var asignadas: Pair<LocalDate, List<Int>>? = null

    override suspend fun obtenerMisionesAsignadas(fecha: LocalDate) = asignadas?.takeIf { it.first == fecha }?.second

    override suspend fun sustituirMision(idMisionCompletada: Int, idMisionNueva: Int, fecha: LocalDate): List<Int>? {
        val actuales = obtenerMisionesAsignadas(fecha) ?: return null
        if (idMisionCompletada !in actuales) return null
        return actuales.map { if (it == idMisionCompletada) idMisionNueva else it }.also { asignadas = fecha to it }
    }

    override suspend fun guardarMisionesAsignadas(ids: List<Int>, fecha: LocalDate) {
        asignadas = fecha to ids
    }
}

class TiempoExtraFake(var activo: Boolean = false) : TiempoExtraApps {
    val otorgados = mutableListOf<Pair<String, Int>>()

    override suspend fun tieneTiempoExtraActivo(grupoId: String) = activo

    override suspend fun otorgarTiempoExtra(grupoId: String, segundos: Int) {
        otorgados += grupoId to segundos
    }
}
