package com.rudoestudio.tiempofuera.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.ProgresoOnboardingGuardado
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.domain.onboarding.ProgresoOnboarding
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si ya se terminó el onboarding y, mientras no, el paso y las respuestas (M1.7, M4.0).
 * En iOS vive en UserDefaults; el API también lo sabe (`onboardingCompletado`).
 */
@Singleton
class EstadoOnboardingDataStore internal constructor(
    private val datos: DataStore<Preferences>,
) : EstadoOnboardingRepository {

    @Inject
    constructor(@ApplicationContext contexto: Context) :
        this(PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("onboarding") })

    private val almacen = AlmacenJson(datos)
    private val claveFinalizado = booleanPreferencesKey("finalizado")

    override val finalizado: Flow<Boolean> = datos.data.map { it[claveFinalizado] ?: false }

    override suspend fun finalizar() {
        datos.edit {
            it[claveFinalizado] = true
            it.remove(stringPreferencesKey(CLAVE_PROGRESO))
        }
    }

    override suspend fun obtenerProgreso(): ProgresoOnboarding? =
        almacen.leer(CLAVE_PROGRESO, ProgresoOnboardingGuardado.serializer())?.aDominio()

    override suspend fun guardarProgreso(progreso: ProgresoOnboarding) {
        almacen.escribir(CLAVE_PROGRESO, ProgresoOnboardingGuardado.serializer(), ProgresoOnboardingGuardado.de(progreso))
    }

    override suspend fun reiniciar() {
        datos.edit {
            it[claveFinalizado] = false
            it.remove(stringPreferencesKey(CLAVE_PROGRESO))
        }
    }

    private companion object {
        const val CLAVE_PROGRESO = "progreso"
    }
}
