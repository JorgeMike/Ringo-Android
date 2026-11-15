package com.rudoestudio.tiempofuera.features.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.configuracion.CampoEditable
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.EdicionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.navigation.EditarGrupoApps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoEdicionGrupo(
    val grupo: TipoGrupoApps,
    val cargando: Boolean = true,
    val campos: List<CampoEditable> = emptyList(),
    val error: String? = null,
)

/** En iOS: el estado de `ConfiguracionGrupoAppsEditView` y `guardarConfiguracionGrupoApps`. */
@HiltViewModel
class EditarGrupoAppsViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val configuraciones: ConfiguracionGrupoAppsRepository,
) : ViewModel() {
    private val grupo = TipoGrupoApps.desdeClave(estadoGuardado.toRoute<EditarGrupoApps>().grupo) ?: TipoGrupoApps.DISTRACCION
    private var actual: ConfiguracionGrupoApps? = null

    private val _estado = MutableStateFlow(EstadoEdicionGrupo(grupo))
    val estado: StateFlow<EstadoEdicionGrupo> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            actual = try {
                configuraciones.obtenerConfiguracion(grupo.clave)
            } catch (e: Exception) {
                AppLogger.error(e, "EditarGrupoAppsViewModel")
                null
            }
            _estado.update { it.copy(cargando = false, campos = EdicionGrupoApps.campos(grupo, actual)) }
        }
    }

    fun cambiar(campo: CampoEditable) = _estado.update { e -> e.copy(campos = e.campos.map { if (it.campo == campo.campo) campo else it }) }

    fun guardar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            try {
                // El motor de bloqueo lee la configuración solo: no hay que reprogramar nada como en iOS.
                configuraciones.guardar(EdicionGrupoApps.aplicar(grupo, actual, _estado.value.campos), grupo.clave)
                alTerminar()
            } catch (e: Exception) {
                AppLogger.error(e, "EditarGrupoAppsViewModel")
                _estado.update { it.copy(error = "No se pudo guardar. Intenta de nuevo.") }
            }
        }
    }
}

/**
 * Lo que se sigue de un grupo de apps (M11.3): cada campo se prende o apaga y se ajusta de
 * 5 en 5 minutos. En iOS: `ConfiguracionGrupoAppsEditView`.
 */
@Composable
fun EditarGrupoAppsScreen(alVolver: () -> Unit, viewModel: EditarGrupoAppsViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenidoEdicionGrupo(estado, alVolver, alCambiar = viewModel::cambiar, alGuardar = { viewModel.guardar(alVolver) })
}

@Composable
private fun ContenidoEdicionGrupo(estado: EstadoEdicionGrupo, alVolver: () -> Unit, alCambiar: (CampoEditable) -> Unit, alGuardar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(ColoresTF.Azul).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonRegresar(alVolver)
            Spacer(Modifier.width(12.dp))
            TextoContorno("Apps de ${estado.grupo.nombreVisible.lowercase()}", MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
        }
        if (estado.cargando) return@Column
        estado.campos.forEach { c -> TarjetaCampo(c, alCambiar) }
        BannerError(estado.error)
        BotonL(ColoresTF.Verde, "GUARDAR", alTocar = alGuardar, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun TarjetaCampo(c: CampoEditable, alCambiar: (CampoEditable) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ColoresTF.Blanco.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.campo.titulo, style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco, modifier = Modifier.weight(1f))
            Switch(
                checked = c.habilitado,
                onCheckedChange = { alCambiar(c.copy(habilitado = it)) },
                colors = SwitchDefaults.colors(checkedTrackColor = ColoresTF.Verde, checkedThumbColor = ColoresTF.Blanco),
            )
        }
        if (c.habilitado) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                BotonS(ColoresTF.Morado, IconoBotonS.ABAJO, alTocar = { alCambiar(c.ajustar(-5)) }, escala = 0.8f)
                TextoContorno(minutos(c.minutos), MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
                BotonS(ColoresTF.Verde, IconoBotonS.ARRIBA, alTocar = { alCambiar(c.ajustar(5)) }, escala = 0.8f)
            }
        }
        Text(c.campo.descripcion, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco.copy(alpha = 0.85f))
    }
}

private fun minutos(m: Int): String = if (m >= 60) "${m / 60} h${if (m % 60 > 0) " ${m % 60} min" else ""}" else "$m min"

@Preview(heightDp = 700)
@Composable
private fun EdicionGrupoPreview() = TiempoFueraTheme {
    val g = TipoGrupoApps.DISTRACCION
    ContenidoEdicionGrupo(
        EstadoEdicionGrupo(g, cargando = false, campos = EdicionGrupoApps.campos(g, ConfiguracionGrupoApps(g, tiempoMaximoDiario = 5400))),
        {}, {}, {},
    )
}
