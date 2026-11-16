package com.rudoestudio.tiempofuera.features.misiones

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.core.designsystem.ColoresTF
import com.rudoestudio.tiempofuera.core.designsystem.componentes.BotonL
import com.rudoestudio.tiempofuera.core.designsystem.componentes.DiagonalShape
import com.rudoestudio.tiempofuera.core.designsystem.componentes.ImagenPixel
import com.rudoestudio.tiempofuera.core.designsystem.componentes.SeccionRecompensas
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoConSombra
import com.rudoestudio.tiempofuera.core.designsystem.componentes.TextoContorno
import com.rudoestudio.tiempofuera.core.designsystem.componentes.Titulo
import com.rudoestudio.tiempofuera.core.hapticos.LocalHaptics
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.misiones.ContadorRepeticiones
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RelojMision
import com.rudoestudio.tiempofuera.domain.misiones.SesionLagartijas
import com.rudoestudio.tiempofuera.domain.misiones.SesionLagartijas.Estado
import com.rudoestudio.tiempofuera.domain.misiones.SuperficieLagartijas
import com.rudoestudio.tiempofuera.domain.servicios.HapticService.EstiloImpacto
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Lagartijas (M6.9): el teléfono boca arriba en el piso, la cámara de enfrente ve tu cara y
 * cuenta cada vez que bajas y subes. Con meta (15 lagartijas) o con tiempo (las que salgan
 * en un minuto, cada una paga). En iOS: `LagartijasView` + `CamaraPreviewManager`.
 */
@Composable
internal fun LagartijasMision(mision: Mision, luchador: Int?, alGuardarMarca: (Instant, Int) -> Unit, alReclamar: (Int) -> Unit) {
    val contexto = LocalContext.current
    var permiso by remember { mutableStateOf(tienePermisoCamara(contexto)) }
    var negado by remember { mutableStateOf(false) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permiso = ok
        negado = !ok
    }
    LaunchedEffect(Unit) { if (!permiso) pedir.launch(Manifest.permission.CAMERA) }

    if (!permiso) {
        SinCamara(negado, alPedir = { pedir.launch(Manifest.permission.CAMERA) })
        return
    }
    Lagartijas(mision, luchador, alGuardarMarca, alReclamar)
}

