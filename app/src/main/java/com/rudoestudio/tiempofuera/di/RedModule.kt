package com.rudoestudio.tiempofuera.di

import com.rudoestudio.tiempofuera.BuildConfig
import com.rudoestudio.tiempofuera.data.red.ApiTiempoFuera
import com.rudoestudio.tiempofuera.data.red.AutenticadorRenovacion
import com.rudoestudio.tiempofuera.data.red.CLIENTE_SIN_AUTH
import com.rudoestudio.tiempofuera.data.red.InterceptorAuth
import com.rudoestudio.tiempofuera.data.red.URL_API
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/** Cliente HTTP de tiempo-fuera-api (en iOS: `URLSessionHTTPClient`). */
@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true // el API puede agregar campos sin romper versiones viejas
        explicitNulls = false // los opcionales nulos no se mandan (PATCH /me)
    }

    @Provides
    @Named(URL_API)
    fun urlApi(): HttpUrl = BuildConfig.API_URL.toHttpUrl()

    /** Sin token ni renovación: lo usa RenovadorSesion para llamar a /auth/refresh. */
    @Provides
    @Singleton
    @Named(CLIENTE_SIN_AUTH)
    fun clienteSinAuth(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // BASIC: método, URL y código. Nunca HEADERS/BODY: escribirían tokens en logcat.
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun cliente(
        @Named(CLIENTE_SIN_AUTH) base: OkHttpClient,
        interceptor: InterceptorAuth,
        autenticador: AutenticadorRenovacion,
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(interceptor)
        .authenticator(autenticador)
        .build()

    @Provides
    @Singleton
    fun api(cliente: OkHttpClient, @Named(URL_API) url: HttpUrl, json: Json): ApiTiempoFuera = Retrofit.Builder()
        .baseUrl(url)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiTiempoFuera::class.java)
}
