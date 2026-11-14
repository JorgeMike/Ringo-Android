package com.rudoestudio.tiempofuera.features.onboarding.pasos

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BannerError
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.IconoBotonS
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ParalelogramoShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.onboarding.ReglasOnboarding
import com.rudoestudio.tiempofuera.domain.perfil.PropositoUsuario
import com.rudoestudio.tiempofuera.domain.perfil.TipoPersonalidad
import com.rudoestudio.tiempofuera.features.onboarding.DatosCapturados
import com.rudoestudio.tiempofuera.features.onboarding.DatosViewModel
import com.rudoestudio.tiempofuera.features.onboarding.EstadoDatos
import com.rudoestudio.tiempofuera.features.onboarding.FaseDatos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

// Pasos 4 a 7 del onboarding: quién eres y qué quieres.

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"))

/** Paso 4: fecha de nacimiento, correo y código. En iOS: `DatosView`. */
@Composable
fun DatosPaso(viewModel: DatosViewModel, alContinuar: (DatosCapturados) -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenidoDatos(
        estado = estado,
        alCambiarFecha = viewModel::cambiarFecha,
        alCambiarEmail = viewModel::cambiarEmail,
        alCambiarCodigo = viewModel::cambiarCodigo,
        alContinuar = { viewModel.continuar(alContinuar) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoDatos(
    estado: EstadoDatos,
    alCambiarFecha: (LocalDate) -> Unit,
    alCambiarEmail: (String) -> Unit,
    alCambiarCodigo: (String) -> Unit,
    alContinuar: () -> Unit,
) {
    var eligiendoFecha by remember { mutableStateOf(false) }
    val titulo = when (estado.fase) {
        FaseDatos.INGRESANDO_DATOS -> "ENVIAR CÓDIGO"
        FaseDatos.ENVIANDO_CODIGO -> "ENVIANDO..."
        FaseDatos.INGRESANDO_CODIGO -> "SÍ SOY, CONFIRMO"
        FaseDatos.VERIFICANDO -> "VERIFICANDO..."
        FaseDatos.SINCRONIZANDO -> "BAJANDO TUS DATOS..."
    }
    FondoOnboarding {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextoPaso("Empecemos con algunas preguntas", tamano = 24)
            CampoFormulario("Fecha de nacimiento", alTocar = { eligiendoFecha = true }) {
                Text(estado.fechaNacimiento.format(formatoFecha), style = arvo(16), color = Color.Black)
            }
            CampoFormulario("Correo electrónico") {
                CampoTexto(estado.email, alCambiarEmail, "Correo electrónico", KeyboardType.Email, habilitado = estado.fase == FaseDatos.INGRESANDO_DATOS)
            }
            if (estado.fase == FaseDatos.INGRESANDO_CODIGO || estado.fase == FaseDatos.VERIFICANDO) {
                CampoFormulario("Código de verificación") {
                    CampoTexto(estado.codigo, alCambiarCodigo, "Código", KeyboardType.NumberPassword, habilitado = estado.fase == FaseDatos.INGRESANDO_CODIGO)
                }
            }
            Spacer(Modifier.height(40.dp))
            BannerError(estado.error)
            BotonL(ColoresTF.Amarillo, titulo, alTocar = alContinuar, habilitado = !estado.ocupado)
        }
    }

    if (eligiendoFecha) {
        val selector = rememberDatePickerState(
            initialSelectedDateMillis = estado.fechaNacimiento.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    selector.selectedDateMillis?.let { alCambiarFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    eligiendoFecha = false
                }) { Text("Listo") }
            },
        ) { DatePicker(selector, title = { Text("Fecha de nacimiento", Modifier.padding(16.dp)) }) }
    }
}

/** Ícono, título y campo blanco chueco sobre el listón. En iOS: `CampoFormulario`. */
@Composable
private fun CampoFormulario(titulo: String, alTocar: (() -> Unit)? = null, contenido: @Composable () -> Unit) {
    ConListon(Modifier.fillMaxWidth().height(130.dp).padding(end = 7.dp, bottom = 7.dp)) {
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.moneda_icon), null, Modifier.size(48.dp).offset(x = (-14).dp))
                Text(titulo, style = arvo(18), color = ColoresTF.Blanco, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(x = (-10).dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(ColoresTF.Blanco, ParalelogramoShape(10.dp))
                    .let { if (alTocar != null) it.clickable(onClick = alTocar) else it }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { contenido() }
        }
    }
}

