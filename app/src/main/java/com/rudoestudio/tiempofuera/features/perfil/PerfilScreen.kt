package com.rudoestudio.tiempofuera.features.perfil

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.BuildConfig
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.AssetsIos
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.preview.DatosPreview
import com.rudoestudio.tiempofuera.domain.configuracion.EdicionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.perfil.RachaVigencia
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.domain.perfil.UserProfile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tab Perfil (M11.1–M11.3): quién eres, tu personalidad, tus apps y lo que se les sigue, y
 * sincronizar con la nube. En iOS: `UserProfileEditView`, que es un formulario de
 * desarrollo; aquí los números y la sesión solo se editan/ven en debug.
 */
@Composable
fun PerfilScreen(
    alElegirApps: (TipoGrupoApps) -> Unit,
    alEditarGrupo: (TipoGrupoApps) -> Unit,
    alAbrirPermisos: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescar() }
    ContenidoPerfil(
        estado,
        alCambiarPersonalidad = viewModel::cambiarPersonalidad,
        alElegirApps = alElegirApps,
        alEditarGrupo = alEditarGrupo,
        alAbrirPermisos = alAbrirPermisos,
        alSincronizar = viewModel::sincronizar,
        alGuardarEstadisticas = viewModel::guardarEstadisticas,
    )
}

@Composable
private fun ContenidoPerfil(
    estado: EstadoPerfil,
    alCambiarPersonalidad: (TipoPersonalidad) -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
    alEditarGrupo: (TipoGrupoApps) -> Unit,
    alAbrirPermisos: () -> Unit,
    alSincronizar: () -> Unit,
    alGuardarEstadisticas: (EstadisticasEditables) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(ColoresTF.Azul).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextoContorno("Tu perfil", MaterialTheme.typography.displayMedium, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)
        val perfil = estado.perfil ?: return@Column
        Encabezado(perfil, estado.sesion?.email ?: perfil.email)

        Titulo("Tu personalidad")
        Tarjeta { SelectorPersonalidad(perfil.personalidadId, alCambiarPersonalidad) }

        Titulo("Tus apps")
        Tarjeta {
            listOf(TipoGrupoApps.DISTRACCION, TipoGrupoApps.EDUCATIVAS, TipoGrupoApps.PRODUCTIVIDAD).forEach { g ->
                val n = estado.appsElegidas[g] ?: 0
                FilaNavegable("Apps de ${g.nombreVisible.lowercase()}", if (n == 0) "Ninguna elegida" else "$n ${if (n == 1) "app" else "apps"}") { alElegirApps(g) }
            }
            FilaNavegable("Permisos del bloqueo", "Revisa que todo esté prendido", alAbrirPermisos)
        }

        Titulo("Seguimiento de apps")
        Tarjeta {
            estado.gruposConfigurables.forEach { g ->
                FilaNavegable(g.nombreVisible, EdicionGrupoApps.resumen(g, estado.configuraciones[g])) { alEditarGrupo(g) }
            }
        }

        Titulo("Tu progreso en la nube")
        Tarjeta {
            Nota("Sube a la nube el progreso de tus ligas, tus luchadores y tus estadísticas. También se hace solo cada 12 horas.")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (estado.sincronizando) {
                    CircularProgressIndicator(color = ColoresTF.Blanco)
                } else {
                    BotonL(ColoresTF.Celeste, "SINCRONIZAR", alTocar = alSincronizar)
                }
            }
            estado.mensajeSincronizacion?.let { Nota(it) }
        }

        if (BuildConfig.DEBUG) {
            Titulo("Debug")
            Tarjeta { EditorEstadisticas(perfil, alGuardarEstadisticas) }
            Tarjeta { InfoSesion(estado) }
        }
        BannerError(estado.error)
    }
}

@Composable
private fun Encabezado(perfil: UserProfile, email: String) {
    val l = perfil.luchadorSeleccionado
    Tarjeta {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ImagenPixel(AssetsIos.imagen(l.imagen) ?: AssetsIos.imagen(l.nombre) ?: R.drawable.ringo, 88.dp, descripcion = l.nombre)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextoContorno(l.nombre, MaterialTheme.typography.headlineMedium, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp, maxLineas = 1)
                Text(perfil.personalidadId.nombreVisible, style = MaterialTheme.typography.titleSmall, color = ColoresTF.Amarillo)
                if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco.copy(alpha = 0.8f), maxLines = 1)
            }
        }
        val racha = RachaVigencia.diasVigentes(perfil.diasRacha, perfil.lastActivityDate)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Dato(R.drawable.puntos_poder_icon, "${perfil.puntosPoder}", "Poder")
            Dato(R.drawable.moneda_icon, "${perfil.monedas}", "Monedas")
            Dato(R.drawable.racha_icon, "$racha", if (racha == 1) "Día invicto" else "Días invicto")
            Dato(null, "${perfil.protectoresRacha}/${UserProfile.MAX_PROTECTORES_RACHA}", "Protectores")
        }
    }
}

