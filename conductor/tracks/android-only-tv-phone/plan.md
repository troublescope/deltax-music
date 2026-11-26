# Plan: Android-Only Build with TV & Smartphone Support

## Phase 1: Remove Desktop & iOS Targets
- [x] Inspect `settings.gradle.kts`, `shared/build.gradle.kts`, `Makefile`, and `AndroidManifest.xml`.
- [x] Update `settings.gradle.kts` to remove `include(":desktopApp")`.
- [x] Update `shared/build.gradle.kts` to remove `desktop` and `ios` targets, source sets, KSP configurations, and desktop-only tasks.
- [x] Update `Makefile` so `build` and `test` target Android.
- [x] Update `androidApp/build.gradle.kts` `applicationId` to `app.deltax.music`.

## Phase 2: Verify Android TV & Smartphone Support
- [x] Confirm `AndroidManifest.xml` has `leanback` (`required="false"`), `touchscreen` (`required="false"`), `tv_banner`, and `LEANBACK_LAUNCHER`.
- [x] Confirm `isAndroidTv()` and `LocalIsTv` are properly provided in `MainActivity.kt` / `DeltaX MusicApp.kt`.

## Phase 3: Verification
- [x] Attempt Gradle verification in Termux environment (document any Termux/Android host limitation if Gradle native services are unavailable).
- [x] Update `conductor/tracks.md` and `conductor/product.md` status upon completion.
