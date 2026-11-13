package com.rudoestudio.tiempofuera.features.laboratorio

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

/** Herramienta de desarrollo (M1): login real contra staging. Quitar el acceso antes de publicar. */
@Composable
fun LaboratorioApiScreen(alVolver: () -> Unit, viewModel: LaboratorioApiViewModel = hiltViewModel()) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    val bitacora by viewModel.bitacora.collectAsStateWithLifecycle()
    val ocupado by viewModel.ocupado.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var paquete by rememberSaveable { mutableStateOf("com.google.android.youtube") }
    val colores = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ColoresTF.Blanco, unfocusedTextColor = ColoresTF.Blanco,
        focusedBorderColor = ColoresTF.Amarillo, unfocusedBorderColor = ColoresTF.Blanco,
        focusedLabelColor = ColoresTF.Amarillo, unfocusedLabelColor = ColoresTF.Blanco,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresTF.Azul)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BotonRegresar(alVolver)
            Spacer(Modifier.width(12.dp))
            Text("Laboratorio", style = MaterialTheme.typography.headlineMedium, color = ColoresTF.Blanco)
        }
        Text(viewModel.urlApi, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco)
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth(), color = ColoresTF.Amarillo)

        val s = sesion
        Text(
            if (s == null) "Sin sesión"
            else "Sesión: ${s.email}\nToken vence: ${formatoHora.format(Instant.ofEpochMilli(s.expiraEnMs))}" +
                if (s.estaExpirada()) " (CADUCADO)" else "",
            style = MaterialTheme.typography.titleSmall,
            color = if (s == null) ColoresTF.Amarillo else ColoresTF.Verde,
        )

        BotonL(ColoresTF.Celeste, "Probar conexión", alTocar = viewModel::probarConexion, habilitado = !ocupado)

        OutlinedTextField(
            email, { email = it }, label = { Text("Correo") }, singleLine = true, colors = colores,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        BotonL(ColoresTF.Amarillo, "Mandar código", alTocar = { viewModel.mandarCodigo(email) }, habilitado = !ocupado && email.isNotBlank())
        OutlinedTextField(
            codigo, { if (it.length <= 6) codigo = it.filter(Char::isDigit) }, label = { Text("Código de 6 dígitos") },
            singleLine = true, colors = colores,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(),
        )
        BotonL(ColoresTF.Verde, "Verificar", alTocar = { viewModel.verificar(email, codigo) }, habilitado = !ocupado && codigo.length == 6)

        BotonL(ColoresTF.Morado, "Leer mi perfil (/me)", alTocar = viewModel::leerPerfil, habilitado = !ocupado && s != null)
        BotonL(ColoresTF.Naranja, "Caducar token", alTocar = viewModel::caducarToken, habilitado = !ocupado && s != null)
        BotonL(ColoresTF.Rojo, "Cerrar sesión", alTocar = viewModel::salir, habilitado = !ocupado && s != null)

        Text("Bloqueo · M3", style = MaterialTheme.typography.headlineMedium, color = ColoresTF.Blanco)
        OutlinedTextField(
            paquete, { paquete = it.trim() }, label = { Text("Paquete de la app") }, singleLine = true, colors = colores,
            modifier = Modifier.fillMaxWidth(),
        )
        BotonL(ColoresTF.Celeste, "Revisar permisos", alTocar = viewModel::revisarPermisos)
        BotonL(ColoresTF.Morado, "Abrir Accesibilidad", alTocar = viewModel::abrirAccesibilidad)
        BotonL(ColoresTF.Morado, "Abrir Acceso al uso", alTocar = viewModel::abrirAccesoAlUso)
        BotonL(ColoresTF.Amarillo, "Misión: bloquear 2 min", alTocar = { viewModel.bloquearPorMision(paquete, 2) }, habilitado = !ocupado)
        BotonL(ColoresTF.Naranja, "Límite diario de 1 min", alTocar = { viewModel.ponerLimite(paquete, 1) }, habilitado = !ocupado)
        BotonL(ColoresTF.Verde, "Quitar bloqueos", alTocar = viewModel::quitarBloqueos, habilitado = !ocupado)

        bitacora.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco, modifier = Modifier.fillMaxWidth()) }
    }
}
