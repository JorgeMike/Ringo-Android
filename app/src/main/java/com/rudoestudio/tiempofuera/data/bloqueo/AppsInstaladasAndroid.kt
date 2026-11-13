package com.rudoestudio.tiempofuera.data.bloqueo

import android.content.Context
import android.content.Intent
import com.rudoestudio.tiempofuera.domain.bloqueo.AppInstalada
import com.rudoestudio.tiempofuera.domain.bloqueo.AppsInstaladas
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Apps con ícono en el launcher. Con el `<queries>` del manifest se ven todas sin pedir
 * `QUERY_ALL_PACKAGES` (en el spike: las 150 del S24+). En el spike: `Apps.lanzables`.
 */
class AppsInstaladasAndroid @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : AppsInstaladas {
    override suspend fun lanzables(): List<AppInstalada> = withContext(Dispatchers.IO) {
        val pm = contexto.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { AppInstalada(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.paquete != contexto.packageName }
            .distinctBy { it.paquete }
            .sortedBy { it.nombre.lowercase() }
    }
}
