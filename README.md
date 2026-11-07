# YouTube — Jetpack Compose + Liquid Glass

Standalone Android app. UI built from Kyant0's liquid glass:
- `io.github.kyant0:backdrop` from Maven Central
- LiquidButton / LiquidBottomTabs / LiquidToggle / LiquidSlider copied unchanged from
  https://github.com/Kyant0/AndroidLiquidGlass into `app/.../liquidglass/`
- Big surfaces (sheets) use the same `drawBackdrop` effects (`Modifier.panel`)

## Features
Home feed + category chips, search (suggestions, history, videos/channels/playlists), Shorts (vertical, autoplay),
watch page (quality, speed, captions, chapters, comments, related, double-tap seek, fullscreen, PiP, mini-player,
autoplay next), channel pages, subscriptions feed, library (history, watch later, liked, downloads, playlists),
settings, Google sign-in (microG compatible) with subscription import.

## Google login
Cloud Console -> enable YouTube Data API v3 -> OAuth consent (scope youtube.readonly) -> Android OAuth client
(package com.youtube.app + your SHA-1). Optional web client id in `res/values/strings.xml`.

## Build
Android Studio or `./gradlew :app:assembleDebug`. Android 13+ for the full refraction shader.
