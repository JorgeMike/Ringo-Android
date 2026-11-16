package com.rudoestudio.tiempofuera.features.recompensas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenIos1x
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.espiral
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.recompensas.EstadoRecompensas
import com.rudoestudio.tiempofuera.domain.recompensas.FlujoRecompensa
import com.rudoestudio.tiempofuera.domain.recompensas.PantallaRecompensa
import kotlinx.coroutines.delay

/**
 * La celebración que pidieron con [PresentadorRecompensas], encima de todo. Va en
 * `MainScaffold`; el onboarding usa [RecompensaFlujo] directo.
 */
@Composable
fun RecompensasEncima(viewModel: RecompensasViewModel = hiltViewModel()) {
    val flujo by viewModel.presentado.collectAsStateWithLifecycle()
    flujo?.let { RecompensaFlujo(it, alTerminar = viewModel::cerrar, viewModel = viewModel) }
}

/**
 * Las pantallas de una celebración, una tras otra (M7.0). Tapa los toques de abajo y el
 * botón de regresar no la salta. En iOS: `RecompensaFlowContainerView`.
 */
@Composable
fun RecompensaFlujo(flujo: FlujoRecompensa, alTerminar: () -> Unit, viewModel: RecompensasViewModel = hiltViewModel()) {
    var estado by remember(flujo) { mutableStateOf(EstadoRecompensas.iniciar(flujo)) }
    val luchador by viewModel.luchadorEquipado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { if (estado.terminado) alTerminar() }
    BackHandler {}

    Box(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Azul)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        CompartibleRecompensa {
        AnimatedContent(estado, contentKey = { it.paso }, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "recompensa") { e ->
            when (val p = e.pantalla) {
                is PantallaRecompensa.Sobre -> SobrePersonajePantalla(
                    rareza = p.rareza,
                    viewModel = viewModel,
                    alFallar = { estado = estado.sobreConError() },
                    alRecolectar = { l, r -> estado = estado.sobreRecolectado(l, r) },
                )
                is PantallaRecompensa.RachaActivada -> RachaActivadaPantalla(p.diasDeRacha, luchador) { estado = estado.continuar() }
                is PantallaRecompensa.MisionCompletada -> MisionCompletadaPantalla(p.recompensaMision, luchador, viewModel::campana) { estado = estado.continuar() }
                is PantallaRecompensa.SubidaNivel -> SubidaNivelPantalla(p.resultado, p.luchador, viewModel) { estado = estado.continuar() }
                is PantallaRecompensa.Pinata -> PinataPantalla(p.rarezas) { estado = estado.continuar() }
                is PantallaRecompensa.LuchaGanada -> LuchaGanadaPantalla(p.recompensa, p.luchador, p.rival, viewModel::campana) { estado = estado.continuar() }
                null -> Unit
            }
        }
        }
    }
}

/**
 * Aparecen los elementos uno tras otro: cada uno espera su retardo después del anterior.
 * Regresa cuántos van. En iOS: el `guion` de cada vista.
 */
@Composable
internal fun rememberGuion(retardosMs: List<Long>): Int {
    var visibles by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        retardosMs.forEach {
            delay(it)
            visibles++
        }
    }
    return visibles
}

/** Entra en espiral cuando [visible] (en iOS: `EspiralEffect` con resorte de 1.1 s). */
@Composable
internal fun Modifier.espiralAlAparecer(visible: Boolean): Modifier {
    val avance = remember { Animatable(0f) }
    LaunchedEffect(visible) { if (visible) avance.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 30f)) }
    return espiral(avance.value).alpha(avance.value.coerceIn(0f, 1f))
}

/**
 * "¡N días de racha!" (M7.4): estrellas, el luchador en espiral, el badge con el número y
 * el ícono. En iOS: `RachaActivadaView`.
 */
