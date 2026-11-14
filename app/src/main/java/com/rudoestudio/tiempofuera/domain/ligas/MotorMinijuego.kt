package com.rudoestudio.tiempofuera.domain.ligas

import com.rudoestudio.tiempofuera.domain.luchadores.Luchador
import com.rudoestudio.tiempofuera.domain.luchadores.TipoLuchador
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Un punto o una velocidad dentro del ring, de 0 a 1 en cada eje. */
data class Punto(val x: Double, val y: Double) {
    operator fun plus(o: Punto) = Punto(x + o.x, y + o.y)
    operator fun minus(o: Punto) = Punto(x - o.x, y - o.y)
    operator fun times(k: Double) = Punto(x * k, y * k)
    val largo: Double get() = hypot(x, y)

    companion object {
        val CERO = Punto(0.0, 0.0)
    }
}

/**
 * La pelea de una barrera (M9.4): te mueves con el joystick, golpeas cuando lo tienes cerca
 * y te cubres cuando avisa su ataque. Pegarle mientras avisa es un golpe potente que lo
 * aturde; cubrirte justo al final es un bloqueo perfecto. El rival lee cómo juegas: te
 * rodea si te cubres mucho y se aleja si atacas mucho.
 *
 * Kotlin puro: el tiempo ([ahora], en segundos) y el azar llegan de afuera, así que se puede
 * probar sin reloj. La pantalla llama [actualizar] en cada cuadro. En iOS:
 * `MinijuegoBarreraViewModel`, con las mismas cifras.
 */
