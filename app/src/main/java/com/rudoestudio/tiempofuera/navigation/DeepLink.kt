package com.rudoestudio.tiempofuera.navigation

import java.net.URI

/**
 * Enlaces que abren la app. Mismo formato que iOS para que el API y las notificaciones
 * manden un solo enlace a ambas plataformas: `ringo://id-mision/<id>`.
 */
sealed interface DeepLink {
    data class IdMision(val id: Int) : DeepLink

    companion object {
        /** `null` si [url] no es un enlace de la app o viene mal formado. */
        fun desde(url: String?): DeepLink? {
            val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return null
            if (uri.scheme != "ringo" || uri.host != "id-mision") return null
            val id = uri.path.orEmpty().split('/').firstOrNull { it.isNotEmpty() }?.toIntOrNull()
                ?: return null
            return IdMision(id)
        }
    }
}
