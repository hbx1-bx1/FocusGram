# FocusGram Android (Experimental MVP)

This is an experimental Android wrapper for Instagram DM-only usage.

## Status

- DM chat threads: working
- Login and 2FA: working
- Voice messages, images, stickers inside DMs: displayed
- Sending images, videos, and audio via the system file picker: working
- Recording voice notes: requires microphone permission
- Full-screen video playback: working
- Calls and in-app camera capture: not guaranteed / not part of the MVP
- Stories: blocked
- Reels: blocked
- Explore: blocked
- Posts: blocked
- Profile browsing: blocked
- Log out: available from the long-press menu

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
- The long-press settings entry can conflict with Instagram's text-selection menu. A native settings screen is planned.
- No external libraries used, only Android SDK.
- Minimum SDK: 24 (Android 7.0)
- No data collection, no analytics, no tracking
