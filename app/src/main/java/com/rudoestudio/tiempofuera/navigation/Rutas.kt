package com.rudoestudio.tiempofuera.navigation

import kotlinx.serialization.Serializable

// Rutas con tipo de Navigation Compose. En iOS: las tabs de `MainTabView`, `AppRoute`
// (navegación dentro de Inicio) y `HeaderDestination` (pantallas completas del encabezado).

/** Tab Inicio: es un grafo porque tiene navegación propia (`AppRoute` en iOS). */
@Serializable
data object GrafoInicio

@Serializable
data object Inicio

@Serializable
data object Luchadores

@Serializable
data object Tienda

@Serializable
data object Perfil

/** `AppRoute.ligaNiveles(Liga)` en iOS: el camino de una liga (M9.2). */
@Serializable
data class LigaNiveles(val ligaId: Int)

/** `HeaderDestination.tiendaMonedas`: pantalla completa, sin barra de tabs. */
@Serializable
data object TiendaMonedas

/** Galería de componentes (herramienta de desarrollo, no existe en iOS). */
@Serializable
data object Galeria

/** Laboratorio para probar el API (herramienta de desarrollo, no existe en iOS). */
@Serializable
data object LaboratorioApi

/** Los pasos del onboarding (M4), inicio de su navegación propia. En iOS: `OnboardingCoordinatorView`. */
@Serializable
data object PasosOnboarding

/** Permisos del bloqueo (M3.1). En iOS es parte del paso 10 del onboarding. */
@Serializable
data object PermisosBloqueo

/** Aviso de accesibilidad que pide Google Play antes del permiso (no existe en iOS). */
@Serializable
data object AvisoAccesibilidad

/** Elegir las apps de un grupo (M3.2). [grupo] es la clave de `TipoGrupoApps`. */
@Serializable
data class SelectorApps(val grupo: String)

/** Una misión a pantalla completa (M6.0). En iOS: `.misionFlow` (`fullScreenCover`). */
@Serializable
data class MisionFlujo(val misionId: Int)

/** Días invicto: calendario y récords (M5.2). En iOS: `RachaStatsView`, desde Inicio. */
@Serializable
data object RachaStats

/** Tiempo en pantalla y su configuración (M3.10). En iOS: `ScreenTimeStatsView`, desde Inicio. */
@Serializable
data object EstadisticasUso

/** Lo que se sigue de un grupo de apps (M11.3), desde Perfil. [grupo] es la clave de `TipoGrupoApps`. */
@Serializable
data class EditarGrupoApps(val grupo: String)
