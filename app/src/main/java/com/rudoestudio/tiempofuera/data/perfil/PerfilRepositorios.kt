package com.rudoestudio.tiempofuera.data.perfil

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.HistoricoRachaDao
import com.rudoestudio.tiempofuera.data.local.HistoricoRachaEntity
import com.rudoestudio.tiempofuera.data.local.LuchadorDao
import com.rudoestudio.tiempofuera.data.local.PerfilDao
import com.rudoestudio.tiempofuera.data.local.PerfilEntity
import com.rudoestudio.tiempofuera.data.local.ProgresoDiarioDao
import com.rudoestudio.tiempofuera.data.local.ProgresoDiarioEntity
import com.rudoestudio.tiempofuera.data.local.ProgresoLigaGuardado
import com.rudoestudio.tiempofuera.data.local.RegistroCarreraGuardado
import com.rudoestudio.tiempofuera.data.local.RegistroLagartijasGuardado
import com.rudoestudio.tiempofuera.data.luchadores.LuchadorRepositoryRoom
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.perfil.EstadisticasEjercicioRepository
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRacha
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRachaRepository
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiario
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiarioRepository
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.RegistroCarrera
import com.rudoestudio.tiempofuera.domain.perfil.RegistroLagartijas
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** En iOS: `InMemoryUserProfileRepository` (SwiftData). */
@Singleton
class UserProfileRepositoryRoom @Inject constructor(
    private val dao: PerfilDao,
    private val luchadorDao: LuchadorDao,
) : UserProfileRepository {

    override val perfil: Flow<UserProfile> = combine(dao.observar(), luchadorDao.observarTodos()) { perfil, progreso ->
        (perfil ?: PerfilEntity()).aDominio(LuchadorRepositoryRoom.combinarConCatalogo(progreso))
    }

    override suspend fun obtenerPerfil(): UserProfile {
        val entidad = dao.obtener() ?: PerfilEntity().also { dao.guardar(it) }
        return entidad.aDominio(LuchadorRepositoryRoom.combinarConCatalogo(luchadorDao.todos()))
    }

    override suspend fun guardarPerfil(perfil: UserProfile) {
        val luchadorId = perfil.luchadorSeleccionado.id.takeIf { id -> CATALOGO_LUCHADORES.any { it.id == id } }
        dao.guardar(
            PerfilEntity(
                email = perfil.email,
                personalidadId = perfil.personalidadId.clave,
                puntosPoder = perfil.puntosPoder,
                monedas = perfil.monedas,
                diasRacha = perfil.diasRacha,
                protectoresRacha = perfil.protectoresRacha,
                lastActivityDate = perfil.lastActivityDate,
                luchadorSeleccionadoId = luchadorId,
                progresoLigasJson = json.encodeToString(serializadorLigas, perfil.progresoLigas.map(ProgresoLigaGuardado::de)),
                proposito = perfil.proposito.clave,
                divisionesSeleccionadas = perfil.divisionesSeleccionadas.map { it.clave }.sorted(),
                tiempoInicial = perfil.tiempoInicial,
                tiempoMaxUso = perfil.tiempoMaxUso,
                horaInicioDia = perfil.horaInicioDia,
                horaFinDia = perfil.horaFinDia,
                colchonInicioFin = perfil.colchonInicioFin,
            ),
        )
    }

    private fun PerfilEntity.aDominio(luchadores: List<Luchador>): UserProfile {
        val porId = luchadores.associateBy { it.id }
        return UserProfile(
            email = email,
            // Vacíos hasta el onboarding; mismos valores por defecto que iOS.
            personalidadId = TipoPersonalidad.desdeClave(personalidadId) ?: TipoPersonalidad.DIPLOMATICO,
            puntosPoder = puntosPoder,
            monedas = monedas,
            diasRacha = diasRacha,
            protectoresRacha = protectoresRacha,
            lastActivityDate = lastActivityDate,
            luchadorSeleccionado = luchadorSeleccionadoId?.let(porId::get) ?: porId[ID_RINGO] ?: RINGO_RESPALDO,
            progresoLigas = json.decodeFromString(serializadorLigas, progresoLigasJson).map { it.aDominio() },
            proposito = PropositoUsuario.desdeClave(proposito) ?: PropositoUsuario.DEJAR_CELULAR,
            divisionesSeleccionadas = divisionesSeleccionadas.mapNotNull(DivisionMision::desdeClave).toSet(),
            tiempoInicial = tiempoInicial,
            tiempoMaxUso = tiempoMaxUso,
            horaInicioDia = horaInicioDia,
            horaFinDia = horaFinDia,
            colchonInicioFin = colchonInicioFin,
        )
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val serializadorLigas = ListSerializer(ProgresoLigaGuardado.serializer())

        /** Ringo: el luchador con el que arranca todo usuario. */
        const val ID_RINGO = 0
        val RINGO_RESPALDO = Luchador(
            id = ID_RINGO, nombre = "Ringo", cantidad = 1, nivel = 1, puntosPoder = 0,
            frase = "", descripcion = "", rareza = Rareza.COMUN, tipoBase = TipoLuchador.PIEDRA,
        )
    }
}

