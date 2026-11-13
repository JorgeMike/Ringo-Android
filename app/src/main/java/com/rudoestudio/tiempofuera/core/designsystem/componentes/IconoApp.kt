package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ícono de una app instalada, por paquete. Se carga fuera del hilo principal; mientras,
 * un cuadro gris. No existe en iOS: allá los íconos los pinta el sistema (`Label(token)`).
 */
@Composable
fun IconoApp(paquete: String, modifier: Modifier = Modifier, tamano: Dp = 40.dp) {
    val contexto = LocalContext.current
    val pixeles = with(LocalDensity.current) { tamano.roundToPx() }
    var icono by remember(paquete, pixeles) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(paquete, pixeles) {
        icono = withContext(Dispatchers.IO) {
            runCatching { contexto.packageManager.getApplicationIcon(paquete).toBitmap(pixeles, pixeles).asImageBitmap() }.getOrNull()
        }
    }
    val actual = icono
    if (actual != null) {
        Image(actual, contentDescription = null, modifier = modifier.size(tamano))
    } else {
        Box(modifier.size(tamano).background(ColoresTF.GrisClaro, RoundedCornerShape(10.dp)))
    }
}
