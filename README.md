# Dextra

Dextra is a GeckoView-based Android browser designed for Samsung DeX. It treats a large screen, keyboard, mouse, window resizing, and multi-pane work as first-class browser experiences while keeping the browser identity real and predictable.

## Status

This project is an early release build intended for internal testing and Samsung DeX-focused development.

- Application ID: `com.dwicao.dextra`
- Version: `0.4.1`
- Minimum Android version: 26
- Target Android SDK: 36
- Web engine: Mozilla GeckoView `154.0.20260824154132`
- Current native ABI: `arm64-v8a`

Runtime testing on physical Samsung DeX hardware is still required before public distribution.

## Features

- DeX-oriented desktop and compact layouts.
- Horizontal and vertical tab strips.
- Stable tab IDs and session restore.
- Named tab session snapshots with restore and deletion.
- Tab groups with rename, collapse, move, and delete actions.
- Tab reorder, pinning, sleeping, and inactive-tab hibernation.
- DeX drag-and-drop URL opening and expanded browser keyboard shortcuts.
- Two-pane split view with focus, swap, and close actions.
- Mouse secondary-click context menus for pages and tabs.
- Searchable tab switcher, command palette, configurable keyboard shortcuts, and hover previews.
- Smart address bar suggestions from bookmarks and normal browsing history, with keyboard navigation.
- Homepage, search engine, theme, desktop-site, and tab layout settings.
- Per-site desktop mode, ad blocking, userscripts, and zoom overrides.
- GeckoView tracking protection and Global Privacy Control.
- Origin-scoped site permission decisions.
- HTTPS-only remote filter lists and userscripts.
- Mozilla-signed Firefox extension installation.
- Durable downloads through WorkManager.
- User-selected SAF download folders and download search/status/type filters.
- Download resume validation, pause, cancel, MediaStore publishing, and notifications.
- Full-page bookmark and history managers with folder filtering, bounded HTML import/export, search, deletion, and history pruning.
- Dedicated Keyboard Shortcuts settings page.
- Reading list with bounded offline article snapshots.
- Native reader mode with article extraction, font sizing, and light/dark/sepia palettes.
- Privacy dashboard for reviewing and clearing per-site or all GeckoView site data.
- Per-site "forget this site" cleanup and live ad/tracker blocking statistics.
- QR URL sharing, PDF export, visible and bounded long screenshots.
- Picture-in-Picture for active media and validated PWA launcher shortcuts.
- Encrypted local login storage with GeckoView autocomplete integration.
- Installed PWA management with separate Android document tasks/windows.
- Private tabs excluded from saved sessions, history, and favicon fetching.
- Crash reports for Java uncaught exceptions.

## Architecture

- `MainActivity` owns Android lifecycle, back handling, and keyboard dispatch.
- `BrowserViewModel` owns browser state, GeckoSession delegates, tab lifecycle, permissions, downloads, and persistence coordination.
- `BrowserApp` contains the Compose desktop/compact browser UI.
- `GeckoRuntimeHolder` keeps one shared GeckoRuntime for the application process.
- Room stores history, bookmarks, site permissions, and download records.
- DataStore stores browser settings and saved tab metadata.
- WorkManager runs downloads beyond the activity lifecycle.
- The built-in WebExtension contains host filtering and browser context-menu integration.

## Requirements

- JDK 17.
- Android SDK platform 37.1.
- Android Build Tools compatible with the installed Android Gradle Plugin.
- An arm64 Android device or emulator for GeckoView runtime testing.
- Internet access to resolve Maven and Mozilla GeckoView dependencies.

## Build

Run commands from the repository root:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew assembleRelease
```

The project intentionally does not require `--no-daemon` or any special Gradle flags.

The release build uses R8 shrinking and resource shrinking. R8 bytecode optimization is disabled in `app/proguard-rules.pro` because GeckoView's native launcher crashed during startup on the test Samsung/DeX environment when optimization was enabled. Do not remove `-dontoptimize` without repeating a cold-start test on the target device.

## Release Signing

Local signing is configured through an ignored `keystore.properties` file and an ignored `dextra-release.jks` keystore. These files must never be committed.

The current release artifact is:

```text
app/build/outputs/apk/release/app-release.apk
```

The local key is suitable for sideloading and internal testing. Back it up securely. Losing the key prevents future APK updates from installing over the current application. Use a properly managed production key for Play Store or public distribution.

## Install

With a connected Android device or emulator:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release APK should report `debuggable=false` and contain only the configured `arm64-v8a` native libraries.

## Crash Logs

Java uncaught exceptions are written to both locations when possible:

- Android 10 and newer: `Download/dextra/crash-<timestamp>-<pid>.txt`
- Internal fallback: the app's private `files/last-crash.txt`

Android native crashes such as a `SIGSEGV` in GeckoView cannot be safely intercepted by a Java `Thread.UncaughtExceptionHandler`. Android records those failures as tombstones. During development, retrieve the crash buffer with:

```bash
adb logcat -b crash -v threadtime
```

## Privacy Model

Dextra does not attempt to impersonate Chrome, Safari, another Android device, or an automation client. It uses the real GeckoView user agent and documented GeckoView settings. Browser and device fingerprint randomization is intentionally not part of the project because inconsistent spoofing can reduce compatibility and make a client less trustworthy.

Privacy-sensitive features should be explicit, origin-scoped where possible, clearable by the user, and compatible with normal website behavior. Private browsing does not hide files downloaded to device storage; Dextra labels those downloads accordingly.

## Testing

Current automated tests cover URL handling, navigation policy, Firefox Add-on URL parsing, bookmark HTML parsing, address suggestions, privacy-origin aggregation, keyboard shortcuts, and the native download request path. The project still needs physical-device and instrumentation coverage for:

- Samsung DeX window resizing and external displays.
- Cold start and session restore after process death.
- Split view, drag/reorder, keyboard, and mouse behavior.
- GeckoView renderer/process crashes.
- Android permission prompts and site permission persistence.
- WorkManager download recovery after the app is stopped.
- Room migrations from every released database version.
- Memory, frame pacing, battery, and startup benchmarks.

## Repository Layout

```text
app/src/main/java/com/dwicao/dextra/
  MainActivity.kt
  DextraApplication.kt
  browser/       Browser state, GeckoView integration, policy, downloads
  data/          Room database and DataStore settings
  ui/            Compose browser interface and theme
app/src/main/assets/adblock/
  manifest.json
  background.js
  content.js
app/src/test/    JVM regression tests
```

## Scope

The project focuses on a fast, private, secure, reliable, and efficient browser for Samsung DeX. Workspaces and AI features are not part of the current product scope unless explicitly requested.

No license has been declared yet.
