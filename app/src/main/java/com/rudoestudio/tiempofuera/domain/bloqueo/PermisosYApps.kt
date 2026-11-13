package com.rudoestudio.tiempofuera.domain.bloqueo

import java.text.Normalizer

/**
 * Los permisos que necesita el bloqueo (M3.1). En iOS es uno solo (Screen Time) más
 * notificaciones; en Android son varios y cada uno se da en una pantalla de Ajustes.
 */
data class EstadoPermisos(
    /** El servicio de accesibilidad está prendido: sin él no se bloquea nada. */
    val accesibilidad: Boolean,
    /** Acceso al uso: sin él no se mide el tiempo ni funciona el límite diario. */
    val accesoAlUso: Boolean,
    val notificaciones: Boolean,
    /** Recomendado, no obligatorio: que Android no duerma la app para ahorrar batería. */
    val sinAhorroBateria: Boolean,
) {
    /** Lo mínimo para que el bloqueo funcione (en iOS: Screen Time y notificaciones). */
    val completos: Boolean get() = accesibilidad && accesoAlUso && notificaciones
}

interface RevisorPermisos {
    fun estado(): EstadoPermisos
}

/** Una app con ícono en el launcher. */
data class AppInstalada(val paquete: String, val nombre: String)

/** Las apps que el usuario puede elegir. En iOS lo da el sistema (`FamilyActivityPicker`). */
interface AppsInstaladas {
    /** Ordenadas por nombre, sin Tiempo Fuera. */
    suspend fun lanzables(): List<AppInstalada>
}

object BuscadorApps {
    /** Filtra por nombre o paquete, sin importar mayúsculas ni acentos ("musica" encuentra "Música"). */
    fun filtrar(apps: List<AppInstalada>, consulta: String): List<AppInstalada> {
        val buscada = normalizar(consulta.trim())
        if (buscada.isEmpty()) return apps
        return apps.filter { normalizar(it.nombre).contains(buscada) || it.paquete.contains(buscada) }
    }

    /**
     * Las elegidas primero (en el orden de [apps]) y luego las demás, para que al abrir el
     * selector se vea lo que ya estaba escogido.
     */
    fun ordenar(apps: List<AppInstalada>, elegidas: Set<String>): List<AppInstalada> =
        apps.sortedBy { it.paquete !in elegidas }

    private fun normalizar(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
}