@Composable
private fun Lagartijas(mision: Mision, luchador: Int?, alGuardarMarca: (Instant, Int) -> Unit, alReclamar: (Int) -> Unit) {
    val haptics = LocalHaptics.current
    val contexto = LocalContext.current
    val camara = remember { CamaraLagartijas(contexto) }
    var sesion by remember { mutableStateOf(SesionLagartijas.para(mision)) }
    val contador = remember { ContadorRepeticiones() }
    var cerca by remember { mutableStateOf(false) }
    var inicio by remember { mutableStateOf<Instant?>(null) }
    val enCurso by rememberUpdatedState(sesion.enCurso)

    SensorSuperficie { plana -> sesion = sesion.superficie(plana) }
    // Cada cuadro de la cámara: ¿qué tan grande se ve la cara? Solo cuenta ya empezada; la
    // cámara sigue prendida antes y después para que te acomodes y veas cómo quedó.
    DetectorRostro(camara) { ancho ->
        if (enCurso) {
            if (contador.evaluar(ancho, SystemClock.elapsedRealtime() / 1000.0)) {
                haptics.impacto(EstiloImpacto.MEDIO)
                sesion = sesion.repeticion()
            }
            cerca = contador.cerca
        }
    }

    LaunchedEffect(sesion.enCurso) {
        if (!sesion.enCurso) return@LaunchedEffect
        while (sesion.enCurso && sesion.conTiempo) {
            delay(1_000)
            sesion = sesion.segundo()
        }
    }
    LaunchedEffect(sesion.completado) {
        if (!sesion.completado) return@LaunchedEffect
        haptics.impacto(EstiloImpacto.FUERTE)
        inicio?.let { alGuardarMarca(it, sesion.repeticiones) }
    }

    val borde by animateColorAsState(
        when (sesion.estado) {
            Estado.EN_PROGRESO -> if (cerca) ColoresTF.Verde else ColoresTF.Amarillo
            Estado.SUPERFICIE_INVALIDA -> ColoresTF.Rojo
            Estado.LISTO, Estado.COMPLETADO -> ColoresTF.Verde
            Estado.VERIFICANDO_SUPERFICIE -> ColoresTF.Blanco
        },
        label = "borde",
    )

    ColumnaMision(Modifier.padding(bottom = 24.dp), Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImagenPixel(luchador ?: R.drawable.ringo, 120.dp, Modifier.rotate(-12f))
            Titulo("Ejercita", "ese cuerpo", Modifier.offset(x = (-40).dp))
        }
        Spacer(Modifier.weight(1f))
        TextoContorno(
            mision.descripcion, arvo(18), ColoresTF.Blanco, Color.Black, 3.dp,
            alineacion = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp),
        )
        Box(Modifier.padding(bottom = 28.dp)) {
            // La sombra negra chueca detrás de la cámara, como en iOS.
            Box(Modifier.size(275.dp).offset(10.dp, 15.dp).rotate(-2f).background(Color.Black, DiagonalShape(18.dp)))
            VistaCamara(
                camara,
                Modifier
                    .size(275.dp)
                    .shadow(if (cerca) 12.dp else 0.dp, ambientColor = borde, spotColor = borde)
                    .clipToBounds()
                    .border(if (cerca) 5.dp else 3.dp, borde),
            )
            ContadorBadge(sesion, Modifier.align(Alignment.BottomEnd).offset(32.dp, 48.dp))
        }
        sesion.restante?.let { TextoConSombra(RelojMision.formato(it), arvo(24), colorSombra = ColoresTF.Amarillo) }
        if (sesion.conTiempo) {
            val r = mision.recompensa.multiplicada(sesion.repeticiones)
            SeccionRecompensas(r.puntosPoder, r.monedas, r.exp)
        }
        sesion.mensaje?.let {
            val color = when (sesion.estado) {
                Estado.SUPERFICIE_INVALIDA -> ColoresTF.Rojo
                Estado.LISTO -> ColoresTF.Verde
                else -> ColoresTF.Blanco
            }
            TextoContorno(it, arvo(16), color, Color.Black, 2.dp, alineacion = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        }
        if (sesion.completado) {
            TextoContorno(sesion.mensajeCompletado, arvo(18), ColoresTF.Verde, Color.Black, 2.dp, alineacion = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        }
        Spacer(Modifier.weight(1f))
        when (sesion.estado) {
            Estado.LISTO -> BotonL(ColoresTF.Verde, "COMENZAR", alTocar = {
                contador.reiniciar()
                cerca = false
                inicio = Instant.now()
                sesion = sesion.iniciar()
            })
            Estado.COMPLETADO -> BotonL(ColoresTF.Amarillo, "RECLAMAR", alTocar = { alReclamar(sesion.unidadesRecompensa) })
            else -> Unit
        }
    }
}

/** "7 / 15" (o solo "7" con tiempo) sobre el badge, abajo a la derecha de la cámara. */
@Composable
private fun ContadorBadge(sesion: SesionLagartijas, modifier: Modifier) {
    val texto = sesion.objetivo?.let { "${sesion.repeticiones} / $it" } ?: "${sesion.repeticiones}"
    Box(
        modifier.semantics { contentDescription = "Repeticiones: $texto" },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.badge_indicador), null)
        TextoConSombra(texto, pirata(56), color = Color.Black, colorSombra = ColoresTF.Blanco, modifier = Modifier.rotate(8f).offset((-10).dp, (-6).dp))
    }
}