@Composable
private fun Dato(icono: Int?, valor: String, etiqueta: String) {
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$etiqueta: $valor" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icono != null) Image(painterResource(icono), null, Modifier.size(28.dp)) else Text("🛡️", fontSize = 22.sp)
        Text(valor, style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = ColoresTF.Blanco.copy(alpha = 0.75f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorPersonalidad(actual: TipoPersonalidad, alElegir: (TipoPersonalidad) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TipoPersonalidad.entries.forEach { t ->
            val elegida = t == actual
            Text(
                t.clave,
                style = MaterialTheme.typography.labelLarge,
                color = if (elegida) ColoresTF.Azul else ColoresTF.Blanco,
                modifier = Modifier
                    .background(if (elegida) ColoresTF.Amarillo else Color.Transparent, CircleShape)
                    .border(1.5.dp, if (elegida) ColoresTF.Amarillo else ColoresTF.Blanco.copy(alpha = 0.5f), CircleShape)
                    .clickable(role = Role.RadioButton, onClickLabel = "Elegir ${t.clave}") { alElegir(t) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
    Nota(actual.descripcion)
}

@Composable
private fun FilaNavegable(titulo: String, detalle: String, alTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco.copy(alpha = 0.75f))
        }
        Text("›", style = MaterialTheme.typography.headlineMedium, color = ColoresTF.Amarillo)
    }
}

/** Debug: cambiar los números a mano para probar (iOS lo deja en el formulario). */
@Composable
private fun ColumnScope.EditorEstadisticas(perfil: UserProfile, alGuardar: (EstadisticasEditables) -> Unit) {
    var poder by rememberSaveable(perfil.puntosPoder) { mutableStateOf("${perfil.puntosPoder}") }
    var racha by rememberSaveable(perfil.diasRacha) { mutableStateOf("${perfil.diasRacha}") }
    var monedas by rememberSaveable(perfil.monedas) { mutableStateOf("${perfil.monedas}") }
    var protectores by rememberSaveable(perfil.protectoresRacha) { mutableStateOf("${perfil.protectoresRacha}") }
    Text("Estadísticas de la cuenta", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
    CampoNumero("Puntos de poder", poder) { poder = it }
    CampoNumero("Días invicto", racha) { racha = it }
    CampoNumero("Monedas", monedas) { monedas = it }
    CampoNumero("Protectores de racha (máx. ${UserProfile.MAX_PROTECTORES_RACHA})", protectores) { protectores = it }
    BotonL(
        ColoresTF.Verde, "GUARDAR",
        alTocar = {
            alGuardar(EstadisticasEditables(poder.toIntOrNull() ?: 0, racha.toIntOrNull() ?: 0, monedas.toIntOrNull() ?: 0, protectores.toIntOrNull() ?: 0))
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}

@Composable
private fun CampoNumero(etiqueta: String, valor: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        valor,
        onValueChange = { nuevo -> alCambiar(nuevo.filter(Char::isDigit).take(7)) },
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ColoresTF.Blanco, unfocusedTextColor = ColoresTF.Blanco,
            focusedBorderColor = ColoresTF.Amarillo, unfocusedBorderColor = ColoresTF.Blanco.copy(alpha = 0.5f),
            focusedLabelColor = ColoresTF.Amarillo, unfocusedLabelColor = ColoresTF.Blanco.copy(alpha = 0.75f),
            cursorColor = ColoresTF.Amarillo,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Debug: la sesión guardada y cuánto le queda al token. En iOS: "Debug - Sesión API". */
@Composable
private fun InfoSesion(estado: EstadoPerfil) {
    Text("Sesión del API", style = MaterialTheme.typography.titleMedium, color = ColoresTF.Blanco)
    val s = estado.sesion
    if (s == null) {
        Nota("🔴 Sin sesión guardada.")
        return
    }
    val restanteMin = (s.expiraEnMs - System.currentTimeMillis()) / 60_000
    val formato = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale("es", "MX"))
    Nota("🟢 Sesión activa")
    Nota("Email: ${s.email}")
    Nota(if (restanteMin > 0) "Al token le quedan $restanteMin min" else "El token ya expiró (se renueva solo en la siguiente petición)")
    Nota("Expira: ${Instant.ofEpochMilli(s.expiraEnMs).atZone(ZoneId.systemDefault()).format(formato)}")
}

@Composable
private fun Titulo(texto: String) = TextoContorno(texto, MaterialTheme.typography.displaySmall, ColoresTF.Blanco, ColoresTF.AzulNoche, 2.dp)

@Composable
private fun Nota(texto: String) = Text(texto, style = MaterialTheme.typography.bodySmall, color = ColoresTF.Blanco.copy(alpha = 0.85f))

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ColoresTF.Blanco.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = contenido,
    )
}

@Preview(heightDp = 1600)
@Composable
private fun PerfilPreview() = TiempoFueraTheme {
    ContenidoPerfil(
        EstadoPerfil(perfil = DatosPreview.perfil, appsElegidas = mapOf(TipoGrupoApps.DISTRACCION to 4)),
        {}, {}, {}, {}, {}, {},
    )
}
