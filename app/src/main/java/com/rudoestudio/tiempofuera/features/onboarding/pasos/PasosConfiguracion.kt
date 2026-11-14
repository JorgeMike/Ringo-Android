package com.rudoestudio.tiempofuera.features.onboarding.pasos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.EtiquetaHora
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadoPermisos
import com.rudoestudio.tiempofuera.domain.configuracion.CampoConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSueno
import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.onboarding.ReglasOnboarding
import com.rudoestudio.tiempofuera.features.onboarding.ConfHabitoViewModel
import com.rudoestudio.tiempofuera.features.onboarding.ConfPermisosViewModel
import com.rudoestudio.tiempofuera.features.onboarding.ConfUsoMaxViewModel
import com.rudoestudio.tiempofuera.features.onboarding.EstadoConfPermisos
import com.rudoestudio.tiempofuera.features.onboarding.EstadoConfUsoMax
import java.time.LocalTime

// Pasos 9 a 12 del onboarding: cuánto usas el cel, permisos y apps, límites y sueño.

/** Paso 9: horas de uso al día. En iOS: `ConfUsoInicialView`. */
@Composable
fun ConfUsoInicialPaso(tiempoInicialMinutos: Int, alContinuar: (Int) -> Unit) {
    val rango = ReglasOnboarding.RANGO_HORAS_USO
    var horas by rememberSaveable {
        mutableIntStateOf((tiempoInicialMinutos / 60).takeIf { it in rango } ?: ReglasOnboarding.HORAS_USO_INICIAL)
    }
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            PreguntaDecorada("¿Cuánto tiempo usas el cel al día?", bajarLineas = 26.dp)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                TextoConSombra("$horas", pirata(94), colorSombra = ColoresTF.Azul)
                TextoConSombra("horas", pirata(34), colorSombra = ColoresTF.Azul, modifier = Modifier.padding(start = 8.dp, bottom = 18.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonS(ColoresTF.Verde, IconoBotonS.ARRIBA, alTocar = { horas = (horas + 1).coerceAtMost(rango.last) }, escala = 1.5f)
                BotonS(ColoresTF.Amarillo, IconoBotonS.ABAJO, alTocar = { horas = (horas - 1).coerceAtLeast(rango.first) }, escala = 1.5f)
            }
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "AHÍ MERO", alTocar = { alContinuar(horas * 60) })
        }
    }
}

/**
 * Paso 10 (M4.11): permisos del bloqueo y apps de cada grupo. En iOS: `ConfPermisosView`
 * (un switch de notificaciones y dos `SelectorAppsButton`). En Android los permisos son
 * varios y se dan en Ajustes: los lleva `PermisosBloqueoScreen` (con el aviso de
 * accesibilidad de Play), y las apps `SelectorAppsScreen`; aquí se ve cómo van.
 */
@Composable
fun ConfPermisosPaso(
    viewModel: ConfPermisosViewModel,
    alAbrirPermisos: () -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
    alContinuar: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refrescar() }
    ContenidoConfPermisos(
        estado = estado,
        alAbrirPermisos = alAbrirPermisos,
        alElegirApps = alElegirApps,
        alContinuar = { if (viewModel.puedeContinuar()) alContinuar() },
    )
}

