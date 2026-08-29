# Changelog

All notable changes to Dextra are documented here. The commit log at the end records every commit in the repository history.

## Unreleased

### Added

- Find in page with match navigation and highlighted results.
- Reopen recently closed tabs from the browser menu and `Ctrl+Shift+T`.
- Active-media indicators and per-tab mute controls.
- Custom HTTPS search engines using `%s` query templates.
- Password-encrypted sync bundles for local transfer between devices.
- GeckoView page translation with remembered per-site target languages.
- Recently closed normal tabs persisted across process restarts.
- Android lock-screen media controls for active non-private media.
- Accessibility text scaling, high contrast, reduced motion, and larger navigation targets.
- Periodic encrypted WebDAV synchronization with ETag-aware atomic uploads.
- WebDAV conflict detection with explicit remote, local, and merge resolution.
- WebAuthn related-origin approval prompts for passkey flows.
- Workspace tab sessions with isolated normal-tab snapshots and workspace management.
- Per-origin permission controls and HTTPS security diagnostics with certificate fingerprints.
- Instrumentation smoke tests for UI rendering and activity recreation.
- Container-backed workspace profiles with isolated GeckoView contexts and per-profile site decisions.
- Sync Center previews with selective encrypted import/export.
- Automatic session recovery timeline with restorable background checkpoints.
- Local performance dashboard for startup, memory, frame pacing, and DeX-like window metrics.
- Room migration from database version 13 to 14 for profile-scoped site data.

## [0.5.0] - 2026-08-29

### Added

- Biometric or device-credential unlocking for the encrypted saved-login vault.
- Automatic saved-login vault locking when the application enters the background.
- Web Push subscription approval, encrypted subscription storage, revoke controls, and Gecko push-event bridging.
- Android website notification delivery with notification click routing back to GeckoView.
- Page printing through the Android print framework.
- Bounded page HTML export through GeckoView page extraction.
- An Open in new window tab action with a dedicated resizable browser document activity.
- Keyboard, drag-and-drop URL, and browser navigation support in separate browser windows.
- PWA manifest refresh, same-origin icon loading, icon-backed launcher shortcuts, and standalone display mode.
- Room migration from database version 11 to 12 for PWA icon metadata.

### Changed

- Release version is now `0.5.0` with version code `6`.
- PWA windows use immersive system bars and standalone Gecko display mode.
- Browser background handling flushes Gecko session state before locking saved credentials.

## [0.4.1] - 2026-08-29

### Added

- Full-page Downloads navigation with search and filters for status, file type, and private-tab downloads.
- Full-page Bookmarks navigation with folder filtering, import, export, editing, moving, and deletion.
- Full-page History navigation with search, per-entry deletion, and clear-all actions.
- A Keyboard Shortcuts subpage so the main Settings page remains compact.
- A Back action from the Keyboard Shortcuts subpage to Settings.

### Changed

- Bookmarks and History are now separate navigation destinations instead of one combined library page.

## [0.4.0] - 2026-08-29

### Added

- Named tab session snapshots with restore and deletion.
- DeX drag-and-drop URL opening and expanded browser keyboard shortcuts.
- User-selected SAF download folders and download search/status/type filters.
- Per-site "forget this site" cleanup and live ad/tracker blocking statistics.
- Encrypted local login storage with GeckoView autocomplete integration.
- Installed PWA management with separate Android document tasks and windows.

### Changed

- Private tabs remain excluded from saved sessions, history, favicon fetching, saved credentials, and named session snapshots.
- Release version is now `0.4.0` with version code `4`.
- Release builds continue to use R8 shrinking with GeckoView optimization disabled for the tested DeX startup workaround.

## [0.3.0] - 2026-08-29

### Added

- Smart address bar suggestions from bookmarks and normal browsing history with keyboard navigation.
- Native reader mode with article extraction, font sizing, and light/dark/sepia palettes.
- Privacy dashboard for reviewing and clearing per-site or all GeckoView site data.
- Versioned browser backup and restore for local browsing data.
- Privacy-origin aggregation tests and expanded bookmark, history, and reading-list behavior.

### Changed

- Release version is now `0.3.0` with version code `3`.

## [0.2.0] - 2026-08-28

### Added

- Searchable tab switcher, command palette, configurable keyboard shortcuts, and hover previews.
- Per-site desktop mode, ad blocking, userscripts, and zoom overrides.
- Reading list with bounded offline article snapshots.
- QR URL sharing, PDF export, visible and bounded long screenshots.
- Picture-in-Picture for active media and validated PWA launcher shortcuts.
- Bookmark HTML parsing and expanded keyboard shortcut tests.

### Changed

- Release version is now `0.2.0` with version code `2`.
- GeckoView-based tab and extension behavior was consolidated for the first feature-complete internal build.

## [0.1.0] - 2026-08-26 to 2026-08-28

### Added

- Initial GeckoView Android browser shell with Jetpack Compose UI.
- Desktop and compact layouts for Samsung DeX and smaller screens.
- Horizontal and vertical tab strips with stable tab state.
- Back, forward, reload, home, address-bar navigation, and session persistence.
- Tab groups, tab reordering, pinning, sleeping, and inactive-tab hibernation.
- Two-pane split view with focus, swap, and close actions.
- Mouse secondary-click context menus for pages, links, media, and tabs.
- Host-based ad blocking and userscript support through a built-in WebExtension.
- Firefox Add-on installation from Mozilla-signed AMO packages.
- Durable downloads with WorkManager, pause/resume, retry, progress, speed, notifications, and MediaStore publishing.
- Bookmark folders, HTML import/export, history search, pruning, and deletion.
- GeckoView tracking protection, Global Privacy Control, DNS over HTTPS, and origin-scoped site permissions.
- Crash reports for Java exceptions and Gecko content-process failures.
- Explicit navigation policy for safe top-level URLs and external links.

