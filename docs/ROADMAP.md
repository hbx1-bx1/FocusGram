# Roadmap

## Status

FocusGram is an **MVP Preview**. Pre-built release files are available on GitHub Releases.

For a machine-readable project summary, see `llms.txt` in the project root.

## Phase 1 (MVP)

- [x] Project structure reorganization
- [x] Android MVP (DM only, URL allowlist, basic JS injection)
- [x] Android DM media preservation (images, video, audio)
- [x] Android media picker + microphone/camera permissions
- [x] Android native shell (brand bar, compose, settings, pull-to-refresh, offline/retry)
- [x] Android native settings (theme, language, privacy, source, version, logout)
- [x] Android Log out with confirmation and session clearing
- [x] Android release signing scaffold
- [x] Gradle wrapper jar restored
- [x] FocusGram brand system and adaptive launcher icon
- [x] Arabic/English UI, RTL, and language-aware logo lockups
- [x] Route-aware top bar and inbox chrome cleanup
- [x] Android image picker smoke test
- [x] Desktop MVP (Electron, DM allowlist, navigation guard, session clear)
- [x] Block Reels, Explore, Posts, Stories
- [x] Clear Session
- [x] Reload
- [x] Find in Page
- [x] Desktop icon for all platforms
- [x] Voice and video calls on Desktop
- [x] FocusGram Tools dialog (Cmd/Ctrl+S)

## Phase 2

- [ ] Android signed release APK / GitHub Release for 1.2.0
- [ ] End-to-end Android send, voice-note recording, and logout verification
- [ ] Android API 35 target upgrade
- [ ] Linux .deb packaging (valid build)
- [ ] Settings memory (persist window size)
- [ ] Blocked media previews refinement
- [ ] Better desktop layout on small screens
- [ ] F-Droid repository

## Phase 3

- [ ] Android stabilization
- [ ] Voice message reliability on Android
- [ ] Camera and microphone permission handling improvements
- [ ] Performance optimization for low-end devices
- [ ] Accessibility pass (TalkBack, dynamic type)