@Composable
private fun ContenidoConfPermisos(
    estado: EstadoConfPermisos,
    alAbrirPermisos: () -> Unit,
    alElegirApps: (TipoGrupoApps) -> Unit,
    alContinuar: () -> Unit,
) {
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp).padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PreguntaDecorada("Ya meritooo")
            TextoPaso("Necesitamos algunos permisos para poder ayudarte.", tamano = 20)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SeccionPaso(
                    titulo = "Permisos",
                    boton = if (estado.permisos.completos) "¡LISTOS!" else "DAR PERMISOS",
                    colorBoton = if (estado.permisos.completos) ColoresTF.Verde else ColoresTF.Morado,
                    alTocar = alAbrirPermisos,
                    descripcion = resumenPermisos(estado.permisos),
                )
                SeccionPaso(
                    titulo = "Apps de distracción",
                    boton = textoApps(estado.appsDistraccion),
                    colorBoton = if (estado.appsDistraccion > 0) ColoresTF.Verde else ColoresTF.Morado,
                    alTocar = { alElegirApps(TipoGrupoApps.DISTRACCION) },
                    descripcion = "Elige las apps a limitar. Se bloquean en las misiones con tiempo y al llegar a tu límite diario.",
                )
                SeccionPaso(
                    titulo = "Apps educativas",
                    boton = textoApps(estado.appsEducativas),
                    colorBoton = if (estado.appsEducativas > 0) ColoresTF.Verde else ColoresTF.Morado,
                    alTocar = { alElegirApps(TipoGrupoApps.EDUCATIVAS) },
                    descripcion = "Elige las apps con las que quieres aprender. Cumple misiones y gana premios por usarlas.",
                )
            }
            BannerError(estado.error)
            TextoPaso("Aviso de Privacidad", tamano = 14)
            BotonL(ColoresTF.Amarillo, "ESTOY DE ACUERDO", alTocar = alContinuar)
            TextoPaso("Al continuar, aceptas nuestro aviso de privacidad", tamano = 14)
        }
    }
}

private fun textoApps(cantidad: Int) = when (cantidad) {
    0 -> "SELECCIONAR APPS"
    1 -> "1 APP ELEGIDA"
    else -> "$cantidad APPS ELEGIDAS"
}

private fun resumenPermisos(p: EstadoPermisos): String {
    fun linea(listo: Boolean, nombre: String) = (if (listo) "✓ " else "✗ ") + nombre
    return listOf(
        linea(p.accesibilidad, "Bloqueo de apps"),
        linea(p.accesoAlUso, "Tiempo de uso"),
        linea(p.notificaciones, "Notificaciones"),
    ).joinToString("   ")
}

/** Título, botón y explicación sobre el listón. En iOS: `seccionApps` de `ConfPermisosView`. */
@Composable
private fun SeccionPaso(titulo: String, boton: String, colorBoton: Color, alTocar: () -> Unit, descripcion: String) {
    ConListon(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextoConSombra(titulo, pirata(24), colorSombra = ColoresTF.Azul)
            BotonL(colorBoton, boton, alTocar = alTocar)
            TextoPaso(descripcion, tamano = 14)
        }
    }
}

/** Paso 11: límite diario y avisos de cada grupo. En iOS: `ConfUsoMaxView`. */
@Composable
fun ConfUsoMaxPaso(viewModel: ConfUsoMaxViewModel, tiempoInicialMinutos: Int, alContinuar: (ConfiguracionGrupoApps) -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(tiempoInicialMinutos) { viewModel.aplicarSugerencia(tiempoInicialMinutos) }
    ContenidoConfUsoMax(
        estado = estado,
        alCambiar = viewModel::cambiar,
        alContinuar = { viewModel.guardar(alContinuar) },
    )
    estado.error?.let { DialogoError(it, viewModel::limpiarError) }
}

@Composable
private fun ContenidoConfUsoMax(
    estado: EstadoConfUsoMax,
    alCambiar: (TipoGrupoApps, CampoConfiguracionGrupoApps, Int) -> Unit,
    alContinuar: () -> Unit,
) {
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp).padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PreguntaDecorada("Configuración inicial")
            TextoPaso("Define cuánto tiempo puedes usar cada grupo de apps al día.", tamano = 16)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                listOf(TipoGrupoApps.DISTRACCION, TipoGrupoApps.EDUCATIVAS).forEach { tipo ->
                    TextoConSombra(tipo.nombreVisible, pirata(32), colorSombra = ColoresTF.Azul, modifier = Modifier.padding(top = 14.dp))
                    val sugerencia = estado.limiteSugeridoMinutos
                    if (tipo == TipoGrupoApps.DISTRACCION && sugerencia != null) {
                        TextoPaso(
                            "Sugerencia: $sugerencia min al día, 30 min menos que tus ${estado.tiempoInicialMinutos} min de ahora. Puedes ajustarlo.",
                            tamano = 14,
                        )
                    }
                    tipo.camposConfigurables.forEach { campo ->
                        val minutos = ((estado.de(tipo).valor(campo) ?: 0L) / 60).toInt()
                        val maximo = if (campo == CampoConfiguracionGrupoApps.TIEMPO_MAXIMO_DIARIO) ConfiguracionGrupoApps.MINUTOS_MAXIMOS_DIARIOS else 600
                        FilaCampo(
                            campo = campo,
                            minutos = minutos,
                            alBajar = { alCambiar(tipo, campo, (minutos - ReglasOnboarding.PASO_MINUTOS).coerceAtLeast(0)) },
                            alSubir = { alCambiar(tipo, campo, (minutos + ReglasOnboarding.PASO_MINUTOS).coerceAtMost(maximo)) },
                        )
                    }
                }
            }
            BotonL(ColoresTF.Amarillo, "CONTINUAR", alTocar = alContinuar)
        }
    }
}

