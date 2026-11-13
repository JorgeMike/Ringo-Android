# Tiempo Fuera · Android

App Android de **Tiempo Fuera** (antes "Ringo"): bienestar digital con identidad de
lucha libre mexicana. Es la versión Android de la app iOS (`../TiempoFuera-iOS`, la
mantiene Jacobo). Este repo lo mantenemos nosotros.

- **Repo:** `JorgeMike/Ringo-Android`. El repo y algunos identificadores dicen "Ringo";
  hacia dentro (nombre de la app, textos, paquete) es **Tiempo Fuera**.
- **Plan:** el tablero de migración [`../ANDROID-TABLERO.md`](../ANDROID-TABLERO.md) desglosa
  la app iOS en módulos y tareas (M0…M14). Los comentarios del código citan esos IDs
  (p. ej. "M5.1") donde algo está pendiente.
- **Bloqueo de apps (M3):** lo que aprendimos y lo que **no** funciona está en
  [`../spike-bloqueo-android/README.md`](../spike-bloqueo-android/README.md) y en
  [`../M3.14-POLITICA-PLAY.md`](../M3.14-POLITICA-PLAY.md). Leerlos antes de tocar M3.

## Stack

Kotlin · Jetpack Compose (Material 3) · Hilt (KSP) · Navigation Compose con rutas
tipadas (kotlinx.serialization). Un solo módulo `:app`. Versiones en
`gradle/libs.versions.toml`.

- `applicationId` = `com.rudoestudio.tiempofuera`: es el ID permanente en Google Play,
  **no cambiarlo**.
- `minSdk 29` (Android 10), porque M3 necesita los eventos `ACTIVITY_RESUMED`/`PAUSED`
  de UsageStats. `compileSdk`/`targetSdk 35`: falta subirlo al que exija Play al
  publicar.

## Estructura (espejo de iOS)

```
app/src/main/java/com/rudoestudio/tiempofuera/
├── core/designsystem   Tema, colores (ColoresTF = colorsets *-RG de iOS), tipografía
│                       (Arvo + Pirata One)
│   └── componentes     Componentes de `Features/Commons/UIComponents` de iOS
│                       (BotonL, BotonS, Boton3D, TextoContorno, Titulo, barras…)
├── core/logging        AppLogger (= AppLogger.shared de iOS)
├── di                  Módulos de Hilt (= AppContainer de iOS)
├── domain/<área>       Entidades, protocolos (interfaces) y casos de uso. Kotlin puro
├── data/<área>         Implementaciones: API, Room, DataStore
├── navigation          RootScreen (onboarding vs tabs), MainScaffold (4 tabs), rutas
│                       y deep links
└── features/<área>     Pantallas (Composable) + ViewModels, una carpeta por feature
                        como `Features/` en iOS
```

## Convenciones

- **Nombres en español**, como en iOS: `EstadoOnboardingRepository`, `alTerminar`,
  `ColoresTF.Amarillo`. Términos técnicos (ViewModel, Repository, Screen) se quedan
  en inglés.
- **Al portar algo de iOS**, mencionar en el KDoc el nombre del archivo o tipo de iOS
  (p. ej. "en iOS: `MainTabView`"). Así es fácil comparar las dos versiones.
- **Catálogo del juego** (luchadores, misiones, ligas): vive en código, en
  `data/catalogo`, y **no se edita a mano**. Se genera desde las `SeedData` de iOS con
  `python3 scripts/importar_semillas_ios.py ../TiempoFuera-iOS/Ringo-iOS/Data/SeedData`;
  `CatalogoTest` revisa que la conversión salió bien. La base (Room) solo guarda el
  progreso del usuario encima del catálogo.
- **Room:** cada cambio de esquema sube `version` en `BaseDatosTiempoFuera` y lleva su
  migración; el esquema de cada versión se versiona en `app/schemas`. Lo que en iOS va en
  UserDefaults va en DataStore como JSON (`AlmacenJson`); los nombres de
  `data/local/Guardados.kt` y las claves no se cambian (romperían lo ya guardado).
