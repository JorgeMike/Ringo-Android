package com.rudoestudio.tiempofuera.features.misiones

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.SeccionRecompensas
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision

internal fun arvo(tamano: Int) = TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = tamano.sp)

internal fun pirata(tamano: Int) = TextStyle(fontFamily = PirataOne, fontSize = tamano.sp)

/**
 * Una misión a pantalla completa (M6.0): tutorial y luego la misión de su tipo. Regresar
 * no detiene un temporizador: se puede volver desde el aviso de arriba (M5.4). En iOS:
 * `MisionFlowContainerView` + `MisionView`.
 */
@Composable
fun MisionFlujoScreen(
    alCerrar: () -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
    viewModel: MisionFlujoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.reclamada) { if (estado.reclamada) alCerrar() }
    BackHandler(onBack = alCerrar)

    FondoMision {
        val mision = estado.mision
        when {
            estado.cargando -> CircularProgressIndicator(color = ColoresTF.Blanco, modifier = Modifier.align(Alignment.Center))
            mision == null -> MensajeMision("Esta misión ya no existe.", "Regresa a Inicio y elige otra.")
            else -> AnimatedContent(estado.tutorialVisto, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tutorial") { visto ->
                if (!visto) {
                    MisionPrevia(mision, estado.luchador, viewModel::comenzar)
                } else {
                    ContenidoMision(mision, estado, viewModel, alElegirApps)
                }
            }
        }
        BotonRegresar(alCerrar, Modifier.safeDrawingPadding().padding(start = 6.dp, top = 6.dp))

        if (estado.guardando) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = ColoresTF.Amarillo) }
        }
    }

    estado.error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = viewModel::limpiarError,
            confirmButton = { TextButton(onClick = viewModel::limpiarError) { Text("Cerrar") } },
            title = { Text("Algo salió mal") },
            text = { Text(mensaje) },
        )
    }
}

@Composable
private fun ContenidoMision(mision: Mision, estado: EstadoFlujo, viewModel: MisionFlujoViewModel, alElegirApps: (TipoGrupoApps) -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(top = 38.dp)) {
        when (mision.tipoMision) {
            TipoMision.TIEMPO -> TiempoMision(mision, estado, viewModel, alElegirApps)
            TipoMision.ACCIONES -> AccionesMision(mision, estado.luchador, alReclamar = { viewModel.reclamar() })
            else -> MensajeMision(
                "Esta misión llega muy pronto",
                "Todavía la estamos armando para Android (${tareaPendiente(mision.tipoMision)}). Mientras, échale ganas a las demás.",
            )
        }
    }
}

/** La tarea del tablero de cada tipo que aún no existe en Android. */
private fun tareaPendiente(tipo: TipoMision?) = when (tipo) {
    TipoMision.MEDITACION -> "M6.3"
    TipoMision.LECTURAS -> "M6.5"
    TipoMision.SECUENCIA -> "M6.6"
    TipoMision.ESCENA -> "M6.7"
    TipoMision.TOMAR_FOTO -> "M6.8"
    TipoMision.LAGARTIJAS -> "M6.9"
    TipoMision.TRACKING -> "M6.10"
    TipoMision.CARRERA -> "M6.11"
    else -> "M6"
}

/** Fondo azul de misión con rayos celestes tenues. En iOS: el fondo de `MisionFlowContainerView`. */
@Composable
private fun FondoMision(contenido: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(ColoresTF.FondoMisiones).clipToBounds()) {
        Image(
            painterResource(R.drawable.rayos), null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(ColoresTF.Celeste),
            modifier = Modifier.align(Alignment.Center).requiredSize(2200.dp).alpha(0.24f),
        )
        Box(Modifier.fillMaxSize().background(ColoresTF.FondoMisiones.copy(alpha = 0.5f)))
        contenido()
    }
}

@Composable
private fun MensajeMision(titulo: String, texto: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        TextoConSombra(titulo, pirata(32), colorSombra = ColoresTF.Celeste, alineacion = TextAlign.Center)
        Text(texto, style = arvo(16), color = ColoresTF.Blanco, textAlign = TextAlign.Center)
    }
}

/** Instrucciones de cada tipo. En iOS: `MisionPreviaView.instrucciones`. */
private fun instrucciones(tipo: TipoMision?) = when (tipo) {
    TipoMision.LAGARTIJAS -> "Pon tu celular en una superficie plana y haz tus repeticiones frente a la cámara frontal."
    TipoMision.ESCENA -> "Memoriza dónde está cada objeto de la escena y luego regrésalos a su lugar."
    TipoMision.SECUENCIA -> "Fíjate en la secuencia de colores de los tambores y repítela sin equivocarte."
    TipoMision.MEDITACION -> "Inhala y exhala al ritmo de la pantalla. Junta vueltas completas y termina cuando quieras para reclamar."
    TipoMision.TOMAR_FOTO -> "Busca a tu alrededor algo del color que te pedimos y tómale foto."
    TipoMision.TIEMPO -> "Aguanta concentrado el tiempo de la misión. Puedes moverte por Tiempo Fuera y regresar sin detener el reloj."
    TipoMision.ACCIONES -> "Haz todas las repeticiones que pide la misión para cumplir tu objetivo del día."
    TipoMision.TRACKING -> "Usa tus apps dentro del tiempo permitido para juntar recompensas."
    TipoMision.CARRERA -> "Registra tu carrera con GPS. Corre al menos 1 km para reclamar tu recompensa."
    TipoMision.LECTURAS -> "Lee con calma y contesta las preguntas."
    null -> "Sigue las instrucciones en pantalla para completar tu misión y reclamar tus recompensas."
}

/** "Tutorial de misión". En iOS: `MisionPreviaView`. */
@Composable
private fun MisionPrevia(mision: Mision, luchador: Int?, alComenzar: () -> Unit) {
    val forma = remember { DiagonalShape(12.dp) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(top = 38.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Titulo("TUTORIAL", "de misión", Modifier.padding(top = 24.dp))
        Box(Modifier.padding(top = 24.dp), contentAlignment = Alignment.Center) {
            ImagenPixel(luchador ?: R.drawable.ringo, 140.dp, Modifier.offset(x = (-8).dp).rotate(-6f))
            ImagenPixel(R.drawable.i_5_kz_hover, 240.dp, Modifier.offset(8.dp, 10.dp).rotate(4f))
        }
        Box(Modifier.offset(y = (-68).dp).padding(horizontal = 24.dp)) {
            Box(Modifier.matchParentSize().offset(12.dp, 12.dp).background(Color.Black, forma))
            Box(Modifier.matchParentSize().background(ColoresTF.Celeste, forma))
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextoConSombra("¿Cómo jugar?", pirata(36), colorSombra = ColoresTF.Azul)
                Text(instrucciones(mision.tipoMision), style = arvo(15), color = ColoresTF.Blanco.copy(alpha = 0.95f), textAlign = TextAlign.Center)
            }
            SeccionRecompensas(
                mision.recompensa.puntosPoder, mision.recompensa.monedas, mision.recompensa.exp,
                Modifier.align(Alignment.BottomEnd).offset(32.dp, 40.dp).graphicsLayer { scaleX = 0.75f; scaleY = 0.75f },
            )
        }
        BotonL(ColoresTF.Amarillo, "VAMOS", alTocar = alComenzar, modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Preview(heightDp = 860)
@Composable
private fun PreviaPreview() = TiempoFueraTheme {
    FondoMision { MisionPrevia(DatosPreview.misionesDiarias.first(), null) {} }
}
