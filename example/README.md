# capacitor-camera-crop — Example / Test Harness

A minimal, runnable Capacitor app that installs the plugin from the parent
directory (`file:..`) and exercises `captureAndCrop` across every option
(source, cropping mode, aspect ratio, output type). Use it to verify plugin
changes on a real device or emulator/simulator.

Defaults to **Capacitor 8**. See [Testing against Capacitor 7](#testing-against-capacitor-7) to switch.

## Prerequisites

- Node 22+ and npm
- iOS: Xcode 26+, CocoaPods (`sudo gem install cocoapods` or `brew install cocoapods`)
- Android: Android Studio (Otter or newer) + an emulator or a connected device, JDK 21

## Setup

From this `example/` directory:

```bash
# 1. Build the plugin so example picks up fresh dist/ (run in the repo root)
cd .. && bun run build && cd example

# 2. Install deps (links the plugin via file:..)
npm install

# 3. Add the native platforms (generates ios/ and android/, which are gitignored)
npx cap add ios
npx cap add android

# 4. Copy web assets + native config into the platforms
npx cap sync
```

> After changing plugin source, re-run `cd .. && bun run build && cd example && npm install && npx cap sync` to pull the update into the platforms.

## Native permissions (one-time, after `cap add`)

The generated platform projects need the plugin's permissions wired up.

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

Add the permissions above the `<application>` tag:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

Add the FileProvider inside `<application>`:

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

```bash
npx cap run ios        # pick a simulator or device
npx cap run android    # pick an emulator or device
```

Then in the app: choose options and tap **Capture & Crop**. The result panel
shows the returned dimensions, mime type, and a preview.

## What to test (acceptance matrix)

Exercise each combination on **both** platforms:

- **source:** `camera`, `gallery`
- **enableCropping:** on, off
- **aspectRatio:** `free`, `1:1`, `4:3`, `16:9`, custom `{x,y}`
- **resultType:** `uri`, `base64`
- Optionally `nativeCropping` on/off (UCrop / TOCropViewController), and `width`/`height` resize caps

> The camera source requires a physical device (simulators/emulators have no
> real camera). Gallery + crop paths work on simulator/emulator.

## Diagnostics & troubleshooting

The page shows a diagnostics line at the top:

```
platform: ios
native CapacitorCameraCrop registered: ✅ yes
all native plugins: CapacitorCookies, CapacitorHttp, WebView, CapacitorCameraCrop
```

- **`native CapacitorCameraCrop registered: ✅ yes`** → the native plugin is wired
  up correctly; any failure is in the call itself.
- **`❌ NO`** → the native plugin did not register with the bridge. Re-run
  `cd .. && bun run build && cd example && npm install && npx cap sync`, and for
  iOS make sure `pod install` ran (it happens inside `cap sync`). Do a clean
  build in Xcode/Android Studio if needed.

> **Note on access pattern:** this build-free harness calls
> `window.Capacitor.registerPlugin('CapacitorCameraCrop')` to reach the plugin.
> `window.Capacitor.Plugins.CapacitorCameraCrop` is **not** populated in a
> no-bundler app (that object is only filled when a bundled app `import`s the
> plugin package), so don't rely on it here.

## Testing against Capacitor 7

The plugin supports `^7.0.0 || ^8.0.0`. To verify the Cap 7 path, temporarily
downgrade this example (do not commit the change):

```bash
npm install @capacitor/core@^7 @capacitor/cli@^7 @capacitor/ios@^7 @capacitor/android@^7
# remove and re-add platforms so the native templates match Cap 7
rm -rf ios android
npx cap add ios && npx cap add android && npx cap sync
# re-apply the native permissions above, then run
```

To return to Cap 8: `npm install @capacitor/core@^8 @capacitor/cli@^8 @capacitor/ios@^8 @capacitor/android@^8` and regenerate platforms.
