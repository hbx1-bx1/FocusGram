# FocusGram Android

FocusGram Android is a messaging-only Android client built around Instagram Direct inside a restricted WebView, with a native FocusGram shell.

## Status

- DM chat threads: working
- Login and 2FA: working
- Received images, video, audio, and voice notes inside DMs: displayed
- Sending media through the system picker: picker opens; end-to-end send still needs a physical-device pass
- Recording voice notes: microphone permission path implemented; end-to-end recording still needs a physical-device pass
- Full-screen video playback: working
- Native shell: brand bar, compose, settings, pull-to-refresh, offline/retry state
- Native settings: theme (system/light/dark), language (system/English/Arabic), privacy, source code, version, logout
- Log out: confirmation dialog; clears cookies, web storage, the active WebView's cache, history, form data, and SSL preferences, then recreates the app
- Arabic/English UI with RTL support and language-aware logo lockups
- Reels, Stories, Explore, Posts, Profiles: blocked
- Calls and in-app camera capture: not guaranteed / not part of the MVP

Current version: **1.2.0** (versionCode 4).

## UI and branding

The native shell uses AppCompat and Material 3. The FocusGram lockups and symbol come from the SVGs in `assets/brand/`; the Android app uses language-qualified drawables so the English lockup is wordmark-left/symbol-right and the Arabic lockup is symbol-left/wordmark-right. The launcher icon is adaptive, with light/dark foregrounds and a monochrome layer.

The native top bar appears on the inbox and auth screens. It hides inside conversation threads so Instagram's own thread header is the only header. The compose action triggers Instagram's hidden new-message link; Settings opens the native screen.

## Build from the command line

Android Studio is **not required** to build or install this project. The repository includes the Gradle wrapper, and the Android SDK command-line tools are enough.

Requirements:

- JDK 17 or newer (the current build was tested with JDK 21)
- Android SDK Platform 34 and Build Tools 34
- `adb` with a connected device (USB debugging enabled)

```bash
cd android/FocusGramAndroid
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must point to your Android SDK:

```properties
sdk.dir=/path/to/Android/Sdk
```

This file is machine-specific and is not committed.

## Release signing

Create a keystore, then copy `keystore.properties.example` to `keystore.properties` and fill in the values:

```bash
keytool -genkeypair -v \
  -keystore focusgram-release.jks \
  -alias focusgram \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=focusgram-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=focusgram
keyPassword=YOUR_KEY_PASSWORD
```

Then build a signed release:

```bash
./gradlew assembleRelease
```

Never commit `keystore.properties` or the `.jks` file.

## Open in Android Studio (optional)

Open the folder `android/FocusGramAndroid` directly in Android Studio. Android Studio is useful for layout previews, Logcat, and signing, but it is not required for command-line builds.

## Known limitations

- The Android version uses the WebView's default User-Agent. Some Instagram flows may behave differently inside a WebView.
- Calls, in-app camera capture, and vanish-mode media may not work reliably.
- Media playback may require a tap; autoplay is intentionally not forced.
- Sending media, recording voice notes, and logging out should be verified end-to-end on a physical device before a public release.
- The current target is Android 14 (API 34); an API 35 target upgrade is planned.
- AndroidX AppCompat, Material Components, Activity, and SwipeRefreshLayout are used.
- Minimum SDK: 24 (Android 7.0)
- No data collection, no analytics, no tracking
