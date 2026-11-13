package com.rudoestudio.tiempofuera.servicios.bloqueo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.bloqueo.AppBlockingRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.AppSelectionRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.GrupoBloqueado
import com.rudoestudio.tiempofuera.domain.bloqueo.MonitorBloqueo
import com.rudoestudio.tiempofuera.domain.bloqueo.UsoApps
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * El bloqueo de apps (M3.5). Android avisa en cuanto cambian las ventanas; en cada cambio
 * se le pregunta qué ventanas de apps se ven (`getWindows()`) y se tapa solo la de cada
 * app bloqueada, con sus coordenadas. Seguir "la última ventana que avisó" no sirve: la
 * barra de captura de Samsung y la pantalla dividida engañaban al spike.
 *
 * Qué está bloqueado lo decide [MonitorBloqueo] (cada 5 s y cuando cambia un bloqueo); los
 * eventos de ventana solo usan esa respuesta guardada, para tapar en ~100 ms.
 */
@AndroidEntryPoint
class ServicioBloqueo : AccessibilityService() {
    @Inject
    lateinit var monitor: MonitorBloqueo

    @Inject
    lateinit var uso: UsoApps

    @Inject
    lateinit var selecciones: AppSelectionRepository

    @Inject
    lateinit var bloqueos: AppBlockingRepository

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var bloqueados: Map<String, GrupoBloqueado> = emptyMap()
    private val pantallas = HashMap<String, PantallaBloqueo>()
    private val seguidor by lazy { SeguidorVentanas(barraFlotantePx = dp(BARRA_FLOTANTE_DP)) }
    private var revisionDiferida: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLogger.info("Bloqueo: servicio conectado")
        alcance.launch {
            while (isActive) {
                actualizar()
                delay(INTERVALO_MONITOR_MS)
            }
        }
        // Con un bloqueo puesto se revisan las ventanas cada segundo: así se quita rápido
        // si la app se fue y Android no avisó.
        alcance.launch {
            while (isActive) {
                delay(1_000)
                if (pantallas.isNotEmpty()) reevaluar(latenciaMs = null)
            }
        }
        // Una misión que empieza o un tiempo extra comprado se aplican al instante.
        alcance.launch {
            merge(selecciones.selecciones.drop(1), bloqueos.misiones.drop(1), bloqueos.tiemposExtra.drop(1)).collect { actualizar() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        reevaluar(latenciaMs = SystemClock.uptimeMillis() - event.eventTime)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        // M3.12: aquí se entera de que apagaron la accesibilidad.
        AppLogger.warning("Bloqueo: servicio desconectado")
        alcance.cancel()
        quitarTodas()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        alcance.cancel()
        quitarTodas()
        super.onDestroy()
    }

    private suspend fun actualizar() {
        bloqueados = try {
            monitor.evaluar()
        } catch (e: Exception) {
            AppLogger.error(e, "ServicioBloqueo.actualizar")
            bloqueados
        }
        reevaluar(latenciaMs = null)
    }

    /** Ventanas de apps (sin sistema, teclado ni nuestras pantallas) visibles ahora. */
    private fun ventanasDeApps(): Map<String, Zona> {
        val resultado = HashMap<String, Zona>()
        for (ventana in windows) {
            if (ventana.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
            val paquete = ventana.root?.packageName?.toString() ?: continue
            if (paquete == packageName) continue
            val r = Rect().also(ventana::getBoundsInScreen)
            resultado[paquete] = Zona(r.left, r.top, r.right, r.bottom)
        }
        return resultado
    }

    private fun reevaluar(latenciaMs: Long?) {
        val visibles = ventanasDeApps()
        val aTapar = visibles.filterKeys { it in bloqueados }

        // Quitar el bloqueo de una app solo si ya no se ve Y Android dice que ya no está en
        // primer plano: nuestra pantalla puede tapar tanto que Android deja de reportar la
        // ventana de abajo (en la captura de pantalla dejó de verla ~400 ms).
        val sinVentana = pantallas.keys - aTapar.keys
        if (sinVentana.isNotEmpty()) {
            alcance.launch {
                val abiertas = runCatching { uso.enPrimerPlano() }.getOrDefault(emptySet())
                sinVentana.filter { it !in abiertas || it in visibles || it !in bloqueados }.forEach(::quitar)
            }
        }

        seguidor.conservarSolo(aTapar.keys)
        val pantalla = tamanoPantalla()
        val ahora = SystemClock.uptimeMillis()
        aTapar.forEach { (paquete, limites) ->
            val decision = seguidor.decidir(paquete, limites, pantalla, ahora)
            decision.revisarEnMs?.let(::programarRevision)
            val existente = pantallas[paquete]
            val zona = decision.zona
            when {
                zona == null -> if (existente != null) quitar(paquete)
                existente == null -> mostrar(paquete, zona, decision.flotante, latenciaMs)
                existente.flotante != decision.flotante -> {
                    // Pasó de flotante a pantalla completa o al revés: rehacer el letrero.
                    quitar(paquete)
                    mostrar(paquete, zona, decision.flotante, latenciaMs)
                }
                else -> existente.mover(zona)
            }
        }
    }

    private fun programarRevision(enMs: Long) {
        revisionDiferida?.cancel()
        revisionDiferida = alcance.launch {
            delay(enMs)
            reevaluar(latenciaMs = null)
        }
    }

    private fun mostrar(paquete: String, zona: Zona, flotante: Boolean, latenciaMs: Long?) {
        val grupo = bloqueados[paquete] ?: return
        val pantalla = PantallaBloqueo(this)
        val ok = pantalla.mostrar(nombreDeApp(this, paquete), grupo.motivo, zona, flotante) {
            quitarTodas()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        if (!ok) {
            AppLogger.warning("Bloqueo: Android no dejó dibujar la pantalla sobre $paquete")
            return
        }
        pantallas[paquete] = pantalla
        AppLogger.info("Bloqueo: $paquete (${grupo.motivo})" + (latenciaMs?.let { " en $it ms" } ?: ""))
    }

    private fun quitar(paquete: String) {
        pantallas.remove(paquete)?.ocultar()
    }

    private fun quitarTodas() {
        pantallas.keys.toList().forEach(::quitar)
    }

    private fun tamanoPantalla(): Zona = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val b = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        Zona(b.left, b.top, b.right, b.bottom)
    } else {
        Zona(0, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    private companion object {
        const val INTERVALO_MONITOR_MS = 5_000L

        /** Alto de la barra de Samsung arriba de una ventana flotante (para moverla o cerrarla). */
        const val BARRA_FLOTANTE_DP = 40
    }
}
