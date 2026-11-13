package com.rudoestudio.tiempofuera.data.usuario

import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.data.red.ApiTiempoFuera
import com.rudoestudio.tiempofuera.data.red.LuchadorDeSincronizacionDto
import com.rudoestudio.tiempofuera.data.red.LuchadorRequestDto
import com.rudoestudio.tiempofuera.data.red.LuchadorResponseDto
import com.rudoestudio.tiempofuera.data.red.PerfilCompletoDto
import com.rudoestudio.tiempofuera.data.red.PerfilDto
import com.rudoestudio.tiempofuera.data.red.PerfilRequestDto
import com.rudoestudio.tiempofuera.data.red.ProgresoDeLigaDto
import com.rudoestudio.tiempofuera.data.red.ProgresoUsuarioDto
import com.rudoestudio.tiempofuera.data.red.SincronizacionDto
import com.rudoestudio.tiempofuera.data.red.mensajeApi
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.LuchadorProgresoRemoto
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilOnboardingExistente
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoLigaRemoto
import com.rudoestudio.tiempofuera.domain.perfil.PerfilActualizado
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.sincronizacion.ActualizacionPerfil
import com.rudoestudio.tiempofuera.domain.sincronizacion.ErrorApi
import com.rudoestudio.tiempofuera.domain.sincronizacion.ProgresoUsuarioRemoto
import com.rudoestudio.tiempofuera.domain.sincronizacion.SincronizacionProgreso
import com.rudoestudio.tiempofuera.domain.sincronizacion.UserApiRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject

/** En iOS: `UserAPIRepositoryImpl` y los `aDominio()` de `PerfilUsuarioDTO.swift`. */
class UserApiRepositoryImpl @Inject constructor(
    private val api: ApiTiempoFuera,
    private val json: Json,
) : UserApiRepository {

    override suspend fun obtenerPerfil(): PerfilOnboardingExistente =
        llamar("No se pudo obtener el perfil.") { api.perfilCompleto() }.aDominio()

    override suspend fun actualizarPerfil(cambios: ActualizacionPerfil): PerfilActualizado {
        val cuerpo = PerfilRequestDto(
            proposito = cambios.proposito.clave,
            personalidad = cambios.personalidad.clave,
            divisionesSeleccionadas = cambios.divisionesSeleccionadas.map { it.clave }.sorted(),
            tiempoInicial = cambios.tiempoInicial,
            horaInicioDia = cambios.horaInicioDia,
            horaFinDia = cambios.horaFinDia,
            colchonUso = cambios.colchonUso,
            onboardingCompletado = cambios.onboardingCompletado,
        )
        return llamar("No se pudo actualizar el perfil.", al400 = { ErrorApi.PerfilIncompleto(it ?: "Tu perfil quedó incompleto.") }) {
            api.actualizarPerfil(cuerpo)
        }.aDominio()
    }

    override suspend fun actualizarLuchador(luchador: Luchador) {
        val cuerpo = LuchadorRequestDto(luchador.nivel, luchador.puntosPoder, luchador.skinEquipada, luchador.cantidad, luchador.rareza.clave)
        llamar("No se pudo actualizar el luchador.") { api.actualizarLuchador(luchador.id, cuerpo) }
    }

    override suspend fun guardarProgresoUsuario(progreso: ProgresoUsuarioRemoto) {
        llamar("No se pudo guardar el progreso del usuario.") { api.guardarProgreso(progreso.aDto()) }
    }

    override suspend fun sincronizar(sincronizacion: SincronizacionProgreso): SincronizacionProgreso {
        val cuerpo = SincronizacionDto(
            ligas = sincronizacion.ligas.map { it.aDto() }.ifEmpty { null },
            // El API pide al menos un luchador en el bloque: sin ninguno, no se manda.
            luchadores = sincronizacion.luchadores.map {
                LuchadorDeSincronizacionDto(it.luchadorId, it.nivel, it.puntosPoder, it.skinEquipada, it.cantidad, it.rareza)
            }.ifEmpty { null },
            progreso = sincronizacion.progreso.aDto(),
        )
        val respuesta = llamar("No se pudo sincronizar el progreso.") { api.sincronizar(cuerpo) }
        // Un bloque que no regresa se toma tal como se mandó: si quedara vacío, al aplicarlo
        // se borrarían las ligas locales.
        return SincronizacionProgreso(
            ligas = respuesta.ligas?.map { it.aDominio() } ?: sincronizacion.ligas,
            luchadores = respuesta.luchadores?.map { it.aDominio() } ?: sincronizacion.luchadores,
            progreso = respuesta.progreso?.aDominio(respaldo = sincronizacion.progreso) ?: sincronizacion.progreso,
        )
    }

    private suspend fun <T> llamar(
        mensajePorDefecto: String,
        al400: ((String?) -> ErrorApi)? = null,
        bloque: suspend () -> T,
    ): T = try {
        bloque()
    } catch (e: HttpException) {
        val mensaje = e.mensajeApi(json)
        AppLogger.warning("API ${e.code()}: ${mensaje ?: e.message()}")
        throw when {
            e.code() == 401 -> ErrorApi.NoAutenticado()
            e.code() == 400 && al400 != null -> al400(mensaje)
            else -> ErrorApi.Desconocido(mensaje ?: mensajePorDefecto)
        }
    } catch (e: IOException) {
        AppLogger.warning("API sin conexión: ${e.message}")
        throw ErrorApi.SinConexion()
    } catch (e: SerializationException) {
        AppLogger.error(e, "UserApiRepository")
        throw ErrorApi.Desconocido("Error al procesar la respuesta del servidor.")
    }
}

