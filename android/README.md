# FocusGram Android (Experimental MVP)

This is an experimental Android wrapper for Instagram DM-only usage.

## Status

- DM chat threads: working
- Login and 2FA: working
- Voice messages, images, stickers inside DMs: displayed
- Calls and camera: not guaranteed
- Stories: blocked
- Reels: blocked
- Explore: blocked
- Posts: blocked
- Profile browsing: blocked

## Open in Android Studio

Open the folder `android/FocusGramAndroid` directly in Android Studio.

## Known limitations

- The Android version uses a Desktop User-Agent for better DM interface, but some features (calls, camera, microphone, vanish mode media) may not work reliably
- No external libraries used, only Android SDK
- Minimum SDK: 24 (Android 7.0)
- No data collection, no analytics, no tracking