@Composable
internal fun RachaActivadaPantalla(dias: Int, luchador: Int?, alContinuar: () -> Unit) {
    // estrella1, estrella2, estrella3, luchador, badge, número, ícono, "días", mensaje, botón
    val v = rememberGuion(listOf(0, 120, 120, 150, 700, 180, 150, 200, 200, 300))
    Box(Modifier.fillMaxSize().background(ColoresTF.Celeste).clipToBounds()) {
        val amarillo = ColorFilter.tint(ColoresTF.Amarillo)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.estrella_fondo), null, colorFilter = amarillo, modifier = Modifier.wrapContentSize(unbounded = true).pop(v > 0))
            Image(painterResource(R.drawable.estrella2_fondo), null, colorFilter = amarillo, modifier = Modifier.wrapContentSize(unbounded = true).offset(40.dp, (-50).dp).pop(v > 1))
            Image(painterResource(R.drawable.estrella3_fondo), null, colorFilter = amarillo, modifier = Modifier.wrapContentSize(unbounded = true).offset(30.dp, (-60).dp).pop(v > 2))
        }
        // Los 8 dp entre elementos son los del `VStack` de iOS: sin ellos, con los mismos
        // traslapes, el número quedaba encima de "días de racha" y el luchador del número.
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            ImagenPixel(luchador ?: R.drawable.ringo, 280.dp, Modifier.offset(y = 48.dp).espiralAlAparecer(v > 3))
            Box(contentAlignment = Alignment.Center) {
                ImagenIos1x(R.drawable.racha_badge, Modifier.pop(v > 4))
                Row(Modifier.offset(8.dp, 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextoContorno("$dias", pirata(48), ColoresTF.Blanco, Color.Black, 5.dp, Modifier.pop(v > 5))
                    Image(painterResource(R.drawable.racha_icon), null, Modifier.pop(v > 6, rebote = 0.4f))
                }
            }
            // iOS sube 46; aquí 30: con Pirata One en Android tapaba el pie del número y del fueguito.
            TextoContorno("días de racha", pirata(36), ColoresTF.Blanco, Color.Black, 5.dp, Modifier.offset(y = (-30).dp).aparece(v > 7))
            TextoContorno(
                "Lo estás haciendo increíble y debes sentirte orgulloso de ello.",
                arvo(18), ColoresTF.Blanco, Color.Black, 2.dp,
                alineacion = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 46.dp).offset(y = (-18).dp).aparece(v > 8),
            )
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Celeste, "¡¡VAMOOOOOS!!", alTocar = alContinuar, habilitado = v > 9, modifier = Modifier.pop(v > 9).ocultoAlCompartir())
        }
    }
}

/**
 * "¡Misión completada!" (M7.2): el luchador en espiral y lo que ganó, con la campana de
 * Ringo. En iOS: `MisionCompletadaView`.
 */
@Composable
internal fun MisionCompletadaPantalla(recompensa: RecompensaMision, luchador: Int?, alSonar: () -> Unit, alContinuar: () -> Unit) {
    LaunchedEffect(Unit) { alSonar() }
    // título, luchador, recompensa, botón
    val v = rememberGuion(listOf(0, 250, 450, 300))
    Box(Modifier.fillMaxSize().background(ColoresTF.FondoMisiones).clipToBounds()) {
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(ColoresTF.Celeste),
            modifier = Modifier.align(Alignment.Center).requiredSize(2200.dp).alpha(0.24f),
        )
        Box(Modifier.fillMaxSize().background(ColoresTF.FondoMisiones.copy(alpha = 0.5f)))
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.aparece(v > 0)) {
                TextoConSombra("¡Misión completada!", pirata(42), colorSombra = Color.Black, grosor = 2.dp)
                Text("Recompensa obtenida:", style = arvo(18), color = ColoresTF.Blanco)
            }
            ImagenPixel(luchador ?: R.drawable.ringo, 200.dp, Modifier.espiralAlAparecer(v > 1))
            Box(Modifier.pop(v > 2, rebote = 0.7f), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.recompensa_lista), null, Modifier.matchParentSize().offset(8.dp, 20.dp))
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recompensa.exp?.takeIf { it > 0 }?.let { FilaRecompensa("Puntos de experiencia:", null, "$it EXP") }
                    recompensa.puntosPoder?.takeIf { it > 0 }?.let { FilaRecompensa("Puntos de poder:", R.drawable.puntos_poder_icon, "$it") }
                    recompensa.monedas?.takeIf { it > 0 }?.let { FilaRecompensa("Monedas:", R.drawable.moneda_icon, "$it") }
                    recompensa.protectorRacha?.takeIf { it > 0 }?.let { FilaRecompensa("Protector de racha:", R.drawable.racha_icon, "$it") }
                }
            }
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Celeste, "TRAE PA' ACÁ", alTocar = alContinuar, habilitado = v > 3, modifier = Modifier.pop(v > 3).ocultoAlCompartir())
        }
    }
}

@Composable
internal fun FilaRecompensa(etiqueta: String, icono: Int?, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextoContorno(etiqueta, arvo(16), ColoresTF.Blanco, Color.Black, 2.dp)
        Row(
            Modifier.background(ColoresTF.Blanco, DiagonalShape(11.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icono?.let { Image(painterResource(it), null, Modifier.size(28.dp)) }
            Text(valor, style = arvo(18), color = ColoresTF.Azul, maxLines = 1)
        }
    }
}
