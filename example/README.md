# capacitor-camera-crop — Example App

A **normal Capacitor app** (Vite + vanilla JS) that consumes the plugin exactly
the way real users will:

```js
import { CapacitorCameraCrop } from 'capacitor-camera-crop';

const result = await CapacitorCameraCrop.captureAndCrop({ source: 'camera', enableCropping: true });
```

Use it to verify plugin changes on a device or simulator/emulator. It targets
**Capacitor 8** (iOS uses Swift Package Manager). See
[Testing against Capacitor 7](#testing-against-capacitor-7) to switch.

The native `ios/` and `android/` projects are **committed** and already configured
(iOS Info.plist usage strings; Android FileProvider; JitPack repo). You do **not**
run `npx cap add` — the platforms already exist.

## Prerequisites

- Node 22+ and npm
- **Bun** — the plugin's own build (`bun run build`, run from the repo root) uses it
- iOS: Xcode 26+ (Capacitor 8 uses Swift Package Manager — no CocoaPods needed)
- Android: Android Studio (Otter or newer) + an emulator or device, JDK 21

## Setup & run

From this `example/` directory:

```bash
# 1. Build the plugin in the repo root so the example picks up fresh dist/
cd .. && bun run build && cd example

# 2. Install deps (links the plugin via file:.. and installs Vite)
npm install

# 3. Build + sync + launch (each script runs `vite build` first)
npm run run:ios       # = vite build + cap sync ios + cap run ios
npm run run:android   # = vite build + cap sync android + cap run android
```

On iOS, the first build resolves Swift packages (capacitor-swift-pm +
TOCropViewController) — let Xcode finish that once, and pick your signing Team
under Signing & Capabilities (the committed project intentionally ships with no
Team set).

> **After editing plugin source:** `cd .. && bun run build && cd example && npm run run:ios` (or `run:android`).

## Permissions (already configured — for reference)

These are **already set** in the committed native projects; listed here so you
know what a consumer app needs.

- **iOS** — `ios/App/App/Info.plist` contains `NSCameraUsageDescription`,
  `NSPhotoLibraryUsageDescription`, and `NSPhotoLibraryAddUsageDescription`.
- **Android** — **no** `CAMERA` or `READ_MEDIA_IMAGES` permission is declared, on
  purpose: the plugin uses delegated intents (`ACTION_IMAGE_CAPTURE`/`ACTION_PICK`)
  that need none, and declaring `CAMERA` without a runtime grant would *block*
  capture. The FileProvider (in `AndroidManifest.xml` + `res/xml/file_paths.xml`)
  is the only Android requirement.

## What to test (acceptance matrix)

Exercise each combination on **both** platforms:

- **source:** `camera`, `gallery`
- **enableCropping:** on, off
- **aspectRatio:** `free`, `1:1`, `4:3`, `16:9`, custom `{x,y}`
- **resultType:** `uri`, `base64`
- Optionally `nativeCropping` on/off, and `width`/`height` resize caps

`camera` requires a physical device; gallery + crop work on simulator/emulator.

## Testing against Capacitor 7

The plugin supports `^7.0.0 || ^8.0.0`. To verify the Cap 7 path (don't commit any
of this — the committed native projects are Capacitor 8):

```bash
npm install @capacitor/core@^7 @capacitor/cli@^7 @capacitor/ios@^7 @capacitor/android@^7
rm -rf ios android          # discard the committed Cap 8 projects locally
npm run build
npx cap add ios && npx cap add android && npx cap sync
# re-apply the iOS Info.plist usage strings, then: npm run run:ios / run:android
```

Restore the committed Cap 8 projects afterward with `git checkout -- ios android`.
`cap sync` automatically adjusts the plugin's `Package.swift` capacitor-swift-pm
version to match Capacitor 7.