### Changed

- Release version was established as `0.1.0` with version code `1`.
- GeckoView was upgraded to the pinned `154.0.20260824154132` engine before the later feature releases.

## Full Commit History

### 2026-08-26

- `9b6849e` `initial commit`: established the Android/Compose/GeckoView browser shell, Room and DataStore persistence, navigation, tabs, and baseline URL tests.
- `34b49ac` `feat: desktop view and sidebar url`: added the desktop browser layout, sidebar URL/address handling, and desktop settings state.
- `458cb72` `ui: fix top bar height and position`: corrected the top-bar layout, sizing, and placement.
- `de5cd48` `feat: add adblock`: added the built-in WebExtension host blocker, filter settings, and blocker UI.
- `778fdb3` `fix: youtube not playing`: adjusted blocker behavior and page integration so YouTube media continued to load.
- `1d1ed79` `feat: download manager`: added download records, download UI, external response handling, and file management.
- `2dbd0e3` `feat: download pause resume`: added the download engine, WorkManager worker, pause/resume flow, FileProvider, and recovery state.
- `b175227` `fix: nav shortcut and download speed`: added navigation shortcuts and corrected download speed/progress reporting.
- `e55f543` `fix: 99& rate adblock on d3ward`: improved filter parsing and cosmetic blocker behavior for compatibility tests.
- `b689069` `feat: add userscsript support`: added HTTPS userscript storage, metadata parsing, injection, and settings.
- `47c4fa8` `feat: add firefox addons`: added Firefox Add-ons URL parsing, signed XPI installation, extension prompts, actions, and tests.

### 2026-08-27

- `a0c6eff` `fix: mouse right click`: added and corrected mouse secondary-click handling for browser content and tabs.
- `655ce85` `fix: extension and crash handling`: hardened extension lifecycle handling and added Java crash report capture.
- `bb3bef9` `fix crash when auto popup`: fixed crashes caused by automatic extension popup handling.
- `805d827` `fix: unlock extentions options`: enabled extension options pages and corrected their navigation behavior.
- `b1e0808` `fix: mouse right click navigation`: expanded right-click navigation actions for links and page content.
- `27fdef6` `fix add-ons detection on AMO`: corrected Mozilla Add-ons download detection and installation routing.
- `8eb1d60` `fix flashing when new/close tabs`: reduced visual flashing during tab creation and closure.
- `84b7df3` `fix: tabs mouse right functions`: completed tab context-menu actions for mouse workflows.
- `a7605cd` `fix context menu position`: corrected context-menu placement against window bounds.
- `17016a0` `fix: flashing if switch between tabs`: reduced flashing while switching active Gecko sessions.
- `676c11a` `fix viewport addons context menu`: corrected extension context-menu behavior in the browser viewport.
- `c760c9b` `feat: more desktop shortcuts`: added more DeX keyboard shortcuts and command routing.
- `3ec78f8` `fix main alltabs dropdown`: added and corrected the desktop all-tabs dropdown.
- `4c1124b` `fix speed scroll on main all tabs`: corrected mouse-wheel scrolling in the all-tabs dropdown.
- `a99098e` `feat: vertical tabs`: added the collapsible vertical tab strip and its desktop layout.
- `e8fb791` `fix url bar flexible width`: made the desktop address bar adapt to available window width.
- `d3e1d58` `feat: tab group, import bookmark, hibernate tab`: added tab groups, bookmark import, and tab hibernation behavior.
- `6ed641e` `fix: reorder vertical tabs`: corrected vertical tab drag reordering.
- `f3b9b78` `fix tab vertical reorder`: fixed a second vertical reorder edge case and persistence behavior.
- `c53f11c` `fix and add some features`: hardened navigation policy, downloads, permissions, database state, and background workers.

### 2026-08-28

- `715114a` `feat: ready to release`: prepared the first internal release with durable downloads, crash handling, release shrinking, signing configuration, and browser identity controls.
- `954e6e9` `chore: init docs`: added `AGENT.md` and the initial project documentation.
- `40d2ce7` `fix: split view`: added and corrected two-pane split view lifecycle, focus, swap, and close behavior.
- `ce5a7c1` `fix: press F to fullscreen`: corrected the F-key fullscreen behavior and removed the conflicting page-level implementation.
- `3c4771f` `Fix: fullscreen on video sites`: corrected GeckoView video fullscreen state, system bars, and session activity.
- `b205469` `refactor: upgrade to GeckoEngine 154`: upgraded and consolidated GeckoView 154 integration while preserving the tested runtime configuration.
- `e40b090` `feat: add a bunch of stuffs lol`: added command palette, tab switcher, shortcut configuration, reader-adjacent sharing tools, offline reading, PWA shortcuts, and expanded browser actions.

### 2026-08-29

- `3883ca9` `add more features`: added address suggestions, reader mode, privacy dashboard, backup/restore, site settings, privacy-origin aggregation, and related tests.
- `69bdf7e` `feat: add more func`: added named sessions, download folders and filters, encrypted credentials, blocker statistics, forget-site cleanup, PWA management, and DeX URL drop support.
- `1c24d13` `separate history and download`: changed library navigation to full-page Bookmarks, History, and Downloads destinations and moved keyboard shortcuts into a Settings subpage.
- `6921c19` `add bunch of features [0.5.0]`: added biometric vault locking, Web Push and website notifications, printing, HTML export, PWA icons/refresh/fullscreen, separate browser windows, and final release hardening.

### Verification

- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`
- Release APK signature verified with the existing Dextra release certificate.