class MotorMinijuego(
    val nivel: NivelLiga,
    val usuario: Luchador,
    val rival: Luchador,
    ahora: Double,
    private val azar: Random = Random.Default,
) {
    enum class Fase { PREPARACION, ADVERTENCIA, IMPACTO, RIVAL_ATURDIDO, VICTORIA, DERROTA }

    enum class ResultadoAtaque { NINGUNO, GOLPE, GOLPE_POTENTE, FALLO, RECIBIDO, ESQUIVADO, BLOQUEADO }

    enum class Sonido { GOLPE, FALLO }

    val tipoRival: TipoLuchador = nivel.tipoLuchadorBarrera ?: rival.tipo
    val tieneDesventajaDeTipo: Boolean = tipoRival == TipoLuchador.VERSATIL || usuario.tipo.estaEnDesventaja(tipoRival)

    private val dificultad: Double = ((nivel.id - 1).coerceAtLeast(0) / 99.0).coerceIn(0.0, 1.0)

    val vidasMaximasJugador: Int = min(3 + (usuario.nivel - 1).coerceAtLeast(0) / 45, 5)
    val vidaMaximaRival: Int = run {
        val inicial = (5 + (nivel.id - 1).coerceAtLeast(0) / 8 + if (tieneDesventajaDeTipo) 2 else 0).coerceIn(5, 20)
        val reduccion = min((usuario.nivel - 1).coerceAtLeast(0) / 30, 4)
        max(inicial - reduccion, 4)
    }

    var fase = Fase.PREPARACION; private set
    var posicionJugador = INICIO_JUGADOR; private set
    var posicionRival = INICIO_RIVAL; private set
    var vidasJugador = vidasMaximasJugador; private set
    var vidaRival = vidaMaximaRival; private set
    var combo = 0; private set
    var mejorCombo = 0; private set
    var guardiaActiva = false; private set
    var resultadoAtaque = ResultadoAtaque.NINGUNO; private set

    /** Cuándo pasó [resultadoAtaque]: la pantalla muestra su efecto un momento. */
    var momentoResultado = Double.NEGATIVE_INFINITY; private set
    var progresoEnfriamiento = 1.0; private set

    /** Suben con cada evento: la pantalla los usa para vibrar y animar. */
    var golpes = 0; private set
    var contadorAnimacionGolpe = 0; private set
    var contadorImpactoRecibido = 0; private set

    private val sonidos = mutableListOf<Sonido>()
    private var velocidadJugador = Punto.CERO
    private var velocidadRival = Punto.CERO
    private var direccionJoystick = Punto.CERO
    private var impulsoReboteJugador = Punto.CERO
    private var impulsoReboteRival = Punto.CERO
    private var objetivoRival = Punto(0.5, 0.55)
    private var faseFin = 0.0
    private var ultimoTick = ahora
    private var siguienteGolpeDisponible = ahora
    private var inicioGuardia = ahora
    private var inicioAtaqueRival = ahora
    private var ultimaAccionJugador = ahora
    private var inicioUltimoGolpe = Double.NEGATIVE_INFINITY
    private var finDeslizamientoJugador = Double.NEGATIVE_INFINITY
    private var tiempoSiguienteDecision = ahora
    private var finPasoRival = ahora
    private var duracionAtaqueRival = 0.8
    private var ataquesJugadorRecientes = 0
    private var guardiasConsecutivas = 0
    private var ultimoSignoMovimientoX = 0
    private var cambiosDeDireccion = 0

    init {
        iniciarFase(Fase.PREPARACION, 0.7, ahora)
    }

    val terminado: Boolean get() = fase == Fase.VICTORIA || fase == Fase.DERROTA
    val progresoVidaRival: Float get() = if (vidaMaximaRival > 0) vidaRival.toFloat() / vidaMaximaRival else 0f
    val progresoVidaJugador: Float get() = if (vidasMaximasJugador > 0) vidasJugador.toFloat() / vidasMaximasJugador else 0f

    /** Los sonidos desde la última vez que se pidieron. */
    fun tomarSonidos(): List<Sonido> = sonidos.toList().also { sonidos.clear() }

    fun puedeAtacar(ahora: Double): Boolean = !terminado && fase != Fase.RIVAL_ATURDIDO && ahora >= siguienteGolpeDisponible

    private val ataqueCooldown get() = 1.1 + dificultad * 0.2
    private val velocidadMovimientoJugador get() = 0.72 - dificultad * 0.12

    private val danioPorGolpe: Int
        get() {
            val ventaja = if (tipoRival != TipoLuchador.VERSATIL && tipoRival.estaEnDesventaja(usuario.tipo)) 1 else 0
            val porNivel = min((usuario.nivel - 1).coerceAtLeast(0) / 45, 2)
            return max(1, 1 + ventaja + porNivel - if (tieneDesventajaDeTipo) 1 else 0)
        }

    private fun factorControlJugador(ahora: Double): Double {
        val intensidad = min(max(finDeslizamientoJugador - ahora, 0.0) / DURACION_DESLIZAMIENTO, 1.0)
        return 1 - 0.75 * intensidad
    }

    /** [direccion]: el joystick, de -1 a 1 por eje (más largo se recorta). */
    fun moverJoystick(direccion: Punto, ahora: Double) {
        if (terminado) return
        direccionJoystick = if (direccion.largo > 1) direccion * (1 / direccion.largo) else direccion
        if (abs(direccionJoystick.x) > 0.18) {
            val signo = if (direccionJoystick.x > 0) 1 else -1
            if (ultimoSignoMovimientoX != 0 && signo != ultimoSignoMovimientoX) cambiosDeDireccion++
            ultimoSignoMovimientoX = signo
            ultimaAccionJugador = ahora
        }
    }

    fun cambiarGuardia(activa: Boolean, ahora: Double) {
        if (activa && !guardiaActiva) {
            inicioGuardia = ahora
            guardiasConsecutivas++
            ultimaAccionJugador = ahora
        }
        guardiaActiva = activa && !terminado
    }

    fun atacar(ahora: Double) {
        if (!puedeAtacar(ahora)) return
        siguienteGolpeDisponible = ahora + ataqueCooldown
        contadorAnimacionGolpe++
        inicioUltimoGolpe = ahora
        ultimaAccionJugador = ahora
        ataquesJugadorRecientes++
        guardiaActiva = false

        if (distancia() > RADIO_GOLPE) {
            combo = 0
            marcar(ResultadoAtaque.FALLO, ahora)
            sonidos += Sonido.FALLO
            return
        }
        // Pegarle mientras avisa su ataque lo interrumpe: doble daño y queda aturdido.
        val interrumpe = fase == Fase.ADVERTENCIA
        aplicarDanioAlRival(if (interrumpe) 2 else 1, interrumpe, ahora)
        if (fase == Fase.VICTORIA) return
        if (interrumpe) {
            velocidadRival = (posicionRival - posicionJugador) * 1.8
            iniciarFase(Fase.RIVAL_ATURDIDO, 0.65 + dificultad * 0.2, ahora)
        }
    }

    fun reiniciar(ahora: Double) {
        posicionJugador = INICIO_JUGADOR
        posicionRival = INICIO_RIVAL
        velocidadJugador = Punto.CERO
        velocidadRival = Punto.CERO
        direccionJoystick = Punto.CERO
        vidasJugador = vidasMaximasJugador
        vidaRival = vidaMaximaRival
        combo = 0
        mejorCombo = 0
        golpes = 0
        guardiaActiva = false
        ataquesJugadorRecientes = 0
        guardiasConsecutivas = 0
        ultimoSignoMovimientoX = 0
        cambiosDeDireccion = 0
        ultimaAccionJugador = ahora
        resultadoAtaque = ResultadoAtaque.NINGUNO
        momentoResultado = Double.NEGATIVE_INFINITY
        progresoEnfriamiento = 1.0
        siguienteGolpeDisponible = ahora
        impulsoReboteJugador = Punto.CERO
        impulsoReboteRival = Punto.CERO
        finDeslizamientoJugador = Double.NEGATIVE_INFINITY
        tiempoSiguienteDecision = ahora
        finPasoRival = ahora
        ultimoTick = ahora
        iniciarFase(Fase.PREPARACION, 0.7, ahora)
    }

    /** Un cuadro de juego. Los saltos grandes (app en pausa) cuentan como 50 ms. */
    fun actualizar(ahora: Double) {
        if (terminado) return
        val delta = (ahora - ultimoTick).coerceIn(0.0, 0.05)
        ultimoTick = ahora
        progresoEnfriamiento = (1 - (siguienteGolpeDisponible - ahora) / ataqueCooldown).coerceIn(0.0, 1.0)
        moverJugador(delta, ahora)
        decidirRival(ahora)
        moverRival(delta, ahora)
        avanzarFase(ahora)
    }

    private fun moverJugador(delta: Double, ahora: Double) {
        val velocidad = velocidadMovimientoJugador * (if (guardiaActiva) 0.58 else 1.0) * factorControlJugador(ahora)
        val actual = limitar(direccionJoystick * velocidad + impulsoReboteJugador, velocidad + 0.85)
        val (nueva, vel) = mover(posicionJugador, actual, delta, esJugador = true, ahora)
        posicionJugador = nueva
        velocidadJugador = vel
        impulsoReboteJugador = impulsoReboteJugador * exp(-2.8 * delta)
    }

    private fun decidirRival(ahora: Double) {
        // Si dejas de hacer algo, el rival poco a poco se olvida de cómo jugabas.
        if (ahora - ultimaAccionJugador > 2.2) {
            ataquesJugadorRecientes = max(ataquesJugadorRecientes - 1, 0)
            guardiasConsecutivas = max(guardiasConsecutivas - 1, 0)
            ultimaAccionJugador = ahora
        }
        if (fase == Fase.ADVERTENCIA) return
        if (ahora >= finPasoRival) velocidadRival = Punto.CERO
        if (ahora >= tiempoSiguienteDecision && ahora >= finPasoRival) {
            decidirObjetivoRival(ahora)
            iniciarPasoRival(ahora)
        }
        if (fase == Fase.PREPARACION && ahora >= faseFin && distancia() < DISTANCIA_ATAQUE) comenzarAtaqueRival(ahora)
    }

    private fun decidirObjetivoRival(ahora: Double) {
        val enGuardia = guardiaActiva || guardiasConsecutivas >= 2
        val agresivo = ataquesJugadorRecientes >= 2 || ahora - inicioUltimoGolpe < 0.45
        val repiteDireccion = cambiosDeDireccion >= 3
        val ladoX = if (posicionRival.x < posicionJugador.x) -1.0 else 1.0
        val arriba = posicionRival.y < posicionJugador.y
        val j = posicionJugador
        val objetivo = when {
            fase == Fase.RIVAL_ATURDIDO -> puntoAleatorioLejos(j, 0.38)
            enGuardia -> Punto(j.x + ladoX * (0.18 + dificultad * 0.1), j.y + if (arriba) -0.08 else 0.12)
            agresivo && distancia() < 0.44 -> Punto(j.x + ladoX * (0.22 + dificultad * 0.08), j.y + if (arriba) 0.13 else -0.13)
            repiteDireccion && abs(velocidadJugador.x) > 0.1 ->
                Punto(j.x + if (velocidadJugador.x >= 0) 0.18 else -0.18, j.y + if (arriba) 0.12 else -0.12)
            else -> Punto(j.x + azar.nextDouble(-0.14, 0.14), j.y + if (arriba) 0.16 else -0.16)
        }
        objetivoRival = dentroDelRing(objetivo, 0.05)
        if (ataquesJugadorRecientes > 0) ataquesJugadorRecientes--
        if (cambiosDeDireccion > 0 && !repiteDireccion) cambiosDeDireccion--
    }

    private fun iniciarPasoRival(ahora: Double) {
        val vector = objetivoRival - posicionRival
        val dist = vector.largo
        if (dist <= 0.025) {
            velocidadRival = Punto.CERO
            finPasoRival = ahora
            tiempoSiguienteDecision = ahora + pausaRival()
            return
        }
        val duracionPaso = azar.nextDouble(0.16, 0.32) - dificultad * 0.04
        velocidadRival = vector * ((0.58 + dificultad * 0.48) / dist)
        finPasoRival = ahora + max(duracionPaso, 0.12)
        tiempoSiguienteDecision = finPasoRival + pausaRival()
    }

    private fun pausaRival() = azar.nextDouble(0.3 - dificultad * 0.14, 0.55 - dificultad * 0.18)

    private fun puntoAleatorioLejos(de: Punto, margen: Double): Punto {
        val angulo = azar.nextDouble(0.0, 2 * PI)
        return dentroDelRing(Punto(de.x + cos(angulo) * margen, de.y + sin(angulo) * margen), 0.05)
    }

    private fun moverRival(delta: Double, ahora: Double) {
        val actual = limitar(velocidadRival + impulsoReboteRival, 1.55)
        posicionRival = mover(posicionRival, actual, delta, esJugador = false, ahora).first
        impulsoReboteRival = impulsoReboteRival * exp(-2.4 * delta)
    }

    /** Mueve y rebota en las cuerdas: sale más rápido y, si eres tú, patinas un rato. */
    private fun mover(posicion: Punto, velocidad: Punto, delta: Double, esJugador: Boolean, ahora: Double): Pair<Punto, Punto> {
        var x = posicion.x + velocidad.x * delta
        var y = posicion.y + velocidad.y * delta
        var vx = velocidad.x
        var vy = velocidad.y
        var reboto = false
        if (x < MIN_RING) {
            x = MIN_RING; vx = abs(vx) * REBOTE; reboto = true
            if (esJugador) direccionJoystick = direccionJoystick.copy(x = abs(direccionJoystick.x))
        } else if (x > MAX_RING) {
            x = MAX_RING; vx = -abs(vx) * REBOTE; reboto = true
            if (esJugador) direccionJoystick = direccionJoystick.copy(x = -abs(direccionJoystick.x))
        }
        if (y < MIN_RING) {
            y = MIN_RING; vy = abs(vy) * REBOTE; reboto = true
            if (esJugador) direccionJoystick = direccionJoystick.copy(y = abs(direccionJoystick.y))
        } else if (y > MAX_RING) {
            y = MAX_RING; vy = -abs(vy) * REBOTE; reboto = true
            if (esJugador) direccionJoystick = direccionJoystick.copy(y = -abs(direccionJoystick.y))
        }
        val vel = Punto(vx, vy)
        if (reboto) {
            if (esJugador) {
                finDeslizamientoJugador = ahora + DURACION_DESLIZAMIENTO
                impulsoReboteJugador = vel * 0.8
            } else {
                impulsoReboteRival = vel * 0.85
            }
        }
        return Punto(x, y) to vel
    }

    private fun avanzarFase(ahora: Double) {
        if (ahora < faseFin) return
        when (fase) {
            Fase.PREPARACION -> if (distancia() < DISTANCIA_ATAQUE) comenzarAtaqueRival(ahora)
            Fase.ADVERTENCIA -> ejecutarAtaqueRival(ahora)
            Fase.IMPACTO, Fase.RIVAL_ATURDIDO -> iniciarFase(Fase.PREPARACION, azar.nextDouble(0.2, 0.42), ahora)
            Fase.VICTORIA, Fase.DERROTA -> Unit
        }
    }

    private fun comenzarAtaqueRival(ahora: Double) {
        if (fase != Fase.PREPARACION) return
        objetivoRival = dentroDelRing(posicionJugador + velocidadJugador * 0.18, 0.0)
        duracionAtaqueRival = max(0.88 - dificultad * 0.38, 0.42)
        inicioAtaqueRival = ahora
        iniciarFase(Fase.ADVERTENCIA, duracionAtaqueRival, ahora)
    }

    private fun ejecutarAtaqueRival(ahora: Double) {
        if (distancia() > 0.4) {
            // Te alejaste a tiempo: falla y queda aturdido.
            marcar(ResultadoAtaque.ESQUIVADO, ahora)
            sumarCombo()
            velocidadRival = (posicionRival - posicionJugador) * 1.5
            iniciarFase(Fase.RIVAL_ATURDIDO, max(0.75 - dificultad * 0.2, 0.48), ahora)
            return
        }
        if (guardiaActiva) {
            // Bloqueo perfecto: te cubriste en el último tercio del aviso.
            val perfecto = inicioGuardia >= inicioAtaqueRival + duracionAtaqueRival * 0.68
            marcar(ResultadoAtaque.BLOQUEADO, ahora)
            sonidos += Sonido.GOLPE
            if (perfecto) {
                sumarCombo()
                velocidadRival = (posicionRival - posicionJugador) * 1.7
                iniciarFase(Fase.RIVAL_ATURDIDO, max(0.8 - dificultad * 0.18, 0.5), ahora)
            } else {
                recibirDanio(reducido = true, ahora)
            }
            return
        }
        recibirDanio(reducido = false, ahora)
    }

    private fun recibirDanio(reducido: Boolean, ahora: Double) {
        val danio = if (reducido || !tieneDesventajaDeTipo) 1 else 2
        vidasJugador = max(vidasJugador - danio, 0)
        contadorImpactoRecibido++
        combo = 0
        marcar(if (reducido) ResultadoAtaque.BLOQUEADO else ResultadoAtaque.RECIBIDO, ahora)
        sonidos += Sonido.FALLO
        if (vidasJugador == 0) {
            guardiaActiva = false
            fase = Fase.DERROTA
            return
        }
        velocidadJugador = (posicionJugador - posicionRival) * 1.4
        iniciarFase(Fase.IMPACTO, 0.28, ahora)
    }

    private fun aplicarDanioAlRival(multiplicador: Int, potente: Boolean, ahora: Double) {
        vidaRival = max(vidaRival - danioPorGolpe * multiplicador, 0)
        golpes++
        sumarCombo()
        marcar(if (potente) ResultadoAtaque.GOLPE_POTENTE else ResultadoAtaque.GOLPE, ahora)
        velocidadRival = (posicionRival - posicionJugador) * 1.25
        sonidos += Sonido.GOLPE
        if (vidaRival == 0) {
            guardiaActiva = false
            direccionJoystick = Punto.CERO
            fase = Fase.VICTORIA
        }
    }

    private fun marcar(resultado: ResultadoAtaque, ahora: Double) {
        resultadoAtaque = resultado
        momentoResultado = ahora
    }

    private fun sumarCombo() {
        combo++
        mejorCombo = max(mejorCombo, combo)
    }

    private fun limitar(v: Punto, maximo: Double): Punto = if (v.largo > maximo && v.largo > 0) v * (maximo / v.largo) else v

    private fun dentroDelRing(p: Punto, margen: Double) =
        Punto(p.x.coerceIn(MIN_RING + margen, MAX_RING - margen), p.y.coerceIn(MIN_RING + margen, MAX_RING - margen))

    private fun distancia() = (posicionJugador - posicionRival).largo

    private fun iniciarFase(nueva: Fase, duracion: Double, ahora: Double) {
        fase = nueva
        faseFin = ahora + duracion
    }

    companion object {
        val INICIO_JUGADOR = Punto(0.5, 0.66)
        val INICIO_RIVAL = Punto(0.5, 0.34)
        const val MIN_RING = 0.09
        const val MAX_RING = 0.91
        const val RADIO_GOLPE = 0.34
        private const val DISTANCIA_ATAQUE = 0.43
        private const val REBOTE = 1.35
        private const val DURACION_DESLIZAMIENTO = 0.9
    }
}
