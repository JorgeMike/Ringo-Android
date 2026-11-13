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
│                       (Arvo + Pirata One) y componentes compartidos
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
- **Pantallas pendientes** usan `PantallaPendiente(titulo, tarea)` con el ID del tablero.
- **Paleta:** siempre clara, como en iOS (no hay versión oscura de los colores de marca).
  Para colores de marca usar `ColoresTF`; `MaterialTheme.colorScheme` solo para lo que
  Material ya cubre.
- **Deep links:** mismo formato que iOS (`ringo://id-mision/<id>`) para que el API mande
  un solo enlace a ambas plataformas.
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
