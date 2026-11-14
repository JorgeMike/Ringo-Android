package com.rudoestudio.tiempofuera.features.ligas

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.Arvo
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.PirataOne
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.ligas.Liga
import com.rudoestudio.tiempofuera.domain.ligas.ObtenerLigasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Las ligas para Inicio (M9.1). En iOS: `LigasListViewModel`. */
@HiltViewModel
class LigasListaViewModel @Inject constructor(obtenerLigas: ObtenerLigasUseCase) : ViewModel() {
    private val _ligas = MutableStateFlow<List<Liga>>(emptyList())
    val ligas: StateFlow<List<Liga>> = _ligas.asStateFlow()

    init {
        viewModelScope.launch {
            _ligas.value = try {
                obtenerLigas()
            } catch (e: Exception) {
                AppLogger.error(e, "LigasListaViewModel")
                emptyList()
            }
        }
    }
}

/** En iOS: `LigasListView`. */
@Composable
fun LigasLista(alAbrirLiga: (Liga) -> Unit, modifier: Modifier = Modifier, viewModel: LigasListaViewModel = hiltViewModel()) {
    val ligas by viewModel.ligas.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-4).dp)) {
        ligas.forEach { liga -> TarjetaLiga(liga) { alAbrirLiga(liga) } }
    }
}

/** Lo que dice cada liga debajo del nombre. En iOS: `LigaCardView.descripcionLiga`. */
private fun descripcion(ligaId: Int) = when (ligaId) {
    1 -> "Fomenta tu lectura"
    2 -> "Deja el ceeel"
    3 -> "Limpia tu mente"
    4 -> "¡A entrenar!"
    else -> "Achis achis, ¿aquí qué?"
}

private fun fondo(ligaId: Int) = when (ligaId) {
    2 -> R.drawable.liga2_card
    3 -> R.drawable.liga3_card
    4 -> R.drawable.liga4_card
    else -> R.drawable.liga1_card
}

/** En iOS: `LigaCardView`. */
@Composable
private fun TarjetaLiga(liga: Liga, alTocar: () -> Unit) {
    Box(Modifier.clickable(role = Role.Button, onClick = alTocar), contentAlignment = Alignment.Center) {
        Image(
            painterResource(fondo(liga.id)), null,
            Modifier.wrapContentSize(unbounded = true).offset(x = (-14).dp).graphicsLayer { scaleX = 1.2f; scaleY = 1.2f },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(240.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextoConSombra(liga.nombre, TextStyle(fontFamily = PirataOne, fontSize = 30.sp), colorSombra = Color.Black)
                TextoContorno(descripcion(liga.id), TextStyle(fontFamily = Arvo, fontWeight = FontWeight.Bold, fontSize = 18.sp), ColoresTF.Blanco, Color.Black, 2.dp)
            }
            val estrella = liga.luchadorEstrella
            ImagenPixel(AssetsIos.imagen(estrella.imagen) ?: AssetsIos.imagen(estrella.nombre) ?: R.drawable.ringo, 100.dp, Modifier.rotate(4f))
        }
    }
}
