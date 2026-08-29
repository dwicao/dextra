# Changelog

All notable changes to Dextra are documented here.

## [0.5.0] - 2026-08-29

### Added

- Added biometric or device-credential unlocking for the encrypted saved-login vault.
- Added automatic saved-login vault locking when the application enters the background.
- Added Web Push subscription approval, encrypted subscription storage, revoke controls, and Gecko push-event bridging.
- Added Android website notification delivery with notification click routing back to GeckoView.
- Added page printing through the Android print framework.
- Added bounded page HTML export through GeckoView page extraction.
- Added an Open in new window tab action with a dedicated resizable browser document activity.
- Added keyboard, drag-and-drop URL, and browser navigation support in separate browser windows.
- Added PWA manifest refresh, same-origin icon loading, icon-backed launcher shortcuts, and standalone display mode.
- Added Room migration from database version 11 to 12 for PWA icon metadata.

### Changed

- Release version is now `0.5.0` with version code `6`.
- PWA windows now use immersive system bars and standalone Gecko display mode.
- Browser background handling flushes Gecko session state before locking saved credentials.

## [0.4.1] - 2026-08-29

### Added

- Added full-page Downloads navigation with search and filters for status, file type, and private-tab downloads.
- Added full-page Bookmarks navigation with folder filtering, import, export, editing, moving, and deletion.
- Added full-page History navigation with search, per-entry deletion, and clear-all actions.
- Added a Keyboard Shortcuts subpage so the main Settings page remains compact.
- Added a Back action from the Keyboard Shortcuts subpage to Settings.

## [0.4.0] - 2026-08-29

### Added

- Added Samsung DeX-oriented desktop and compact layouts.
- Added horizontal and vertical tab strips with stable tab IDs.
- Added tab groups with rename, collapse, move, and delete actions.
- Added tab reordering, pinning, sleeping, hibernation, hover previews, and context menus.
- Added two-pane split view with focus, swap, and close actions.
- Added searchable tab switching and a command palette.
- Added configurable keyboard shortcuts for browser actions.
- Added address-bar suggestions from bookmarks and normal browsing history.
- Added homepage, search engine, theme, desktop-site, and tab layout settings.
- Added per-site desktop mode, ad blocking, userscripts, and zoom overrides.
- Added DNS over HTTPS provider selection.
- Added GeckoView tracking protection and Global Privacy Control.
- Added origin-scoped site permission decisions and privacy dashboard controls.
- Added HTTPS-only remote ad-block filter lists and userscripts.
- Added Mozilla-signed Firefox extension installation, updates, private-tab access, and removal.
- Added durable WorkManager downloads with resume validation, pause, cancel, retry, notifications, and MediaStore publishing.
- Added user-selected SAF download folders.
- Added bookmark folders with bounded HTML import/export.
- Added history search, pruning, and deletion.
- Added reading list entries with bounded offline article snapshots.
- Added native reader mode with article extraction, font sizing, and light/dark/sepia palettes.
- Added QR URL sharing, page sharing, PDF export, visible screenshots, and bounded long screenshots.
- Added Picture-in-Picture support for active media.
- Added validated PWA installation and launcher shortcuts.
- Added installed PWA management and separate Android document tasks/windows.
- Added local encrypted login storage using Android Keystore AES-GCM.
- Added GeckoView autocomplete integration for saved logins.
- Added ad-block and Gecko tracker blocking statistics.
- Added per-site "Forget this site" cleanup for site data, permissions, history, reading list, downloads, and saved logins.
- Added Java crash reports and Gecko content-process failure reports.
- Added explicit Room migration from database version 10 to 11.
- Added PWA data to browser backup and restore.
- Added regression coverage for privacy blocking statistics and keyboard shortcut behavior.

### Changed

- Private tabs remain excluded from normal session restore, history, favicon fetching, saved credentials, and named session snapshots.
- Release version is now `0.4.1` with version code `5`.
- Release builds continue to use R8 shrinking with GeckoView optimization disabled for the tested DeX startup workaround.

### Verification

- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`
- Release APK signature verified with the existing Dextra release certificate.
