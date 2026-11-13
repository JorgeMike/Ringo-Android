package com.rudoestudio.tiempofuera.features.galeria

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.color
import com.rudoestudio.tiempofuera.core.designsystem.colorObscuro
import com.rudoestudio.tiempofuera.domain.luchadores.Rareza
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BadgeTemporizador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BarraProgresoParalelogramo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Boton3D
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonRegresar
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.EtiquetaHora
import com.rudoestudio.tiempofuera.core.designsystem.componentes.FondoLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.HeaderPuntos
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorNivelLuchador
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoApp
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IndicadorTiempo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.SeccionRecompensas
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TemporizadorBadges
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoEnParalelogramo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.designsystem.componentes.espiral
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.core.hapticos.hapticAlTocar
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.TipoNotificacion
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.random.Random

/**
 * Herramienta de desarrollo: muestra los componentes de M0.6 para compararlos con iOS en
 * el teléfono. No es una pantalla de la app; quitar el acceso antes de publicar.
 */
@Composable
fun GaleriaScreen(alVolver: () -> Unit) {
    var progreso by remember { mutableFloatStateOf(0.4f) }
    var error by remember { mutableStateOf<String?>(null) }
    var hora by remember { mutableStateOf(LocalTime.of(22, 30)) }
    val espiral = remember { Animatable(1f) }
    val alcance = rememberCoroutineScope()

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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BotonRegresar(alVolver)
            Spacer(Modifier.width(12.dp))
            Text("Galería · M0", style = MaterialTheme.typography.headlineMedium, color = ColoresTF.Blanco)
        }

        Seccion("Botones") {
            BotonL(ColoresTF.Amarillo, "Continuar", alTocar = {})
            BotonL(ColoresTF.Verde, "Aceptar", alTocar = {})
            BotonL(ColoresTF.Verde, "No disponible", alTocar = {}, habilitado = false)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconoBotonS.entries.filter { it != IconoBotonS.VACIO }.forEach { BotonS(ColoresTF.Celeste, it, alTocar = {}) }
                BotonS(ColoresTF.Verde, IconoBotonS.CHECK, alTocar = {}, habilitado = false)
            }
            Boton3D(alTocar = {}) {
                Text("Botón 3D", style = MaterialTheme.typography.titleLarge, color = ColoresTF.Blanco,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp))
            }
        }

        Seccion("Textos") {
            Titulo("Ligas", "del barrio", Modifier.padding(top = 24.dp, bottom = 40.dp))
            TextoContorno("Contorno", MaterialTheme.typography.displayMedium, ColoresTF.Blanco, ColoresTF.AzulNoche, 3.dp)
            TextoConSombra("Con sombra", MaterialTheme.typography.displayMedium)
            Column(verticalArrangement = Arrangement.spacedBy((-6).dp)) {
                TextoEnParalelogramo("5 horas")
                TextoEnParalelogramo("al día", Modifier.padding(start = 40.dp))
            }
        }

        Seccion("Indicadores") {
            BarraProgresoParalelogramo(progreso, ColoresTF.Amarillo, Modifier.fillMaxWidth().height(18.dp))
            IndicadorTiempo(progreso, (progreso * 60).toInt(), ColoresTF.Verde, Modifier.fillMaxWidth())
            BotonL(ColoresTF.Naranja, "Cambiar progreso", alTocar = { progreso = Random.nextFloat() })
            TemporizadorBadges("12:34")
            BadgeTemporizador('7')
            BannerError(error, Modifier.fillMaxWidth())
            BotonL(ColoresTF.Rojo, if (error == null) "Mostrar error" else "Ocultar error", alTocar = {
                error = if (error == null) "No se pudo guardar tu progreso. Intenta otra vez." else null
            })
        }

        Seccion("Encabezados") {
            HeaderPuntos(puntosPoder = 1250, monedas = 340, alTocarPerfil = {}, alTocarTienda = {})
            SeccionRecompensas(puntosPoder = 50, monedas = 20, exp = 100, modifier = Modifier.padding(vertical = 16.dp))
            EtiquetaHora("Hora de dormir", hora, alCambiar = { hora = it })
        }

        Seccion("Luchador") {
            Rareza.entries.forEachIndexed { i, rareza ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                        FondoLuchador(rareza.color, rareza.colorObscuro, Modifier.size(90.dp))
                        Image(painterResource(R.drawable.ringo), contentDescription = null, modifier = Modifier.size(70.dp))
                    }
                    IndicadorNivelLuchador(
                        nivel = 10 + i * 25, progreso = progreso, nombre = rareza.nombreVisible,
                        colorRareza = rareza.color, colorRarezaObscuro = rareza.colorObscuro,
                        muestraIndicadorSubirNivel = i == 3,
                    )
                }
            }
        }

        Seccion("Íconos de apps · M3.2") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Ajustes y la propia app existen en cualquier teléfono; el último no existe (cuadro gris).
                IconoApp("com.android.settings")
                IconoApp(LocalContext.current.packageName)
                IconoApp("app.que.no.existe")
            }
        }

        Seccion("Hápticos · M0.8") {
            val h = LocalHaptics.current
            val pruebas = listOf<Pair<String, () -> Unit>>(
                "Ligero" to { h.impacto(EstiloImpacto.LIGERO) },
                "Medio" to { h.impacto(EstiloImpacto.MEDIO) },
                "Fuerte" to { h.impacto(EstiloImpacto.FUERTE) },
                "Suave" to { h.impacto(EstiloImpacto.SUAVE) },
                "Rígido" to { h.impacto(EstiloImpacto.RIGIDO) },
                "Selección" to { h.seleccion() },
                "Éxito" to { h.notificacion(TipoNotificacion.EXITO) },
                "Advertencia" to { h.notificacion(TipoNotificacion.ADVERTENCIA) },
                "Error" to { h.notificacion(TipoNotificacion.ERROR) },
                "Sacudida 1 s" to { h.sacudidaFuerte(1.0) },
                "Vidrio roto" to { h.vibrarRompimientoDeVidrio() },
                "Respiración" to { h.vibrarRespiracion(0.5f) },
            )
            pruebas.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    fila.forEach { (nombre, accion) ->
                        Boton3D(alTocar = accion, modifier = Modifier.width(150.dp)) {
                            Text(nombre, style = MaterialTheme.typography.titleSmall, color = ColoresTF.Blanco,
                                modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                }
            }
            Text("Ticks (contador, de suave a fuerte):", style = MaterialTheme.typography.bodyMedium, color = ColoresTF.Blanco)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f).forEach { i ->
                    Boton3D(alTocar = { h.vibrarTick(i) }, color = ColoresTF.Morado, colorSombra = ColoresTF.MoradoObscuro) {
                        Text("${(i * 100).toInt()}", style = MaterialTheme.typography.titleSmall, color = ColoresTF.Blanco,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                    }
                }
            }
            BotonL(ColoresTF.Celeste, "Botón con hapticAlTocar", alTocar = {}, modifier = Modifier.hapticAlTocar())
        }

        Seccion("Efecto espiral") {
            Image(
                painterResource(R.drawable.sticker_3), contentDescription = null,
                modifier = Modifier.size(120.dp).espiral(espiral.value),
            )
            BotonL(ColoresTF.Morado, "Repetir espiral", alTocar = {
                alcance.launch {
                    espiral.snapTo(0f)
                    espiral.animateTo(1f, tween(1200))
                }
            })
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(ColoresTF.Azul.copy(alpha = 0.35f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(titulo, style = MaterialTheme.typography.displaySmall, color = ColoresTF.Amarillo)
        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        contenido()
    }
}
