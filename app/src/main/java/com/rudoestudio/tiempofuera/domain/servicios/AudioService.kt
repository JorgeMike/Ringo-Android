package com.rudoestudio.tiempofuera.domain.servicios

/**
 * Efectos de sonido. En iOS: `AudioServiceProtocol`. Los golpes de barrera
 * (`reproducirGolpeBarrera`, `reproducirFalloBarrera`) llegan con las ligas (M9).
 */
interface AudioService {
    /** Al completar una misión. */
    fun reproducirCampanaRingo()
}
