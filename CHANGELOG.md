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
- iOS: **added Swift Package Manager support** (`Package.swift`). Capacitor 8 defaults new iOS apps to SPM, so this is required for the plugin to link and register there.
- iOS: migrated plugin registration to Swift-native `CAPBridgedPlugin` conformance and **removed the legacy `CapacitorCameraCropPlugin.m`**. SPM targets cannot mix Objective-C and Swift; the Swift conformance also works for CocoaPods.

### Fixed

- Android: apply EXIF orientation on decode so captured/picked photos are no longer returned rotated on the no-crop path.
- Android: decode with downsampling (`inSampleSize`) to avoid `OutOfMemoryError` on high-resolution images.
- Android: `width`/`height` now honored independently (parity with iOS).
- iOS: recoverable encode/write failures now reject the promise instead of crashing via `fatalError`.
- iOS: present a crop UI when `enableCropping` is requested and the system editor didn't run (previously that combo silently returned an uncropped image).
- iOS: report true pixel dimensions; clear stored call state so promises don't leak on overlapping calls.
- Overlay: reference-counted and skipped on web (no flash before the unimplemented rejection).

### Notes

- Public API (`captureAndCrop`) is unchanged; no consumer code changes required beyond upgrading Capacitor.
- uCrop (Android) and CropViewController (iOS) retained. On Android, uCrop is distributed via JitPack — see the README for the required repository.

## [1.0.3] - 2025-11-19

### Changed

- Added a web overlay to prevent transition flicker between the image input and crop views.
- Removed the iOS overlay and fixed the Android toolbar position (overlay-manager rework, #1).

## [1.0.1] - 2025-11-14

### Changed

- Packaging cleanup and plugin rename to `capacitor-camera-crop`.

## [1.0.0] - 2025-11-14

### Added

- Initial release of the plugin.
- Native camera capture and gallery picker for iOS and Android.
- Image cropping with uCrop (Android) and native editing / TOCropViewController (iOS).
- Aspect ratio options: free, 1:1, 4:3, 16:9, and custom.
- URI and base64 result types; image resizing with max width/height; JPEG quality control.
- TypeScript definitions, error handling, and user-cancellation support.

Platform support at 1.0.0: iOS 13.0+, Android API 22+, Capacitor 6. (Web: not supported — throws `unimplemented`.)

<!-- Add the matching GitHub release/tag links as releases are published. -->
[1.0.3]: https://github.com/lucatescari/capacitor-camera-crop/releases/tag/1.0.3
[1.0.1]: https://github.com/lucatescari/capacitor-camera-crop/releases/tag/1.0.1
