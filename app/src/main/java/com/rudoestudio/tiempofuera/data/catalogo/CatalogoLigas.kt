package com.rudoestudio.tiempofuera.data.catalogo

import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.ElementoEnEscena
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.misiones.Secuencia
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision

// GENERADO por scripts/importar_semillas_ios.py desde iOS (LigasSeedData).
// No editar a mano: corregir en iOS y volver a correr el script.

/** Una liga antes de resolver sus luchadores y misiones (en iOS: `LigaModel`). */
data class LigaDefinicion(
    val id: Int,
    val nombre: String,
    val luchadorEstrellaId: Int,
    val misionIds: List<Int>,
    /** Puede repetir ids: un luchador repetido sale más en los sobres de la liga. */
    val luchadoresLigaIds: List<Int>,
)

/** Misiones de liga (ids 101–499): se repiten diario y dan experiencia. */
val CATALOGO_MISIONES_LIGA: List<Mision> = listOf(
    Mision(id = 101, titulo = "Lectura enfocada", descripcion = "Gana 10 de experiencia por cada vuelta de 5 minutos de lectura enfocada.", recompensa = RecompensaMision(exp = 10), categoria = "Centinelas", configuracion = ConfiguracionMision(segundosParaRecompensa = 300), tipoMision = TipoMision.TIEMPO, ligaId = 1),
    Mision(id = 102, titulo = "Rastreo educativo", descripcion = "Gana 10 de experiencia por cada 5 minutos de uso en tus apps educativas.", recompensa = RecompensaMision(exp = 10), categoria = "Centinelas", configuracion = ConfiguracionMision(segundosParaRecompensa = 300), tipoMision = TipoMision.TRACKING, ligaId = 1, identificadorGrupoApps = TipoGrupoApps.EDUCATIVAS),
    Mision(id = 201, titulo = "Entrenamiento con El Serenísimo", descripcion = "Gana 15 de experiencia por cada 15 minutos entrenando con El Serenísimo", recompensa = RecompensaMision(exp = 15), categoria = "Centinelas", configuracion = ConfiguracionMision(segundosParaRecompensa = 900), tipoMision = TipoMision.TIEMPO, ligaId = 2),
    Mision(id = 203, titulo = "Desintoxicación exprés", descripcion = "Gana 10 de experiencia por realizar una pausa digital de 5 minutos.", recompensa = RecompensaMision(exp = 10), categoria = "Centinelas", configuracion = ConfiguracionMision(duracionSegundos = 300), tipoMision = TipoMision.TIEMPO, ligaId = 2),
    Mision(id = 204, titulo = "Dominio de pantalla", descripcion = "Gana 50 de experiencia por cada hora continua sin abrir las aplicaciones de distracción configuradas.", recompensa = RecompensaMision(exp = 50), categoria = "Centinelas", configuracion = ConfiguracionMision(segundosParaRecompensa = 3600), tipoMision = TipoMision.TRACKING, ligaId = 2, identificadorGrupoApps = TipoGrupoApps.DISTRACCION),
    Mision(id = 301, titulo = "Respira con Katita", descripcion = "Gana 15 de experiencia por cada vuelta de 5 minutos de meditación con Katita. Termina cuando quieras.", recompensa = RecompensaMision(exp = 15), categoria = "Diplomáticos", configuracion = ConfiguracionMision(segundosParaRecompensa = 300), tipoMision = TipoMision.MEDITACION, ligaId = 3),
    Mision(id = 302, titulo = "Es bueno aburrirse", descripcion = "Gana 20 de experiencia por cada 5 minutos sin hacer nada. Así es, NA-DA.", recompensa = RecompensaMision(exp = 20), categoria = "Diplomáticos", configuracion = ConfiguracionMision(segundosParaRecompensa = 300), tipoMision = TipoMision.TIEMPO, ligaId = 3),
    Mision(id = 303, titulo = "Querido diario...", descripcion = "Gana 15 de experiencia escribiendo por 5 minutos en tu diario.", recompensa = RecompensaMision(exp = 15), categoria = "Diplomáticos", configuracion = ConfiguracionMision(duracionSegundos = 300), tipoMision = TipoMision.TIEMPO, ligaId = 3),
    Mision(id = 404, titulo = "Trote de calentamiento", descripcion = "Corre al menos 1 km y registra tu recorrido con GPS.", recompensa = RecompensaMision(exp = 35), categoria = "Analíticos", configuracion = ConfiguracionMision(distanciaObjetivoMetros = 1_000.0), tipoMision = TipoMision.CARRERA, ligaId = 4),
    Mision(id = 402, titulo = "Entrenamiento con TitoRito", descripcion = "Gana 10 de experiencia por cada lagartija que logres completar (puedes apoyarte en las rodillas si lo necesitas)", recompensa = RecompensaMision(exp = 10), categoria = "Analíticos", configuracion = ConfiguracionMision(duracionSegundos = 60, umbralCercaniaCM = 15.0), tipoMision = TipoMision.LAGARTIJAS, ligaId = 4),
    Mision(id = 403, titulo = "Entrenamiento temporizado", descripcion = "Gana 50 de experiencia por cada 45 minutos continuos de entrenamiento.", recompensa = RecompensaMision(exp = 50), categoria = "Analíticos", configuracion = ConfiguracionMision(segundosParaRecompensa = 2700), tipoMision = TipoMision.TIEMPO, ligaId = 4),
)

val CATALOGO_LIGAS: List<LigaDefinicion> = listOf(
    LigaDefinicion(
        id = 1,
        nombre = "La llave del Saber",
        luchadorEstrellaId = 12,
        misionIds = listOf(101, 102),
        luchadoresLigaIds = listOf(9, 9, 10, 11),
    ),
    LigaDefinicion(
        id = 2,
        nombre = "Fuera de las cuerdas",
        luchadorEstrellaId = 16,
        misionIds = listOf(201, 203, 204),
        luchadoresLigaIds = listOf(13, 13, 14, 3),
    ),
    LigaDefinicion(
        id = 3,
        nombre = "Alianza del Ring",
        luchadorEstrellaId = 3,
        misionIds = listOf(301, 302, 303),
        luchadoresLigaIds = listOf(1, 1, 15, 4),
    ),
    LigaDefinicion(
        id = 4,
        nombre = "Reyes de la lona",
        luchadorEstrellaId = 8,
        misionIds = listOf(404, 402, 403),
        luchadoresLigaIds = listOf(5, 5, 6, 7),
    ),
)
