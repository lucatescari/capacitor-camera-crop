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

## Prerequisites

- Node 22+ and npm
- iOS: Xcode 26+ (Capacitor 8 uses Swift Package Manager — no CocoaPods needed)
- Android: Android Studio (Otter or newer) + an emulator or device, JDK 21

## Setup

From this `example/` directory:

```bash
# 1. Build the plugin in the repo root so the example picks up fresh dist/
cd .. && bun run build && cd example

# 2. Install deps (links the plugin via file:.. and installs Vite)
npm install

# 3. Build the web app (Vite → dist/)
npm run build

# 4. Add the native platforms (generated, gitignored)
npx cap add ios
npx cap add android

# 5. Copy the web build + native config into the platforms
npx cap sync
```

## Native permissions (one-time, after `cap add`)

### iOS — `ios/App/App/Info.plist`

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to take pictures.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>We need access to your photo library.</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>We need to save cropped photos to your library.</string>
```

### Android — `android/app/src/main/AndroidManifest.xml`

Add above `<application>`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

Add inside `<application>`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Create `android/app/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="cache" path="." />
    <external-cache-path name="external_cache" path="." />
</paths>
```

## Run

`npm run run:ios` / `npm run run:android` rebuild the web app, sync, and launch:

```bash
npm run run:ios       # = vite build + cap sync ios + cap run ios
npm run run:android   # = vite build + cap sync android + cap run android
```

On iOS, the first build resolves Swift packages (capacitor-swift-pm +
TOCropViewController) — let Xcode finish that once.

> **After editing plugin source:** `cd .. && bun run build && cd example && npm run build && npx cap sync`.

## What to test (acceptance matrix)

Exercise each combination on **both** platforms:

- **source:** `camera`, `gallery`
- **enableCropping:** on, off
- **aspectRatio:** `free`, `1:1`, `4:3`, `16:9`, custom `{x,y}`
- **resultType:** `uri`, `base64`
- Optionally `nativeCropping` on/off, and `width`/`height` resize caps

The diagnostics line at the top shows the platform and whether the native plugin
registered. `camera` requires a physical device; gallery + crop work on
simulator/emulator.

## Testing against Capacitor 7

The plugin supports `^7.0.0 || ^8.0.0`. To verify the Cap 7 path (don't commit it):

```bash
npm install @capacitor/core@^7 @capacitor/cli@^7 @capacitor/ios@^7 @capacitor/android@^7
rm -rf ios android
npm run build
npx cap add ios && npx cap add android && npx cap sync
# re-apply the native permissions above, then: npm run run:ios / run:android
```

`cap sync` automatically adjusts the plugin's `Package.swift` capacitor-swift-pm
version to match Capacitor 7, so no manual plugin edits are needed.