- **Pantallas pendientes** usan `PantallaPendiente(titulo, tarea)` con el ID del tablero.
- **Componentes:** antes de crear uno nuevo, revisar `core/designsystem/componentes` y la
  **galería** (Inicio → "Galería de componentes", `features/galeria`). Un componente nuevo
  se agrega también a la galería. La galería es de desarrollo: quitar el acceso antes de
  publicar.
- **Formas de iOS** que vienen de SVG (`buildPath(points:)`) se portan con `FormaPoligono`
  y los mismos puntos y lienzo, para que sean idénticas.
- **Assets:** no se editan a mano. Vienen de iOS con
  `python3 scripts/importar_assets_ios.py` (requiere Inkscape), que regenera
  `res/drawable-nodpi` (PNG), `res/drawable-xxxhdpi` (SVG rasterizados a 4x) y `res/raw`
  (WAV). Para encontrar el equivalente de `Image("Nombre")` de iOS, ver
  [`docs/ASSETS.md`](docs/ASSETS.md). El script solo borra y reescribe lo que generó
  (`scripts/assets_importados.txt`); aun así, un asset que sea solo de Android va en
  `drawable/` para no mezclarlo ni chocar de nombre con los de iOS.
- **Paleta:** siempre clara, como en iOS (no hay versión oscura de los colores de marca).
  Para colores de marca usar `ColoresTF`; `MaterialTheme.colorScheme` solo para lo que
  Material ya cubre.
- **Deep links:** mismo formato que iOS (`ringo://id-mision/<id>`) para que el API mande
  un solo enlace a ambas plataformas.
- **API (`../tiempo-fuera-api`):** el contrato está en su `openapi.json`. La URL va en
  `BuildConfig.API_URL`; hoy las dos compilaciones apuntan a **staging** porque
  producción no está desplegada. El access token se renueva solo (`data/red/RenovacionSesion.kt`):
  ninguna pantalla ni repositorio maneja tokens. El logging HTTP es `BASIC` y solo en
  debug: nunca subirlo a `HEADERS`/`BODY` (escribiría tokens en logcat).
- **Sincronización:** `SincronizacionWorker` (WorkManager, `data/sincronizacion`) sube el
  progreso cada 12 h y con `ProgramadorSincronizacion.sincronizarAhora()`. Sobrescribe la
  nube con lo local: por eso solo corre con sesión y onboarding terminado. Los workers
  usan `@HiltWorker`; WorkManager se inicia en `TiempoFueraApp` (el manifest apaga su
  arranque automático).
- **Previews:** usar `core/preview/DatosPreview` (sale del catálogo real), no inventar
  luchadores o misiones a mano.
- **Errores del API:** cada repositorio traduce los códigos de *su* endpoint a errores del
  dominio (ver `AuthRepositoryImpl`); el mensaje del API (`{"error": …}`) se respeta.
- **Laboratorio de API** (Inicio → "Laboratorio de API", `features/laboratorio`):
  herramienta de desarrollo para probar contra staging. Quitar el acceso antes de publicar.
- **Tono de los textos:** el del estudio (directo, chilango, con humor; ver el
  `CLAUDE.md` de `rudo-studio/`).

## Comandos

Las herramientas están en el home, sin sudo: JDK 21 en `~/.local/jdk/`, SDK en
`~/Android/Sdk/` (`local.properties` apunta ahí y no se sube).

```bash
export JAVA_HOME=$(ls -d ~/.local/jdk/jdk-21*)
./gradlew assembleDebug           # compilar
./gradlew testDebugUnitTest       # tests de JVM
./gradlew installDebug            # instalar en el teléfono conectado
~/Android/Sdk/platform-tools/adb logcat -s TiempoFuera
```

Teléfono de prueba: Samsung Galaxy S24+ (Android 17). En Samsung hay que apagar el
*Bloqueador automático* para que funcione la depuración USB.

## Git

- Identidad **local** de este repo: `Jorge <alvaradojr2001@gmail.com>`. Nunca configurar
  identidad global.
- Rama principal: `main`.
