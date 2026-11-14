package com.rudoestudio.tiempofuera.core.designsystem.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp

/**
 * Imagen pixel art (sin suavizado), con ancho fijo. En iOS: `.resizable().interpolation(.none)
 * .scaledToFit().frame(width:)`. La sombra suave que iOS les pone (`.shadow`) no se porta:
 * en Compose solo sigue formas.
 *
 * La proporción va explícita: los sprites miden 32 o 64 px y, con solo el ancho, Compose
 * deja la altura en esos pixeles y la imagen sale diminuta.
 */
@Composable
fun ImagenPixel(@DrawableRes imagen: Int, ancho: Dp, modifier: Modifier = Modifier, descripcion: String? = null) {
    val mapa = ImageBitmap.imageResource(imagen)
    Image(
        mapa,
        contentDescription = descripcion,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = modifier.width(ancho).aspectRatio(mapa.width.toFloat() / mapa.height),
    )
}