/** En iOS: `InMemoryProgresoDiarioRepository` (SwiftData). */
@Singleton
class ProgresoDiarioRepositoryRoom @Inject constructor(
    private val dao: ProgresoDiarioDao,
) : ProgresoDiarioRepository {

    override suspend fun obtenerProgreso(fecha: LocalDate): ProgresoDiario? = dao.porDia(fecha)?.let { ProgresoDiario(it.dia, it.misiones) }

    override suspend fun obtenerHistorico(): List<ProgresoDiario> = dao.todos().map { ProgresoDiario(it.dia, it.misiones) }

    override suspend fun guardarProgreso(progreso: ProgresoDiario) {
        dao.guardar(ProgresoDiarioEntity(progreso.dia, progreso.misiones))
    }
}

/** En iOS: `InMemoryHistoricoRachaRepository` (SwiftData). */
@Singleton
class HistoricoRachaRepositoryRoom @Inject constructor(
    private val dao: HistoricoRachaDao,
) : HistoricoRachaRepository {

    override suspend fun obtenerRegistro(fecha: LocalDate): HistoricoRacha? = dao.porFecha(fecha)?.aDominio()

    override suspend fun guardarRegistro(registro: HistoricoRacha) {
        dao.guardar(HistoricoRachaEntity(registro.fecha, registro.usoProtectorRacha))
    }

    override suspend fun obtenerTodosLosRegistros(): List<HistoricoRacha> = dao.todos().map { it.aDominio() }

    private fun HistoricoRachaEntity.aDominio() =
        if (usoProtectorRacha) HistoricoRacha.protegido(fecha) else HistoricoRacha.completado(fecha)
}

/** En iOS: `EstadisticasEjercicioRepository` (UserDefaults). */
@Singleton
class EstadisticasEjercicioDataStore @Inject constructor(
    private val almacen: AlmacenJson,
) : EstadisticasEjercicioRepository {
    private val carreras = ListSerializer(RegistroCarreraGuardado.serializer())
    private val lagartijas = ListSerializer(RegistroLagartijasGuardado.serializer())

    override suspend fun registrarCarrera(registro: RegistroCarrera) {
        almacen.actualizar(CLAVE_CARRERAS, carreras) { actuales ->
            topCarreras(actuales.orEmpty().map { it.aDominio() } + registro).map(RegistroCarreraGuardado::de)
        }
    }

    override suspend fun registrarLagartijas(registro: RegistroLagartijas) {
        almacen.actualizar(CLAVE_LAGARTIJAS, lagartijas) { actuales ->
            topLagartijas(actuales.orEmpty().map { it.aDominio() } + registro).map(RegistroLagartijasGuardado::de)
        }
    }

    override suspend fun obtenerTopCarreras(): List<RegistroCarrera> =
        topCarreras(almacen.leer(CLAVE_CARRERAS, carreras).orEmpty().map { it.aDominio() })

    override suspend fun obtenerTopLagartijas(): List<RegistroLagartijas> =
        topLagartijas(almacen.leer(CLAVE_LAGARTIJAS, lagartijas).orEmpty().map { it.aDominio() })

    private fun topCarreras(registros: List<RegistroCarrera>) = registros.sortedWith(
        compareByDescending<RegistroCarrera> { it.distanciaMetros }.thenByDescending { it.velocidadPromedioKmh }.thenByDescending { it.fecha },
    ).take(EstadisticasEjercicioRepository.TOP)

    private fun topLagartijas(registros: List<RegistroLagartijas>) = registros.sortedWith(
        compareByDescending<RegistroLagartijas> { it.cantidad }.thenByDescending { it.fecha },
    ).take(EstadisticasEjercicioRepository.TOP)

    private companion object {
        const val CLAVE_CARRERAS = "estadisticasEjercicio.carreras"
        const val CLAVE_LAGARTIJAS = "estadisticasEjercicio.lagartijas"
    }
}
