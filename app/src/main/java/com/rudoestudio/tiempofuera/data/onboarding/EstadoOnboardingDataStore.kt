package com.rudoestudio.tiempofuera.data.onboarding

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si ya se terminó el onboarding, guardado en el teléfono (M1.7). En iOS vive en
 * UserDefaults; el API también lo sabe (`onboardingCompletado`), y se reconcilian en M4.
 */
@Singleton
class EstadoOnboardingDataStore @Inject constructor(
    @ApplicationContext contexto: Context,
) : EstadoOnboardingRepository {
    private val datos = PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("onboarding") }
    private val clave = booleanPreferencesKey("finalizado")

    override val finalizado: Flow<Boolean> = datos.data.map { it[clave] ?: false }

    override suspend fun finalizar() {
        datos.edit { it[clave] = true }
    }
}
