package com.rudoestudio.tiempofuera.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rudoestudio.tiempofuera.MainActivity
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.perfil.EstadoWidgetRacha
import com.rudoestudio.tiempofuera.domain.perfil.ObtenerEstadoWidgetRachaUseCase
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.servicios.AlcanceApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Blanco = ColorProvider(Color.White)
private val Amarillo = ColorProvider(ColoresTF.Amarillo)
private val Verde = ColorProvider(Color(0xFF8FD16A))
/** Lo que mide el widget en su tamaño mínimo (2×2): de ahí crece todo parejo. */
private val Base = 110.dp

/**
 * El widget de la pantalla de inicio (M13.2): tus días invicto, tu luchador y si ya
 * hiciste la misión de hoy. Al tocarlo abre la app. Cuadrado va en columna; a lo ancho,
 * el luchador a la izquierda. Todo crece con el tamaño real del widget (`SizeMode.Exact`):
 * con tamaños fijos, uno grande se quedaba con el diseño chico en medio. En iOS:
 * `Ringo_Widget` (allá sigue siendo la plantilla).
 */
class WidgetRacha : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entrada = EntryPointAccessors.fromApplication(context, PuntoEntradaWidget::class.java)
        // Sin perfil (onboarding sin terminar) se invita a abrir la app.
        val estado = runCatching { entrada.estadoRacha()() }.onFailure { AppLogger.error(it, "WidgetRacha") }.getOrNull()
        val sprite = estado?.let { spriteNitido(context, AssetsIos.imagen(it.luchadorImagen) ?: AssetsIos.imagen(it.luchadorNombre) ?: R.drawable.ringo) }
        provideContent { Contenido(estado, sprite) }
    }

    @Composable
    private fun Contenido(estado: EstadoWidgetRacha?, sprite: Bitmap?) {
        Box(GlanceModifier.fillMaxSize().cornerRadius(20.dp).clickable(actionStartActivity<MainActivity>())) {
            // Los rayos azules del onboarding, oscurecidos para que el texto se lea.
            Image(ImageProvider(R.drawable.patron_azul), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(GlanceModifier.fillMaxSize().background(ColoresTF.Azul.copy(alpha = 0.6f))) {}
            Interior(estado, sprite)
        }
    }

    @Composable
    private fun Interior(estado: EstadoWidgetRacha?, sprite: Bitmap?) {
        val base = GlanceModifier.fillMaxSize().padding(10.dp)
        if (estado == null) {
            Column(base, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Abre Tiempo Fuera para empezar tu racha", style = TextStyle(color = Blanco, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
            }
            return
        }
        val luchador = sprite?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.ringo)
        val tamano = LocalSize.current
        val ancho = tamano.width >= tamano.height * 1.6f
        // Cuánto crece respecto al 2×2: por el lado que limita (alto si es ancho).
        val e = ((if (ancho) tamano.height else minOf(tamano.width, tamano.height)) / Base).coerceIn(1f, 2.2f)
        if (ancho) {
            Row(base, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Image(luchador, estado.luchadorNombre, GlanceModifier.size(80.dp * e))
                Spacer(GlanceModifier.width(12.dp))
                Column {
                    Dias(estado, e)
                    Text(estado.mensaje, style = TextStyle(color = colorMensaje(estado), fontSize = (13 * e).sp, fontWeight = FontWeight.Bold), maxLines = 2)
                    if (estado.protectores > 0) {
                        Text("🛡️ ${estado.protectores} protector${if (estado.protectores == 1) "" else "es"}", style = TextStyle(color = Blanco, fontSize = (12 * e).sp))
                    }
                }
            }
        } else {
            Column(base, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Dias(estado, e)
                Image(luchador, estado.luchadorNombre, GlanceModifier.size(44.dp * e))
                Text(estado.mensaje, style = TextStyle(color = colorMensaje(estado), fontSize = (11 * e).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), maxLines = 2)
            }
        }
    }

    @Composable
    private fun Dias(estado: EstadoWidgetRacha, e: Float) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.racha_icon), null, GlanceModifier.size(24.dp * e, 32.dp * e))
            Spacer(GlanceModifier.width(4.dp))
            Text("${estado.diasRacha}", style = TextStyle(color = Blanco, fontSize = (30 * e).sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(4.dp))
            Text(if (estado.diasRacha == 1) "día\ninvicto" else "días\ninvicto", style = TextStyle(color = Blanco, fontSize = (11 * e).sp))
        }
    }

    private fun colorMensaje(estado: EstadoWidgetRacha) = if (estado.activadaHoy) Verde else Amarillo

    /**
     * Los sprites miden 32–64 px: el widget los agranda suavizados y salen borrosos. Aquí se
     * agrandan antes, pixel por pixel, como `ImagenPixel` en la app.
     */
    private fun spriteNitido(context: Context, imagen: Int): Bitmap? {
        val original = BitmapFactory.decodeResource(context.resources, imagen, BitmapFactory.Options().apply { inScaled = false }) ?: return null
        val factor = (256 / maxOf(original.width, original.height)).coerceAtLeast(1)
        return Bitmap.createScaledBitmap(original, original.width * factor, original.height * factor, false)
    }
}

class ReceptorWidgetRacha : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetRacha()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoEntradaWidget {
    fun estadoRacha(): ObtenerEstadoWidgetRachaUseCase
}

/**
 * Vuelve a dibujar el widget cuando cambia lo que muestra. Con la app abierta lo hace al
 * cambiar el perfil (racha, protectores, luchador); con la app cerrada, `FinDeDiaWorker`
 * cada hora (así cambia también al empezar un día nuevo).
 */
@Singleton
class ActualizadorWidgetRacha @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val perfiles: UserProfileRepository,
    @AlcanceApp private val alcance: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun iniciar() {
        alcance.launch {
            perfiles.perfil
                .map { listOf(it.diasRacha, it.protectoresRacha, it.lastActivityDate, it.luchadorSeleccionado.imagen) }
                .distinctUntilChanged()
                .debounce(1_000)
                .collect { actualizar() }
        }
    }

    suspend fun actualizar() {
        runCatching { WidgetRacha().updateAll(contexto) }.onFailure { AppLogger.error(it, "ActualizadorWidgetRacha") }
    }
}
