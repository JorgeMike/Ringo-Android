package com.rudoestudio.tiempofuera.features.bloqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoApp
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.domain.bloqueo.AppInstalada
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps

/** Elegir las apps de un grupo (M3.2). En iOS lo hace el sistema con `FamilyActivityPicker`. */
@Composable
fun SelectorAppsScreen(alTerminar: () -> Unit, viewModel: SelectorAppsViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenidoSelector(
        grupo = viewModel.grupo,
        estado = estado,
        alBuscar = viewModel::buscar,
        alAlternar = viewModel::alternar,
        alGuardar = { viewModel.guardar(alTerminar) },
        alVolver = alTerminar,
    )
}

private fun descripcion(grupo: TipoGrupoApps) = when (grupo) {
    TipoGrupoApps.DISTRACCION -> "Las que te comen el tiempo. Se bloquean en tus misiones, a la hora de dormir y al llegar a tu límite."
    TipoGrupoApps.EDUCATIVAS -> "Las que usas para aprender. Úsalas y gana experiencia en tus misiones."
    else -> "Elige las apps de este grupo."
}

@Composable
private fun ContenidoSelector(
    grupo: TipoGrupoApps,
    estado: EstadoSelectorApps,
    alBuscar: (String) -> Unit,
    alAlternar: (String) -> Unit,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(ColoresTF.Azul).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BotonRegresar(alVolver)
            Spacer(Modifier.width(12.dp))
            TextoContorno("Apps de ${grupo.nombreVisible.lowercase()}", MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
        }
        Text(descripcion(grupo), style = MaterialTheme.typography.bodyMedium, color = ColoresTF.Blanco)
        OutlinedTextField(
            value = estado.consulta,
            onValueChange = alBuscar,
            label = { Text("Buscar app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColoresTF.Blanco, unfocusedTextColor = ColoresTF.Blanco,
                focusedBorderColor = ColoresTF.Amarillo, unfocusedBorderColor = ColoresTF.Blanco,
                focusedLabelColor = ColoresTF.Amarillo, unfocusedLabelColor = ColoresTF.Blanco,
            ),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (estado.cargando) {
                CircularProgressIndicator(color = ColoresTF.Amarillo)
            } else if (estado.visibles.isEmpty()) {
                Text("No encontramos esa app.", style = MaterialTheme.typography.bodyLarge, color = ColoresTF.Blanco)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(estado.visibles, key = { it.paquete }) { app ->
                        FilaApp(app, elegida = app.paquete in estado.elegidas, alAlternar = { alAlternar(app.paquete) })
                    }
                }
            }
        }
        BannerError(estado.error)
        val cuantas = estado.elegidas.size
        BotonL(ColoresTF.Amarillo, if (cuantas == 0) "GUARDAR" else "GUARDAR ($cuantas)", alTocar = alGuardar, habilitado = !estado.cargando)
    }
}

@Composable
private fun FilaApp(app: AppInstalada, elegida: Boolean, alAlternar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Checkbox, onClick = alAlternar).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoApp(app.paquete)
        Spacer(Modifier.width(12.dp))
        Text(
            app.nombre,
            style = MaterialTheme.typography.titleMedium,
            color = ColoresTF.Blanco,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = elegida,
            onCheckedChange = null, // lo maneja la fila completa
            colors = CheckboxDefaults.colors(checkedColor = ColoresTF.Amarillo, uncheckedColor = ColoresTF.Blanco, checkmarkColor = ColoresTF.Azul),
        )
    }
}

@Preview(heightDp = 700)
@Composable
private fun SelectorPreview() = TiempoFueraTheme {
    val apps = listOf(AppInstalada("com.instagram.android", "Instagram"), AppInstalada("com.google.android.youtube", "YouTube"))
    ContenidoSelector(TipoGrupoApps.DISTRACCION, EstadoSelectorApps(cargando = false, apps = apps, elegidas = setOf("com.instagram.android")), {}, {}, {}, {})
}
