# Contributing

Thanks for your interest in improving `capacitor-camera-crop`! This is a small,
single-method plugin (`captureAndCrop`) with iOS (Swift), Android (Kotlin), and a
TypeScript bridge.

## Project layout

- `src/` — TypeScript API (`definitions.ts`, `index.ts`, `web.ts`, `overlay-manager.ts`)
- `ios/Plugin/` — Swift implementation + `Package.swift` (SPM) and the podspec
- `android/` — Kotlin implementation, `build.gradle`, manifest
- `example/` — a runnable Capacitor 8 demo app that consumes the plugin (see its README)

## Building the plugin

Requires [Bun](https://bun.sh) and Node 22+.

```bash
bun install
bun run build      # clean + tsc + rollup -> dist/
node validate.cjs  # sanity-checks the build output
bun run format     # prettier over src/
```

`dist/` is not committed; it is produced at publish time (`prepublishOnly`) and by
the command above. CI runs `prettier --check`, `build`, and `validate` on every PR.

## Testing native changes

Use the demo app in [`example/`](./example) — it exercises the full API on a real
device or simulator/emulator. Follow `example/README.md` for setup. Please verify
both platforms when touching native code (camera capture needs a physical device).

## Keeping your Apple team ID out of git (iOS)

The example's `ios/` project is committed, and Xcode's Automatic signing writes
your personal `DEVELOPMENT_TEAM` into `project.pbxproj` when you build. The
committed value is intentionally blank; keep it that way:

1. **Ignore your local signing change** (recommended, per clone):

   ```bash
   git update-index --skip-worktree example/ios/App/App.xcodeproj/project.pbxproj
   ```

   Your Xcode keeps your team locally (signing works), but git never stages it.
   To intentionally change that file later: `--no-skip-worktree`, edit, re-set.

2. **Enable the pre-commit hook** (blocks a team ID from being committed):

   ```bash
   git config core.hooksPath .githooks
   ```

CI also fails if a non-empty `DEVELOPMENT_TEAM` is ever committed, as a backstop.

## Conventions

- TypeScript is `strict`; keep the public API and `definitions.ts` in sync with both
  native implementations, and match method names across TS/Swift/Kotlin.
- Format TS with Prettier (`.prettierrc.json`); no leftover debug logging in native code.
- Keep changes focused; note any platform-behavior differences in the README/JSDoc.

## Pull requests

Open a PR against `main` with a clear description and, for behavior changes, a note
on how you verified it (platforms tested). Update `CHANGELOG.md` under an
`## [Unreleased]` heading if your change is user-facing.
