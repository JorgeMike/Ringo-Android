package com.rudoestudio.tiempofuera

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rudoestudio.tiempofuera.core.designsystem.TiempoFueraTheme
import com.rudoestudio.tiempofuera.navigation.DeepLink
import com.rudoestudio.tiempofuera.navigation.RootScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Deep link que llegó y la UI todavía no atiende (en iOS: `.onOpenURL`). */
    private val deepLinkPendiente = MutableStateFlow<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Íconos claros en las barras del sistema: los fondos de la app son colores de
        // marca oscuros (azul, rojo, morado…), aunque el tema de Material sea claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) recibir(intent)
        setContent {
            val deepLink by deepLinkPendiente.collectAsState()
            TiempoFueraTheme {
                RootScreen(
                    deepLinkPendiente = deepLink,
                    alAtenderDeepLink = { deepLinkPendiente.value = null },
                )
            }
        }
    }

    // launchMode="singleTask": un enlace con la app abierta llega aquí, no a onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibir(intent)
    }

    private fun recibir(intent: Intent?) {
        DeepLink.desde(intent?.dataString)?.let { deepLinkPendiente.value = it }
    }
}
