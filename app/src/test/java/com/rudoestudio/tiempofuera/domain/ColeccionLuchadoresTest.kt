package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.domain.luchadores.ComprarSubidaNivelUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.EquiparLuchadorUseCase
import com.rudoestudio.tiempofuera.domain.luchadores.ErrorSubidaNivel
import com.rudoestudio.tiempofuera.domain.luchadores.NivelPoderCalculator
import com.rudoestudio.tiempofuera.domain.luchadores.SubirDeNivelLuchadorUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ColeccionLuchadoresTest {
    private val calculador = NivelPoderCalculator()

    @Test
    fun `comprar una subida sube al luchador y descuenta los puntos del perfil`() = runTest {
        val luchadores = LuchadoresFake()
        val ringo = luchadores.luchadores.getValue(0).copy(cantidad = 1, nivel = 1)
        luchadores.luchadores[0] = ringo
        val perfiles = PerfilesFake(perfilBase(ringo).copy(puntosPoder = 100))
        val comprar = ComprarSubidaNivelUseCase(SubirDeNivelLuchadorUseCase(luchadores, calculador), perfiles, reloj())

        val resultado = comprar(ringo)

        assertEquals(2, luchadores.luchadores.getValue(0).nivel)
        assertEquals(100 - calculador.costoParaSubirA(2), perfiles.actual.puntosPoder)
        assertEquals(calculador.costoParaSubirA(2), resultado.puntosGastados)
    }

    @Test
    fun `sin puntos suficientes no sube ni descuenta`() = runTest {
        val luchadores = LuchadoresFake()
        val ringo = luchadores.luchadores.getValue(0).copy(cantidad = 1, nivel = 1)
        val perfiles = PerfilesFake(perfilBase(ringo).copy(puntosPoder = 0))
        val comprar = ComprarSubidaNivelUseCase(SubirDeNivelLuchadorUseCase(luchadores, calculador), perfiles, reloj())
        assertThrows(ErrorSubidaNivel.PuntosInsuficientes::class.java) { kotlinx.coroutines.runBlocking { comprar(ringo) } }
        assertEquals(0, perfiles.actual.puntosPoder)
    }

    @Test
    fun `solo se equipan luchadores que ya tiene`() = runTest {
        val luchadores = LuchadoresFake()
        val perfiles = PerfilesFake()
        val ajeno = luchadores.luchadores.getValue(3).copy(cantidad = 0)
        EquiparLuchadorUseCase(perfiles)(ajeno)
        assertEquals(0, perfiles.actual.luchadorSeleccionado.id)
        EquiparLuchadorUseCase(perfiles)(ajeno.copy(cantidad = 1))
        assertEquals(3, perfiles.actual.luchadorSeleccionado.id)
    }
}
