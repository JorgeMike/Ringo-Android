package com.rudoestudio.tiempofuera.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.rudoestudio.tiempofuera.data.local.AlmacenJson
import com.rudoestudio.tiempofuera.data.local.BaseDatosTiempoFuera
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Base de datos (lo que en iOS es SwiftData) y valores sueltos (lo que es UserDefaults). */
@Module
@InstallIn(SingletonComponent::class)
object PersistenciaModule {

    @Provides
    @Singleton
    fun baseDatos(@ApplicationContext contexto: Context): BaseDatosTiempoFuera =
        Room.databaseBuilder(contexto, BaseDatosTiempoFuera::class.java, BaseDatosTiempoFuera.NOMBRE).build()

    @Provides
    fun luchadores(db: BaseDatosTiempoFuera) = db.luchadores()

    @Provides
    fun progresoLigas(db: BaseDatosTiempoFuera) = db.progresoLigas()

    @Provides
    fun progresoDiario(db: BaseDatosTiempoFuera) = db.progresoDiario()

    @Provides
    fun historicoRacha(db: BaseDatosTiempoFuera) = db.historicoRacha()

    @Provides
    fun perfil(db: BaseDatosTiempoFuera) = db.perfil()

    @Provides
    @Singleton
    fun almacenJson(@ApplicationContext contexto: Context): AlmacenJson =
        AlmacenJson(PreferenceDataStoreFactory.create { contexto.preferencesDataStoreFile("juego") })
}
