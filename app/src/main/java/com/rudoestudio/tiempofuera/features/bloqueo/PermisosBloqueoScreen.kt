package com.rudoestudio.tiempofuera.features.bloqueo

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadoPermisos
import com.rudoestudio.tiempofuera.domain.bloqueo.GuiaFabricante
import com.rudoestudio.tiempofuera.domain.bloqueo.GuiasFabricante

/**
 * Los permisos del bloqueo, uno por tarjeta (M3.1). En iOS es la parte de permisos del
 * paso 10 del onboarding (`ConfPermisosView`); el onboarding de Android (M4.11) usará esta
 * pantalla. La accesibilidad pasa antes por [AvisoAccesibilidadScreen], que pide Google Play.
 */
@Composable
fun PermisosBloqueoScreen(
    alAbrirAvisoAccesibilidad: () -> Unit,
    alTerminar: () -> Unit,
    alVolver: (() -> Unit)? = null,
    viewModel: PermisosBloqueoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescar() }
    val pedirNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refrescar() }

    ContenidoPermisos(
        estado = estado,
        guia = GuiasFabricante.para(Build.MANUFACTURER),
        alActivarAccesibilidad = alAbrirAvisoAccesibilidad,
        alActivarAccesoAlUso = viewModel::abrirAccesoAlUso,
        alActivarNotificaciones = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.notificacionesPedidas) {
                viewModel.marcarNotificacionesPedidas()
                pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.abrirNotificaciones()
            }
        },
        alActivarBateria = viewModel::abrirBateria,
        alTerminar = alTerminar,
        alVolver = alVolver,
    )
}

@Composable
private fun ContenidoPermisos(
    estado: EstadoPermisos,
    guia: GuiaFabricante,
    alActivarAccesibilidad: () -> Unit,
    alActivarAccesoAlUso: () -> Unit,
    alActivarNotificaciones: () -> Unit,
    alActivarBateria: () -> Unit,
    alTerminar: () -> Unit,
    alVolver: (() -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Celeste)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (alVolver != null) {
            Row(Modifier.fillMaxWidth()) { BotonRegresar(alVolver) }
        }
        TextoContorno("Prepara tu esquina", MaterialTheme.typography.displayLarge, ColoresTF.Blanco, ColoresTF.Azul, 3.dp)
        Text(
            "Para echarte la mano con el celular necesitamos unos permisos. Todos se dan en Ajustes y regresas aquí solito.",
            style = MaterialTheme.typography.titleMedium,
            color = ColoresTF.Blanco,
            textAlign = TextAlign.Center,
        )
        TarjetaPermiso(
            titulo = "Bloqueo de apps",
            descripcion = "Para tapar tus apps de distracción cuando te toca descanso.",
            concedido = estado.accesibilidad,
            alActivar = alActivarAccesibilidad,
        )
        TarjetaPermiso(
            titulo = "Tiempo de uso",
            descripcion = "Para contar cuánto usas cada app y saber cuándo llegas a tu límite.",
            concedido = estado.accesoAlUso,
            alActivar = alActivarAccesoAlUso,
        )
        TarjetaPermiso(
            titulo = "Notificaciones",
            descripcion = "Para avisarte de tu racha y de cuánto llevas en tus apps.",
            concedido = estado.notificaciones,
            alActivar = alActivarNotificaciones,
        )
        TarjetaPermiso(
            titulo = "Batería (recomendado)",
            descripcion = "Para que tu ${guia.marca} no apague Tiempo Fuera por ahorrar pila:\n" +
                guia.pasos.joinToString("\n") { "• $it" },
            concedido = estado.sinAhorroBateria,
            alActivar = alActivarBateria,
        )
        BotonL(ColoresTF.Amarillo, "¡LISTO!", alTocar = alTerminar, habilitado = estado.completos)
        if (!estado.completos) {
            Text(
                "Faltan permisos: sin ellos no podemos bloquear ni medir tus apps.",
                style = MaterialTheme.typography.bodySmall,
                color = ColoresTF.Blanco,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TarjetaPermiso(titulo: String, descripcion: String, concedido: Boolean, alActivar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ColoresTF.Blanco.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(enabled = !concedido, role = Role.Button, onClick = alActivar)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = ColoresTF.Blanco, modifier = Modifier.weight(1f))
            Text(
                if (concedido) "✓ Listo" else "Activar ›",
                style = MaterialTheme.typography.titleMedium,
                color = if (concedido) ColoresTF.Blanco else ColoresTF.Amarillo,
            )
        }
        Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = ColoresTF.Blanco)
    }
}

@Preview(heightDp = 900)
@Composable
private fun PermisosPreview() = TiempoFueraTheme {
    ContenidoPermisos(EstadoPermisos(true, false, false, false), GuiasFabricante.para("samsung"), {}, {}, {}, {}, {}, alVolver = {})
}
