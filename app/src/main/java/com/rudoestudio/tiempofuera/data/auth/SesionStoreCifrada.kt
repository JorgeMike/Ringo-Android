package com.rudoestudio.tiempofuera.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rudoestudio.tiempofuera.core.logging.AppLogger
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.auth.SesionUsuario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cifra con AES-GCM usando una llave del Android Keystore, que nunca sale del hardware
 * seguro. (En iOS esto lo da el Keychain; `EncryptedSharedPreferences` está deprecado.)
 */
@Singleton
class CifradorKeystore @Inject constructor() {

    fun cifrar(datos: ByteArray): String {
        val cifrado = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, llave()) }
        return Base64.encodeToString(cifrado.iv + cifrado.doFinal(datos), Base64.NO_WRAP)
    }

    fun descifrar(texto: String): ByteArray {
        val todo = Base64.decode(texto, Base64.NO_WRAP)
        val cifrado = Cipher.getInstance(TRANSFORMACION)
        cifrado.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, todo, 0, LARGO_IV))
        return cifrado.doFinal(todo, LARGO_IV, todo.size - LARGO_IV)
    }

    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generador.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "tiempofuera_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val LARGO_IV = 12
    }
}

/** Forma en disco de [SesionUsuario] (el dominio no depende de kotlinx.serialization). */
@Serializable
private data class SesionGuardada(
    val accessToken: String,
    val refreshToken: String,
    val expiraEnSegundos: Int,
    val usuarioId: String,
    val email: String,
    val onboardingCompletado: Boolean,
    val creadaEnMs: Long,
) {
    fun aDominio() = SesionUsuario(accessToken, refreshToken, expiraEnSegundos, usuarioId, email, onboardingCompletado, creadaEnMs)

    companion object {
        fun de(s: SesionUsuario) =
            SesionGuardada(s.accessToken, s.refreshToken, s.expiraEnSegundos, s.usuarioId, s.email, s.onboardingCompletado, s.creadaEnMs)
    }
}

/** Sesión cifrada en DataStore (en iOS: `SesionStoreKeychain`). */
@Singleton
class SesionStoreCifrada @Inject constructor(
    @ApplicationContext contexto: Context,
    private val cifrador: CifradorKeystore,
    private val json: Json,
) : SesionStore {
    private val datos = PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("sesion") }
    private val clave = stringPreferencesKey("sesion_cifrada")

    override val sesion: Flow<SesionUsuario?> = datos.data
        .map { it[clave]?.let(::leer) }
        .distinctUntilChanged()

    override suspend fun actual(): SesionUsuario? = sesion.first()

    override suspend fun guardar(sesion: SesionUsuario) {
        val texto = json.encodeToString(SesionGuardada.serializer(), SesionGuardada.de(sesion))
        val cifrado = cifrador.cifrar(texto.toByteArray())
        datos.edit { it[clave] = cifrado }
    }

    override suspend fun limpiar() {
        datos.edit { it.remove(clave) }
    }

    /** Si la llave ya no sirve (p. ej. se borraron los datos de seguridad), no hay sesión. */
    private fun leer(cifrado: String): SesionUsuario? = runCatching {
        json.decodeFromString(SesionGuardada.serializer(), String(cifrador.descifrar(cifrado))).aDominio()
    }.onFailure { AppLogger.error(it, "SesionStore") }.getOrNull()
}
