package com.rudoestudio.tiempofuera.data.bloqueo

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.domain.bloqueo.UsoApps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El uso de hoy desde `UsageStatsManager` + [ContadorUsoDiario]. Necesita el permiso de
 * "acceso al uso" (M3.1); sin él, Android no da eventos y todo sale en 0. En el spike:
 * `UsageRepo` y `UsoDiario`.
 */
@Singleton
class UsoAppsUsageStats @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val reloj: Clock,
) : UsoApps {
    private val usm = contexto.getSystemService(UsageStatsManager::class.java)

    // Archivo propio: se escribe cada pocos segundos y no debe reescribir el del juego.
    private val almacen = AlmacenJson(PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("uso_diario") })
    private val candado = Mutex()

    override suspend fun usoDeHoy(): Map<String, Duration> = candado.withLock {
        val hoy = LocalDate.now(reloj)
        val inicioDia = hoy.atStartOfDay(reloj.zone).toInstant().toEpochMilli()
        val arranque = Settings.Global.getInt(contexto.contentResolver, Settings.Global.BOOT_COUNT, -1)
        val horaArranque = reloj.millis() - SystemClock.elapsedRealtime()

        val guardado = almacen.leer(CLAVE, ContadorUsoDiario.serializer()) ?: ContadorUsoDiario.nuevo(hoy, arranque, inicioDia)
        val vigente = guardado.vigente(hoy, arranque, horaArranque, inicioDia)
        if (vigente.desdeMs != guardado.desdeMs || vigente.fecha != guardado.fecha) {
            AppLogger.info("Uso: se mide desde ${Instant.ofEpochMilli(vigente.desdeMs)} (arranque ${vigente.arranque}, antes ${guardado.arranque})")
        }
        val ahora = reloj.millis()
        // Se lee desde 3 h antes para atrapar apps que ya estaban abiertas al empezar.
        val parcial = CalculoUso.usoMs(eventos(vigente.desdeMs - TRES_HORAS, ahora), vigente.desdeMs, ahora)
        val actualizado = vigente.con(parcial)
        almacen.escribir(CLAVE, ContadorUsoDiario.serializer(), actualizado)
        actualizado.ultimo.mapValues { Duration.ofMillis(it.value) }
    }

    override suspend fun enPrimerPlano(): Set<String> {
        val ahora = reloj.millis()
        return CalculoUso.reanudadas(eventos(ahora - TRES_HORAS, ahora))
    }

    private suspend fun eventos(desde: Long, hasta: Long): List<EventoUso> = withContext(Dispatchers.IO) {
        val consulta = usm.queryEvents(desde, hasta) ?: return@withContext emptyList()
        val e = UsageEvents.Event()
        buildList {
            while (consulta.hasNextEvent()) {
                consulta.getNextEvent(e)
                val tipo = when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> EventoUso.Tipo.REANUDADA
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> EventoUso.Tipo.PAUSADA
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.DEVICE_SHUTDOWN,
                    UsageEvents.Event.DEVICE_STARTUP,
                    -> EventoUso.Tipo.CIERRE_GENERAL
                    else -> null
                }
                if (tipo != null) add(EventoUso(tipo, e.packageName, e.className.orEmpty(), e.timeStamp))
            }
        }
    }

    private companion object {
        const val CLAVE = "contador"
        const val TRES_HORAS = 3 * 3_600_000L
    }
}
