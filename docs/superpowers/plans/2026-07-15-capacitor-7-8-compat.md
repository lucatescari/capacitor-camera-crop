# Capacitor 7 + 8 Compatibility Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Release `capacitor-camera-crop` v2.0.0 that installs and builds cleanly in apps running Capacitor 7 or 8, with the public API unchanged.

**Architecture:** Single package with a dual peer-dependency range (`^7.0.0 || ^8.0.0`). Native layers are pinned to the *lower* of the two Capacitor versions' minimum-OS/SDK floors (so a plugin binary is consumable by both), while compile/target SDK values continue to be inherited from the consuming app via `rootProject.ext`. Verification is done by installing the plugin into a throwaway Cap 7 app and a throwaway Cap 8 app and exercising it end-to-end.

**Tech Stack:** TypeScript + Rollup (JS layer), Kotlin + Gradle + uCrop (Android), Swift + CocoaPods + CropViewController (iOS), Bun (package manager).

## Global Constraints

Every task's requirements implicitly include this section. Values are verified against the official Capacitor 7 and 8 migration guides (fetched 2026-07-15).

- **Peer dependency:** `@capacitor/core` = `^7.0.0 || ^8.0.0`
- **Release version:** `2.0.0`
- **Public API:** UNCHANGED — `captureAndCrop(options?)` and all types in `src/definitions.ts` stay identical.
- **Android namespace:** `com.example.capacitorcameracrop` → `dev.tescari.capacitorcameracrop`
- **iOS deployment target:** `14.0` (Cap 7 floor; a pod at 14.0 is consumable by Cap 8 apps at 15.0)
- **iOS swift_version:** `5.9`
- **Android minSdk (fallback default):** `23` (Cap 7 floor; consumable by Cap 8 apps at 24)
- **Android compileSdk / targetSdk (fallback default):** `36` (Cap 8 level; real value inherited from app's `rootProject.ext`)
- **Android AGP (plugin buildscript):** `8.7.2` · **Kotlin:** `1.9.25` · **JDK / Java compat:** `21`
- **uCrop:** keep `com.github.yalantis:ucrop`; use the latest available tag (currently `2.2.8` unless a newer tag exists).
- **Do NOT:** add a web implementation, build a custom cropper, change the public API, or do unrelated refactoring.

Reference version matrix:

| | Capacitor 7 | Capacitor 8 | Plugin uses |
|---|---|---|---|
| iOS deployment target | 14.0 | 15.0 | **14.0** |
| Android minSdk | 23 | 24 | **23** |
| Android compile/target SDK | 35 | 36 | **36** (fallback; inherited from app) |
| AGP | 8.7.2 | 8.13.0 | **8.7.2** (buildscript) |
| Kotlin | 1.9.25 | 2.2.20 | **1.9.25** (buildscript) |
| Node | 20+ | 22+ | n/a |

---

### Task 1: JS/TypeScript layer — bump Capacitor deps and peer range

**Files:**
- Modify: `package.json:41-56` (devDependencies + peerDependencies)
- Rebuild: `dist/` (generated)

**Interfaces:**
- Consumes: nothing (first task)
- Produces: a `package.json` with `peerDependencies["@capacitor/core"] === "^7.0.0 || ^8.0.0"` and Cap 8 devDeps; a rebuilt `dist/` that `validate.cjs` passes.

- [ ] **Step 1: Baseline — confirm the current build validates**

Run: `bun run build && node validate.cjs`
Expected: PASS (`All checks passed!`) — establishes the build works before changes.

- [ ] **Step 2: Bump the three Capacitor devDependencies to 8.x**

In `package.json` devDependencies, change:
```json
    "@capacitor/android": "^8.0.0",
    "@capacitor/core": "^8.0.0",
    "@capacitor/ios": "^8.0.0",
```
(from `^6.0.0`). Leave `@capacitor/docgen`, rollup, prettier, typescript, swiftlint entries as-is.

- [ ] **Step 3: Widen the peer dependency range**

In `package.json` peerDependencies, change:
```json
  "peerDependencies": {
    "@capacitor/core": "^7.0.0 || ^8.0.0"
  },
```

- [ ] **Step 4: Reinstall and rebuild**

Run: `bun install && bun run build`
Expected: install resolves with no peer-dependency errors; `tsc` and rollup complete; `dist/` is regenerated.

- [ ] **Step 5: Validate the build**

Run: `node validate.cjs`
Expected: PASS (`All checks passed!`). If it fails on the hardcoded Android path check, that is expected to be fixed in Task 2 — but at this point the path is unchanged, so it should still PASS here.

- [ ] **Step 6: Commit**

```bash
git add package.json bun.lock dist/
git commit -m "build: bump Capacitor deps to 8.x and widen peer range to 7||8"
```

---

### Task 2: Android — rename placeholder namespace to `dev.tescari.capacitorcameracrop`

**Files:**
- Move: `android/src/main/java/com/example/capacitorcameracrop/CapacitorCameraCropPlugin.kt` → `android/src/main/java/dev/tescari/capacitorcameracrop/CapacitorCameraCropPlugin.kt`
- Modify: the `package` declaration on line 1 of that file
- Modify: `android/build.gradle:20` (`namespace`)
- Modify: `validate.cjs:73` (hardcoded plugin path)

**Interfaces:**
- Consumes: Task 1's `package.json`.
- Produces: Kotlin plugin under package `dev.tescari.capacitorcameracrop`; `build.gradle` `namespace "dev.tescari.capacitorcameracrop"`; `validate.cjs` pointing at the new path. The FileProvider authority is `${context.packageName}.fileprovider` (the app's package) and is unaffected by this rename.

- [ ] **Step 1: Move the Kotlin source into the new package directory**

```bash
mkdir -p android/src/main/java/dev/tescari/capacitorcameracrop
git mv android/src/main/java/com/example/capacitorcameracrop/CapacitorCameraCropPlugin.kt \
       android/src/main/java/dev/tescari/capacitorcameracrop/CapacitorCameraCropPlugin.kt
rmdir android/src/main/java/com/example/capacitorcameracrop android/src/main/java/com/example 2>/dev/null || true
```

- [ ] **Step 2: Update the package declaration**

In `android/src/main/java/dev/tescari/capacitorcameracrop/CapacitorCameraCropPlugin.kt` line 1, change:
```kotlin
package dev.tescari.capacitorcameracrop
```
(from `package com.example.capacitorcameracrop`). No other line in this file references `com.example` (verified: no `import com.example`, no `BuildConfig`, authority uses `context.packageName`).

- [ ] **Step 3: Update the Gradle namespace**

In `android/build.gradle`, change:
```gradle
    namespace "dev.tescari.capacitorcameracrop"
```
(from `com.example.capacitorcameracrop`).

- [ ] **Step 4: Update the hardcoded path in validate.cjs**

In `validate.cjs`, the `Android Kotlin implementation` check, change the path to:
```js
  fs.existsSync('android/src/main/java/dev/tescari/capacitorcameracrop/CapacitorCameraCropPlugin.kt'),
```

- [ ] **Step 5: Verify no stray references remain**

Run: `grep -rn "com.example" android/ validate.cjs`
Expected: no output (exit code 1). If anything prints, fix it before continuing.

- [ ] **Step 6: Validate**

Run: `node validate.cjs`
Expected: PASS (`All checks passed!`) — confirms the new path resolves.

- [ ] **Step 7: Commit**

```bash
git add android/ validate.cjs
git commit -m "refactor(android): rename namespace com.example -> dev.tescari.capacitorcameracrop"
```

---

### Task 3: Android — bump toolchain, SDK floors, and uCrop

**Files:**
- Modify: `android/build.gradle` (buildscript classpath, compileSdk/min/target defaults, compileOptions, kotlinOptions, kotlin-stdlib, uCrop version)

**Interfaces:**
- Consumes: Task 2's renamed namespace.
- Produces: a `build.gradle` whose fallback SDK/toolchain values match the Cap 7/8 matrix in Global Constraints and that builds standalone.

- [ ] **Step 1: Bump the buildscript classpath versions**

In `android/build.gradle` `buildscript.dependencies`, change to:
```gradle
        classpath 'com.android.tools.build:gradle:8.7.2'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25'
```
(from `8.2.1` / `1.9.20`).

- [ ] **Step 2: Bump SDK fallback defaults**

In `android/build.gradle` `android { }`, change the fallback defaults (the value after `:` in each ternary):
```gradle
    compileSdkVersion project.hasProperty('compileSdkVersion') ? rootProject.ext.compileSdkVersion : 36
```
```gradle
        minSdkVersion project.hasProperty('minSdkVersion') ? rootProject.ext.minSdkVersion : 23
        targetSdkVersion project.hasProperty('targetSdkVersion') ? rootProject.ext.targetSdkVersion : 36
```
(compileSdk `34`→`36`, minSdk `22`→`23`, targetSdk `34`→`36`). Leave the `rootProject.ext` inheritance intact so consuming apps' values win.

- [ ] **Step 3: Bump Java/Kotlin compile targets to 21**

In `android/build.gradle`, change:
```gradle
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_21
        targetCompatibility JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = '21'
    }
```
(from `VERSION_17` / `'17'`).

- [ ] **Step 4: Bump the kotlin-stdlib dependency**

In `android/build.gradle` `dependencies`, change:
```gradle
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.25'
```
(from `1.9.20`).

- [ ] **Step 5: Check for a newer uCrop tag and bump if one exists**

Run: `git ls-remote --tags https://github.com/Yalantis/uCrop.git | grep -o 'refs/tags/[0-9.]*' | sort -V | tail -5`
Decision rule: if the highest tag is newer than `2.2.8`, update the dependency to that tag; otherwise leave `2.2.8`. Edit `android/build.gradle` accordingly:
```gradle
    // uCrop for image cropping
    implementation 'com.github.yalantis:ucrop:2.2.8'
```
(replace `2.2.8` with the newer tag if found).

- [ ] **Step 6: Verify the Android library builds standalone**

Run: `cd android && ./gradlew assembleRelease --offline || ./gradlew assembleRelease`
Expected: `BUILD SUCCESSFUL`. (Requires JDK 21 on PATH. If Gradle cannot resolve `:capacitor-android` standalone, this is expected — note it and rely on the in-app build verification in Tasks 6–7 instead, but the uCrop/toolchain resolution errors, if any, must be resolved here.)

- [ ] **Step 7: Commit**

```bash
git add android/build.gradle
git commit -m "build(android): bump AGP/Kotlin/JDK to 21 + SDK floors for Cap 7/8, refresh uCrop"
```

---

### Task 4: iOS — bump podspec deployment target and Swift version

**Files:**
- Modify: `CapacitorCameraCrop.podspec:12-13`

**Interfaces:**
- Consumes: Task 1's `package.json` (podspec reads `version`/`description` from it).
- Produces: a podspec with `ios.deployment_target = '14.0'` and `swift_version = '5.9'`, consumable by both Cap 7 and Cap 8 apps.

- [ ] **Step 1: Raise the deployment target and Swift version**

In `CapacitorCameraCrop.podspec`, change:
```ruby
  s.ios.deployment_target  = '14.0'
```
(from `'13.0'`), and:
```ruby
  s.swift_version = '5.9'
```
(from `'5.1'`). Leave `s.dependency 'Capacitor'` and `s.dependency 'CropViewController', '~> 2.6'` unchanged.

- [ ] **Step 2: Lint the podspec**

Run: `pod spec lint CapacitorCameraCrop.podspec --allow-warnings --skip-tests || echo "pod not available — verify in Task 6/7 instead"`
Expected: lint passes, OR (if CocoaPods isn't installed locally) defer verification to the in-app builds in Tasks 6–7. The Swift compile itself is exercised there.

- [ ] **Step 3: Commit**

```bash
git add CapacitorCameraCrop.podspec
git commit -m "build(ios): raise deployment target to 14.0 and swift_version to 5.9 for Cap 7/8"
```

---

### Task 5: Metadata — version bump, changelog, README

**Files:**
- Modify: `package.json:3` (`version`)
- Modify: `CHANGELOG.md`
- Modify: `README.md` (compatibility statement)

**Interfaces:**
- Consumes: all prior tasks.
- Produces: `version` = `2.0.0`; a `2.0.0` changelog entry; README stating Cap 7 + 8 support.

- [ ] **Step 1: Bump the package version**

In `package.json` change `"version": "1.0.3"` to:
```json
  "version": "2.0.0",
```

- [ ] **Step 2: Add the 2.0.0 changelog entry**

At the top of `CHANGELOG.md` (below the intro block, above `## [0.1.0]`), add:
```markdown
## [2.0.0] - 2026-07-15

### Changed

- **BREAKING:** now requires Capacitor 7 or 8 (`@capacitor/core` `^7.0.0 || ^8.0.0`). Dropped Capacitor 6 support.
- Android: `minSdk` 22 → 23, `compileSdk`/`targetSdk` → 36, AGP 8.7.2, Kotlin 1.9.25, JDK 21.
- Android: renamed plugin namespace `com.example.capacitorcameracrop` → `dev.tescari.capacitorcameracrop`.
- iOS: raised deployment target 13.0 → 14.0, Swift 5.1 → 5.9.

### Notes

- Public API (`captureAndCrop`) is unchanged; no consumer code changes required beyond upgrading Capacitor.
- uCrop (Android) and CropViewController (iOS) retained.
```
Also correct the stale intro: the existing file's only prior entry is labelled `0.1.0` although the last published version was `1.0.3` — leave the historical `0.1.0` entry as-is (do not rewrite history) but ensure the new `2.0.0` entry is the topmost.

- [ ] **Step 3: Update the README compatibility statement**

In `README.md`, locate any statement of supported Capacitor version (search: `grep -ni "capacitor 6\|requirements\|compatib" README.md`). Update/add a requirements line reading:
```markdown
- Capacitor 7 or 8
- iOS 14.0+
- Android API 23+ (Android 6.0+)
```
If no such section exists, add a short `## Requirements` section directly under `## Installation`.

- [ ] **Step 4: Rebuild and validate**

Run: `bun run build && node validate.cjs`
Expected: PASS, and `Package.json` version check still passes (name unchanged).

- [ ] **Step 5: Commit**

```bash
git add package.json CHANGELOG.md README.md dist/
git commit -m "chore: release 2.0.0 metadata (changelog, README, version)"
```

---

### Task 6: Verify in a throwaway Capacitor 7 app

**Files:**
- Create: throwaway app outside the repo (e.g. `../cap7-cameracrop-test/`) — NOT committed.

**Interfaces:**
- Consumes: the full built plugin from all prior tasks.
- Produces: a pass/fail record of the end-to-end matrix on Cap 7. This is an acceptance gate, not a code deliverable.

- [ ] **Step 1: Scaffold a Capacitor 7 app**

```bash
cd .. && npm create @capacitor/app@latest cap7-cameracrop-test -- --name cap7test --package-id dev.tescari.cap7test
cd cap7-cameracrop-test
npm install @capacitor/core@^7 @capacitor/cli@^7 @capacitor/ios@^7 @capacitor/android@^7
```

- [ ] **Step 2: Install the plugin from the local path**

```bash
npm install ../capacitor-camera-crop
npx cap add ios && npx cap add android && npx cap sync
```
Expected: install completes with NO peer-dependency force flags; `cap sync` reports the plugin found for both platforms.

- [ ] **Step 3: Wire a minimal call**

In the app's entry (e.g. `src/js/capacitor-welcome.js` or `main` file), add a button handler:
```js
import { CapacitorCameraCrop } from 'capacitor-camera-crop';
window.testCrop = async (opts) => {
  const res = await CapacitorCameraCrop.captureAndCrop(opts);
  console.log('RESULT', res);
  return res;
};
```

- [ ] **Step 4: Build and run Android (emulator)**

```bash
npx cap run android
```
Expected: Gradle build SUCCESSFUL under AGP 8.7.2 / SDK 35 (app's values); app launches. Exercise gallery source with `enableCropping:true` for each aspect ratio (`free`,`1:1`,`4:3`,`16:9`,`{x:2,y:3}`) and both `resultType` (`uri`,`base64`); confirm `RESULT` logs sane width/height and a value.

- [ ] **Step 5: Build and run iOS (simulator)**

```bash
npx cap run ios
```
Expected: pod install resolves CropViewController; Swift compiles under deployment target 14; app launches. Exercise the gallery + crop matrix as in Step 4.

- [ ] **Step 6: Human on-device camera check**

The maintainer runs the app on a physical device and exercises `source:'camera'` with cropping on both platforms (camera cannot be driven headlessly). Record pass/fail.

- [ ] **Step 7: Record results**

Note the Cap 7 matrix outcome in the PR description / a scratch note. No commit (test app is not in the repo).

---

### Task 7: Verify in a throwaway Capacitor 8 app

**Files:**
- Create: throwaway app outside the repo (e.g. `../cap8-cameracrop-test/`) — NOT committed.

**Interfaces:**
- Consumes: the full built plugin from all prior tasks.
- Produces: a pass/fail record of the end-to-end matrix on Cap 8 (the second half of the acceptance gate).

- [ ] **Step 1: Scaffold a Capacitor 8 app**

```bash
cd .. && npm create @capacitor/app@latest cap8-cameracrop-test -- --name cap8test --package-id dev.tescari.cap8test
cd cap8-cameracrop-test
npm install @capacitor/core@^8 @capacitor/cli@^8 @capacitor/ios@^8 @capacitor/android@^8
```

- [ ] **Step 2: Install the plugin and sync**

```bash
npm install ../capacitor-camera-crop
npx cap add ios && npx cap add android && npx cap sync
```
Expected: install completes with NO peer-dependency force flags; `cap sync` finds the plugin for both platforms.

- [ ] **Step 3: Wire the same minimal call as Task 6 Step 3.**

- [ ] **Step 4: Build and run Android (emulator)**

```bash
npx cap run android
```
Expected: Gradle build SUCCESSFUL under AGP 8.13.0 / Kotlin 2.2.20 / SDK 36 (app's values) — confirms the plugin's Kotlin (compiled at 1.9.25 level) is consumable under Kotlin 2.2.20. Exercise the gallery + crop matrix.

- [ ] **Step 5: Build and run iOS (simulator)**

```bash
npx cap run ios
```
Expected: Swift compiles; deployment target 14 pod is accepted by the iOS 15 app. Exercise the gallery + crop matrix.

- [ ] **Step 6: Human on-device camera check** (as in Task 6 Step 6).

- [ ] **Step 7: Record results and finalize**

Record the Cap 8 matrix outcome. If both Task 6 and Task 7 pass fully, the acceptance criteria are met and the branch is ready for PR / publish.

---

## Post-plan: release (only after both verification tasks pass)

Publishing is a separate, human-authorized step (do not auto-publish):
```bash
git checkout main && git merge --no-ff feature/capacitor-7-8-compat   # or via PR
npm publish   # requires npm auth; tags 2.0.0
git tag v2.0.0 && git push origin v2.0.0   # so the podspec :tag resolves
```

## Acceptance criteria (from spec)

- [ ] `npm install` succeeds in both a Cap 7 and a Cap 8 app with no peer-dependency force flags. (Tasks 6/7 Step 2)
- [ ] Android and iOS builds succeed under both apps. (Tasks 6/7 Steps 4–5)
- [ ] `captureAndCrop` returns correct results across source × crop × aspect-ratio × resultType on both platforms. (Tasks 6/7 Steps 4–6)
- [ ] Public API and types unchanged from v1.x. (No task modifies `src/definitions.ts`.)
