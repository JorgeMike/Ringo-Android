package com.rudoestudio.tiempofuera.domain.servicios

/** Vibraciones de la app. En iOS: `HapticServiceProtocol` + `HapticService`. */
interface HapticService {

    enum class EstiloImpacto {
        /** Tap suave: botones secundarios, elegir una opción. */
        LIGERO,

        /** Tap medio: botones principales. */
        MEDIO,

        /** Tap fuerte: acciones importantes, nivel máximo de un slider. */
        FUERTE,

        /** Suave y amortiguado: transiciones. */
        SUAVE,

        /** Seco y preciso: snaps, confirmaciones. */
        RIGIDO,
    }

    enum class TipoNotificacion {
        /** Onboarding completado, guardado exitoso. */
        EXITO,

        /** Validación fallida. */
        ADVERTENCIA,

        /** Error de red, error al guardar. */
        ERROR,
    }

    fun impacto(estilo: EstiloImpacto)

    fun notificacion(tipo: TipoNotificacion)

    fun seleccion()

    /** Vibración continua fuerte, como un temblor. */
    fun sacudidaFuerte(duracionSegundos: Double)

    /** Golpe seco con fragmentos que se apagan (piñata, sobres). */
    fun vibrarRompimientoDeVidrio()

    /** Tick con [intensidad] de 0 a 1, para contadores y sliders. */
    fun vibrarTick(intensidad: Float)

    /** Pulso suave con [intensidad] de 0 a 1, para la meditación. */
    fun vibrarRespiracion(intensidad: Float)
}
