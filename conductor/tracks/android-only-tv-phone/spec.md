# Spec: Android-Only Build with TV & Smartphone Support

## Summary
Restrict `deltax-music` Gradle build targets to Android only (removing Desktop and iOS targets) while ensuring first-class support for both **Android Smartphone** (touch UI) and **Android TV** (Leanback / D-pad 10-foot UI).

## Requirements

### 1. Android-Only Build Configuration
- Set Android `applicationId` to `app.deltax.music` in `androidApp/build.gradle.kts`.
- Remove `:desktopApp` from `settings.gradle.kts`.
- Remove `jvm("desktop")` and `iosArm64()` / `iosSimulatorArm64()` targets from `shared/build.gradle.kts`.
- Remove desktop-only tasks/configurations (`desktopFfmpegNatives`, `desktopIcu4j`, `windowsArm64FfmpegNatives`, `desktopTest` task config, `verifyDesktopJarIntegrity`, `testServerWithoutReflection`, `kspDesktop`, `kspIosArm64`, `kspIosSimulatorArm64`, `desktopMain`, `desktopTest`, `iosMain`) from `shared/build.gradle.kts`.
- Preserve `commonMain`, `jvmSharedMain`, `androidMain`, `commonTest`, `jvmSharedTest`, `androidHostTest`, and `androidDeviceTest` so all Android code compiles without breaking shared/JVM imports.
- Update `Makefile` default `build` and `test` targets to build/test Android instead of Desktop/iOS.

### 2. Android Smartphone & Android TV Support
- Preserve `android.software.leanback` (`required="false"`) and `android.hardware.touchscreen` (`required="false"`) in `androidApp/src/main/AndroidManifest.xml` so APKs install on both phones/tablets and Android TV.
- Preserve `LEANBACK_LAUNCHER` intent filters and `android:banner="@mipmap/tv_banner"` in `AndroidManifest.xml`.
- Ensure `isAndroidTv()` and `LocalIsTv` form-factor detection and TV focus/navigation helpers (`TvFocus`, `TvNavigationFocus`) remain wired in `androidApp` and `shared`.

## Acceptance Criteria
1. `settings.gradle.kts` includes only `:shared`, `:listenTogetherProto`, `:opusDecoder`, and `:androidApp`.
2. `shared/build.gradle.kts` declares only the `android` Kotlin target (no `desktop` or `ios` targets).
3. `AndroidManifest.xml` supports both smartphone (`LAUNCHER`) and TV (`LEANBACK_LAUNCHER`, `leanback` optional, `touchscreen` optional).
4. Gradle configuration / Android tasks succeed (or any environment limitation like missing JDK 25 / Android SDK is clearly reported).
