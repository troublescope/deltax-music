# DeltaX Music (Android-Only Edition)

> A YouTube Music client (`app.deltax.music`) targeting Android Smartphones and Android TV.

## Problem
Provide a fast, native, ad-free YouTube Music client tailored exclusively for Android devices—both handheld touchscreens (smartphones/tablets) and 10-foot living-room displays (Android TV)—without maintaining Desktop or iOS targets.

## Target Users & Form Factors

| Form Factor | Input & UI Mode | Key Capabilities |
|---|---|---|
| Android Smartphone | Touch UI, portrait/landscape, Material 3, floating/bottom navigation, PiP | Full playback, offline caching, lyrics, Listen Together, Android Auto, widgets |
| Android TV | D-pad / remote control (`LEANBACK_LAUNCHER`), `LocalIsTv`, focus outlines, navigation rail | 10-foot TV navigation, D-pad focus management, TV banner, remote-friendly player & menus |

## Core Features

| Feature | Status | Description |
|---|---|---|
| Android Smartphone UI | implemented | Material 3 touch interface in `androidApp` and `shared` |
| Android TV Support | implemented | Leanback manifest config, `isAndroidTv()`, `LocalIsTv`, `TvFocus`, `TvNavigationFocus` |
| Android-Only Build | implemented | Remove Desktop (`desktopApp`, `jvm("desktop")`) and iOS (`iosArm64`, `iosSimulatorArm64`) build targets; `applicationId = "app.deltax.music"` |
