package com.rudoestudio.tiempofuera.di

import com.rudoestudio.tiempofuera.data.auth.AuthRepositoryImpl
import com.rudoestudio.tiempofuera.data.auth.SesionStoreCifrada
import com.rudoestudio.tiempofuera.data.bloqueo.AppBlockingDataStore
import com.rudoestudio.tiempofuera.data.bloqueo.AppSelectionDataStore
import com.rudoestudio.tiempofuera.data.bloqueo.AppsInstaladasAndroid
import com.rudoestudio.tiempofuera.data.bloqueo.HistorialUsoDataStore
import com.rudoestudio.tiempofuera.data.bloqueo.ProgresoSeguimientoDataStore
import com.rudoestudio.tiempofuera.data.bloqueo.ProteccionDataStore
import com.rudoestudio.tiempofuera.data.bloqueo.TiempoBloqueadoDataStore
import com.rudoestudio.tiempofuera.data.bloqueo.UsoAppsUsageStats
import com.rudoestudio.tiempofuera.data.configuracion.ConfiguracionGrupoAppsDataStore
import com.rudoestudio.tiempofuera.data.configuracion.ConfiguracionHabitoSuenoDataStore
import com.rudoestudio.tiempofuera.data.ligas.LigaRepositoryCatalogo
import com.rudoestudio.tiempofuera.data.ligas.ProgresoLigaRepositoryRoom
import com.rudoestudio.tiempofuera.data.ligas.ProgresoMisionLigaDataStore
import com.rudoestudio.tiempofuera.data.luchadores.LuchadorRepositoryRoom
import com.rudoestudio.tiempofuera.data.misiones.MisionRepositoryCatalogo
import com.rudoestudio.tiempofuera.data.misiones.MisionesDiariasDataStore
import com.rudoestudio.tiempofuera.data.onboarding.EstadoOnboardingDataStore
import com.rudoestudio.tiempofuera.data.onboarding.PerfilLocalSeederRoom
import com.rudoestudio.tiempofuera.data.perfil.EstadisticasEjercicioDataStore
import com.rudoestudio.tiempofuera.data.perfil.HistoricoRachaRepositoryRoom
import com.rudoestudio.tiempofuera.data.perfil.ProgresoDiarioRepositoryRoom
import com.rudoestudio.tiempofuera.data.perfil.UserProfileRepositoryRoom
import com.rudoestudio.tiempofuera.data.usuario.UserApiRepositoryImpl
import com.rudoestudio.tiempofuera.domain.auth.AuthRepository
import com.rudoestudio.tiempofuera.domain.auth.SesionStore
import com.rudoestudio.tiempofuera.domain.bloqueo.AppBlockingRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.AppSelectionRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.AppsInstaladas
import com.rudoestudio.tiempofuera.domain.bloqueo.HistorialUsoRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.RevisorPermisos
import com.rudoestudio.tiempofuera.domain.bloqueo.ProgresoSeguimientoRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.ProteccionRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.TiempoBloqueadoRepository
import com.rudoestudio.tiempofuera.domain.bloqueo.TiempoExtraAppsImpl
import com.rudoestudio.tiempofuera.domain.bloqueo.UsoApps
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionGrupoAppsRepository
import com.rudoestudio.tiempofuera.domain.configuracion.ConfiguracionHabitoSuenoRepository
import com.rudoestudio.tiempofuera.domain.ligas.LigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoLigaRepository
import com.rudoestudio.tiempofuera.domain.ligas.ProgresoMisionLigaRepository
import com.rudoestudio.tiempofuera.domain.luchadores.LuchadorRepository
import com.rudoestudio.tiempofuera.domain.misiones.MisionRepository
import com.rudoestudio.tiempofuera.domain.misiones.MisionesDiariasRepository
import com.rudoestudio.tiempofuera.domain.onboarding.EstadoOnboardingRepository
import com.rudoestudio.tiempofuera.domain.onboarding.PerfilLocalSeeder
import com.rudoestudio.tiempofuera.domain.perfil.EstadisticasEjercicioRepository
import com.rudoestudio.tiempofuera.domain.perfil.HistoricoRachaRepository
import com.rudoestudio.tiempofuera.domain.perfil.ProgresoDiarioRepository
import com.rudoestudio.tiempofuera.domain.perfil.UserProfileRepository
import com.rudoestudio.tiempofuera.domain.sincronizacion.UserApiRepository
import com.rudoestudio.tiempofuera.domain.tienda.TiempoExtraApps
import com.rudoestudio.tiempofuera.servicios.bloqueo.PermisosBloqueo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Qué implementación usa cada repositorio del dominio (en iOS: `AppContainer`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {

    @Binds
    abstract fun estadoOnboarding(impl: EstadoOnboardingDataStore): EstadoOnboardingRepository

    @Binds
    abstract fun sesionStore(impl: SesionStoreCifrada): SesionStore

    @Binds
    abstract fun auth(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun luchadores(impl: LuchadorRepositoryRoom): LuchadorRepository

    @Binds
    abstract fun misiones(impl: MisionRepositoryCatalogo): MisionRepository

    @Binds
    abstract fun misionesDiarias(impl: MisionesDiariasDataStore): MisionesDiariasRepository

    @Binds
    abstract fun ligas(impl: LigaRepositoryCatalogo): LigaRepository

    @Binds
    abstract fun progresoLiga(impl: ProgresoLigaRepositoryRoom): ProgresoLigaRepository

    @Binds
    abstract fun progresoMisionLiga(impl: ProgresoMisionLigaDataStore): ProgresoMisionLigaRepository

    @Binds
    abstract fun perfil(impl: UserProfileRepositoryRoom): UserProfileRepository

    @Binds
    abstract fun progresoDiario(impl: ProgresoDiarioRepositoryRoom): ProgresoDiarioRepository

    @Binds
    abstract fun historicoRacha(impl: HistoricoRachaRepositoryRoom): HistoricoRachaRepository

    @Binds
    abstract fun estadisticasEjercicio(impl: EstadisticasEjercicioDataStore): EstadisticasEjercicioRepository

    @Binds
    abstract fun configuracionGrupoApps(impl: ConfiguracionGrupoAppsDataStore): ConfiguracionGrupoAppsRepository

    @Binds
    abstract fun configuracionHabitoSueno(impl: ConfiguracionHabitoSuenoDataStore): ConfiguracionHabitoSuenoRepository

    @Binds
    abstract fun userApi(impl: UserApiRepositoryImpl): UserApiRepository

    @Binds
    abstract fun perfilLocalSeeder(impl: PerfilLocalSeederRoom): PerfilLocalSeeder

    @Binds
    abstract fun seleccionApps(impl: AppSelectionDataStore): AppSelectionRepository

    @Binds
    abstract fun bloqueos(impl: AppBlockingDataStore): AppBlockingRepository

    @Binds
    abstract fun progresoSeguimiento(impl: ProgresoSeguimientoDataStore): ProgresoSeguimientoRepository

    @Binds
    abstract fun tiempoBloqueado(impl: TiempoBloqueadoDataStore): TiempoBloqueadoRepository

    @Binds
    abstract fun usoApps(impl: UsoAppsUsageStats): UsoApps

    @Binds
    abstract fun tiempoExtra(impl: TiempoExtraAppsImpl): TiempoExtraApps

    @Binds
    abstract fun appsInstaladas(impl: AppsInstaladasAndroid): AppsInstaladas

    @Binds
    abstract fun revisorPermisos(impl: PermisosBloqueo): RevisorPermisos

    @Binds
    abstract fun proteccion(impl: ProteccionDataStore): ProteccionRepository

    @Binds
    abstract fun historialUso(impl: HistorialUsoDataStore): HistorialUsoRepository
}
