package com.rudoestudio.tiempofuera.domain

import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoRecoleccion
import com.rudoestudio.tiempofuera.domain.luchadores.ResultadoSubidaNivel
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.recompensas.EstadoRecompensas
import com.rudoestudio.tiempofuera.domain.recompensas.FlujoRecompensa
import com.rudoestudio.tiempofuera.domain.recompensas.PantallaRecompensa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlujoRecompensasTest {
    private val recompensa = RecompensaMision(puntosPoder = 10, monedas = 5)
    private val luchador = DatosPreview.luchadorSeleccionado

    private fun recoleccion(duplicado: Boolean) = ResultadoRecoleccion(
        luchador.id, luchador.nombre, duplicado, 10, 30, 2, 3, Rareza.COMUN, Rareza.COMUN, if (duplicado) 2 else 1,
    )

    @Test
    fun `con racha activada primero va la racha y luego la mision completada`() {
        var estado = EstadoRecompensas.iniciar(FlujoRecompensa.alCompletarMision(true, 4, recompensa))
        assertEquals(PantallaRecompensa.RachaActivada(4, recompensa), estado.pantalla)
        estado = estado.continuar()
        assertEquals(PantallaRecompensa.MisionCompletada(recompensa), estado.pantalla)
        assertTrue(estado.continuar().terminado)
    }

    @Test
    fun `sin racha nueva va directo a la mision completada`() {
        val estado = EstadoRecompensas.iniciar(FlujoRecompensa.alCompletarMision(false, 4, recompensa))
        assertEquals(PantallaRecompensa.MisionCompletada(recompensa), estado.pantalla)
    }

    @Test
    fun `un sobre repetido festeja la subida de nivel antes de terminar`() {
        var estado = EstadoRecompensas.iniciar(FlujoRecompensa.SobrePersonaje(Rareza.COMUN))
        estado = estado.sobreRecolectado(luchador, recoleccion(duplicado = true))
        val subida = estado.pantalla as PantallaRecompensa.SubidaNivel
        assertEquals(3, subida.resultado.nivelNuevo)
        assertEquals(0, subida.resultado.puntosGastados)
        assertTrue(estado.continuar().terminado)
    }

    @Test
    fun `un sobre nuevo o con error termina el flujo`() {
        val inicio = EstadoRecompensas.iniciar(FlujoRecompensa.SobrePersonaje(Rareza.RARO))
        assertTrue(inicio.sobreRecolectado(luchador, recoleccion(duplicado = false)).terminado)
        assertTrue(inicio.sobreConError().terminado)
    }

    @Test
    fun `con varios sobres sigue con el siguiente y el paso siempre cambia`() {
        val inicio = EstadoRecompensas(PantallaRecompensa.Sobre(Rareza.COMUN), sobresPendientes = listOf(Rareza.COMUN))
        val segundo = inicio.sobreRecolectado(luchador, recoleccion(duplicado = false))
        assertEquals(PantallaRecompensa.Sobre(Rareza.COMUN), segundo.pantalla)
        assertTrue(segundo.paso != inicio.paso)
    }

    @Test
    fun `una subida de nivel comprada se festeja sola y termina`() {
        val subida = ResultadoSubidaNivel(
            luchador.id, 10, 40, 2, 3, Rareza.COMUN, Rareza.COMUN, puntosGastados = 30,
        )
        val estado = EstadoRecompensas.iniciar(FlujoRecompensa.SubidaNivel(subida, luchador))
        assertEquals(PantallaRecompensa.SubidaNivel(subida, luchador), estado.pantalla)
        assertTrue(estado.continuar().terminado)
    }

    @Test
    fun `cobrar un nivel festeja la subida y luego abre sus sobres en orden`() {
        val subida = ResultadoSubidaNivel(luchador.id, 10, 90, 2, 4, Rareza.COMUN, Rareza.COMUN, 0)
        var estado = EstadoRecompensas.iniciar(
            FlujoRecompensa.alCobrarNivel(listOf(Rareza.EPICO, Rareza.COMUN), 50, subida, luchador)!!,
        )
        assertTrue(estado.pantalla is PantallaRecompensa.SubidaNivel)
        estado = estado.continuar()
        assertEquals(PantallaRecompensa.Sobre(Rareza.COMUN), estado.pantalla)
        estado = estado.sobreRecolectado(luchador, recoleccion(duplicado = false))
        assertEquals(PantallaRecompensa.Sobre(Rareza.EPICO), estado.pantalla)
    }

    @Test
    fun `cobrar un nivel sin sobres ni subida no festeja nada`() {
        val sinSubir = ResultadoSubidaNivel(luchador.id, 10, 20, 2, 2, Rareza.COMUN, Rareza.COMUN, 0)
        assertEquals(null, FlujoRecompensa.alCobrarNivel(emptyList(), 3, sinSubir, luchador))
    }
}
