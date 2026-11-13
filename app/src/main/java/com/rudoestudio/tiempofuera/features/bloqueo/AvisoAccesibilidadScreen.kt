package com.rudoestudio.tiempofuera.features.bloqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno

/**
 * El aviso destacado que Google Play exige antes de mandar al usuario a activar la
 * accesibilidad (ver `M3.14-POLITICA-PLAY.md`): pantalla propia, separada de otros avisos,
 * que dice **qué datos** se leen y **para qué**, con un "acepto" explícito. Si no acepta,
 * regresa a los permisos y puede volver a abrir este aviso cuando quiera.
 *
 * No existe en iOS: allá el permiso de Screen Time lo explica el sistema. Cualquier cambio
 * en este texto o en lo que hace el servicio pide volver a declararlo en Play Console.
 */
@Composable
fun AvisoAccesibilidadScreen(alVolver: () -> Unit, viewModel: PermisosBloqueoViewModel = hiltViewModel()) {
    ContenidoAviso(
        alAceptar = {
            viewModel.abrirAccesibilidad()
            alVolver()
        },
        alRechazar = alVolver,
    )
}

@Composable
private fun ContenidoAviso(alAceptar: () -> Unit, alRechazar: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Azul)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextoContorno("Antes de activar el bloqueo", MaterialTheme.typography.displayMedium, ColoresTF.Amarillo, ColoresTF.AzulNoche, 3.dp)
        Text(
            "Tiempo Fuera usa el permiso de accesibilidad de Android. Te contamos derecho qué hace con él.",
            style = MaterialTheme.typography.titleMedium,
            color = ColoresTF.Blanco,
            textAlign = TextAlign.Center,
        )
        Seccion(
            "Qué vemos",
            "Qué apps tienes abiertas en pantalla y en qué parte de la pantalla están. Nada más.",
        )
        Seccion(
            "Para qué",
            "Para tapar con la pantalla de \"¡Tiempo fuera!\" las apps que tú elegiste, cuando estás en una misión, " +
                "a la hora de dormir o cuando se acaba tu tiempo del día.",
        )
        Seccion(
            "Qué NO hacemos",
            "No leemos lo que escribes, tus mensajes ni lo que ves dentro de las apps. No tocamos nada por ti. " +
                "Esta información no sale de tu teléfono y no se la mandamos a nadie: ni a nosotros, ni a tus papás, ni a otras empresas.",
        )
        Seccion(
            "Tú mandas",
            "Puedes apagarlo cuando quieras en Ajustes → Accesibilidad. Sin este permiso, Tiempo Fuera no puede bloquear tus apps.",
        )
        BotonL(ColoresTF.Amarillo, "ACEPTO, ACTIVAR", alTocar = alAceptar, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = alRechazar) {
            Text("Ahora no", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        }
    }
}

@Composable
private fun Seccion(titulo: String, texto: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, color = ColoresTF.Amarillo)
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = ColoresTF.Blanco)
    }
}

@Preview(heightDp = 900)
@Composable
private fun AvisoPreview() = TiempoFueraTheme { ContenidoAviso({}, {}) }
