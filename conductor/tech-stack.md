# Tech Stack

## Languages & Frameworks

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | KMP / Android | Primary application and shared logic |
| Jetpack Compose / Compose Multiplatform | Material 3 | Declarative UI for Android Smartphone & Android TV |
| AndroidX Media3 (ExoPlayer) | - | Audio playback, background service, and `opusDecoder` JNI |
| Room + DataStore | - | Local music library database and preferences |
| InnerTubeX | v0.13.1 | YouTube Music API client |

## Gradle Modules (Android-Only)

| Module | Purpose |
|---|---|
| `:androidApp` | Android application (`app.deltax.music`), Leanback/TV & Smartphone manifest, services, widgets, UI |
| `:shared` | Shared Kotlin & Compose logic (`app.deltax.music.shared`: `commonMain`, `jvmSharedMain`, `androidMain`) |
| `:listenTogetherProto` | Protobuf definitions for Listen Together |
| `:opusDecoder` | Vendored Media3/libopus JNI decoder (ARM ABIs) |

## Dev Tools

| Tool | Purpose | Config |
|---|---|---|
| Gradle | Build & dependency management | `build.gradle.kts`, `settings.gradle.kts` |
| ktlint | Kotlin formatting & lint | Root `build.gradle.kts` |
