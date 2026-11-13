package com.rudoestudio.tiempofuera.domain.bloqueo

/**
 * Cómo evitar que el ahorro de batería de cada marca apague Tiempo Fuera (M3.11). Los
 * fabricantes agregan sus propios "matadores" de apps encima de Android; ver
 * dontkillmyapp.com. Los menús cambian entre versiones: son guías, no rutas exactas.
 * Probado solo en Samsung (S24+, One UI 9); falta confirmarlo en las demás marcas.
 */
data class GuiaFabricante(val marca: String, val pasos: List<String>)

object GuiasFabricante {
    private val generica = GuiaFabricante(
        "Android",
        listOf(
            "Abre Ajustes → Apps → Tiempo Fuera → Batería.",
            "Elige \"Sin restricciones\".",
        ),
    )

    private val guias = mapOf(
        "samsung" to GuiaFabricante(
            "Samsung",
            listOf(
                "Ajustes → Apps → Tiempo Fuera → Batería → \"Sin restricciones\".",
                "Ajustes → Batería → Límites de uso en segundo plano: revisa que Tiempo Fuera no esté en \"Apps en suspensión\" ni en \"Apps en suspensión profunda\".",
            ),
        ),
        "xiaomi" to GuiaFabricante(
            "Xiaomi",
            listOf(
                "Ajustes → Apps → Administrar apps → Tiempo Fuera → Inicio automático: actívalo.",
                "En la misma pantalla, Ahorro de batería → \"Sin restricciones\".",
                "En apps recientes, deja presionada Tiempo Fuera y ponle el candado para que no se cierre.",
            ),
        ),
        "huawei" to GuiaFabricante(
            "Huawei",
            listOf(
                "Ajustes → Batería → Inicio de aplicaciones → Tiempo Fuera: cámbialo a manual y activa las tres opciones.",
            ),
        ),
        "oppo" to GuiaFabricante(
            "OPPO",
            listOf(
                "Ajustes → Batería → Tiempo Fuera: permite la actividad en segundo plano y el inicio automático.",
            ),
        ),
        "motorola" to GuiaFabricante(
            "Motorola",
            listOf(
                "Ajustes → Apps → Tiempo Fuera → Batería → \"Sin restricciones\".",
            ),
        ),
    )

    /** Marcas que comparten capa: Redmi y POCO usan la de Xiaomi; realme y OnePlus, la de OPPO; Honor, la de Huawei. */
    private val alias = mapOf(
        "redmi" to "xiaomi",
        "poco" to "xiaomi",
        "realme" to "oppo",
        "oneplus" to "oppo",
        "honor" to "huawei",
    )

    /** [fabricante] es `Build.MANUFACTURER` ("samsung", "Xiaomi"…). */
    fun para(fabricante: String): GuiaFabricante {
        val clave = fabricante.trim().lowercase()
        return guias[alias[clave] ?: clave] ?: generica
    }
}
