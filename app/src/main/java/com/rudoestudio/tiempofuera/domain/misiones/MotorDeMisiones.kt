package com.rudoestudio.tiempofuera.domain.misiones

import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiario
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import kotlin.random.Random

/**
 * Elige las misiones del día según propósito, personalidad y divisiones del usuario.
 * En iOS: `MotorDeMisiones` (Domain/Services). Los sorteos reciben [Random] para poder
 * probarlos; en la app se usa el de por defecto.
 */
object MotorDeMisiones {
    const val MAXIMO_MISIONES_POR_DIVISION = 2

    // Paso 1: peso base de cada división según el propósito.
    private val pesoBasePorProposito: Map<PropositoUsuario, Map<DivisionMision, Double>> = mapOf(
        PropositoUsuario.DEJAR_CELULAR to pesos(35, 15, 10, 15, 15, 5, 5),
        PropositoUsuario.MEJORAR_MENTAL to pesos(15, 40, 10, 10, 15, 5, 5),
        PropositoUsuario.CUIDAR_CUERPO to pesos(10, 10, 40, 10, 5, 5, 20),
        PropositoUsuario.RECONECTAR_GENTE to pesos(15, 10, 10, 45, 5, 5, 10),
    )

    // Paso 2: modificador por personalidad.
    private val modificadorPorPersonalidad: Map<TipoPersonalidad, Map<DivisionMision, Double>> = mapOf(
        TipoPersonalidad.ANALISTA to pesos(1.0, 0.7, 1.0, 0.6, 1.2, 1.5, 1.3),
        TipoPersonalidad.DIPLOMATICO to pesos(1.2, 1.5, 0.7, 1.2, 1.1, 0.7, 0.9),
        TipoPersonalidad.CENTINELA to pesos(1.2, 1.0, 1.2, 0.9, 1.0, 0.9, 1.4),
        TipoPersonalidad.EXPLORADOR to pesos(0.7, 0.6, 1.4, 1.3, 0.7, 1.1, 1.1),
    )

    /** Mismo orden que las tablas de iOS: dejarCelular, bienestar, ejercicio, conectar, lectura, juegos, progreso. */
    private fun pesos(vararg v: Number): Map<DivisionMision, Double> = listOf(
        DivisionMision.DEJAR_CELULAR, DivisionMision.BIENESTAR_MENTAL, DivisionMision.EJERCICIO, DivisionMision.CONECTAR,
        DivisionMision.LECTURA, DivisionMision.JUEGOS_MENTALES, DivisionMision.PROGRESO_PERSONAL,
    ).zip(v.map { it.toDouble() }).toMap()

    // Paso 3: elegir una división en el onboarding es la señal más fuerte que hay.
    private const val BOOST_DIVISION_SELECCIONADA = 2.0
    private const val PENALIZACION_DIVISION_NO_SELECCIONADA = 0.5

    /** Peso final de cada división para el usuario, con decaimiento por lo que ya le tocó. */
    fun calcularPesos(perfil: UserProfile, frecuenciaPorDivision: Map<DivisionMision, Int> = emptyMap()): Map<DivisionMision, Double> {
        val base = pesoBasePorProposito[perfil.proposito].orEmpty()
        val personalidad = modificadorPorPersonalidad[perfil.personalidadId].orEmpty()
        val pesos = DivisionMision.entries.associateWith { division ->
            val seleccion = if (division in perfil.divisionesSeleccionadas) BOOST_DIVISION_SELECCIONADA else PENALIZACION_DIVISION_NO_SELECCIONADA
            (base[division] ?: 1.0) * (personalidad[division] ?: 1.0) * seleccion
        }.toMutableMap()

        // Decaimiento suave: lo más frecuente baja hasta a la mitad (nunca de 0.3).
        val maxFrecuencia = frecuenciaPorDivision.values.maxOrNull() ?: 0
        if (maxFrecuencia > 0) {
            for ((division, frecuencia) in frecuenciaPorDivision) {
                val actual = pesos[division] ?: continue
                val factor = 1.0 - (frecuencia.toDouble() / maxFrecuencia) * 0.5
                pesos[division] = actual * maxOf(factor, 0.3)
            }
        }
        return pesos
    }

    /** Sortea [cantidad] divisiones distintas, proporcional a su peso. */
    fun sortearDivisiones(pesos: Map<DivisionMision, Double>, cantidad: Int, azar: Random = Random.Default): List<DivisionMision> {
        val disponibles = pesos.filterValues { it > 0 }.toMutableMap()
        val resultado = mutableListOf<DivisionMision>()
        repeat(cantidad) {
            val elegida = sortearUnaDivision(disponibles, azar) ?: return resultado
            resultado += elegida
            disponibles.remove(elegida)
        }
        return resultado
    }

