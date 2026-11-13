package com.rudoestudio.tiempofuera.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Valores sueltos en JSON sobre DataStore: lo que iOS guarda en UserDefaults con
 * `JSONEncoder`. Un valor corrupto o de un formato viejo se registra y se trata como
 * si no existiera, igual que en iOS (nunca truena la app).
 */
class AlmacenJson(private val datos: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun <T> leer(clave: String, serializador: KSerializer<T>): T? = observar(clave, serializador).first()

    /** El valor cada vez que cambia (solo cuando cambia esa clave, no el resto del archivo). */
    fun <T> observar(clave: String, serializador: KSerializer<T>): Flow<T?> =
        datos.data.map { it[stringPreferencesKey(clave)] }.distinctUntilChanged().map { texto ->
            texto?.let { decodificar(clave, serializador, it) }
        }

    private fun <T> decodificar(clave: String, serializador: KSerializer<T>, texto: String): T? = try {
        json.decodeFromString(serializador, texto)
    } catch (e: SerializationException) {
        AppLogger.error(e, "AlmacenJson.$clave")
        null
    } catch (e: IllegalArgumentException) {
        AppLogger.error(e, "AlmacenJson.$clave")
        null
    }

    suspend fun <T> escribir(clave: String, serializador: KSerializer<T>, valor: T) {
        val texto = json.encodeToString(serializador, valor)
        datos.edit { it[stringPreferencesKey(clave)] = texto }
    }

    /** Lee, transforma y escribe en una sola edición (sin carreras entre dos escrituras). */
    suspend fun <T> actualizar(clave: String, serializador: KSerializer<T>, transformar: (T?) -> T) {
        datos.edit { prefs ->
            val llave = stringPreferencesKey(clave)
            val actual = prefs[llave]?.let { decodificar(clave, serializador, it) }
            prefs[llave] = json.encodeToString(serializador, transformar(actual))
        }
    }

    suspend fun borrar(clave: String) {
        datos.edit { it.remove(stringPreferencesKey(clave)) }
    }
}
