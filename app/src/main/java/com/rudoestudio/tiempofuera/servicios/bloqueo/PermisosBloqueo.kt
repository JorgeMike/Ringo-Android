package com.rudoestudio.tiempofuera.servicios.bloqueo

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Los dos permisos del bloqueo y cómo abrir sus pantallas de Ajustes. En iOS:
 * `ScreenTimeAuthorizationService`. El flujo guiado para pedirlos es M3.1.
 */
object PermisosBloqueo {
    /** El servicio de accesibilidad está prendido en Ajustes (no garantiza que esté conectado: ver M3.12). */
    fun accesibilidadActiva(contexto: Context): Boolean {
        val activos = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val nuestro = ComponentName(contexto, ServicioBloqueo::class.java)
        return activos.split(':').any { ComponentName.unflattenFromString(it) == nuestro }
    }

    fun accesoAlUso(contexto: Context): Boolean {
        val appOps = contexto.getSystemService(AppOpsManager::class.java)
        val modo = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), contexto.packageName)
        return modo == AppOpsManager.MODE_ALLOWED
    }

    fun abrirAccesibilidad(contexto: Context) = abrir(contexto, Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun abrirAccesoAlUso(contexto: Context) = abrir(contexto, Settings.ACTION_USAGE_ACCESS_SETTINGS)

    private fun abrir(contexto: Context, accion: String) {
        contexto.startActivity(Intent(accion).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
