# Architecture

## Overview

FocusGram has two independent implementations sharing the same core logic:

- **Android**: Kotlin + WebView (Android SDK only)
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

## Guard layers (both platforms)

1. **Navigation intercept**: `shouldOverrideUrlLoading` (Android) / `will-navigate` + `did-navigate-in-page` (Electron) blocks URL changes before they load
2. **History API override**: Injected JavaScript overrides `pushState`/`replaceState` to block SPA navigation
3. **Click capture**: Event listener in capture mode intercepts clicks on blocked links and aria-label elements
4. **DOM mutation observer**: Watches for newly added blocked elements and disables them
5. **Periodic check**: 250ms interval re-checks pathname and re-disables blocked links

## Injected script (guard.js / buildInjectionScript)

The same JavaScript logic is used on both platforms:

- Allowlist matching
- History API override
- Click capture (href links + aria-label for reel/story/post/profile)
- Link disabling (remove href, aria-disabled, pointerEvents: none)
- Blocked media stopping (video/audio in dialogs and blocked links only, NOT chat media)
- CSS injection (hide sidebar suggestions, footer, desktop layout fixes)
- MutationObserver
- Periodic check at 250ms

## Privacy model

- No Instagram API usage
- No external servers
- No data collection
- No analytics
- No tracking
- No message reading
- No password storage
- Login is done exclusively on instagram.com inside the app
