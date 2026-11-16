package com.rudoestudio.tiempofuera.data.bloqueo

import kotlinx.serialization.Serializable
import java.time.LocalDate

// Lógica de la medición de uso, separada de Android para poder probarla. Viene del spike
// (`UsageRepo` y `UsoDiario`), donde se comparó contra Bienestar digital: 7 de 7 apps a
// menos de 1 min.

/** Un evento de `UsageStatsManager` reducido a lo que importa. */
data class EventoUso(val tipo: Tipo, val paquete: String, val clase: String, val ms: Long) {
    enum class Tipo {
        REANUDADA,
        PAUSADA,

        /** Pantalla apagada, apagado o encendido: ninguna app sigue en primer plano. */
        CIERRE_GENERAL,
    }
}

object CalculoUso {
    /**
     * Milisegundos en primer plano desde [inicio], por paquete. Una sesión dura mientras la
     * app tenga **alguna actividad** (por clase) reanudada: en apps que apilan pantallas, el
     * STOPPED de la anterior llega después del RESUMED de la nueva y, cerrando por
     * paquete, Facebook daba 14 min en vez de 33.
     */
    fun usoMs(eventos: List<EventoUso>, inicio: Long, ahora: Long): Map<String, Long> {
        val activas = HashMap<String, MutableSet<String>>()
        val desdeSesion = HashMap<String, Long>()
        val total = HashMap<String, Long>()

        fun cerrarSiVacia(paquete: String, fin: Long) {
            if (!activas[paquete].isNullOrEmpty()) return
            val desde = desdeSesion.remove(paquete) ?: return
            val ms = fin - maxOf(desde, inicio)
            if (ms > 0) total[paquete] = (total[paquete] ?: 0L) + ms
        }

        fun cerrarTodas(fin: Long) {
            activas.values.forEach { it.clear() }
            desdeSesion.keys.toList().forEach { cerrarSiVacia(it, fin) }
        }

        for (e in eventos) {
            when (e.tipo) {
                EventoUso.Tipo.REANUDADA -> {
                    activas.getOrPut(e.paquete) { HashSet() }.add(e.clase)
                    desdeSesion.putIfAbsent(e.paquete, e.ms)
                }
                EventoUso.Tipo.PAUSADA -> {
                    activas[e.paquete]?.remove(e.clase)
                    cerrarSiVacia(e.paquete, e.ms)
                }
                // Al apagar o prender, las apps abiertas nunca reciben PAUSED: sin esto,
                // Ajustes contaba 13 min en vez de 1.
                EventoUso.Tipo.CIERRE_GENERAL -> cerrarTodas(e.ms)
            }
        }
        cerrarTodas(ahora)
        return total
    }

    /** Apps con alguna actividad reanudada al final de los [eventos]. */
    fun reanudadas(eventos: List<EventoUso>): Set<String> {
        val activas = HashMap<String, MutableSet<String>>()
        for (e in eventos) {
            when (e.tipo) {
                EventoUso.Tipo.REANUDADA -> activas.getOrPut(e.paquete) { HashSet() }.add(e.clase)
                EventoUso.Tipo.PAUSADA -> activas[e.paquete]?.remove(e.clase)
                EventoUso.Tipo.CIERRE_GENERAL -> activas.clear()
            }
        }
        return activas.filterValues { it.isNotEmpty() }.keys
    }
}

/**
 * Contador propio del uso del día. Existe porque Android pierde los eventos que no
 * alcanzó a escribir antes de un reinicio, y con eso el límite diario se "reiniciaba"
 * reiniciando el teléfono.
 *
 * Total = lo guardado antes del último arranque ([base]) + lo medido desde ese arranque.
 * Nunca baja: si UsageStats "olvida" algo, se queda lo último guardado ([ultimo]).
 */
@Serializable
data class ContadorUsoDiario(
    val fecha: String,
    val arranque: Int,
    /** Desde cuándo se mide con UsageStats (inicio del día o del último arranque). */
    val desdeMs: Long,
    val base: Map<String, Long> = emptyMap(),
    val ultimo: Map<String, Long> = emptyMap(),
    /** Los contadores sin versión (0) son de antes del arreglo de abajo y se vuelven a medir. */
    val version: Int = 0,
) {
    /**
     * Día nuevo (o contador viejo): todo desde cero, medido desde la medianoche. Mismo día
     * con un reinicio de verdad: lo guardado pasa a ser la base y se mide desde el arranque.
     *
     * Un reinicio de verdad es el que pasó **después** de que se empezó a medir. Antes solo
     * se comparaba el número de arranque: un día Android no lo dio (-1) y luego sí, y se
     * midió "desde el arranque", que era de tres días antes: amaneció con 40 h de uso.
     */
    fun vigente(hoy: LocalDate, arranqueActual: Int, horaArranqueMs: Long, inicioDiaMs: Long): ContadorUsoDiario {
        if (version != VERSION || fecha != hoy.toString()) return nuevo(hoy, arranqueActual, inicioDiaMs)
        if (arranqueActual < 0 || arranqueActual == arranque) return this
        if (horaArranqueMs <= desdeMs) return copy(arranque = arranqueActual)
        return copy(arranque = arranqueActual, desdeMs = horaArranqueMs, base = ultimo)
    }

    /** Suma lo medido ([parcial]) y regresa el contador actualizado. */
    fun con(parcial: Map<String, Long>): ContadorUsoDiario {
        val total = (base.keys + parcial.keys + ultimo.keys).associateWith { paquete ->
            maxOf((base[paquete] ?: 0L) + (parcial[paquete] ?: 0L), ultimo[paquete] ?: 0L)
        }
        return copy(ultimo = total)
    }

    companion object {
        const val VERSION = 2

        fun nuevo(hoy: LocalDate, arranque: Int, inicioDiaMs: Long) = ContadorUsoDiario(hoy.toString(), arranque, inicioDiaMs, version = VERSION)
    }
}