@Composable
private fun CampoTexto(valor: String, alCambiar: (String) -> Unit, pista: String, teclado: KeyboardType, habilitado: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        if (valor.isEmpty()) Text(pista, style = arvo(16), color = Color.Black.copy(alpha = 0.35f))
        BasicTextField(
            valor,
            alCambiar,
            enabled = habilitado,
            singleLine = true,
            textStyle = arvo(16).copy(color = Color.Black, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = teclado, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Paso 5: el propósito. En iOS: `MetaView`. */
@Composable
fun MetaPaso(inicial: PropositoUsuario, alContinuar: (PropositoUsuario) -> Unit) {
    var elegido by rememberSaveable { mutableStateOf(inicial) }
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ImagenPixel(R.drawable.el_serenisimo, 180.dp)
            PreguntaDecorada("¿Cuál es tu meta?", Modifier.padding(bottom = 24.dp))
            PropositoUsuario.entries.forEach { proposito ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlacaChueca(240.dp, 40.dp) {
                        Text(proposito.titulo, style = arvo(16), color = ColoresTF.Blanco, modifier = Modifier.align(Alignment.Center).offset(x = (-10).dp))
                    }
                    BotonS(
                        ColoresTF.Amarillo,
                        if (elegido == proposito) IconoBotonS.CHECK else IconoBotonS.VACIO,
                        alTocar = { elegido = proposito },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            BotonL(ColoresTF.Amarillo, "SÍ SE PUEDE", alTocar = { alContinuar(elegido) })
        }
    }
}

/** Colores, fondo y luchadores de cada bando. En iOS: `OnboardingBand`, armado en `BandoView`. */
private data class Bando(val fondo: Int, val anchoFondo: Dp, val luchadores: List<Int>, val color: Color, val colorObscuro: Color)

private fun bando(tipo: TipoPersonalidad) = when (tipo) {
    TipoPersonalidad.DIPLOMATICO -> Bando(R.drawable.fondo_misticos, 285.dp, listOf(R.drawable.fenix, R.drawable.el_dummy, R.drawable.katita), ColoresTF.Verde, ColoresTF.VerdeObscuro)
    TipoPersonalidad.ANALISTA -> Bando(R.drawable.fondo_rudos, 285.dp, listOf(R.drawable.la_viuda, R.drawable.enchi_lalo, R.drawable.tito_rito), ColoresTF.Morado, ColoresTF.MoradoObscuro)
    TipoPersonalidad.CENTINELA -> Bando(R.drawable.fondo_tecnicos, 585.dp, listOf(R.drawable.el_tallado, R.drawable.el_serenisimo, R.drawable.el_chaman), AzulMedio, ColoresTF.Azul)
    TipoPersonalidad.EXPLORADOR -> Bando(R.drawable.fondo_rifados, 285.dp, listOf(R.drawable.lu_katita, R.drawable.gaspar, R.drawable.chascarrin), ColoresTF.Amarillo, ColoresTF.AmarilloObscuro)
}

private val ANCHO_TARJETA = 260.dp
private val ALTO_TARJETA = 420.dp
private val SEPARACION_TARJETAS = 20.dp
private const val INCLINACION_MAXIMA = 12f

/** Paso 6: el bando (personalidad), en carrusel. En iOS: `BandoView` + `BandoCard`. */
@Composable
fun BandoPaso(inicial: TipoPersonalidad, alContinuar: (TipoPersonalidad) -> Unit) {
    val tipos = TipoPersonalidad.entries
    val pagina = rememberPagerState(initialPage = tipos.indexOf(inicial)) { tipos.size }
    FondoOnboarding(color = ColoresTF.Azul) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Titulo("Selecciona", "tu bando", Modifier.padding(top = 40.dp))
            TextoPaso(
                "Esto es para crear misiones diarias personalizadas, acorde a tu tipo de perfil.",
                Modifier.padding(top = 18.dp).padding(horizontal = 22.dp),
                tamano = 14,
                alpha = 0.8f,
            )
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val lado = ((maxWidth - ANCHO_TARJETA) / 2).coerceAtLeast(0.dp)
                HorizontalPager(
                    pagina,
                    pageSize = PageSize.Fixed(ANCHO_TARJETA),
                    pageSpacing = SEPARACION_TARJETAS,
                    contentPadding = PaddingValues(horizontal = lado, vertical = 20.dp),
                    modifier = Modifier.align(Alignment.Center),
                ) { i ->
                    val distancia = (pagina.currentPage - i) + pagina.currentPageOffsetFraction
                    TarjetaBando(
                        tipos[i],
                        Modifier.graphicsLayer {
                            val d = -distancia
                            rotationZ = d * INCLINACION_MAXIMA
                            val escala = 1f - d.absoluteValue * 0.05f
                            scaleX = escala
                            scaleY = escala
                            translationX = (10 * d).dp.toPx()
                            translationY = (d.absoluteValue * 30).dp.toPx()
                            alpha = if (d.absoluteValue < 0.5f) 1f else 0.7f
                        },
                    )
                }
            }
            BotonL(
                ColoresTF.Amarillo, "TOTALMENTE YO",
                alTocar = { alContinuar(tipos[pagina.currentPage]) },
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun TarjetaBando(tipo: TipoPersonalidad, modifier: Modifier = Modifier) {
    val b = bando(tipo)
    val forma = DiagonalShape(10.dp)
    val flote = rememberInfiniteTransition(label = "bando")
    val y1 by flote.animateFloat(16f, 4f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "izq")
    val y2 by flote.animateFloat(18f, 6f, infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "der")
    val y3 by flote.animateFloat(8f, -8f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "centro")

    Box(modifier.size(ANCHO_TARJETA, ALTO_TARJETA)) {
        Box(Modifier.fillMaxSize().offset(20.dp, 20.dp).background(Color.Black, forma))
        Column(
            Modifier
                .fillMaxSize()
                .clip(forma)
                .background(b.color, forma)
                .border(BorderStroke(2.dp, Color.Black), forma),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(ALTO_TARJETA * 0.45f).padding(top = 15.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(b.fondo), null, contentScale = ContentScale.Fit, modifier = Modifier.requiredSize(b.anchoFondo, 195.dp))
                ImagenPixel(b.luchadores[0], 125.dp, Modifier.offset(x = ANCHO_TARJETA * -0.28f, y = y1.dp).rotate(-15f))
                ImagenPixel(b.luchadores[1], 125.dp, Modifier.offset(x = ANCHO_TARJETA * 0.28f, y = y2.dp).rotate(15f).graphicsLayer { scaleX = -1f })
                ImagenPixel(b.luchadores[2], 145.dp, Modifier.offset(y = y3.dp))
            }
            TextoConSombra(tipo.nombreVisible, pirata(36), colorSombra = b.colorObscuro)
            Text(
                tipo.descripcion,
                style = arvo(16),
                color = ColoresTF.Blanco.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 6.dp),
            )
        }
    }
}

/** Paso 7: tres divisiones de misiones. En iOS: `DivisionesView`. */
@Composable
fun DivisionesPaso(inicial: Set<DivisionMision>, alContinuar: (Set<DivisionMision>) -> Unit) {
    var seleccion by rememberSaveable { mutableStateOf(inicial.toList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val faltantes = (ReglasOnboarding.DIVISIONES_REQUERIDAS - seleccion.size).coerceAtLeast(0)
    FondoOnboarding {
        Column(Modifier.fillMaxSize().padding(16.dp).padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PreguntaDecorada("¿Qué misiones prefieres?", bajarLineas = 20.dp)
            TextoPaso("Elige ${ReglasOnboarding.DIVISIONES_REQUERIDAS} divisiones ($faltantes restantes)")
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                DivisionMision.entries.forEach { division ->
                    val elegida = division in seleccion
                    TarjetaDivision(division, elegida, habilitada = elegida || faltantes > 0) {
                        seleccion = ReglasOnboarding.alternarDivision(seleccion.toSet(), division).let { nueva ->
                            seleccion.filter { it in nueva } + nueva.filter { it !in seleccion }
                        }
                        error = null
                    }
                }
            }
            error?.let { Text(it, style = arvo(12), color = ColoresTF.RojoError) }
            BotonL(ColoresTF.Amarillo, "CONTINUAR", alTocar = {
                if (seleccion.size == ReglasOnboarding.DIVISIONES_REQUERIDAS) alContinuar(seleccion.toSet())
                else error = "Elige exactamente ${ReglasOnboarding.DIVISIONES_REQUERIDAS} divisiones."
            })
        }
    }
}

private fun icono(division: DivisionMision) = when (division) {
    DivisionMision.CONECTAR -> R.drawable.conexion_icon
    DivisionMision.LECTURA -> R.drawable.lectura_icon
    DivisionMision.DEJAR_CELULAR -> R.drawable.dejar_cel_icon
    DivisionMision.JUEGOS_MENTALES -> R.drawable.retos_icon
    DivisionMision.PROGRESO_PERSONAL -> R.drawable.progreso_icon
    DivisionMision.BIENESTAR_MENTAL -> R.drawable.meditacion_icon
    DivisionMision.EJERCICIO -> R.drawable.ejercicio_icon
}

@Composable
private fun TarjetaDivision(division: DivisionMision, elegida: Boolean, habilitada: Boolean, alAlternar: () -> Unit) {
    Box(Modifier.width(330.dp), contentAlignment = Alignment.Center) {
        PlacaChueca(260.dp, 84.dp) {
            Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(icono(division)), null, Modifier.size(56.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text(division.titulo, style = pirata(22), color = ColoresTF.Blanco, maxLines = 1)
                    Text(division.descripcion, style = arvo(13), color = ColoresTF.Blanco, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        BotonS(
            ColoresTF.Amarillo,
            if (elegida) IconoBotonS.CHECK else IconoBotonS.VACIO,
            alTocar = alAlternar,
            habilitado = habilitada,
            modifier = Modifier.offset(x = 120.dp),
        )
    }
}

@Preview(heightDp = 860)
@Composable
private fun DatosPreview() = TiempoFueraTheme {
    ContenidoDatos(EstadoDatos(email = "luchador@rudo.mx", fase = FaseDatos.INGRESANDO_CODIGO), {}, {}, {}, {})
}

@Preview(heightDp = 860)
@Composable
private fun BandoPreview() = TiempoFueraTheme { BandoPaso(TipoPersonalidad.DIPLOMATICO) {} }

@Preview(heightDp = 860)
@Composable
private fun DivisionesPreview() = TiempoFueraTheme { DivisionesPaso(setOf(DivisionMision.LECTURA)) {} }
