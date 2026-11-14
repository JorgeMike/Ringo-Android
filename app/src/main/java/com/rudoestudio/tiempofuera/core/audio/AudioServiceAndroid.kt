package com.rudoestudio.tiempofuera.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.rudoestudio.tiempofuera.R
import com.rudoestudio.tiempofuera.domain.servicios.AudioService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sonidos cortos con `SoundPool`: se cargan una vez y suenan sin retraso. Respeta el volumen
 * de "juego" del teléfono (en silencio no suena). En iOS: `AudioService` (AVAudioPlayer).
 */
@Singleton
class AudioServiceAndroid @Inject constructor(
    @ApplicationContext contexto: Context,
) : AudioService {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val campana = pool.load(contexto, R.raw.campana_ringo, 1)

    override fun reproducirCampanaRingo() {
        // Si todavía no termina de cargar, `play` regresa 0 y no suena: no pasa nada.
        pool.play(campana, 1f, 1f, 1, 0, 1f)
    }
}
