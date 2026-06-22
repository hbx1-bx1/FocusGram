# Release

## Files to upload

- FocusGram-Windows-x64-1.0.0.exe
- FocusGram-macOS-arm64-1.0.0.dmg
- FocusGram-Linux-x64-1.0.0.AppImage
- FocusGram-Linux-arm64-1.0.0.AppImage
- Android APK when ready

## Files not to upload

- *.blockmap
- latest-linux.yml
- latest-linux-arm64.yml
- builder-debug.yml
- builder-effective-config.yaml
- linux-unpacked/
- linux-arm64-unpacked/
- mac-arm64/
- win-unpacked/
- win-arm64-unpacked/
- 96B .deb files

## Build commands

```bash
npm run build:mac
npm run build:win -- --x64
npm run build:linux -- --x64
npm run build:linux
cd android/FocusGramAndroid && ./gradlew assembleDebug
```

## Signing notes

- macOS unsigned warning
- Windows unsigned warning
- Android external APK warning
- Release APK needs a keystore
- Never commit keystore files
