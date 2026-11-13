package com.rudoestudio.tiempofuera.core.logging

import android.util.Log

/**
 * Equivalente a `AppLogger.shared` de iOS. Sale en logcat con la etiqueta `TiempoFuera`
 * (`adb logcat -s TiempoFuera`). Cuando haya reporte de errores (Crashlytics u otro),
 * se conecta aquí y el resto de la app no cambia.
 */
object AppLogger {
    private const val ETIQUETA = "TiempoFuera"

    fun debug(mensaje: String) {
        Log.d(ETIQUETA, mensaje)
    }

    fun info(mensaje: String) {
        Log.i(ETIQUETA, mensaje)
    }

    fun warning(mensaje: String) {
        Log.w(ETIQUETA, mensaje)
    }

    fun error(error: Throwable, contexto: String) {
        Log.e(ETIQUETA, "[$contexto] ${error.message}", error)
    }
}
