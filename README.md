# capacitor-camera-crop

A Capacitor plugin providing native camera and crop support for iOS and Android.

## Features

- ✅ Open native camera or gallery
- ✅ Native image cropping with customizable aspect ratios
- ✅ Returns file URI or base64 encoded string
- ✅ Image resizing support
- ✅ TypeScript support
- ✅ iOS (Swift) and Android (Kotlin) implementations

## Installation

```bash
npm install capacitor-camera-crop
# or
bun install capacitor-camera-crop
```

Then sync your Capacitor project:

```bash
npx cap sync
```

## Requirements

- Capacitor 7 or 8
- iOS 14.0+
- Android API 23+ (Android 6.0+)

## iOS Setup

Add the following keys to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to take pictures.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>We need access to your photo library.</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>We need to save cropped photos to your library.</string>
```

## Android Setup

The plugin automatically requests the necessary permissions. Make sure your `AndroidManifest.xml` includes:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

You also need to add a FileProvider to your app's `AndroidManifest.xml`:

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

## Usage

```typescript
import { CapacitorCameraCrop } from 'capacitor-camera-crop';

// Open camera with cropping
async function takePicture() {
  try {
    const result = await CapacitorCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: '1:1',
      resultType: 'uri',
      quality: 90,
    });

    console.log('Image URI:', result.value);
    console.log('Dimensions:', result.width, 'x', result.height);
  } catch (error) {
    console.error('Error:', error);
  }
}

// Open gallery without cropping
async function selectImage() {
  try {
    const result = await CapacitorCameraCrop.captureAndCrop({
      source: 'gallery',
      enableCropping: false,
      resultType: 'uri',
    });

    console.log('Image URI:', result.value);
  } catch (error) {
    console.error('Error:', error);
  }
}

// Get base64 encoded image with custom aspect ratio
async function captureBase64() {
  try {
    const result = await CapacitorCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      aspectRatio: { x: 16, y: 9 },
      resultType: 'base64',
      width: 1920,
      height: 1080,
      quality: 85,
    });

    console.log('Base64 image:', result.value);
  } catch (error) {
    console.error('Error:', error);
  }
}

// Use native crop controller (TOCropViewController on iOS, UCrop on Android)
async function captureWithNativeCropper() {
  try {
    const result = await CapacitorCameraCrop.captureAndCrop({
      source: 'camera',
      enableCropping: true,
      nativeCropping: true, // Uses TOCropViewController on iOS, UCrop on Android
      aspectRatio: '1:1',
      resultType: 'uri',
      quality: 90,
    });

    console.log('Cropped image:', result.value);
  } catch (error) {
    console.error('Error:', error);
  }
}
```

## API

### `captureAndCrop(options?: CaptureAndCropOptions): Promise<CaptureAndCropResult>`

Opens the camera or gallery, optionally crops the image, and returns the result.

#### CaptureAndCropOptions

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `source` | `'camera' \| 'gallery'` | `'camera'` | Source to pick the image from |
| `enableCropping` | `boolean` | `false` | Enable cropping after capturing/selecting |
| `aspectRatio` | `'free' \| '1:1' \| '4:3' \| '16:9' \| { x: number; y: number }` | `'free'` | Aspect ratio for cropping |
| `resultType` | `'uri' \| 'base64'` | `'uri'` | Result type: file URI or base64 encoded string |
| `width` | `number` | - | Maximum width for the output image |
| `height` | `number` | - | Maximum height for the output image |
| `quality` | `number` | `90` | JPEG quality (0-100) |
| `useSystemEditingIfAvailable` | `boolean` | `true` | Use iOS system editing UI if available (ignored when `nativeCropping=true`) |
| `nativeCropping` | `boolean` | `false` | Use native crop controller (TOCropViewController on iOS, locked aspect UCrop on Android). Overrides `useSystemEditingIfAvailable` on iOS |

#### CaptureAndCropResult

| Property | Type | Description |
|----------|------|-------------|
| `value` | `string` | The file URI or base64 encoded string |
| `mimeType` | `string` | MIME type of the returned image |
| `width` | `number` | Width of the image in pixels |
| `height` | `number` | Height of the image in pixels |

## Development

### Building

```bash
bun install
bun run build
```

### Example app

A runnable test harness lives in [`example/`](./example). It installs the
plugin from the repo root and exercises `captureAndCrop` across every option
on iOS and Android. See [`example/README.md`](./example/README.md) for setup
and the acceptance-test matrix.

## License

MIT

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

