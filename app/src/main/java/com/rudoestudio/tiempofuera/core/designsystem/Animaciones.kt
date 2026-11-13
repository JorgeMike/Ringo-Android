package com.rudoestudio.tiempofuera.core.designsystem

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/** Animaciones compartidas. En iOS: `extension Animation` (Core/Extensions/Animation.swift). */
object AnimacionesTF {
    /**
     * `Animation.navigationTransition` de iOS: resorte con response 0.38 s y damping 0.86.
     * Rigidez = (2π / response)² ≈ 273.
     */
    fun <T> transicionNavegacion(): SpringSpec<T> = spring(dampingRatio = 0.86f, stiffness = 273f)
}