/** Avisa si el teléfono está acostado boca arriba, con el sensor de gravedad. */
@Composable
private fun SensorSuperficie(alCambiar: (Boolean) -> Unit) {
    val contexto = LocalContext.current
    val alCambiarActual by rememberUpdatedState(alCambiar)
    DisposableEffect(Unit) {
        val gestor = contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = gestor.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: gestor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var ultima: Boolean? = null
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                val plana = SuperficieLagartijas.esPlana(evento.values[2])
                if (plana != ultima) {
                    ultima = plana
                    alCambiarActual(plana)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        if (sensor == null) alCambiarActual(true) else gestor.registerListener(oyente, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { gestor.unregisterListener(oyente) }
    }
}

/** La cámara frontal: el controlador (análisis de cuadros) y su vista previa. */
private class CamaraLagartijas(contexto: Context) {
    val controlador = LifecycleCameraController(contexto).apply {
        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
    }
    val vista = PreviewView(contexto).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
}

/** La vista previa de la cámara frontal. */
@Composable
private fun VistaCamara(camara: CamaraLagartijas, modifier: Modifier) {
    AndroidView(factory = { camara.vista.apply { controller = camara.controlador } }, modifier = modifier)
}

/**
 * Prende la cámara frontal y, en cada cuadro, manda qué tanto del ancho ocupa la cara (0 si
 * no hay). ML Kit en modo rápido; si sigue procesando el cuadro anterior, este se tira
 * (llegan ~30 por segundo). En iOS: `SensorRepeticionesFacialService.procesarFrame`.
 */
@Composable
private fun DetectorRostro(camara: CamaraLagartijas, alAncho: (Float) -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val alAnchoActual by rememberUpdatedState(alAncho)
    DisposableEffect(ciclo) {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build(),
        )
        val hilo = Executors.newSingleThreadExecutor()
        val principal = ContextCompat.getMainExecutor(contexto)
        camara.controlador.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        camara.controlador.setImageAnalysisAnalyzer(hilo) { imagen -> analizar(imagen, detector) { ancho -> principal.execute { alAnchoActual(ancho) } } }
        camara.controlador.bindToLifecycle(ciclo)
        onDispose {
            camara.controlador.clearImageAnalysisAnalyzer()
            camara.controlador.unbind()
            detector.close()
            hilo.shutdown()
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analizar(imagen: ImageProxy, detector: FaceDetector, alAncho: (Float) -> Unit) {
    val media = imagen.image ?: run { imagen.close(); return }
    val rotacion = imagen.imageInfo.rotationDegrees
    // El ancho "derecho" de la imagen: rotada 90° o 270° es su alto.
    val ancho = (if (rotacion % 180 == 0) imagen.width else imagen.height).toFloat()
    detector.process(InputImage.fromMediaImage(media, rotacion))
        .addOnSuccessListener { caras -> alAncho((caras.maxOfOrNull { it.boundingBox.width() } ?: 0) / ancho) }
        .addOnFailureListener { AppLogger.error(it, "LagartijasMision") }
        .addOnCompleteListener { imagen.close() }
}

private fun tienePermisoCamara(contexto: Context) =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Sin cámara no se puede contar: se explica y se vuelve a pedir (o se manda a Ajustes). */
@Composable
private fun SinCamara(negado: Boolean, alPedir: () -> Unit) {
    val contexto = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text("📷", style = pirata(56))
        TextoContorno(
            "Para contar tus lagartijas necesitamos ver tu cara con la cámara de enfrente. Nada se graba ni se guarda.",
            arvo(18), ColoresTF.Blanco, Color.Black, 2.dp, alineacion = TextAlign.Center,
        )
        if (negado) {
            BotonL(ColoresTF.Amarillo, "ABRIR AJUSTES", alTocar = {
                contexto.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
        }
        BotonL(ColoresTF.Verde, "DAR PERMISO", alTocar = alPedir)
    }
}
