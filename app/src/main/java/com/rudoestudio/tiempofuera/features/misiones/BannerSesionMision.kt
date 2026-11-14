package com.rudoestudio.tiempofuera.features.misiones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.ObtenerMisionPorIdUseCase
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionTiempo
import com.rudoestudio.tiempofuera.domain.misiones.SesionesMision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

/** La misión en curso con su sesión, para el aviso de arriba. */
data class MisionEnCurso(val mision: Mision, val sesion: SesionTiempo)

@HiltViewModel
class BannerSesionViewModel @Inject constructor(
    sesiones: SesionesMision,
    private val obtenerMision: ObtenerMisionPorIdUseCase,
) : ViewModel() {
    val enCurso: StateFlow<MisionEnCurso?> = sesiones.sesion
        .map { s -> s?.let { obtenerMision(it.misionId)?.let { m -> MisionEnCurso(m, it) } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * Aviso arriba de las tabs mientras hay una misión con reloj (temporizador o meditación): muestra el tiempo y
 * regresa a la misión al tocarlo (M5.4). En iOS: `SesionMisionBannerView`.
 */
@Composable
fun BannerSesionMision(alAbrir: (Mision) -> Unit, visible: Boolean, viewModel: BannerSesionViewModel = hiltViewModel()) {
    val enCurso by viewModel.enCurso.collectAsStateWithLifecycle()
    AnimatedVisibility(visible && enCurso != null, enter = expandVertically(), exit = shrinkVertically()) {
        enCurso?.let { BannerContenido(it, alAbrir) }
    }
}

@Composable
private fun BannerContenido(enCurso: MisionEnCurso, alAbrir: (Mision) -> Unit) {
    var ahora by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(enCurso.sesion) {
        while (true) {
            ahora = Instant.now()
            delay(1_000)
        }
    }
    val (mision, sesion) = enCurso
    val estado = RelojMision.textoEstado(mision, sesion, ahora)

    Row(
        Modifier
            .fillMaxWidth()
            .background(ColoresTF.Azul)
            .clickable { alAbrir(mision) }
            .statusBarsPadding()
            .heightIn(min = 44.dp)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Volver a ${mision.titulo}, $estado" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⏱", style = arvo(18))
        Column(Modifier.weight(1f)) {
            Text(mision.titulo, style = arvo(14), color = ColoresTF.Blanco, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Volver a la misión", style = arvo(11), color = ColoresTF.Blanco.copy(alpha = 0.8f))
        }
        Text(estado, style = arvo(14), color = ColoresTF.Blanco)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ColoresTF.Blanco)
    }
}
