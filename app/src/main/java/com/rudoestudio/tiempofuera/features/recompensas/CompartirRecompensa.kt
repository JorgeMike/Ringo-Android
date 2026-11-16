package com.rudoestudio.tiempofuera.features.recompensas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** `true` mientras se toma la imagen para compartir. En iOS: `\.preparandoImagenRecompensa`. */
val LocalPreparandoImagenRecompensa = compositionLocalOf { false }

/**
 * Los botones de una celebración no salen en la imagen que se comparte. Se vuelven
 * transparentes en vez de quitarse para que nada se mueva de lugar.
 */
internal fun Modifier.ocultoAlCompartir(): Modifier = composed {
    alpha(if (LocalPreparandoImagenRecompensa.current) 0f else 1f)
}

/**
 * Compartir una celebración (M7.8): el botón verde de arriba a la derecha toma la pantalla
 * sin sus botones y abre "Compartir" de Android. En iOS: `CompartirRecompensaModifier`.
 */
@Composable
internal fun CompartibleRecompensa(contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val capa = rememberGraphicsLayer()
    var preparando by remember { mutableStateOf(false) }
    var fallo by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    capa.record { this@drawWithContent.drawContent() }
                    drawLayer(capa)
                },
        ) {
            CompositionLocalProvider(LocalPreparandoImagenRecompensa provides preparando, content = contenido)
        }
        if (!preparando) {
            BotonS(
                ColoresTF.Verde, IconoBotonS.COMPARTIR, escala = 0.9f,
                alTocar = {
                    alcance.launch {
                        preparando = true
                        // Dos cuadros: uno para esconder los botones y otro ya dibujado sin ellos.
                        withFrameNanos { }
                        withFrameNanos { }
                        val imagen = runCatching { capa.toImageBitmap().asAndroidBitmap() }
                        preparando = false
                        imagen.mapCatching { compartir(contexto, it) }.onFailure {
                            AppLogger.error(it, "CompartibleRecompensa")
                            fallo = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
            )
        }
    }
    if (fallo) {
        AlertDialog(
            onDismissRequest = { fallo = false },
            confirmButton = { TextButton(onClick = { fallo = false }) { Text("Aceptar") } },
            title = { Text("No se pudo compartir la recompensa") },
            text = { Text("Inténtalo de nuevo.") },
        )
    }
}

/** Guarda la imagen en el caché (se reescribe cada vez) y abre el menú de compartir. */
private suspend fun compartir(contexto: Context, imagen: Bitmap) {
    val archivo = withContext(Dispatchers.IO) {
        File(contexto.cacheDir, "compartir").apply { mkdirs() }.resolve("recompensa.png").also { f ->
            f.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
    val enviar = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    contexto.startActivity(Intent.createChooser(enviar, "Compartir recompensa"))
}
