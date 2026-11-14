package com.rudoestudio.tiempofuera.domain.servicios

/** Efectos de sonido. En iOS: `AudioServiceProtocol`. */
interface AudioService {
    /** Al completar una misión. */
    fun reproducirCampanaRingo()

    /** Golpe que conecta en el minijuego de barrera (o una guardia que para el golpe). */
    fun reproducirGolpeBarrera()

    /** Golpe al aire o golpe recibido en el minijuego. */
    fun reproducirFalloBarrera()
}
