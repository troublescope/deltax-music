# DeltaX Music

A YouTube Music client (`app.deltax.music`) built for **Android Smartphones** and **Android TV**.

## Targets
- **Android Smartphone**: Touch UI with Material 3, background playback, offline caching, lyrics, Listen Together, Android Auto, and home screen widgets.
- **Android TV**: 10-foot Leanback launcher support (`LEANBACK_LAUNCHER`), D-pad focus management, navigation rail, and TV banner.

## Build & Test

```bash
# Lint checks
./gradlew :shared:ktlintCheck :androidApp:ktlintCheck

# Unit tests
./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest

# Assemble APKs
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleRelease
```

## License
Licensed under GPL-3.0. See [LICENSE](LICENSE).