// region Mapeo

private fun proposito(clave: String?) = clave?.let { PropositoUsuario.desdeClave(it) ?: throw ErrorApi.RespuestaDesconocida("propósito $it") }
    ?: PropositoUsuario.DEJAR_CELULAR

private fun personalidad(clave: String?) = clave?.let { TipoPersonalidad.desdeClave(it) ?: throw ErrorApi.RespuestaDesconocida("personalidad $it") }
    ?: TipoPersonalidad.DIPLOMATICO

private fun divisiones(claves: List<String>) =
    claves.map { DivisionMision.desdeClave(it) ?: throw ErrorApi.RespuestaDesconocida("división $it") }.toSet()

/** Instante ISO 8601 con o sin fracciones (en iOS: `FechaISO8601`). */
private fun instante(texto: String?): Instant? = texto?.let {
    try {
        Instant.parse(it)
    } catch (e: DateTimeParseException) {
        AppLogger.warning("Fecha del API ilegible: $it")
        null
    }
}

/**
 * Un perfil a medio onboarding trae nulos: se usan los mismos valores por defecto que el
 * perfil local. Un valor que no existe en esta versión sí es error, como en iOS.
 */
internal fun PerfilCompletoDto.aDominio() = PerfilOnboardingExistente(
    puntosPoder = puntosPoder,
    monedas = monedas,
    diasRacha = diasRacha,
    protectoresRacha = protectoresRacha,
    lastActivityDate = instante(lastActivityDate),
    luchadorEquipadoId = luchadorId ?: 0,
    proposito = proposito(proposito),
    personalidadId = personalidad(personalidad),
    divisionesSeleccionadas = divisiones(divisionesSeleccionadas),
    tiempoInicial = tiempoInicial ?: 0,
    horaInicioDia = horaInicioDia ?: 0,
    horaFinDia = horaFinDia ?: 0,
    colchonUso = colchonUso ?: 0,
    luchadores = luchadores.map {
        LuchadorProgresoRemoto(it.luchadorId, it.cantidad, it.nivel, it.puntosPoder, it.rareza, it.skinEquipada, instante(it.desbloqueadoEn))
    },
    ligas = ligas.map { it.aDominio() },
)

private fun PerfilDto.aDominio() = PerfilActualizado(
    proposito = proposito(proposito),
    personalidadId = personalidad(personalidad),
    divisionesSeleccionadas = divisiones(divisionesSeleccionadas),
    tiempoInicial = tiempoInicial ?: 0,
    horaInicioDia = horaInicioDia ?: 0,
    horaFinDia = horaFinDia ?: 0,
    colchonUso = colchonUso ?: 0,
    onboardingCompletado = onboardingCompletado,
)

private fun ProgresoDeLigaDto.aDominio() = ProgresoLigaRemoto(ligaId, totalExp, nivelActualId, nivelesDesbloqueados.toSet())

private fun ProgresoLigaRemoto.aDto() = ProgresoDeLigaDto(ligaId, totalExp, nivelActualId, nivelesDesbloqueados.sorted())

private fun LuchadorResponseDto.aDominio() =
    LuchadorProgresoRemoto(luchadorId, cantidad, nivel, puntosPoder, rareza, skinEquipada, instante(desbloqueadoEn))

private fun ProgresoUsuarioRemoto.aDto() =
    ProgresoUsuarioDto(luchadorId, puntosPoder, monedas, diasRacha, protectoresRacha, lastActivityDate?.toString())

/** El API puede regresar `luchadorId` nulo (usuario sin luchador): se queda el que se mandó. */
private fun ProgresoUsuarioDto.aDominio(respaldo: ProgresoUsuarioRemoto) = ProgresoUsuarioRemoto(
    luchadorId = luchadorId ?: respaldo.luchadorId,
    puntosPoder = puntosPoder,
    monedas = monedas,
    diasRacha = diasRacha,
    protectoresRacha = protectoresRacha,
    lastActivityDate = instante(lastActivityDate),
)

// endregion
