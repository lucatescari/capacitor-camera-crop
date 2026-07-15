# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-07-15

### Changed

- **BREAKING:** now requires Capacitor 7 or 8 (`@capacitor/core` `^7.0.0 || ^8.0.0`). Dropped Capacitor 6 support.
- Android: `minSdk` 22 → 23, `compileSdk`/`targetSdk` → 36, AGP 8.7.2, Kotlin 1.9.25, JDK 21.
- Android: renamed plugin namespace `com.example.capacitorcameracrop` → `dev.tescari.capacitorcameracrop`.
- iOS: raised deployment target 13.0 → 14.0, Swift 5.1 → 5.9.

### Notes

- Public API (`captureAndCrop`) is unchanged; no consumer code changes required beyond upgrading Capacitor.
- uCrop (Android) and CropViewController (iOS) retained.

## [0.1.0] - 2025-11-12

### Added

- Initial release of capacitor-camera-crop plugin
- Native camera capture for iOS and Android
- Native gallery picker for iOS and Android
- Image cropping with uCrop (Android) and native editing (iOS)
- Multiple aspect ratio options: free, 1:1, 4:3, 16:9, custom
- Support for URI and base64 result types
- Image resizing with max width/height constraints
- JPEG quality control
- TypeScript definitions and type safety
- Comprehensive documentation and examples
- iOS implementation using UIImagePickerController and PHPickerViewController
- Android implementation using MediaStore and uCrop library
- Permission handling for camera and photo library access
- Error handling and user cancellation support

### Features

- ✅ Open native camera or gallery
- ✅ Native image cropping
- ✅ Custom aspect ratios
- ✅ Image resizing
- ✅ Base64 and URI output
- ✅ iOS 13+ support
- ✅ Android API 22+ support
- ✅ Capacitor 6 compatible
- ✅ TypeScript support
- ✅ Bun package manager support

### Platform Support

- iOS: 13.0+
- Android: API 22+ (Android 5.1+)
- Web: Not supported (throws unimplemented error)

[0.1.0]: https://github.com/yourusername/capacitor-camera-crop/releases/tag/v0.1.0