/** En iOS: `CampoConfiguracionRow`. */
@Composable
private fun FilaCampo(campo: CampoConfiguracionGrupoApps, minutos: Int, alBajar: () -> Unit, alSubir: () -> Unit) {
    ConListon(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextoContorno(campo.titulo, arvo(16), ColoresTF.Blanco, ColoresTF.Azul, 3.dp, alineacion = TextAlign.Center)
            ContadorMinutos(minutos, alBajar, alSubir)
            TextoPaso(campo.descripcion, tamano = 14, alpha = 0.85f)
        }
    }
}

/** Paso 12: horario de sueño y colchón. En iOS: `ConfHabitoView`. */
@Composable
fun ConfHabitoPaso(viewModel: ConfHabitoViewModel, alContinuar: (ConfiguracionHabitoSueno) -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp).padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PreguntaDecorada("Hábitos de sueño", tamano = 32.sp)
            TextoPaso("Cuéntanos tus horarios para cuidar tu descanso.", tamano = 16)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                ConListon(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TextoConSombra("Horario de sueño", pirata(24), colorSombra = ColoresTF.Azul)
                        EtiquetaHora("Despiertas a las", hora(estado.horaInicioDia), alCambiar = { viewModel.cambiarInicio(it.hour * 60 + it.minute) })
                        EtiquetaHora("Te duermes a las", hora(estado.horaFinDia), alCambiar = { viewModel.cambiarFin(it.hour * 60 + it.minute) })
                    }
                }
                ConListon(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextoConSombra("Colchón de uso", pirata(24), colorSombra = ColoresTF.Azul)
                        ContadorMinutos(
                            estado.colchonUso,
                            alBajar = { viewModel.cambiarColchon(-ReglasOnboarding.PASO_MINUTOS) },
                            alSubir = { viewModel.cambiarColchon(ReglasOnboarding.PASO_MINUTOS) },
                            colorBajar = ColoresTF.Amarillo,
                            colorSubir = ColoresTF.Verde,
                        )
                        TextoPaso("Tiempo antes de dormir y después de despertar en el que no podrás usar el celular.", tamano = 12, alpha = 0.85f)
                        TextoPaso("Mínimo 20 min, máximo 2 horas.", tamano = 11, alpha = 0.7f)
                    }
                }
            }
            BotonL(ColoresTF.Amarillo, "CONTINUAR", alTocar = { viewModel.guardar(alContinuar) })
        }
    }
    estado.error?.let { DialogoError(it, viewModel::limpiarError) }
}

private fun hora(minutos: Int): LocalTime = LocalTime.of(minutos / 60 % 24, minutos % 60)

@Composable
private fun DialogoError(mensaje: String, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
        title = { Text("No se pudo guardar") },
        text = { Text(mensaje) },
    )
}

@Preview(heightDp = 860)
@Composable
private fun ConfPermisosPreview() = TiempoFueraTheme {
    ContenidoConfPermisos(EstadoConfPermisos(EstadoPermisos(true, false, true, false), appsDistraccion = 3), {}, {}, {})
}

@Preview(heightDp = 860)
@Composable
private fun ConfUsoMaxPreview() = TiempoFueraTheme {
    ContenidoConfUsoMax(EstadoConfUsoMax(tiempoInicialMinutos = 300), { _, _, _ -> }, {})
}
