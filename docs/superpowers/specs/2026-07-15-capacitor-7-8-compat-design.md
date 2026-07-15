# Design: Capacitor 7 + 8 compatibility release (`capacitor-camera-crop` v2.0.0)

**Date:** 2026-07-15
**Status:** Approved
**Branch:** `feature/capacitor-7-8-compat`

## Goal & scope

Make the plugin installable and buildable in apps running **Capacitor 7 or 8** from a
single published package, released as **v2.0.0** (major bump — drops Capacitor 6).

The public API (`captureAndCrop` + its options and result type) stays **unchanged** so
consumer app code needs no edits when upgrading.

### Explicitly out of scope

- Building a custom Android cropper (uCrop stays). Tracked as a separate future initiative.
- Adding a web implementation (web still throws "unimplemented").
- Any change to the public TypeScript API surface.
- Unrelated refactoring.

## Decisions (locked during brainstorming)

| Decision | Choice |
|---|---|
| Capacitor versions supported | Cap 7 + 8 only (`^7.0.0 \|\| ^8.0.0`); drop Cap 6 |
| Release version | `2.0.0` (major bump) |
| Android cropper | Keep uCrop; bump to latest tag; verify under new toolchain |
| Android namespace | `com.example.capacitorcameracrop` → `dev.tescari.capacitorcameracrop` |
| iOS cropper | Keep `CropViewController ~> 2.6` (maintained) |

## Approach: single package, dual peer range

`peerDependencies: { "@capacitor/core": "^7.0.0 || ^8.0.0" }`.

The native layers are built against the **higher** of the two Capacitor versions' floors, so
the plugin satisfies both: an app on Cap 7 and an app on Cap 8 each receive a plugin whose
min-OS / min-SDK targets are at or below their own.

**Critical:** the exact target numbers (iOS deployment target, Android compile/min/target SDK,
AGP / Gradle / Kotlin / JDK versions) are **read from the official Capacitor 7 and 8 migration
guides during implementation**, not assumed in this design. Verifying those numbers is the
first implementation task, not a baked-in assumption. Values named below are current
expectations to be confirmed.

## Work breakdown

### 1. JS / TypeScript layer

- Bump devDependencies `@capacitor/core`, `@capacitor/android`, `@capacitor/ios` → 8.x.
- Keep `@capacitor/docgen`, rollup, prettier, typescript current.
- Update `peerDependencies` to `^7.0.0 || ^8.0.0`.
- Rebuild `dist/` and re-run docgen. No API or type changes expected.

### 2. Android

- Rename namespace `com.example.capacitorcameracrop` → `dev.tescari.capacitorcameracrop`:
  - `android/build.gradle` `namespace`
  - Move Kotlin source dir `android/src/main/java/com/example/...` → `.../dev/tescari/...`
  - `AndroidManifest.xml`, `res/xml/file_paths.xml` FileProvider authority, any
    `R` / `BuildConfig` imports.
- Bump `compileSdk` / `targetSdk` → 35 (or 36 if Cap 8 requires — verify).
- Bump `minSdk` to the Cap 7/8 floor (expected 23 — verify).
- Bump AGP → 8.7.x, Gradle wrapper accordingly, Kotlin → 1.9.25 / 2.x, JDK 17 → 21.
- Bump uCrop to its latest tag; confirm it resolves and builds under the new toolchain.

### 3. iOS

- `CapacitorCameraCrop.podspec`: raise `ios.deployment_target` to the Cap 7/8 floor
  (14 or 15 — verify), bump `swift_version`.
- Keep `CropViewController ~> 2.6`.
- Verify the `CAP_PLUGIN` ObjC bridging registration and the Swift plugin compile against
  the new `Capacitor` pod. Cap 7/8 still support the `.m` macro, so no rewrite expected —
  confirm during build.

### 4. Metadata & release

- `package.json` `version` → `2.0.0`.
- Update `CHANGELOG.md` with a `2.0.0` entry. Also reconcile the existing changelog, which
  currently reads `0.1.0` while `package.json` is `1.0.3`.
- Update `README.md` compatibility line (currently "Capacitor 6 compatible").

### 5. Verification (acceptance gate)

This repo has no consuming app, so "it compiles" is **not** sufficient. Build a throwaway
test app (fresh `@capacitor/create-app`) and install the plugin into it **twice** — once on
Capacitor 7, once on Capacitor 8 — and run `captureAndCrop` end-to-end on each platform:

- Sources: camera + gallery
- Cropping: enabled + disabled
- Each aspect ratio: free, 1:1, 4:3, 16:9, custom
- Output: `uri` + `base64`

On-device camera paths must be exercised by a human (the maintainer), since a physical
camera can't be driven headlessly. Simulator/emulator covers gallery + crop paths.

## Acceptance criteria

- `npm install` succeeds in both a Cap 7 and a Cap 8 app with no peer-dependency force flags.
- Android and iOS builds succeed under both apps.
- `captureAndCrop` returns correct results across the matrix above on both platforms.
- Public API and types unchanged from v1.x.
