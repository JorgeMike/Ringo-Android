package com.rudoestudio.tiempofuera.data.catalogo

import com.rudoestudio.tiempofuera.domain.configuracion.TipoGrupoApps
import com.rudoestudio.tiempofuera.domain.misiones.ConfiguracionMision
import com.rudoestudio.tiempofuera.domain.misiones.DivisionMision
import com.rudoestudio.tiempofuera.domain.misiones.ElementoEnEscena
import com.rudoestudio.tiempofuera.domain.misiones.Mision
import com.rudoestudio.tiempofuera.domain.misiones.RecompensaMision
import com.rudoestudio.tiempofuera.domain.misiones.Secuencia
import com.rudoestudio.tiempofuera.domain.misiones.TipoMision

// GENERADO por scripts/importar_semillas_ios.py desde iOS (MisionesSeedData).
// No editar a mano: corregir en iOS y volver a correr el script.

/** Misiones diarias (ids 1000+), repartidas por el MotorDeMisiones. */
val CATALOGO_MISIONES_DIARIAS: List<Mision> = listOf(
    Mision(
        id = 1001,
        titulo = "Calentamiento rápido",
        descripcion = "Realiza una serie de sentadillas para activar el flujo sanguíneo antes de continuar con tu día.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 20),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(cantidadObjetivo = 20),
        tipoMision = TipoMision.ACCIONES,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1002,
        titulo = "15 lagartijas sin cebolla",
        descripcion = "Échate 15 lagartijas usando el sensor de cercanía.",
        recompensa = RecompensaMision(puntosPoder = 25, monedas = 30),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(repeticionesObjetivo = 15, umbralCercaniaCM = 15.0),
        tipoMision = TipoMision.LAGARTIJAS,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1003,
        titulo = "Sacúdete la flojera",
        descripcion = "Guarda el cel y muévete sin parar durante el tiempo indicado.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 20),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 180),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1004,
        titulo = "El Monte Everest no tiene nada en contra de ti",
        descripcion = "Realiza una serie corta de lagartijas usando el sensor de proximidad en cuanto tengas un momento libre.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 20),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(repeticionesObjetivo = 10, umbralCercaniaCM = 15.0),
        tipoMision = TipoMision.LAGARTIJAS,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1005,
        titulo = "Pausa activa",
        descripcion = "Realiza sentadillas de forma fluida para liberar la tensión acumulada en el cuerpo.",
        recompensa = RecompensaMision(puntosPoder = 12, monedas = 15),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(cantidadObjetivo = 30),
        tipoMision = TipoMision.ACCIONES,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1006,
        titulo = "El novato que se atreve",
        descripcion = "Prueba un ejercicio que nunca has hecho, aunque te veas chueco al principio. Todo campeón fue novato una vez.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 25),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(cantidadObjetivo = 15),
        tipoMision = TipoMision.ACCIONES,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1007,
        titulo = "Camina por donde nunca has pisado",
        descripcion = "Toma una ruta nueva, come en un local nuevo, etc. Sal de lo cotidiano.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 900),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.EJERCICIO,
        completada = false
    ),
    Mision(
        id = 1008,
        titulo = "Tú, tú solito y tus pensamientos",
        descripcion = "Medita a tu ritmo y acumula una recompensa por cada vuelta de 5 minutos completos.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(segundosParaRecompensa = 300),
        tipoMision = TipoMision.MEDITACION,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1009,
        titulo = "Mente en modo avión",
        descripcion = "Deja que tu cabeza se acomode unos minutos sin que ninguna pantalla se meta en medio.",
        recompensa = RecompensaMision(puntosPoder = 10, monedas = 25),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(duracionSegundos = 300),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1010,
        titulo = "Estírate como gato en domingo",
        descripcion = "Respira profundo y suelta el aire despacio. Acumula una recompensa por cada vuelta de 2 minutos completos.",
        recompensa = RecompensaMision(puntosPoder = 10, monedas = 15),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(segundosParaRecompensa = 120),
        tipoMision = TipoMision.MEDITACION,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1011,
        titulo = "Respira, que no truena nada",
        descripcion = "Enfría tu cabeza a tu ritmo. Acumula una recompensa por cada vuelta de 3 minutos completos.",
        recompensa = RecompensaMision(puntosPoder = 12, monedas = 20),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(segundosParaRecompensa = 180),
        tipoMision = TipoMision.MEDITACION,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1012,
        titulo = "Enfriamiento de puños",
        descripcion = "Date un descanso mental. Camina sin el celular en la mano durante el tiempo indicado.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1013,
        titulo = "Medalla para el campeón",
        descripcion = "Medita sobre tus logros de hoy. Acumula una recompensa por cada vuelta de 4 minutos completos.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 25),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(segundosParaRecompensa = 240),
        tipoMision = TipoMision.MEDITACION,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1014,
        titulo = "Suéltate la lengua bonita contigo mismo",
        descripcion = "Tómate este ratito para hablarte como le hablarías a tu mejor amigo: con paciencia y cariño.",
        recompensa = RecompensaMision(puntosPoder = 12, monedas = 20),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(duracionSegundos = 300),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.BIENESTAR_MENTAL,
        completada = false
    ),
    Mision(
        id = 1015,
        titulo = "Aquí presente, sin distracciones",
        descripcion = "Deja el cel a un ladito mientras platicas o comes con alguien. Que la pantalla no se robe el momento.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 40),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(duracionSegundos = 900),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1016,
        titulo = "Captúrale lo bonito al momento",
        descripcion = "Encuentra algo de color rosa que te saque una sonrisa y tómale la foto junto a alguien que quieras.",
        recompensa = RecompensaMision(puntosPoder = 10, monedas = 15),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(colorObjetivo = "Rosa"),
        tipoMision = TipoMision.TOMAR_FOTO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1017,
        titulo = "¿Con ganas de un cafecito... acompañado?",
        descripcion = "Busca a alguien cerca, invítale un café o plática, y tómale foto a algo café que compartan.",
        recompensa = RecompensaMision(puntosPoder = 12, monedas = 20),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(colorObjetivo = "Café"),
        tipoMision = TipoMision.TOMAR_FOTO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1018,
        titulo = "Vitamina D, no de C-el",
        descripcion = "Sal al balcón, la ventana o la banqueta y échale un ojo a las personas al rededor.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 35),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 450),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1019,
        titulo = "Paparazzi de plantas (no la del pie)",
        descripcion = "Búscate una plantita o algo verde por ahí y tómale su foto, así bien bonita.",
        recompensa = RecompensaMision(puntosPoder = 10, monedas = 20),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(colorObjetivo = "Verde"),
        tipoMision = TipoMision.TOMAR_FOTO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1020,
        titulo = "Chisme de tendedero",
        descripcion = "Platica con alguien que no conozcas, sácale un chisme. No tienes que hacer amigos de por vida, solo cruzar palabras.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 35),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 180),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1021,
        titulo = "Veo veo...",
        descripcion = "¡ALGO ROJO! Busca algo rojo en tu entorno y toma una foto.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 25),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(colorObjetivo = "Rojo"),
        tipoMision = TipoMision.TOMAR_FOTO,
        division = DivisionMision.CONECTAR,
        completada = false
    ),
    Mision(
        id = 1022,
        titulo = "Una cita con tu libro",
        descripcion = "Guarda el celular y dedica este bloque exclusivamente a leer, como si fuera una cita innegociable.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 35),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 1200),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1023,
        titulo = "Bloque de estudio como examen",
        descripcion = "Aparta el celular durante el tiempo indicado y respeta el bloque de lectura como si fuera un examen cronometrado.",
        recompensa = RecompensaMision(puntosPoder = 12, monedas = 25),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1024,
        titulo = "Bloque de trabajo profundo (con libro)",
        descripcion = "Aparta el celular y dedica este bloque exclusivamente a avanzar en tu lectura pendiente.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(duracionSegundos = 900),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1025,
        titulo = "Página tras página, sin el fon de por medio",
        descripcion = "Bloquea las distracciones y avanza en tu libro o artículo pendiente durante el tiempo indicado.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 30),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(duracionSegundos = 1500),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1026,
        titulo = "Póngase a leer, ándele",
        descripcion = "Guarda el celular boca abajo durante el tiempo indicado y no rompas el bloque de lectura.",
        recompensa = RecompensaMision(puntosPoder = 14, monedas = 25),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(duracionSegundos = 480),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1027,
        titulo = "Aprendiz de otro idioma",
        descripcion = "Dedica este bloque a practicar una lengua nueva. Hola, konnichiwa uwu, pialli, tú escoge.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 30),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1028,
        titulo = "Sal de tu esquina",
        descripcion = "Lee algo de un género que normalmente ni voltearías a ver. La mente también necesita estirarse fuera de su zona cómoda.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 25),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 900),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.LECTURA,
        completada = false
    ),
    Mision(
        id = 1029,
        titulo = "La papa caliente...",
        descripcion = "¡Tu fon es la papa caliente! Guárdalo fuera de tu vista durante el tiempo indicado.",
        recompensa = RecompensaMision(puntosPoder = 25, monedas = 40),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(duracionSegundos = 1500),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1030,
        titulo = "Una cita contigo mismo... owww",
        descripcion = "Guarda el celular en un cajón y cumple el bloque de tiempo indicado como si fuera una cita.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 35),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 1200),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1031,
        titulo = "Salte a orear un rato, no hace daño",
        descripcion = "Sal a caminar sin el celular en la mano durante tu descanso establecido, sin extenderlo ni acortarlo.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1032,
        titulo = "¿Ya escuchaste los grillos?",
        descripcion = "Deja el aparato lejos y ponle oído a los sonidos de tu entorno, aunque sea el perro del vecino.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 300),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1033,
        titulo = "Suelta el cel y estírate, compadre",
        descripcion = "Deja el teléfono guardado y camina un rato para despejar la vista y las piernas.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 40),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1034,
        titulo = "Los sagrados alimentos",
        descripcion = "Guarda el celular durante toda tu comida, aunque estés solo.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 900),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1035,
        titulo = "Reto del silencio incómodo",
        descripcion = "Aguanta este bloque sin celular en un espacio público, sin música ni nada.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 35),
        categoria = "Diplomáticos",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.DEJAR_CELULAR,
        completada = false
    ),
    Mision(
        id = 1036,
        titulo = "Secuencia lógica",
        descripcion = "Memoriza el patrón antes de que se agote el tiempo y repítelo con precisión.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 25),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(
            numSecuencias = 2,
            secuencias = listOf(
                Secuencia(secuencia = listOf("1", "4", "2", "3"), numeroTambores = 4, tiempoPorSecuencia = 5),
                Secuencia(secuencia = listOf("3", "1", "4", "2", "4"), numeroTambores = 4, tiempoPorSecuencia = 6)
            )
        ),
        tipoMision = TipoMision.SECUENCIA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1037,
        titulo = "Memoriza el ring",
        descripcion = "Observa la escena detenidamente y recuerda la posición exacta de cada luchador.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 30),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(
            elementosEscena = listOf(
                ElementoEnEscena(idElemento = 1, imagen = "Chascarrín", posicionX = 0.15, posicionY = 0.25),
                ElementoEnEscena(idElemento = 2, imagen = "El Chamán", posicionX = 0.80, posicionY = 0.25),
                ElementoEnEscena(idElemento = 3, imagen = "TitoRito", posicionX = 0.50, posicionY = 0.70)
            ),
            tiempoVisualizacionSegundos = 3
        ),
        tipoMision = TipoMision.ESCENA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1038,
        titulo = "Al ritmo de la máquina",
        descripcion = "Reto de memoria ágil: memoriza la secuencia y replica el patrón exacto.",
        recompensa = RecompensaMision(puntosPoder = 25, monedas = 30),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(
            numSecuencias = 2,
            secuencias = listOf(
                Secuencia(secuencia = listOf("1", "3", "2", "4", "1"), numeroTambores = 4, tiempoPorSecuencia = 5),
                Secuencia(secuencia = listOf("4", "2", "3", "1", "4", "2"), numeroTambores = 4, tiempoPorSecuencia = 6)
            )
        ),
        tipoMision = TipoMision.SECUENCIA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1039,
        titulo = "Inventario visual",
        descripcion = "Comprueba tu memoria: ubica la posición exacta de las figuras en la escena antes de que cambien de turno.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 25),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(
            elementosEscena = listOf(
                ElementoEnEscena(idElemento = 1, imagen = "Chascarrín", posicionX = 0.20, posicionY = 0.30),
                ElementoEnEscena(idElemento = 2, imagen = "Katita", posicionX = 0.80, posicionY = 0.70)
            ),
            tiempoVisualizacionSegundos = 6
        ),
        tipoMision = TipoMision.ESCENA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1040,
        titulo = "El último jaripeo",
        descripcion = "No te me pierdas güerx, memoriza bien el patrón o no te dan propina.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 25),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(
            numSecuencias = 2,
            secuencias = listOf(
                Secuencia(secuencia = listOf("4", "1", "2", "3", "4"), numeroTambores = 4, tiempoPorSecuencia = 4),
                Secuencia(secuencia = listOf("2", "3", "1", "4", "2"), numeroTambores = 4, tiempoPorSecuencia = 4)
            )
        ),
        tipoMision = TipoMision.SECUENCIA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1041,
        titulo = "Al ritmo de la cumbia",
        descripcion = "A que no puedes seguirme el ritmo.",
        recompensa = RecompensaMision(puntosPoder = 25, monedas = 30),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(
            numSecuencias = 3,
            secuencias = listOf(
                Secuencia(secuencia = listOf("1", "3", "4", "2"), numeroTambores = 4, tiempoPorSecuencia = 5),
                Secuencia(secuencia = listOf("2", "4", "1", "3", "2"), numeroTambores = 4, tiempoPorSecuencia = 6),
                Secuencia(secuencia = listOf("3", "1", "2", "4", "3", "1"), numeroTambores = 4, tiempoPorSecuencia = 7)
            )
        ),
        tipoMision = TipoMision.SECUENCIA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1042,
        titulo = "Escena nueva, cerebro nuevo",
        descripcion = "Más elementos, menos tiempo. Ponle todo el ojo antes de que la escena se te desaparezca.",
        recompensa = RecompensaMision(puntosPoder = 20, monedas = 25),
        categoria = "Exploradores",
        configuracion = ConfiguracionMision(
            elementosEscena = listOf(
                ElementoEnEscena(idElemento = 1, imagen = "El Chamán", posicionX = 0.20, posicionY = 0.20),
                ElementoEnEscena(idElemento = 2, imagen = "TitoRito", posicionX = 0.80, posicionY = 0.20),
                ElementoEnEscena(idElemento = 3, imagen = "Katita", posicionX = 0.50, posicionY = 0.80)
            ),
            tiempoVisualizacionSegundos = 3
        ),
        tipoMision = TipoMision.ESCENA,
        division = DivisionMision.JUEGOS_MENTALES,
        completada = false
    ),
    Mision(
        id = 1043,
        titulo = "Ese desastre no se va a acomodar solo",
        descripcion = "Tómale foto a algo negro que traigas tirado por ahí y que sepas que deberías guardar.",
        recompensa = RecompensaMision(puntosPoder = 12, monedas = 20),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(colorObjetivo = "Negro"),
        tipoMision = TipoMision.TOMAR_FOTO,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
    Mision(
        id = 1044,
        titulo = "El campeón del ring",
        descripcion = "Aparta el celular y dedica este bloque de tiempo exclusivamente a avanzar en un proyecto personal.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 35),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(duracionSegundos = 600),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
    Mision(
        id = 1045,
        titulo = "Combo de guamazos",
        descripcion = "Completa un bloque corto de tareas pendientes, tocando la pantalla cada vez que taches una.",
        recompensa = RecompensaMision(puntosPoder = 10, monedas = 20),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(cantidadObjetivo = 5),
        tipoMision = TipoMision.ACCIONES,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
    Mision(
        id = 1046,
        titulo = "¡A tu esquina!",
        descripcion = "Ordena un rincón de tu espacio y fotografía algo azul que hayas acomodado, como prueba de tu progreso.",
        recompensa = RecompensaMision(puntosPoder = 10, monedas = 15),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(colorObjetivo = "Azul"),
        tipoMision = TipoMision.TOMAR_FOTO,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
    Mision(
        id = 1047,
        titulo = "¡Aprende una llave nueva!",
        descripcion = "Dedicale un tiempo a aprender algo nuevo, un idioma, por ejemplo.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 30),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 1200),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
    Mision(
        id = 1048,
        titulo = "Un movimiento nuevo en tu repertorio",
        descripcion = "Aprende un truco, palabra o dato nuevo hoy. Cada campeón agrega movimientos a su arsenal, no se queda con los mismos de siempre.",
        recompensa = RecompensaMision(puntosPoder = 15, monedas = 20),
        categoria = "Analíticos",
        configuracion = ConfiguracionMision(cantidadObjetivo = 1),
        tipoMision = TipoMision.ACCIONES,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
    Mision(
        id = 1049,
        titulo = "Entrena una nueva llave",
        descripcion = "Dedica este bloque a practicar esa habilidad que llevas tiempo posponiendo. La práctica hace al maestro.",
        recompensa = RecompensaMision(puntosPoder = 18, monedas = 30),
        categoria = "Centinelas",
        configuracion = ConfiguracionMision(duracionSegundos = 900),
        tipoMision = TipoMision.TIEMPO,
        division = DivisionMision.PROGRESO_PERSONAL,
        completada = false
    ),
)
