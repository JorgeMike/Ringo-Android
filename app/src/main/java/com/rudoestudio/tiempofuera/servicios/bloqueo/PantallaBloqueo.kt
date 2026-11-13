package com.rudoestudio.tiempofuera.servicios.bloqueo

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.domain.bloqueo.MotivoBloqueo

/**
 * La pantalla "¡TIEMPO FUERA!" que tapa una app bloqueada: el equivalente al shield de
 * `ManagedSettings` en iOS (allá es la pantalla genérica de Apple). Es una ventana
 * `TYPE_ACCESSIBILITY_OVERLAY`, que no pide permiso extra, dibujada con Compose.
 */
class PantallaBloqueo(private val contexto: Context) {
    private val wm = contexto.getSystemService(WindowManager::class.java)
    private var vista: ComposeView? = null
    private var dueno: DuenoVentana? = null
    private var zonaActual: Zona? = null

    var flotante: Boolean = false
        private set

    /** `false` si Android no dejó dibujar la ventana. */
    fun mostrar(nombreApp: String, motivo: MotivoBloqueo, zona: Zona, flotante: Boolean, alSalir: () -> Unit): Boolean {
        if (vista != null) return true
        val dueno = DuenoVentana().also { it.iniciar() }
        val composeView = ComposeView(contexto).apply {
            setViewTreeLifecycleOwner(dueno)
            setViewTreeSavedStateRegistryOwner(dueno)
            setContent { TiempoFueraTheme { ContenidoBloqueo(nombreApp, motivo, flotante, alSalir) } }
        }
        this.flotante = flotante
        return runCatching { wm.addView(composeView, parametros(zona)) }
            .onSuccess {
                vista = composeView
                this.dueno = dueno
                zonaActual = zona
            }
            .onFailure { dueno.destruir() }
            .isSuccess
    }

    /** Reacomoda la pantalla si la ventana de la app se movió o cambió de tamaño. */
    fun mover(zona: Zona) {
        val v = vista ?: return
        if (zona == zonaActual) return
        runCatching { wm.updateViewLayout(v, parametros(zona)) }.onSuccess { zonaActual = zona }
    }

    fun ocultar() {
        vista?.let { runCatching { wm.removeView(it) } }
        dueno?.destruir()
        vista = null
        dueno = null
        zonaActual = null
    }

    private fun parametros(zona: Zona) = WindowManager.LayoutParams(
        zona.ancho,
        zona.alto,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = zona.izquierda
        y = zona.arriba
    }
}

@Composable
fun ContenidoBloqueo(nombreApp: String, motivo: MotivoBloqueo, flotante: Boolean, alSalir: () -> Unit) {
    val mensaje = when {
        // Ir al inicio no cierra una ventana flotante: se cierra con su barra de arriba.
        flotante -> "Ciérrala desde la barrita de arriba ☝️"
        motivo == MotivoBloqueo.MISION -> "Estás en plena misión. $nombreApp te espera al final del round."
        motivo == MotivoBloqueo.SUENO -> "Ya es hora de dormir, campeón. $nombreApp abre mañana."
        else -> "Se acabó tu tiempo de $nombreApp por hoy. Si de plano lo necesitas, en la tienda hay tiempo extra."
    }
    Column(
        modifier = Modifier.fillMaxSize().background(ColoresTF.Azul).padding(if (flotante) 16.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val titulo = if (flotante) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge
        TextoContorno("¡TIEMPO FUERA!", titulo, ColoresTF.Amarillo, ColoresTF.AzulNoche, 3.dp)
        Spacer(Modifier.height(if (flotante) 8.dp else 24.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, color = ColoresTF.Blanco, textAlign = TextAlign.Center)
        if (!flotante) {
            Spacer(Modifier.height(40.dp))
            BotonL(ColoresTF.Rojo, "Salir", alTocar = alSalir)
        }
    }
}

/** Compose necesita un ciclo de vida; una ventana suelta de un servicio no lo tiene. */
private class DuenoVentana : LifecycleOwner, SavedStateRegistryOwner {
    private val registro = LifecycleRegistry(this)
    private val controlador = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registro
    override val savedStateRegistry: SavedStateRegistry get() = controlador.savedStateRegistry

    fun iniciar() {
        controlador.performRestore(null)
        registro.currentState = Lifecycle.State.RESUMED
    }

    fun destruir() {
        registro.currentState = Lifecycle.State.DESTROYED
    }
}

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun ContenidoBloqueoPreview() = TiempoFueraTheme { ContenidoBloqueo("YouTube", MotivoBloqueo.LIMITE, flotante = false) {} }
