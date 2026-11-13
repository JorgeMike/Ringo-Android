package com.rudoestudio.tiempofuera.servicios.bloqueo

import android.content.Context

/** Nombre visible de una app ("YouTube"); el paquete si no se encuentra. */
fun nombreDeApp(contexto: Context, paquete: String): String = runCatching {
    val pm = contexto.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
}.getOrDefault(paquete)
