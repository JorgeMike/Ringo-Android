package com.rudoestudio.tiempofuera.core.preview

import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LIGAS
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_LUCHADORES
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_DIARIAS
import com.rudoestudio.tiempofuera.data.catalogo.CATALOGO_MISIONES_LIGA
import com.rudoestudio.tiempofuera.domain.ligas.Liga
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoUsuarioLiga
import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import java.time.Instant

/**
 * Datos de ejemplo para `@Preview` y la galería (M0.10). En iOS: `Core/Mocks`. Allá son
 * copias a mano del catálogo (y ya difieren: rarezas y niveles distintos); aquí salen del
 * catálogo real, así que no se desfasan. Solo para previews: el código de la app lee los
 * repositorios.
 */
object DatosPreview {
    private val luchadoresPorId = CATALOGO_LUCHADORES.associateBy { it.id }

    fun luchador(id: Int, cantidad: Int = 1): Luchador = luchadoresPorId.getValue(id).copy(cantidad = cantidad)

    /** El Serenísimo, como el perfil de ejemplo de iOS. */
    val luchadorSeleccionado: Luchador = CATALOGO_LUCHADORES.first { it.nombre.contains("Seren") }.copy(cantidad = 1, puntosPoder = 120)

    /** Todos los luchadores; los primeros 6 ya recolectados. */
    val luchadores: List<Luchador> = CATALOGO_LUCHADORES.map { if (it.id < 6) it.copy(cantidad = 1) else it }

    val misionesDiarias: List<Mision> = CATALOGO_MISIONES_DIARIAS.take(5)

    /** La primera misión del catálogo de ese tipo; `null` si no hay (p. ej. `LECTURAS`, deprecado en iOS). */
    fun mision(tipo: TipoMision): Mision? =
        (CATALOGO_MISIONES_DIARIAS + CATALOGO_MISIONES_LIGA).firstOrNull { it.tipoMision == tipo }

    val ligas: List<Liga> = CATALOGO_LIGAS.map { definicion ->
        val misiones = CATALOGO_MISIONES_LIGA.associateBy { it.id }
        Liga(
            id = definicion.id,
            nombre = definicion.nombre,
            luchadorEstrella = luchador(definicion.luchadorEstrellaId, cantidad = 0),
            misiones = definicion.misionIds.mapNotNull(misiones::get),
            luchadoresLiga = definicion.luchadoresLigaIds.map { luchador(it, cantidad = 0) },
        )
    }

    val progresoLiga = ProgresoUsuarioLiga(ligaId = 1, totalExp = 180.0, nivelActualId = 6, nivelesDesbloqueados = setOf(1, 2, 3, 4))

    /** Mismos valores que `UserProfile.mock` de iOS. */
    val perfil = UserProfile(
        email = "luchador@tiempofuera.app",
        personalidadId = TipoPersonalidad.EXPLORADOR,
        puntosPoder = 450,
        monedas = 120,
        diasRacha = 7,
        protectoresRacha = 1,
        lastActivityDate = Instant.parse("2026-11-13T18:00:00Z"),
        luchadorSeleccionado = luchadorSeleccionado,
        progresoLigas = listOf(progresoLiga),
        proposito = PropositoUsuario.DEJAR_CELULAR,
        divisionesSeleccionadas = setOf(DivisionMision.EJERCICIO, DivisionMision.LECTURA),
        tiempoInicial = 120,
        tiempoMaxUso = 180,
        horaInicioDia = 8,
        horaFinDia = 22,
        colchonInicioFin = 30,
    )
}
