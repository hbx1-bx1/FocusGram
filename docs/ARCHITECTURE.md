# Architecture

## Overview

FocusGram has two independent implementations sharing the same core logic:

- **Android**: Kotlin + AppCompat/Material 3 + WebView (AndroidX)
- **Desktop**: Electron + Chromium (Node.js + Electron)

Both use the same approach: load instagram.com in a web view and restrict navigation.

## Core concept: URL allowlist

Only these paths are allowed:

- `/direct`
- `/accounts`
- `/challenge`
- `/two_factor`
- `/re_login`

All other paths and external hosts are blocked and redirected to `/direct/inbox/`.

## Android native shell

- `MainActivity` hosts a native top brand bar, a 2dp page-progress strip, a SwipeRefreshLayout-wrapped WebView, a fullscreen container, and a native offline/retry view.
- The top bar shows the FocusGram lockup, compose, and settings. It is visible on `/direct/inbox` and auth routes and hidden on thread routes so Instagram's thread header is not duplicated.
- Compose triggers Instagram's own `a[href="/direct/new/"]` link. The guard also hides the Instagram inbox top row containing that link, plus the redundant "Messages" / "الرسائل" heading.
- `SettingsActivity` is a native screen with Account (logout), Appearance (theme + language), and About (privacy, source, version).
- Theme is persisted in `focusgram` SharedPreferences and applied through `AppCompatDelegate.setDefaultNightMode`.
- Language uses `AppCompatDelegate.setApplicationLocales` with `res/xml/locales_config.xml` and AppCompat's `AppLocalesMetadataHolderService`; the platform stores the per-app choice.
- Logout clears cookies, web storage, the active WebView's cache/history/form data/SSL preferences, then recreates `MainActivity` with a clean task.
- Swipe-to-refresh reloads the WebView. The page-progress strip keeps its 2dp height while hidden to avoid layout shifts.
- The native shell uses AppCompat, Material Components, Activity, and SwipeRefreshLayout. Android build settings: `minSdk 24`, `targetSdk 34`, `compileSdk 34`, version `1.2.0` (versionCode 4).

## Guard layers (both platforms)

1. **Navigation intercept**: `shouldOverrideUrlLoading` (Android) / `will-navigate` + `did-navigate-in-page` (Electron) blocks URL changes before they load
2. **History API override**: Injected JavaScript overrides `pushState`/`replaceState` to block SPA navigation
3. **Click capture**: Event listener in capture mode intercepts clicks on blocked links and aria-label elements
4. **DOM mutation observer**: Watches for newly added blocked elements and disables them
5. **Periodic check**: 1000ms on Android, 250ms on Desktop; re-checks pathname and re-disables blocked links
6. **Inbox chrome cleanup**: Hides the Instagram inbox top row (the hidden new-message link's row) and the redundant "Messages" / "الرسائل" heading while preserving thread headers

## Injected script (guard.js / buildInjectionScript)

The same JavaScript logic is used on both platforms:

- Allowlist matching
- History API override
- Click capture (href links + aria-label for reel/story/post/profile)
- Link disabling (aria-disabled, pointerEvents: none)
- Blocked media stopping (video/audio inside blocked links only, NOT chat media)
- CSS injection (hide sidebar suggestions, footer, desktop layout fixes)
- Inbox chrome cleanup (hide the old inbox top row and redundant heading; keep thread headers)
- MutationObserver with debounce
- Periodic check (250ms on Desktop, 1000ms on Android)

Android no longer removes all `img`, `picture`, `canvas`, `video`, or `audio` elements. Chat images, video attachments, voice notes, avatars, and CAPTCHA/2FA images are preserved. Only media inside blocked links (Reels, Stories, Posts) is stopped.

## Media pipeline (Android)

- `WebChromeClient.onShowFileChooser` opens the system file picker for images, videos, and audio.
- `WebChromeClient.onPermissionRequest` grants microphone/camera access only to `instagram.com` origins after the Android runtime permission is granted.
- `WebChromeClient.onShowCustomView` / `onHideCustomView` provide full-screen HTML5 video.
- Received media is rendered by Instagram Web inside the conversation; FocusGram does not proxy or store it.
- The system picker path was smoke-tested on a Samsung SM-A155F running Android 16. End-to-end sending, recording, and logout should still be verified in a real conversation before a public release.

## Brand assets

- Source SVGs live in `assets/brand/` (symbol, wordmark, light/dark lockups).
- Android vector drawables are generated from those SVGs and selected by locale and night qualifiers.
- The adaptive launcher icon uses light/dark foregrounds, a forest background, and a monochrome layer; legacy PNGs and exported previews are included.

## Privacy model

- No Instagram API usage
- No external servers
- No data collection
- No analytics
- No tracking
- No message reading
- No password storage
- Login is done exclusively on instagram.com inside the app
