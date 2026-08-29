# Dextra

Dextra is a GeckoView-based Android browser designed for Samsung DeX. It treats a large screen, keyboard, mouse, window resizing, and multi-pane work as first-class browser experiences while keeping the browser identity real and predictable.

## Status

This project is an early release build intended for internal testing and Samsung DeX-focused development.

- Application ID: `com.dwicao.dextra`
- Version: `0.5.0`
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
- Browser tabs can open in separate resizable Android document windows.
- Two-pane split view with focus, swap, and close actions.
- Mouse secondary-click context menus for pages and tabs.
- Searchable tab switcher, command palette, configurable keyboard shortcuts, and hover previews.
- Find in page with highlighted matches, match navigation, and `Ctrl+F` support.
- Reopen recently closed tabs with `Ctrl+Shift+T` or the browser menu.
- Active-media tab indicators and per-tab mute controls.
- Smart address bar suggestions from bookmarks and normal browsing history, with keyboard navigation.
- Custom HTTPS search engines with `%s` query templates.
- Homepage, search engine, theme, desktop-site, and tab layout settings.
- Customizable new-tab start page with default quick links, privacy-tip toggles, and safe HTTPS links.
- Cookie consent control with global reject modes and per-site banner overrides.
- Privacy automation with scheduled history/download/recovery retention, origin allowlisting, and delayed site-data cleanup on exit.
- Per-site desktop mode, ad blocking, userscripts, and zoom overrides.
- Global and per-site HTTPS-only navigation policy with automatic HTTP upgrade.
- GeckoView tracking protection and Global Privacy Control.
- Origin-scoped site permission decisions.
- HTTPS-only remote filter lists and userscripts.
- Mozilla-signed Firefox extension installation.
- Extension permission center showing required/granted optional permissions and allowing optional access revocation.
- Encrypted address/contact autofill through GeckoView's address storage delegate; cards are intentionally not stored.
- Durable downloads through WorkManager.
- Download queue controls with priority, Wi-Fi-only constraints, delayed scheduling, and bulk pause/resume.
- User-selected SAF download folders and download search/status/type filters.
- Download resume validation, pause, cancel, MediaStore publishing, and notifications.
- Full-page bookmark and history managers with folder filtering, bounded HTML import/export, search, deletion, and history pruning.
- Dedicated Keyboard Shortcuts settings page.
- Reading list with bounded offline article snapshots.
- Native reader mode with article extraction, font sizing, and light/dark/sepia palettes.
- Page translation through GeckoView's on-device translation engine with per-site language preferences.
- Privacy dashboard for reviewing and clearing per-site or all GeckoView site data.
- Per-site "forget this site" cleanup and live ad/tracker blocking statistics.
- QR URL sharing, PDF export, visible and bounded long screenshots.
- Page printing and bounded HTML export.
- Picture-in-Picture for active media and validated PWA launcher shortcuts.
- Encrypted local login storage with GeckoView autocomplete integration.
- Password-encrypted sync bundles for bookmarks, history, settings, and reading list transfer.
- Sync Center preview with selective import/export for settings, bookmarks, history, reading list, permissions, and site overrides.
- WebDAV live session sync for normal open tabs, tab groups, active workspace, and workspace snapshots.
- Biometric/device-credential lock for saved logins with automatic background locking.
- Web Push subscription management and Android website notifications.
- Installed PWA management with separate Android document tasks/windows.
- PWA manifest refresh, same-origin icons, and standalone fullscreen display mode.
- Private tabs excluded from saved sessions, history, and favicon fetching.
- Persisted recently closed normal tabs with restore support after process restart.
- Android lock-screen media controls for active non-private tabs.
- Accessibility text scaling, high-contrast controls, reduced-motion behavior, and per-site page zoom.
- Optional periodic encrypted WebDAV synchronization with HTTPS-only transport and Keystore-protected credentials.
- WebDAV conflict detection with explicit keep-remote, keep-local, and merge resolution.
- WebAuthn related-origin approval prompts for passkey flows.
- Workspace tab sessions with separate normal tabs, groups, and active-tab state.
- Container-backed workspaces isolate GeckoView cookies and storage with per-container site permissions and overrides.
- Automatic session recovery timeline with restorable background checkpoints.
- Site permission center with per-origin allow, block, ask, and reset controls.
- Security diagnostics with HTTPS certificate details, SHA-256 fingerprint, DNS mode, and blocker statistics.
- Local performance dashboard with startup, memory, frame pacing, jank, and DeX-like window metrics.
- Observable network activity inspector for navigation, responses, downloads, and content-blocking events.
- Android share/drag-drop transfer for normal workspace tabs between windows or devices without cookies or credentials.
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
- Room migration from database version 13 to 14 for per-container permissions and site overrides.
- Room migrations through version 16 cover profile-scoped site data, download queue metadata, and HTTPS site policy.
- Biometric unlock and Android notification permission flows.
- Web Push provider delivery and service-worker notification behavior.
- Android print UI, separate browser document windows, and PWA launcher icon refresh.
- Memory, frame pacing, battery, and startup benchmarks.
- Instrumentation smoke coverage for browser UI rendering and activity recreation.

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

The project focuses on a fast, private, secure, reliable, and efficient browser for Samsung DeX. Workspace tab sessions are part of the current product scope; AI features are not.

No license has been declared yet.
