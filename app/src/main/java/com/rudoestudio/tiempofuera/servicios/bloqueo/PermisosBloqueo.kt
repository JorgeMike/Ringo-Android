package com.rudoestudio.tiempofuera.servicios.bloqueo

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.rudoestudio.tiempofuera.domain.bloqueo.EstadoPermisos
import com.rudoestudio.tiempofuera.domain.bloqueo.RevisorPermisos
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Revisa los permisos del bloqueo y abre sus pantallas de Ajustes. En iOS:
 * `ScreenTimeAuthorizationService`. Las notificaciones se piden con el diálogo del
 * sistema desde la pantalla (necesita una Activity).
 */
class PermisosBloqueo @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : RevisorPermisos {

    override fun estado() = EstadoPermisos(
        accesibilidad = accesibilidadActiva(),
        accesoAlUso = accesoAlUso(),
        notificaciones = notificaciones(),
        sinAhorroBateria = contexto.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(contexto.packageName),
    )

    /** Prendido en Ajustes (no garantiza que esté conectado: ver M3.12). */
    fun accesibilidadActiva(): Boolean {
        val activos = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val nuestro = ComponentName(contexto, ServicioBloqueo::class.java)
        return activos.split(':').any { ComponentName.unflattenFromString(it) == nuestro }
    }

    fun accesoAlUso(): Boolean {
        val appOps = contexto.getSystemService(AppOpsManager::class.java)
        return appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), contexto.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    private fun notificaciones(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun abrirAccesibilidad() = abrir(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun abrirAccesoAlUso() = abrir(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    /** Ajustes de notificaciones de la app: para cuando el usuario ya las negó en el diálogo. */
    fun abrirNotificaciones() =
        abrir(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName))

    /**
     * La lista de "optimización de batería". No se usa el diálogo directo
     * (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`): Play solo lo permite a ciertos tipos de app.
     */
    fun abrirBateria() = abrir(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    private fun abrir(intent: Intent) {
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
