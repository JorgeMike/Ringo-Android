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

/** `AppRoute.ligaNiveles(Liga)` en iOS. Se pasa el id; la liga se carga en M9. */
@Serializable
data class LigaNiveles(val ligaId: String)

/** `HeaderDestination.tiendaMonedas`: pantalla completa, sin barra de tabs. */
@Serializable
data object TiendaMonedas
