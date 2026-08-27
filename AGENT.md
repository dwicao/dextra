# Agent Guidance for Dextra

## Project Context

Dextra is an Android application written in Kotlin and Jetpack Compose. It embeds Mozilla GeckoView and targets Samsung DeX, large Android tablets, keyboard/mouse input, resizable windows, and external displays.

The main runtime components are:

- `MainActivity`: lifecycle, back handling, and hardware keyboard dispatch.
- `DextraApplication`: shared GeckoRuntime and crash handling.
- `BrowserViewModel`: browser state, GeckoSession delegates, persistence, permissions, tabs, extensions, and downloads.
- `BrowserApp`: Compose UI for desktop and compact layouts.
- `BrowserDatabase`: Room persistence and explicit migrations.
- `SettingsRepository`: DataStore settings and saved session metadata.
- `DownloadEngine` and `DownloadWorker`: durable download execution.

## Working Rules

- Inspect the existing implementation before changing behavior.
- Make the smallest correct change that solves the problem.
- Preserve unrelated user changes in a dirty worktree.
- Never use destructive commands such as `git reset --hard` or `git checkout --`.
- Do not commit, amend, push, or create pull requests unless explicitly requested.
- Use `apply_patch` for manual edits.
- Prefer ASCII in source and documentation unless existing content requires otherwise.
- Keep comments short and explain intent, not obvious syntax.
- Keep public behavior stable unless the requested change requires a behavior change.

## Build and Verification

Use the Gradle wrapper from the repository root:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew assembleRelease
```

Do not add `--no-daemon`.

Before reporting completion:

- Run `git diff --check`.
- Run the relevant unit tests.
- Run `lintDebug` for Kotlin/Android changes.
- Build the affected debug or release variant.
- Verify release APK signing and `debuggable=false` when release configuration changes.
- Inspect the diff and confirm only intended files changed.

## Release and R8

- The app is currently arm64-only because the GeckoView dependency is `geckoview-arm64-v8a`.
- Release uses R8 shrinking and resource shrinking.
- `-dontoptimize` in `app/proguard-rules.pro` is intentional. Enabling R8 optimization caused a reproducible native GeckoView `SIGSEGV` during startup on the test DeX environment.
- Keep rules for application entry points, `BrowserViewModel`, `DownloadWorker`, and Room generated classes are intentional.
- Do not add a broad `-keep class ** { *; }` rule to hide a release bug. First reproduce the failure and identify the reflection or native boundary involved.
- Do not commit `dextra-release.jks` or `keystore.properties`.

## GeckoView Rules

- Use documented GeckoView APIs and the pinned dependency version unless upgrading is intentional and tested.
- Do not set a custom UA to impersonate another browser or device.
- Do not inject `navigator.webdriver` changes or spoof canvas, WebGL, fonts, sensors, media devices, TLS, or Android identifiers.
- Keep browser identity and viewport behavior internally consistent.
- Use a real GeckoRuntime shared by sessions.
- Apply runtime-wide settings, especially DNS and content color settings, before creating or loading the first session.
- Treat private sessions as separate from normal browsing state.
- Do not bypass certificate errors or unsafe navigation checks.

## DeX UX Rules

- Design for keyboard, mouse, hover, right-click, and resizable windows.
- Preserve both desktop and compact layouts.
- Use stable tab IDs and stable lazy-list keys.
- Keep split view limited to two panes unless the memory model is redesigned.
- Do not remove visible loading, crash, permission, or download states.
- Test narrow windows as well as 16:9 and ultrawide DeX layouts.
- Avoid changing UA or viewport mode unexpectedly during an existing navigation.

## State and Performance

- Keep database and network work off the main thread.
- Do not block a coroutine with `runBlocking` in production code.
- Throttle high-frequency progress/state updates when they do not affect correctness.
- Avoid updating AndroidView listeners or compositor state on every Compose recomposition.
- Do not add `remember`, `useMemo`, or callbacks solely by habit. Use profiling or a clear stability boundary as justification.
- Keep inactive sessions inactive and preserve the existing tab hibernation behavior.
- Measure startup, frame pacing, memory, battery, and navigation before making broad performance claims.

## Privacy and Security

- Minimize persistent identifiers and sensitive data.
- Make permission decisions origin-scoped and clearable.
- Keep remote filter lists, userscripts, and extension packages on HTTPS.
- Bound imported files, downloaded extension packages, filter lists, and userscripts.
- Treat userscripts and extensions as privileged code and make their permissions visible.
- Do not silently persist private browsing metadata without documenting it.
- Private downloads remain device files; keep the UI warning accurate.
- Prefer normal, transparent request identities over spoofed browser headers.
- Do not add proxy rotation, fingerprint rotation, or anti-fraud evasion behavior.

## Persistence

- Any Room entity/schema change requires a version increment and an explicit migration.
- Test migration behavior where practical; never use a destructive fallback that silently deletes browsing data.
- Preserve stable tab IDs across session save and restore.
- Private tabs must not enter saved normal sessions or history.
- Keep download records recoverable after process death.

## Downloads

- Downloads run through WorkManager, not an activity-only coroutine.
- Preserve pause, resume, cancel, retry, and process-death behavior.
- Use conservative connection behavior. One stream is the default; do not add aggressive parallel range requests without measurements.
- Validate response codes, ranges, content lengths, URL schemes, and HTTPS downgrade redirects.
- Use MediaStore for public Downloads on Android 10 and newer.
- Do not expose private credentials or cookies in logs.

## Crash Reporting

- Java uncaught exceptions should remain available in the app-private `last-crash.txt` fallback.
- On Android 10 and newer, write text reports to `Download/dextra` through MediaStore.
- Crash handlers must be best-effort and must not throw while handling another crash.
- A Java uncaught handler cannot catch native `SIGSEGV`; use Android crash/tombstone logs for native failures instead of unsafe signal-handler code.
- Never include signing passwords, tokens, cookies, or unnecessary private browsing data in reports.

## Scope Boundaries

- Do not add Workspaces or AI features unless the user explicitly changes product scope.
- Do not trade security or compatibility for a fake fingerprint score.
- Do not claim physical-device reliability from a successful Gradle build alone.
- Do not make release-signing changes without preserving the key identity.

## Useful Files

- `app/build.gradle.kts`: Android, ABI, dependencies, and release build settings.
- `app/proguard-rules.pro`: R8 rules and GeckoView release workaround.
- `app/src/main/AndroidManifest.xml`: permissions, activity, provider, and WorkManager service.
- `app/src/main/java/com/dwicao/dextra/DextraApplication.kt`: GeckoRuntime and crash reporter.
- `app/src/main/java/com/dwicao/dextra/MainActivity.kt`: lifecycle and keyboard behavior.
- `app/src/main/java/com/dwicao/dextra/browser/BrowserViewModel.kt`: browser behavior and state.
- `app/src/main/java/com/dwicao/dextra/browser/DownloadEngine.kt`: HTTP download behavior.
- `app/src/main/java/com/dwicao/dextra/data/BrowserDatabase.kt`: Room entities and migrations.
- `app/src/main/java/com/dwicao/dextra/data/SettingsRepository.kt`: DataStore settings.
- `app/src/main/java/com/dwicao/dextra/ui/BrowserApp.kt`: Compose UI.