    /** Una misión de la [division], de preferencia con un tipo que aún no salió hoy. */
    fun seleccionarMision(
        division: DivisionMision,
        pool: List<Mision>,
        excluir: Set<Int>,
        tiposMisionYaUsados: Set<TipoMision> = emptySet(),
        azar: Random = Random.Default,
    ): Mision? {
        val candidatas = pool.filter { it.division == division && it.id !in excluir }
        val sinTipoRepetido = candidatas.filter { it.tipoMision == null || it.tipoMision !in tiposMisionYaUsados }
        return sinTipoRepetido.randomOrNull(azar) ?: candidatas.randomOrNull(azar)
    }

    /**
     * Arma las misiones del día: máximo 2 por división, variedad de tipos y, si no
     * alcanzan, rellena con misiones sin división.
     */
    fun seleccionarMisionesDiarias(
        pool: List<Mision>,
        pesos: Map<DivisionMision, Double>,
        cantidad: Int,
        excluir: Set<Int> = emptySet(),
        yaSeleccionadas: List<Mision> = emptyList(),
        azar: Random = Random.Default,
    ): List<Mision> {
        val seleccionadas = yaSeleccionadas.toMutableList()
        val idsUsados = (excluir + yaSeleccionadas.map { it.id }).toMutableSet()
        val conteoPorDivision = yaSeleccionadas.mapNotNull { it.division }.groupingBy { it }.eachCount().toMutableMap()
        val tiposUsados = yaSeleccionadas.mapNotNull { it.tipoMision }.toMutableSet()

        while (seleccionadas.size < cantidad) {
            val elegibles = pool
                .filter { it.id !in idsUsados && it.division != null }
                .groupBy { it.division!! }
                .filterKeys { (conteoPorDivision[it] ?: 0) < MAXIMO_MISIONES_POR_DIVISION }
            if (elegibles.isEmpty()) break
            val pesosElegibles = elegibles.keys.associateWith { maxOf(pesos[it] ?: 1.0, 0.01) }
            val division = sortearUnaDivision(pesosElegibles, azar) ?: break
            val candidatas = elegibles.getValue(division)
            val conTipoNuevo = candidatas.filter { it.tipoMision != null && it.tipoMision !in tiposUsados }
            val elegida = (conTipoNuevo.ifEmpty { candidatas }).randomOrNull(azar) ?: break

            seleccionadas += elegida
            idsUsados += elegida.id
            conteoPorDivision[division] = (conteoPorDivision[division] ?: 0) + 1
            elegida.tipoMision?.let { tiposUsados += it }
        }

        if (seleccionadas.size < cantidad) {
            val sinDivision = pool.filter { it.division == null && it.id !in idsUsados }.shuffled(azar)
            seleccionadas += sinDivision.take(cantidad - seleccionadas.size)
        }
        return seleccionadas
    }

    fun cumpleLimiteDeDivision(misiones: List<Mision>): Boolean =
        misiones.mapNotNull { it.division }.groupingBy { it }.eachCount().values.all { it <= MAXIMO_MISIONES_POR_DIVISION }

    /** Si [mision] cabe en el día sin pasar el límite de su división. */
    fun puedeAgregar(mision: Mision, a: List<Mision>, maximoPorDivision: Int = MAXIMO_MISIONES_POR_DIVISION): Boolean {
        val division = mision.division ?: return true
        return a.count { it.division == division && it.id != mision.id } < maximoPorDivision
    }

    /** Cuántas veces salió cada división en el historial (para el decaimiento). */
    fun calcularFrecuenciaPorDivision(historial: List<ProgresoDiario>, catalogo: List<Mision>): Map<DivisionMision, Int> {
        val porId = catalogo.associateBy { it.id }
        return historial.flatMap { it.misiones }.mapNotNull { porId[it]?.division }.groupingBy { it }.eachCount()
    }

    private fun sortearUnaDivision(pesos: Map<DivisionMision, Double>, azar: Random): DivisionMision? {
        val positivos = pesos.filterValues { it > 0 }
        val total = positivos.values.sum()
        if (total <= 0) return null
        val objetivo = azar.nextDouble(total)
        var acumulado = 0.0
        for ((division, peso) in positivos) {
            acumulado += peso
            if (objetivo < acumulado) return division
        }
        return positivos.keys.first()
    }
}
