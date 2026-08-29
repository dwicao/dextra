package com.dwicao.dextra.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.view.MotionEvent
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwicao.dextra.MainActivity
import com.dwicao.dextra.browser.BrowserTabState
import com.dwicao.dextra.browser.BrowserContextMenu
import com.dwicao.dextra.browser.BrowserUrl
import com.dwicao.dextra.browser.BrowserViewModel
import com.dwicao.dextra.browser.BrowserOverlay
import com.dwicao.dextra.browser.SecurityDiagnostics
import com.dwicao.dextra.browser.PerformanceMetrics
import com.dwicao.dextra.browser.NetworkActivity
import com.dwicao.dextra.browser.CompatibilityEvent
import com.dwicao.dextra.browser.sitePermissionLabel
import com.dwicao.dextra.browser.PrivacyOrigin
import com.dwicao.dextra.browser.AddressSuggestionSource
import com.dwicao.dextra.browser.NavigationPolicy
import com.dwicao.dextra.browser.BrowserCommand
import com.dwicao.dextra.browser.BrowserCommandId
import com.dwicao.dextra.browser.KeyChord
import com.dwicao.dextra.browser.QrCodeGenerator
import com.dwicao.dextra.browser.BrowserCommands
import com.dwicao.dextra.browser.CommandPaletteState
import com.dwicao.dextra.browser.ContextMenuAction
import com.dwicao.dextra.browser.DefaultKeyboardShortcuts
import com.dwicao.dextra.browser.ExtensionInstallPrompt
import com.dwicao.dextra.browser.ExtensionPopupState
import com.dwicao.dextra.browser.ExtensionToolbarAction
import com.dwicao.dextra.browser.ExtensionUpdatePrompt
import com.dwicao.dextra.browser.FindInPageState
import com.dwicao.dextra.browser.PageTranslationState
import com.dwicao.dextra.browser.WebAuthnPromptState
import com.dwicao.dextra.browser.InstalledExtension
import com.dwicao.dextra.browser.MediaPermissionPrompt
import com.dwicao.dextra.browser.OfflineArticle
import com.dwicao.dextra.browser.ReaderModeState
import com.dwicao.dextra.browser.TabSwitcherState
import com.dwicao.dextra.browser.buildAddressSuggestions
import com.dwicao.dextra.browser.buildPrivacyOrigins
import com.dwicao.dextra.data.SavedTabGroup
import com.dwicao.dextra.data.AdBlockFilter
import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.HistoryEntry
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import com.dwicao.dextra.data.InstalledWebApp
import com.dwicao.dextra.data.SiteSetting
import com.dwicao.dextra.data.SitePermission
import com.dwicao.dextra.data.TabWorkspace
import com.dwicao.dextra.data.StoredCredential
import com.dwicao.dextra.data.StoredAddress
import com.dwicao.dextra.data.StoredWebPushSubscription
import com.dwicao.dextra.data.DnsProvider
import com.dwicao.dextra.data.SearchEngine
import com.dwicao.dextra.data.CustomSearchEngine
import com.dwicao.dextra.data.ThemeMode
import com.dwicao.dextra.data.WebDavSettingsState
import com.dwicao.dextra.data.SyncSelection
import com.dwicao.dextra.data.SyncPreview
import com.dwicao.dextra.data.StartPageLink
import com.dwicao.dextra.data.StartPageSettings
import com.dwicao.dextra.ui.LocalDextraAccessibility
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.ScreenLength
import kotlin.math.roundToInt
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DextraApp(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
    val allDownloads by viewModel.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val downloads = allDownloads.filter { it.workspaceId == state.settings.activeWorkspaceId }
    val readingList by viewModel.readingList.collectAsStateWithLifecycle(initialValue = emptyList())
    val sitePermissions by viewModel.sitePermissions.collectAsStateWithLifecycle(initialValue = emptyList())
    val siteSettings by viewModel.siteSettings.collectAsStateWithLifecycle(initialValue = emptyList())
    val installedWebApps by viewModel.installedWebApps.collectAsStateWithLifecycle(initialValue = emptyList())
     val visibleWebPushSubscriptions = state.webPushSubscriptions.filter {
         it.profileId == state.settings.activeWorkspaceId
     }
    val profileSitePermissions = sitePermissions.filter { it.profileId == state.settings.activeWorkspaceId }
    val profileSiteSettings = siteSettings.filter { it.profileId == state.settings.activeWorkspaceId }
    val privacyOrigins = buildPrivacyOrigins(profileSitePermissions, profileSiteSettings, state.blockerStats.byOrigin)
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.resolveAndroidPermission(result.values.all { it })
    }
    val bookmarkExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri -> uri?.let(viewModel::exportBookmarks) }
    val bookmarkImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBookmarks) }
    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBackup) }
    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let(viewModel::exportPdf) }
    val htmlExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri -> uri?.let(viewModel::exportHtml) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val downloadDirectoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::setDownloadDirectory) }
    val appContext = LocalContext.current
    val activity = appContext as? Activity
    val mainActivity = activity as? MainActivity
    var pendingSyncExportPassphrase by remember { mutableStateOf<String?>(null) }
    var pendingSyncExportSelection by remember { mutableStateOf(SyncSelection()) }
    var pendingSyncImportPassphrase by remember { mutableStateOf<String?>(null) }
    val syncExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
                  pendingSyncExportPassphrase?.let { passphrase ->
                      val selection = pendingSyncExportSelection
                      pendingSyncExportPassphrase = null
                      uri?.let { viewModel.exportSync(it, passphrase, selection) }
                  }
    }
    val syncImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingSyncImportPassphrase?.let { passphrase ->
            pendingSyncImportPassphrase = null
            uri?.let { viewModel.previewSync(it, passphrase) }
        }
    }
    val isWebFullScreen = state.tabs.any { it.isFullScreen }
    val isImmersive = isWebFullScreen || state.standalonePwa || state.standaloneWindow

    LaunchedEffect(activity, isImmersive) {
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isImmersive) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = if (it == "Download started") "Downloads" else null,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && it == "Download started") {
                viewModel.setOverlay(BrowserOverlay.DOWNLOADS)
            }
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.markFirstFrame()
    }
    LaunchedEffect(state.credentialUnlockRequest) {
        if (state.credentialUnlockRequest > 0) mainActivity?.authenticateCredentialVault()
    }
    LaunchedEffect(state.androidPermission?.id) {
        state.androidPermission?.let { permissionLauncher.launch(it.permissions.toTypedArray()) }
    }
    LaunchedEffect(state.snackbar) {
        if ((state.snackbar == "Download started" || state.snackbar?.startsWith("Web Push enabled") == true || state.snackbar == "Media controls enabled") &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DextraTheme(
        themeMode = state.settings.themeMode,
        accessibilityTextScale = state.settings.accessibilityTextScale,
        highContrast = state.settings.highContrast,
        reduceMotion = state.settings.reduceMotion,
    ) {
        Scaffold(
            modifier = Modifier.testTag("browser_surface"),
            snackbarHost = {},
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
             BrowserScreen(
                modifier = Modifier.padding(padding),
                snackbarHostState = snackbarHostState,
                state = state,
                 history = history,
                  bookmarks = bookmarks,
                  downloads = downloads,
                 installedWebApps = installedWebApps,
                  privacyOrigins = privacyOrigins,
                  sitePermissions = profileSitePermissions,
                  onSetSitePermission = viewModel::setSitePermission,
                  onOpenSecurityDiagnostics = viewModel::openSecurityDiagnostics,
                  onOpenPerformanceDashboard = viewModel::openPerformanceDashboard,
                  onCopyPerformanceReport = viewModel::copyPerformanceReport,
                  onOpenNetworkInspector = viewModel::openNetworkInspector,
                  onClearNetworkActivity = viewModel::clearNetworkActivity,
                  onShareCurrentWorkspaceTabs = viewModel::shareCurrentWorkspaceTabs,
                  onOpenMediaManager = viewModel::openMediaManager,
                  onControlMedia = viewModel::controlMedia,
                  onOpenCompatibilityDiagnostics = viewModel::openCompatibilityDiagnostics,
                  onClearCompatibilityEvents = viewModel::clearCompatibilityEvents,
                  workspaces = state.settings.workspaces,
                  activeWorkspaceId = state.settings.activeWorkspaceId,
                  onCreateWorkspace = viewModel::createWorkspace,
                  onSwitchWorkspace = viewModel::switchWorkspace,
                  onRenameWorkspace = viewModel::renameWorkspace,
                  onDeleteWorkspace = viewModel::deleteWorkspace,
                  onNavigate = viewModel::navigateActive,
                 onHome = { viewModel.navigateActive(state.settings.homepage) },
                 onCloseSplit = viewModel::closeSplit,
                 onSwapSplit = viewModel::swapSplit,
                 onFocusSplitPane = viewModel::focusSplitPane,
                onBack = viewModel::goBack,
                onForward = viewModel::goForward,
                onReload = viewModel::reloadOrStop,
                onReloadCrashedTab = viewModel::reloadCrashedTab,
                onNewTab = { viewModel.createTab() },
                 onNewPrivateTab = viewModel::createPrivateTab,
                 onReopenClosedTab = viewModel::reopenClosedTab,
                 onToggleTabAudio = viewModel::toggleTabAudio,
                  onSelectTab = viewModel::selectTab,
                  onCloseTab = viewModel::closeTab,
                  onOpenTabInSplit = viewModel::openTabInSplit,
                  onMoveTabBefore = viewModel::moveTabBefore,
                 onMoveTabAfter = viewModel::moveTabAfter,
                 onCreateTabGroup = viewModel::createTabGroup,
                 onMoveTabToGroup = viewModel::moveTabToGroup,
                 onRenameTabGroup = viewModel::renameTabGroup,
                 onToggleTabGroup = viewModel::toggleTabGroup,
                 onDeleteTabGroup = viewModel::deleteTabGroup,
                 onToggleTabSleeping = viewModel::toggleTabSleeping,
                 onHibernateInactiveTabs = viewModel::hibernateInactiveTabs,
                 onToggleBookmark = viewModel::toggleBookmark,
                 onSetBookmarkFolder = viewModel::setBookmarkFolder,
                 onUpdateBookmark = viewModel::updateBookmark,
                 onDeleteBookmark = viewModel::deleteBookmark,
                 onExportBookmarks = { bookmarkExportLauncher.launch("dextra-bookmarks.html") },
                  onImportBookmarks = { bookmarkImportLauncher.launch(arrayOf("text/html", "text/*")) },
                  onExportBackup = { backupExportLauncher.launch("dextra-backup.json") },
                  onImportBackup = { backupImportLauncher.launch(arrayOf("application/json", "text/*")) },
                 onOpenSavedPage = viewModel::openSavedPage,
                 onClearHistory = viewModel::clearHistory,
                 onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                onSetOverlay = viewModel::setOverlay,
                onDismissOverlay = viewModel::dismissOverlay,
                onSetTheme = viewModel::setThemeMode,
                 onSetSearchEngine = viewModel::setSearchEngine,
                 customSearchEngines = state.settings.customSearchEngines,
                 selectedCustomSearchEngineId = state.settings.selectedCustomSearchEngineId,
                 onSetCustomSearchEngine = viewModel::setCustomSearchEngine,
                 onAddCustomSearchEngine = viewModel::addCustomSearchEngine,
                 onRemoveCustomSearchEngine = viewModel::removeCustomSearchEngine,
                  onExportSync = { passphrase, selection ->
                      pendingSyncExportPassphrase = passphrase
                      pendingSyncExportSelection = selection
                      syncExportLauncher.launch("dextra-sync.json")
                 },
                 onImportSync = { passphrase ->
                     pendingSyncImportPassphrase = passphrase
                     syncImportLauncher.launch(arrayOf("application/octet-stream", "application/json", "text/*"))
                 },
                 accessibilityTextScale = state.settings.accessibilityTextScale,
                 highContrast = state.settings.highContrast,
                 reduceMotion = state.settings.reduceMotion,
                 onSetAccessibilityTextScale = viewModel::setAccessibilityTextScale,
                 onSetHighContrast = viewModel::setHighContrast,
                 onSetReduceMotion = viewModel::setReduceMotion,
                 webDav = state.webDav,
                 onSaveWebDavSettings = viewModel::saveWebDavSettings,
                  onDisableWebDav = viewModel::disableWebDavSync,
                  onRunWebDavSync = viewModel::runWebDavSyncNow,
                  onResolveWebDavConflict = viewModel::resolveWebDavConflict,
                   onSetDesktopSites = viewModel::setDesktopSites,
                   httpsOnly = state.settings.httpsOnly,
                   onSetHttpsOnly = viewModel::setHttpsOnly,
                   cookieBannerMode = state.settings.cookieBannerMode,
                   onSetCookieBannerMode = viewModel::setCookieBannerMode,
                   historyRetentionDays = state.settings.historyRetentionDays,
                   downloadRetentionDays = state.settings.downloadRetentionDays,
                   recoveryRetentionDays = state.settings.recoveryRetentionDays,
                   clearSiteDataOnExit = state.settings.clearSiteDataOnExit,
                   privacyCleanupAllowlist = state.settings.privacyCleanupAllowlist.toList(),
                   onSetHistoryRetentionDays = viewModel::setHistoryRetentionDays,
                   onSetDownloadRetentionDays = viewModel::setDownloadRetentionDays,
                   onSetRecoveryRetentionDays = viewModel::setRecoveryRetentionDays,
                   onSetClearSiteDataOnExit = viewModel::setClearSiteDataOnExit,
                   onAddPrivacyCleanupAllowlist = viewModel::addPrivacyCleanupAllowlist,
                   onRemovePrivacyCleanupAllowlist = viewModel::removePrivacyCleanupAllowlist,
                   onRunPrivacyCleanup = viewModel::runPrivacyCleanupNow,
                   onSetHomepage = viewModel::setHomepage,
                   startPage = state.settings.startPage,
                   onSetStartPageQuickLinks = viewModel::setStartPageQuickLinks,
                   onSetStartPagePrivacyTip = viewModel::setStartPagePrivacyTip,
                   onAddStartPageLink = viewModel::addStartPageLink,
                   onRemoveStartPageLink = viewModel::removeStartPageLink,
                   onClearSitePermissions = viewModel::clearSitePermissions,
                   onOpenPrivacyDashboard = { viewModel.setOverlay(BrowserOverlay.PRIVACY) },
                    onClearSiteData = viewModel::clearSiteData,
                    onForgetSite = viewModel::forgetSite,
                    onClearAllSiteData = viewModel::clearAllSiteData,
                   onSetTabBarWithAddressBar = viewModel::setTabBarWithAddressBar,
                   onSetVerticalTabs = viewModel::setVerticalTabs,
                   onPickDownloadDirectory = { downloadDirectoryLauncher.launch(state.settings.downloadDirectoryUri?.let(Uri::parse)) },
                   onResetDownloadDirectory = { viewModel.setDownloadDirectory(null) },
                  shortcutBindings = state.settings.shortcutBindings,
                  onSetKeyboardShortcut = { command, chord -> viewModel.setKeyboardShortcut(command, chord) },
                  capturingShortcut = state.capturingShortcut,
                  onBeginKeyboardShortcutCapture = viewModel::beginKeyboardShortcutCapture,
                  onCancelKeyboardShortcutCapture = viewModel::cancelKeyboardShortcutCapture,
                 onSetDnsOverHttpsEnabled = viewModel::setDnsOverHttpsEnabled,
                 onSetDnsProvider = viewModel::setDnsProvider,
                 onSetAdBlockingEnabled = viewModel::setAdBlockingEnabled,
                 onSetAdBlockFilterEnabled = viewModel::setAdBlockFilterEnabled,
                 onRefreshAdBlockFilters = viewModel::refreshAdBlockFilters,
                 onAddAdBlockFilter = viewModel::addAdBlockFilter,
                 onRemoveAdBlockFilter = viewModel::removeAdBlockFilter,
                 onAddUserScript = viewModel::addUserScript,
                  onSetUserScriptEnabled = viewModel::setUserScriptEnabled,
                  onRefreshUserScripts = viewModel::refreshUserScripts,
                  onRemoveUserScript = viewModel::removeUserScript,
                  readingList = readingList,
                  onToggleReadingList = viewModel::toggleReadingList,
                  onDeleteReadingListEntry = viewModel::deleteReadingListEntry,
                  onSetReadingListRead = viewModel::setReadingListRead,
                  onOpenSiteSettings = viewModel::openSiteSettings,
                  onOpenCommandPalette = viewModel::openCommandPalette,
                  onOpenTabSwitcher = viewModel::openTabSwitcher,
                  onCloseSiteSettings = viewModel::closeSiteSettings,
                  onSetSiteDesktopOverride = viewModel::setCurrentSiteDesktopOverride,
                  onSetSiteAdBlockingOverride = viewModel::setCurrentSiteAdBlockingOverride,
                  onSetSiteUserScriptsOverride = viewModel::setCurrentSiteUserScriptsOverride,
                   onSetSiteZoomOverride = viewModel::setCurrentSiteZoomOverride,
                   onSetSiteHttpsOnly = viewModel::setCurrentSiteHttpsOnly,
                   onSetSiteCookieBannerMode = viewModel::setCurrentSiteCookieBannerMode,
                  onClearSiteSettings = viewModel::clearCurrentSiteSettings,
                   onOpenQrCode = viewModel::openQrCode,
                    onOpenReaderMode = viewModel::openReaderMode,
                    onOpenTranslation = viewModel::openTranslation,
                    onSharePage = viewModel::shareActiveUrl,
                  onExportPdf = { pdfExportLauncher.launch("dextra-page.pdf") },
                  onPrintPage = viewModel::printActivePage,
                  onExportHtml = { htmlExportLauncher.launch("dextra-page.html") },
                  onSaveScreenshot = viewModel::saveScreenshot,
                  onInstallWebApp = viewModel::installCurrentWebApp,
                   onEnterPictureInPicture = {
                       if (state.tabs.firstOrNull { it.id == state.activeTabId }?.let { it.hasActiveMedia && !it.isPrivate } == true) {
                           mainActivity?.enterBrowserPictureInPicture()
                       }
                   },
                    onSaveOffline = viewModel::saveCurrentPageOffline,
                    onOpenOffline = viewModel::openOfflineArticle,
                     sessionSnapshots = state.settings.sessionSnapshots,
                     sessionTimeline = state.settings.sessionTimeline,
                     onCreateSessionSnapshot = viewModel::createSessionSnapshot,
                     onRestoreSessionSnapshot = viewModel::restoreSessionSnapshot,
                     onDeleteSessionSnapshot = viewModel::deleteSessionSnapshot,
                     onDeleteSessionTimeline = viewModel::deleteSessionTimeline,
                   credentials = state.credentials,
                   credentialCount = state.credentialCount,
                  credentialVaultUnlocked = state.credentialVaultUnlocked,
                    addresses = state.addresses,
                   webPushSubscriptions = visibleWebPushSubscriptions,
                   onDeleteCredential = viewModel::deleteCredential,
                   onRequestCredentialUnlock = viewModel::requestCredentialUnlock,
                   onLockCredentialVault = viewModel::lockCredentialVault,
                    onClearCredentials = viewModel::clearCredentials,
                    onDeleteAddress = viewModel::deleteAddress,
                    onSaveAddress = viewModel::saveAddress,
                    onClearAddresses = viewModel::clearAddresses,
                    onCopyCredentialUsername = viewModel::copyCredentialUsername,
                   onCopyCredentialPassword = viewModel::copyCredentialPassword,
                   onResolveWebPushPrompt = viewModel::resolveWebPushPrompt,
                   onRevokeWebPushSubscription = viewModel::revokeWebPushSubscription,
                   onClearWebPushSubscriptions = viewModel::clearWebPushSubscriptions,
                   onOpenInstalledWebApp = viewModel::openInstalledWebApp,
                   onRefreshInstalledWebApp = viewModel::refreshInstalledWebApp,
                   onUninstallWebApp = viewModel::uninstallWebApp,
                installedExtensions = state.installedExtensions,
                extensionInstallInProgress = state.extensionInstallInProgress,
                onInstallExtension = viewModel::installExtension,
                onOpenFirefoxAddons = viewModel::openFirefoxAddons,
                 onSetExtensionEnabled = viewModel::setExtensionEnabled,
                  onSetExtensionPrivateBrowsing = viewModel::setExtensionPrivateBrowsing,
                  onRevokeExtensionOptionalPermissions = viewModel::revokeExtensionOptionalPermissions,
                  onUpdateExtension = viewModel::updateExtension,
                 onOpenExtensionOptions = viewModel::openExtensionOptions,
                 onUninstallExtension = viewModel::uninstallExtension,
                 extensionActions = state.extensionActions,
                 onClickExtensionAction = viewModel::clickExtensionAction,
                onOpenDownload = viewModel::openDownload,
                onShareDownload = viewModel::shareDownload,
                  onToggleDownload = viewModel::toggleDownload,
                  onSetDownloadPriority = viewModel::setDownloadPriority,
                  onSetDownloadWifiOnly = viewModel::setDownloadWifiOnly,
                  onScheduleDownload = viewModel::scheduleDownload,
                  onPauseAllDownloads = viewModel::pauseAllDownloads,
                  onResumeAllDownloads = viewModel::resumeAllDownloads,
                  onCancelDownload = viewModel::cancelDownload,
                 onRemoveDownload = viewModel::removeDownload,
                 onClearCompletedDownloads = viewModel::clearCompletedDownloads,
                 onResolvePermission = viewModel::resolveContentPermission,
                extensionInstallPrompt = state.extensionInstallPrompt,
                onResolveExtensionInstall = viewModel::resolveExtensionInstall,
                extensionUpdatePrompt = state.extensionUpdatePrompt,
                onResolveExtensionUpdate = viewModel::resolveExtensionUpdate,
                  onContextMenuAction = viewModel::handleContextMenuAction,
                  onDismissContextMenu = viewModel::dismissContextMenu,
                  onTabContextMenu = viewModel::showTabContextMenu,
                  extensionPopup = state.extensionPopup,
                  onCloseExtensionPopup = viewModel::closeExtensionPopup,
                  findInPage = state.findInPage,
                  onUpdateFindInPage = viewModel::updateFindInPage,
                  onFindNext = viewModel::findNext,
                   onCloseFindInPage = viewModel::closeFindInPage,
               )
             state.commandPalette?.let { palette ->
                 CommandPaletteDialog(
                     state = palette,
                     onQueryChange = viewModel::updateCommandPalette,
                     onMoveSelection = viewModel::moveCommandSelection,
                     onExecute = viewModel::executeCommand,
                     onDismiss = viewModel::closeCommandPalette,
                 )
             }
              state.tabSwitcher?.let { switcher ->
                 TabSwitcherDialog(
                     state = switcher,
                     tabs = state.tabs,
                     onQueryChange = viewModel::updateTabSwitcher,
                     onMoveSelection = viewModel::moveTabSelection,
                     onSelect = viewModel::selectTabFromSwitcher,
                     onDismiss = viewModel::closeTabSwitcher,
                  )
              }
               state.qrCodeUrl?.let { url ->
                  QrCodeDialog(
                      url = url,
                       onShare = viewModel::shareQrCode,
                      onDismiss = viewModel::closeQrCode,
                   )
               }
                state.offlineArticle?.let { article ->
                    OfflineArticleDialog(article, viewModel::closeOfflineArticle)
                }
                 state.readerMode?.let { reader ->
                    ReaderModeDialog(
                        reader = reader,
                        onShare = viewModel::shareActiveUrl,
                        onSaveOffline = viewModel::saveCurrentPageOffline,
                        onDismiss = viewModel::closeReaderMode,
                    )
                 }
                 state.translation?.let { translation ->
                     TranslationDialog(
                         translation = translation,
                         onTranslate = viewModel::translateActivePage,
                         onRestoreOriginal = viewModel::restoreOriginalPage,
                         onNeverTranslate = viewModel::neverTranslateCurrentSite,
                         onDismiss = viewModel::closeTranslation,
                     )
                 }
                 state.webAuthnPrompt?.let { prompt ->
                     WebAuthnDialog(prompt, viewModel::resolveWebAuthn)
                 }
             state.lastCrashReport?.let { report ->
                CrashReportDialog(
                    report = report,
                    onCopy = viewModel::copyCrashReport,
                    onDismiss = viewModel::dismissCrashReport,
                )
            }
             state.mediaPermission?.let { prompt ->
                 MediaPermissionDialog(
                     prompt = prompt,
                     onResolve = viewModel::resolveMediaPermission,
                 )
             }
               state.webPushPrompt?.let { prompt ->
                  WebPushPermissionDialog(
                      origin = prompt.origin,
                      onResolve = viewModel::resolveWebPushPrompt,
                  )
              }
              state.syncPreview?.let { preview ->
                  SyncPreviewDialog(
                      preview = preview.preview,
                      onConfirm = viewModel::confirmSyncImport,
                      onDismiss = viewModel::dismissSyncPreview,
                  )
              }
              if (state.syncPreviewLoading) {
                  AlertDialog(
                      onDismissRequest = viewModel::dismissSyncPreview,
                      title = { Text("Reading encrypted sync") },
                      text = { CircularProgressIndicator() },
                      confirmButton = {},
                      dismissButton = { TextButton(onClick = viewModel::dismissSyncPreview) { Text("Cancel") } },
                  )
              }
             if (state.extensionInstallInProgress && state.extensionInstallPrompt == null) {
                ExtensionInstallProgressDialog()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreen(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    state: com.dwicao.dextra.browser.BrowserUiState,
    history: List<HistoryEntry>,
    bookmarks: List<Bookmark>,
    downloads: List<DownloadEntry>,
    installedWebApps: List<InstalledWebApp>,
    privacyOrigins: List<PrivacyOrigin>,
    sitePermissions: List<SitePermission>,
    onSetSitePermission: (String, String, String) -> Unit,
    onOpenSecurityDiagnostics: () -> Unit,
    onOpenPerformanceDashboard: () -> Unit,
    onCopyPerformanceReport: () -> Unit,
    onOpenNetworkInspector: () -> Unit,
    onShareCurrentWorkspaceTabs: () -> Unit,
    onOpenMediaManager: () -> Unit,
     onControlMedia: (String, String) -> Unit,
    onOpenCompatibilityDiagnostics: () -> Unit,
    onClearCompatibilityEvents: () -> Unit,
    onClearNetworkActivity: () -> Unit,
    workspaces: List<TabWorkspace>,
    activeWorkspaceId: String,
    onCreateWorkspace: (String) -> Unit,
    onSwitchWorkspace: (String) -> Unit,
    onRenameWorkspace: (String, String) -> Unit,
    onDeleteWorkspace: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onCloseSplit: () -> Unit,
    onSwapSplit: () -> Unit,
    onFocusSplitPane: (Boolean) -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onReloadCrashedTab: () -> Unit,
     onNewTab: () -> Unit,
     onNewPrivateTab: () -> String,
     onReopenClosedTab: () -> Unit,
     onToggleTabAudio: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenTabInSplit: (String) -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onCreateTabGroup: (String?) -> Unit,
    onMoveTabToGroup: (String, String?) -> Unit,
    onRenameTabGroup: (String, String) -> Unit,
    onToggleTabGroup: (String) -> Unit,
    onDeleteTabGroup: (String) -> Unit,
    onToggleTabSleeping: (String) -> Unit,
    onHibernateInactiveTabs: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSetBookmarkFolder: (Bookmark, String?) -> Unit,
    onUpdateBookmark: (Bookmark, String, String?) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onExportBookmarks: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenSavedPage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit,
    onSetOverlay: (BrowserOverlay) -> Unit,
    onDismissOverlay: () -> Unit,
     onSetTheme: (ThemeMode) -> Unit,
     onSetSearchEngine: (SearchEngine) -> Unit,
     customSearchEngines: List<CustomSearchEngine>,
     selectedCustomSearchEngineId: String?,
     onSetCustomSearchEngine: (CustomSearchEngine) -> Unit,
     onAddCustomSearchEngine: (String, String) -> Unit,
     onRemoveCustomSearchEngine: (CustomSearchEngine) -> Unit,
      onExportSync: (String, SyncSelection) -> Unit,
     onImportSync: (String) -> Unit,
     accessibilityTextScale: Float,
     highContrast: Boolean,
     reduceMotion: Boolean,
     onSetAccessibilityTextScale: (Float) -> Unit,
     onSetHighContrast: (Boolean) -> Unit,
     onSetReduceMotion: (Boolean) -> Unit,
     webDav: WebDavSettingsState,
     onSaveWebDavSettings: (String, String, String, String, String, Int) -> Unit,
     onDisableWebDav: () -> Unit,
      onRunWebDavSync: () -> Unit,
      onResolveWebDavConflict: (String) -> Unit,
      onSetDesktopSites: (Boolean) -> Unit,
      httpsOnly: Boolean,
      onSetHttpsOnly: (Boolean) -> Unit,
      cookieBannerMode: Int,
      onSetCookieBannerMode: (Int) -> Unit,
      historyRetentionDays: Int,
      downloadRetentionDays: Int,
      recoveryRetentionDays: Int,
      clearSiteDataOnExit: Boolean,
      privacyCleanupAllowlist: List<String>,
      onSetHistoryRetentionDays: (Int) -> Unit,
      onSetDownloadRetentionDays: (Int) -> Unit,
      onSetRecoveryRetentionDays: (Int) -> Unit,
      onSetClearSiteDataOnExit: (Boolean) -> Unit,
      onAddPrivacyCleanupAllowlist: (String) -> Unit,
      onRemovePrivacyCleanupAllowlist: (String) -> Unit,
      onRunPrivacyCleanup: () -> Unit,
      startPage: StartPageSettings,
      onSetStartPageQuickLinks: (Boolean) -> Unit,
      onSetStartPagePrivacyTip: (Boolean) -> Unit,
      onAddStartPageLink: (String, String) -> Unit,
      onRemoveStartPageLink: (StartPageLink) -> Unit,
     onSetHomepage: (String) -> Unit,
     onClearSitePermissions: () -> Unit,
     onOpenPrivacyDashboard: () -> Unit,
      onClearSiteData: (String) -> Unit,
      onForgetSite: (String) -> Unit,
      onClearAllSiteData: () -> Unit,
      onSetTabBarWithAddressBar: (Boolean) -> Unit,
      onSetVerticalTabs: (Boolean) -> Unit,
      onPickDownloadDirectory: () -> Unit,
      onResetDownloadDirectory: () -> Unit,
     shortcutBindings: Map<BrowserCommandId, KeyChord>,
     onSetKeyboardShortcut: (BrowserCommandId, KeyChord?) -> Unit,
     capturingShortcut: BrowserCommandId?,
     onBeginKeyboardShortcutCapture: (BrowserCommandId) -> Unit,
     onCancelKeyboardShortcutCapture: () -> Unit,
    onSetDnsOverHttpsEnabled: (Boolean) -> Unit,
    onSetDnsProvider: (DnsProvider) -> Unit,
    onSetAdBlockingEnabled: (Boolean) -> Unit,
    onSetAdBlockFilterEnabled: (AdBlockFilter, Boolean) -> Unit,
    onRefreshAdBlockFilters: () -> Unit,
    onAddAdBlockFilter: (String) -> Unit,
    onRemoveAdBlockFilter: (AdBlockFilter) -> Unit,
    onAddUserScript: (String) -> Unit,
    onSetUserScriptEnabled: (String, Boolean) -> Unit,
    onRefreshUserScripts: () -> Unit,
     onRemoveUserScript: (String) -> Unit,
     readingList: List<com.dwicao.dextra.data.ReadingListEntry>,
     onToggleReadingList: () -> Unit,
     onDeleteReadingListEntry: (com.dwicao.dextra.data.ReadingListEntry) -> Unit,
     onSetReadingListRead: (com.dwicao.dextra.data.ReadingListEntry, Boolean) -> Unit,
      onOpenSiteSettings: () -> Unit,
      onOpenCommandPalette: () -> Unit,
      onOpenTabSwitcher: () -> Unit,
     onCloseSiteSettings: () -> Unit,
     onSetSiteDesktopOverride: (Boolean?) -> Unit,
     onSetSiteAdBlockingOverride: (Boolean?) -> Unit,
     onSetSiteUserScriptsOverride: (Boolean?) -> Unit,
      onSetSiteZoomOverride: (Int?) -> Unit,
      onSetSiteHttpsOnly: (Boolean?) -> Unit,
      onSetSiteCookieBannerMode: (Int?) -> Unit,
      onClearSiteSettings: () -> Unit,
       onOpenQrCode: () -> Unit,
       onOpenReaderMode: () -> Unit,
      onSharePage: () -> Unit,
        onExportPdf: () -> Unit,
        onPrintPage: () -> Unit,
        onExportHtml: () -> Unit,
       onSaveScreenshot: (android.graphics.Bitmap) -> Unit,
       onInstallWebApp: () -> Unit,
       onEnterPictureInPicture: () -> Unit,
        onSaveOffline: () -> Unit,
        onOpenOffline: (com.dwicao.dextra.data.ReadingListEntry) -> Unit,
         sessionSnapshots: List<com.dwicao.dextra.data.SessionSnapshot>,
         sessionTimeline: List<com.dwicao.dextra.data.SessionSnapshot>,
         onCreateSessionSnapshot: (String) -> Unit,
         onRestoreSessionSnapshot: (com.dwicao.dextra.data.SessionSnapshot) -> Unit,
         onDeleteSessionSnapshot: (com.dwicao.dextra.data.SessionSnapshot) -> Unit,
         onDeleteSessionTimeline: (com.dwicao.dextra.data.SessionSnapshot) -> Unit,
         credentials: List<StoredCredential>,
         addresses: List<StoredAddress>,
         credentialCount: Int,
        credentialVaultUnlocked: Boolean,
        webPushSubscriptions: List<StoredWebPushSubscription>,
        onDeleteCredential: (StoredCredential) -> Unit,
        onRequestCredentialUnlock: () -> Unit,
        onLockCredentialVault: () -> Unit,
         onClearCredentials: () -> Unit,
         onDeleteAddress: (StoredAddress) -> Unit,
         onSaveAddress: (StoredAddress) -> Unit,
         onClearAddresses: () -> Unit,
        onCopyCredentialUsername: (StoredCredential) -> Unit,
        onCopyCredentialPassword: (StoredCredential) -> Unit,
        onResolveWebPushPrompt: (Boolean) -> Unit,
        onRevokeWebPushSubscription: (StoredWebPushSubscription) -> Unit,
        onClearWebPushSubscriptions: () -> Unit,
        onOpenInstalledWebApp: (InstalledWebApp) -> Unit,
        onRefreshInstalledWebApp: (InstalledWebApp) -> Unit,
        onUninstallWebApp: (InstalledWebApp) -> Unit,
    installedExtensions: List<InstalledExtension>,
    extensionInstallInProgress: Boolean,
    onInstallExtension: (String) -> Unit,
    onOpenFirefoxAddons: () -> Unit,
    onSetExtensionEnabled: (String, Boolean) -> Unit,
     onSetExtensionPrivateBrowsing: (String, Boolean) -> Unit,
     onRevokeExtensionOptionalPermissions: (String) -> Unit,
     onUpdateExtension: (String) -> Unit,
    onOpenExtensionOptions: (String) -> Unit,
    onUninstallExtension: (String) -> Unit,
    extensionActions: List<ExtensionToolbarAction>,
    onClickExtensionAction: (String) -> Unit,
    onOpenDownload: (DownloadEntry) -> Unit,
    onShareDownload: (DownloadEntry) -> Unit,
    onToggleDownload: (DownloadEntry) -> Unit,
    onSetDownloadPriority: (DownloadEntry, Int) -> Unit,
    onSetDownloadWifiOnly: (DownloadEntry, Boolean) -> Unit,
    onScheduleDownload: (DownloadEntry, Long?) -> Unit,
    onPauseAllDownloads: () -> Unit,
    onResumeAllDownloads: () -> Unit,
    onCancelDownload: (DownloadEntry) -> Unit,
     onRemoveDownload: (DownloadEntry) -> Unit,
     onClearCompletedDownloads: () -> Unit,
    onResolvePermission: (Boolean, Boolean) -> Unit,
    extensionInstallPrompt: ExtensionInstallPrompt?,
    onResolveExtensionInstall: (Boolean, Boolean, Boolean) -> Unit,
    extensionUpdatePrompt: ExtensionUpdatePrompt?,
    onResolveExtensionUpdate: (Boolean) -> Unit,
    onContextMenuAction: (ContextMenuAction) -> Unit,
    onDismissContextMenu: () -> Unit,
     onTabContextMenu: (String, Int, Int) -> Unit,
    extensionPopup: ExtensionPopupState?,
    onCloseExtensionPopup: () -> Unit,
     findInPage: FindInPageState?,
    onUpdateFindInPage: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
     onCloseFindInPage: () -> Unit,
     onOpenTranslation: () -> Unit,
 ) {
    val activeTab = state.tabs.firstOrNull { it.id == state.activeTabId }
    val fullScreenTab = state.tabs.firstOrNull { it.isFullScreen }
    var menuExpanded by remember { mutableStateOf(false) }
    var screenshotView by remember { mutableStateOf<GeckoView?>(null) }
    val addressFocusRequester = remember { FocusRequester() }
    val captureScreenshot = {
        screenshotView?.capturePixels()?.accept(
            { bitmap -> bitmap?.let(onSaveScreenshot) },
            {},
        )
    }
    val captureLongScreenshot = {
        val view = screenshotView
        if (view != null) {
            val tiles = mutableListOf<Bitmap>()
            fun captureTile(index: Int) {
                view.capturePixels().accept(
                    { bitmap ->
                        if (bitmap == null) return@accept
                        tiles += bitmap
                        if (index < 3) {
                            view.session?.panZoomController?.scrollBy(
                                ScreenLength.zero(),
                                ScreenLength.fromVisualViewportHeight(0.85),
                                org.mozilla.geckoview.PanZoomController.SCROLL_BEHAVIOR_AUTO,
                            )
                            Handler(Looper.getMainLooper()).postDelayed({ captureTile(index + 1) }, 350L)
                        } else {
                            view.session?.panZoomController?.scrollTo(ScreenLength.zero(), ScreenLength.zero())
                            val overlap = (tiles.first().height * 0.15f).toInt()
                            val height = (tiles.sumOf { it.height } - overlap * (tiles.size - 1)).coerceAtMost(12_000)
                            val stitched = Bitmap.createBitmap(tiles.first().width, height, Bitmap.Config.ARGB_8888)
                            Canvas(stitched).apply {
                                var top = 0f
                                tiles.forEach { tile ->
                                    drawBitmap(tile, 0f, top, null)
                                    top += (tile.height - overlap).toFloat()
                                    if (tile !== tiles.last()) tile.recycle()
                                }
                            }
                            onSaveScreenshot(stitched)
                        }
                    },
                    {},
                )
            }
            view.session?.panZoomController?.scrollTo(ScreenLength.zero(), ScreenLength.zero())
            Handler(Looper.getMainLooper()).postDelayed({ captureTile(0) }, 250L)
        }
    }
    when (state.overlay) {
        BrowserOverlay.BOOKMARKS -> {
            BookmarksPage(
                bookmarks = bookmarks,
                onOpen = onOpenSavedPage,
                onSetBookmarkFolder = onSetBookmarkFolder,
                onUpdateBookmark = onUpdateBookmark,
                onDeleteBookmark = onDeleteBookmark,
                onExportBookmarks = onExportBookmarks,
                onImportBookmarks = onImportBookmarks,
                onBack = onDismissOverlay,
            )
            return
        }
        BrowserOverlay.HISTORY -> {
            HistoryPage(
                history = history,
                onOpen = onOpenSavedPage,
                onClearHistory = onClearHistory,
                onDeleteHistoryEntry = onDeleteHistoryEntry,
                onBack = onDismissOverlay,
            )
            return
        }
        BrowserOverlay.DOWNLOADS -> {
            DownloadsPage(
                downloads = downloads,
                onOpen = onOpenDownload,
                onShare = onShareDownload,
                onToggle = onToggleDownload,
                onSetPriority = onSetDownloadPriority,
                onSetWifiOnly = onSetDownloadWifiOnly,
                onSchedule = onScheduleDownload,
                onPauseAll = onPauseAllDownloads,
                onResumeAll = onResumeAllDownloads,
                onCancel = onCancelDownload,
                onRemove = onRemoveDownload,
                onClearCompleted = onClearCompletedDownloads,
                onBack = onDismissOverlay,
            )
            return
        }
        BrowserOverlay.KEYBOARD_SHORTCUTS -> {
            KeyboardShortcutsPage(
                shortcutBindings = shortcutBindings,
                onSetKeyboardShortcut = onSetKeyboardShortcut,
                capturingShortcut = capturingShortcut,
                onBeginKeyboardShortcutCapture = onBeginKeyboardShortcutCapture,
                onCancelKeyboardShortcutCapture = onCancelKeyboardShortcutCapture,
                onBack = { onSetOverlay(BrowserOverlay.SETTINGS) },
            )
            return
        }
        else -> Unit
    }
    if (state.standaloneWindow) {
        WindowedBrowserLayout(
            tab = activeTab,
            bookmarks = bookmarks,
            history = history,
            onNavigate = onNavigate,
            onHome = onHome,
            onBack = onBack,
            onForward = onForward,
            onReload = onReload,
            onToggleBookmark = onToggleBookmark,
            onReloadCrashedTab = onReloadCrashedTab,
            onViewReady = { id, view -> if (id == state.activeTabId) screenshotView = view },
            startPage = startPage,
            modifier = modifier,
        )
        return
    }
    if (state.standalonePwa) {
            BrowserViewport(
            tab = activeTab,
            onNavigate = onNavigate,
            onReloadCrashedTab = onReloadCrashedTab,
                onViewReady = { id, view -> if (id == state.activeTabId) screenshotView = view },
                startPage = startPage,
                modifier = modifier,
        )
        return
    }
    if (state.isPictureInPictureMode) {
            BrowserViewport(
            tab = activeTab,
            onNavigate = onNavigate,
            onReloadCrashedTab = onReloadCrashedTab,
                onViewReady = { id, view -> if (id == state.activeTabId) screenshotView = view },
                startPage = startPage,
                modifier = modifier,
        )
        return
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(top = if (fullScreenTab == null) 8.dp else 0.dp),
    ) {
        LaunchedEffect(state.addressFocusRequest) {
            runCatching { addressFocusRequester.requestFocus() }
        }
        if (state.overlay == BrowserOverlay.SETTINGS) {
            SettingsScreen(
                onBack = onDismissOverlay,
                themeMode = state.settings.themeMode,
                  searchEngine = state.settings.searchEngine,
                  onSetTheme = onSetTheme,
                  onSetSearchEngine = onSetSearchEngine,
                  customSearchEngines = customSearchEngines,
                  selectedCustomSearchEngineId = selectedCustomSearchEngineId,
                  onSetCustomSearchEngine = onSetCustomSearchEngine,
                  onAddCustomSearchEngine = onAddCustomSearchEngine,
                  onRemoveCustomSearchEngine = onRemoveCustomSearchEngine,
                  onExportSync = onExportSync,
                  onImportSync = onImportSync,
                  accessibilityTextScale = accessibilityTextScale,
                  highContrast = highContrast,
                  reduceMotion = reduceMotion,
                  onSetAccessibilityTextScale = onSetAccessibilityTextScale,
                  onSetHighContrast = onSetHighContrast,
                  onSetReduceMotion = onSetReduceMotion,
                  webDav = webDav,
                  onSaveWebDavSettings = onSaveWebDavSettings,
                   onDisableWebDav = onDisableWebDav,
                   onRunWebDavSync = onRunWebDavSync,
                   onResolveWebDavConflict = onResolveWebDavConflict,
                  desktopSites = state.settings.desktopSites,
                 tabBarWithAddressBar = state.settings.tabBarWithAddressBar,
                 verticalTabs = state.settings.verticalTabs,
                  onSetDesktopSites = onSetDesktopSites,
                  httpsOnly = httpsOnly,
                  onSetHttpsOnly = onSetHttpsOnly,
                  cookieBannerMode = cookieBannerMode,
                  onSetCookieBannerMode = onSetCookieBannerMode,
                  historyRetentionDays = historyRetentionDays,
                  downloadRetentionDays = downloadRetentionDays,
                  recoveryRetentionDays = recoveryRetentionDays,
                  clearSiteDataOnExit = clearSiteDataOnExit,
                  privacyCleanupAllowlist = privacyCleanupAllowlist,
                  onSetHistoryRetentionDays = onSetHistoryRetentionDays,
                  onSetDownloadRetentionDays = onSetDownloadRetentionDays,
                  onSetRecoveryRetentionDays = onSetRecoveryRetentionDays,
                  onSetClearSiteDataOnExit = onSetClearSiteDataOnExit,
                  onAddPrivacyCleanupAllowlist = onAddPrivacyCleanupAllowlist,
                  onRemovePrivacyCleanupAllowlist = onRemovePrivacyCleanupAllowlist,
                  onRunPrivacyCleanup = onRunPrivacyCleanup,
                  startPage = startPage,
                  onSetStartPageQuickLinks = onSetStartPageQuickLinks,
                  onSetStartPagePrivacyTip = onSetStartPagePrivacyTip,
                  onAddStartPageLink = onAddStartPageLink,
                  onRemoveStartPageLink = onRemoveStartPageLink,
                  downloadDirectoryUri = state.settings.downloadDirectoryUri,
                  onPickDownloadDirectory = onPickDownloadDirectory,
                  onResetDownloadDirectory = onResetDownloadDirectory,
                  homepage = state.settings.homepage,
                    onSetHomepage = onSetHomepage,
                    onClearSitePermissions = onClearSitePermissions,
                  onOpenPrivacyDashboard = onOpenPrivacyDashboard,
                    credentials = credentials,
                    addresses = addresses,
                   credentialCount = credentialCount,
                   credentialVaultUnlocked = credentialVaultUnlocked,
                   webPushSubscriptions = webPushSubscriptions,
                   onDeleteCredential = onDeleteCredential,
                   onRequestCredentialUnlock = onRequestCredentialUnlock,
                   onLockCredentialVault = onLockCredentialVault,
                    onClearCredentials = onClearCredentials,
                    onDeleteAddress = onDeleteAddress,
                    onSaveAddress = onSaveAddress,
                    onClearAddresses = onClearAddresses,
                  onCopyCredentialUsername = onCopyCredentialUsername,
                   onCopyCredentialPassword = onCopyCredentialPassword,
                   onRevokeWebPushSubscription = onRevokeWebPushSubscription,
                   onClearWebPushSubscriptions = onClearWebPushSubscriptions,
                  installedWebApps = installedWebApps,
                   onOpenInstalledWebApp = onOpenInstalledWebApp,
                   onRefreshInstalledWebApp = onRefreshInstalledWebApp,
                   onUninstallWebApp = onUninstallWebApp,
                    onSetTabBarWithAddressBar = onSetTabBarWithAddressBar,
                    onSetVerticalTabs = onSetVerticalTabs,
                    onOpenKeyboardShortcuts = { onSetOverlay(BrowserOverlay.KEYBOARD_SHORTCUTS) },
                 dnsOverHttpsEnabled = state.settings.dnsOverHttpsEnabled,
                onSetDnsOverHttpsEnabled = onSetDnsOverHttpsEnabled,
                dnsProvider = state.settings.dnsProvider,
                onSetDnsProvider = onSetDnsProvider,
                adBlockingEnabled = state.settings.adBlockingEnabled,
                adBlockFilters = state.settings.adBlockFilters,
                onSetAdBlockingEnabled = onSetAdBlockingEnabled,
                onSetAdBlockFilterEnabled = onSetAdBlockFilterEnabled,
                onRefreshAdBlockFilters = onRefreshAdBlockFilters,
                onAddAdBlockFilter = onAddAdBlockFilter,
                onRemoveAdBlockFilter = onRemoveAdBlockFilter,
                userScriptUrls = state.settings.userScriptUrls,
                disabledUserScriptUrls = state.settings.disabledUserScriptUrls,
                onAddUserScript = onAddUserScript,
                onSetUserScriptEnabled = onSetUserScriptEnabled,
                onRefreshUserScripts = onRefreshUserScripts,
                 onRemoveUserScript = onRemoveUserScript,
                 installedExtensions = installedExtensions,
                extensionInstallInProgress = extensionInstallInProgress,
                onInstallExtension = onInstallExtension,
                onOpenFirefoxAddons = onOpenFirefoxAddons,
                onSetExtensionEnabled = onSetExtensionEnabled,
                 onSetExtensionPrivateBrowsing = onSetExtensionPrivateBrowsing,
                 onRevokeExtensionOptionalPermissions = onRevokeExtensionOptionalPermissions,
                 onUpdateExtension = onUpdateExtension,
                onOpenExtensionOptions = onOpenExtensionOptions,
                onUninstallExtension = onUninstallExtension,
            )
        } else {
            val expanded = maxWidth >= 600.dp
            if (expanded) {
                     DesktopBrowserLayout(
                         state = state,
                         activeTab = activeTab,
                         fullScreenTab = fullScreenTab,
                         bookmarks = bookmarks,
                         history = history,
                     onNavigate = onNavigate,
                     onHome = onHome,
                     onCloseSplit = onCloseSplit,
                     onSwapSplit = onSwapSplit,
                     onFocusSplitPane = onFocusSplitPane,
                    onBack = onBack,
                    onForward = onForward,
                     onReload = onReload,
                     onReloadCrashedTab = onReloadCrashedTab,
                     onRegisterViewport = { id, view -> if (id == state.activeTabId) screenshotView = view },
                      onTabContextMenu = onTabContextMenu,
                      onToggleTabAudio = onToggleTabAudio,
                    extensionActions = extensionActions,
                    onClickExtensionAction = onClickExtensionAction,
                 onNewTab = { onNewTab() },
                       onSelectTab = onSelectTab,
                  onCloseTab = onCloseTab,
                  onOpenTabInSplit = onOpenTabInSplit,
                  onMoveTabBefore = onMoveTabBefore,
                     onMoveTabAfter = onMoveTabAfter,
                     onCreateTabGroup = onCreateTabGroup,
                     onMoveTabToGroup = onMoveTabToGroup,
                     onRenameTabGroup = onRenameTabGroup,
                 onToggleTabGroup = onToggleTabGroup,
                 onDeleteTabGroup = onDeleteTabGroup,
                 onToggleTabSleeping = onToggleTabSleeping,
                     onHibernateInactiveTabs = onHibernateInactiveTabs,
                     onToggleBookmark = onToggleBookmark,
                     onShowDownloads = { onSetOverlay(BrowserOverlay.DOWNLOADS) },
                          onMenu = { menuExpanded = true },
                          startPage = startPage,
                         addressFocusRequester = addressFocusRequester,
                     showTabBarWithAddressBar = state.settings.tabBarWithAddressBar,
                     verticalTabs = state.settings.verticalTabs,
                 )
            } else {
                 CompactBrowserLayout(
                     state = state,
                     activeTab = activeTab,
                     fullScreenTab = fullScreenTab,
                     bookmarks = bookmarks,
                     history = history,
                     onNavigate = onNavigate,
                     onHome = onHome,
                    onBack = onBack,
                    onForward = onForward,
                     onReload = onReload,
                     onReloadCrashedTab = onReloadCrashedTab,
                     onRegisterViewport = { id, view -> if (id == state.activeTabId) screenshotView = view },
                      onTabContextMenu = onTabContextMenu,
                      onToggleTabAudio = onToggleTabAudio,
                    extensionActions = extensionActions,
                    onClickExtensionAction = onClickExtensionAction,
                    onNewTab = { onNewTab() },
                     onSelectTab = onSelectTab,
                     onCloseTab = onCloseTab,
                     onMoveTabBefore = onMoveTabBefore,
                     onMoveTabAfter = onMoveTabAfter,
                     onToggleBookmark = onToggleBookmark,
                     onMenu = { menuExpanded = true },
                     startPage = startPage,
                     addressFocusRequester = addressFocusRequester,
                    onShowTabs = { onSetOverlay(BrowserOverlay.TABS) },
                    showTabBarWithAddressBar = state.settings.tabBarWithAddressBar,
                )
            }

            if (fullScreenTab == null) BrowserMenu(
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                 onNewPrivateTab = {
                     menuExpanded = false
                     onNewPrivateTab()
                 },
                 onReopenClosedTab = {
                     menuExpanded = false
                     onReopenClosedTab()
                 },
                 closedTabCount = state.closedTabCount,
                 onOpenTranslation = {
                     menuExpanded = false
                     onOpenTranslation()
                 },
                onShowTabs = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.TABS)
                },
                onShowBookmarks = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.BOOKMARKS)
                },
                onShowHistory = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.HISTORY)
                },
                 onShowDownloads = {
                     menuExpanded = false
                     onSetOverlay(BrowserOverlay.DOWNLOADS)
                 },
                  onShowSiteSettings = {
                      menuExpanded = false
                      onOpenSiteSettings()
                  },
                  onOpenPrivacyDashboard = {
                      menuExpanded = false
                      onOpenPrivacyDashboard()
                  },
                  onOpenSecurityDiagnostics = {
                      menuExpanded = false
                      onOpenSecurityDiagnostics()
                  },
                  onOpenPerformanceDashboard = {
                      menuExpanded = false
                      onOpenPerformanceDashboard()
                  },
                  onOpenNetworkInspector = {
                      menuExpanded = false
                      onOpenNetworkInspector()
                  },
                  onShareCurrentWorkspaceTabs = {
                      menuExpanded = false
                      onShareCurrentWorkspaceTabs()
                  },
                  onOpenMediaManager = {
                      menuExpanded = false
                      onOpenMediaManager()
                  },
                  onOpenCompatibilityDiagnostics = {
                      menuExpanded = false
                      onOpenCompatibilityDiagnostics()
                  },
                  onOpenWorkspaces = {
                      menuExpanded = false
                      onSetOverlay(BrowserOverlay.WORKSPACES)
                  },
                 onToggleReadingList = {
                     menuExpanded = false
                     onToggleReadingList()
                 },
                  onOpenQrCode = {
                      menuExpanded = false
                      onOpenQrCode()
                  },
                  onOpenReaderMode = {
                      menuExpanded = false
                      onOpenReaderMode()
                  },
                 onSharePage = {
                     menuExpanded = false
                     onSharePage()
                 },
                  onExportPdf = {
                      menuExpanded = false
                      onExportPdf()
                  },
                  onPrintPage = {
                      menuExpanded = false
                      onPrintPage()
                  },
                  onExportHtml = {
                      menuExpanded = false
                      onExportHtml()
                  },
                  onCaptureScreenshot = {
                      menuExpanded = false
                      captureScreenshot()
                  },
                  onCaptureLongScreenshot = {
                      menuExpanded = false
                      captureLongScreenshot()
                  },
                  onInstallWebApp = {
                      menuExpanded = false
                      onInstallWebApp()
                  },
                  onEnterPictureInPicture = {
                      menuExpanded = false
                      onEnterPictureInPicture()
                  },
                  onSaveOffline = {
                      menuExpanded = false
                      onSaveOffline()
                  },
                  onOpenCommandPalette = {
                      menuExpanded = false
                      onOpenCommandPalette()
                  },
                  onOpenTabSwitcher = {
                      menuExpanded = false
                      onOpenTabSwitcher()
                  },
                  onShowSettings = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.SETTINGS)
                },
            )

            if (state.overlay != BrowserOverlay.NONE) {
                ModalBottomSheet(onDismissRequest = onDismissOverlay) {
                    when (state.overlay) {
                        BrowserOverlay.TABS -> TabsSheet(
                            tabs = state.tabs,
                            activeTabId = state.activeTabId,
                            onNewTab = { onDismissOverlay(); onNewTab() },
                            onNewPrivateTab = { onDismissOverlay(); onNewPrivateTab() },
                            onSelectTab = onSelectTab,
                            onCloseTab = onCloseTab,
                        )
                         BrowserOverlay.LIBRARY -> LibrarySheet(
                             bookmarks = bookmarks,
                             history = history,
                             readingList = readingList,
                             onOpen = onOpenSavedPage,
                            onClearHistory = onClearHistory,
                            onDeleteHistoryEntry = onDeleteHistoryEntry,
                            onSetBookmarkFolder = onSetBookmarkFolder,
                            onUpdateBookmark = onUpdateBookmark,
                            onDeleteBookmark = onDeleteBookmark,
                            onExportBookmarks = onExportBookmarks,
                             onImportBookmarks = onImportBookmarks,
                              onExportBackup = onExportBackup,
                              onImportBackup = onImportBackup,
                             onDeleteReadingListEntry = onDeleteReadingListEntry,
                              onSetReadingListRead = onSetReadingListRead,
                              onOpenOffline = onOpenOffline,
                               sessionSnapshots = sessionSnapshots,
                               sessionTimeline = sessionTimeline,
                               onCreateSessionSnapshot = onCreateSessionSnapshot,
                               onRestoreSessionSnapshot = onRestoreSessionSnapshot,
                               onDeleteSessionSnapshot = onDeleteSessionSnapshot,
                               onDeleteSessionTimeline = onDeleteSessionTimeline,
                          )
                        BrowserOverlay.PRIVACY -> PrivacyDashboardSheet(
                               origins = privacyOrigins,
                               permissions = sitePermissions,
                               totalBlocked = state.blockerStats.totalBlocked,
                               currentOrigin = activeTab?.takeIf { !it.isPrivate }?.let { NavigationPolicy.origin(it.url) },
                               onClearOrigin = onClearSiteData,
                               onForgetSite = onForgetSite,
                               onClearAll = onClearAllSiteData,
                               onSetPermission = onSetSitePermission,
                          )
                         BrowserOverlay.SECURITY -> SecurityDiagnosticsSheet(
                             diagnostics = state.securityDiagnostics,
                             onSetPermission = onSetSitePermission,
                         )
                         BrowserOverlay.WORKSPACES -> WorkspaceSheet(
                             workspaces = workspaces,
                             activeWorkspaceId = activeWorkspaceId,
                             onCreate = onCreateWorkspace,
                             onSwitch = onSwitchWorkspace,
                             onRename = onRenameWorkspace,
                             onDelete = onDeleteWorkspace,
                         )
                         BrowserOverlay.PERFORMANCE -> PerformanceDashboardSheet(
                             metrics = state.performance,
                             onRefresh = { onOpenPerformanceDashboard() },
                             onCopy = onCopyPerformanceReport,
                         )
                         BrowserOverlay.NETWORK -> NetworkActivitySheet(
                             activity = state.networkActivity,
                             tabs = state.tabs,
                             onClear = onClearNetworkActivity,
                         )
                         BrowserOverlay.MEDIA -> MediaSessionSheet(
                             tabs = state.tabs,
                             onSelect = onSelectTab,
                             onControl = onControlMedia,
                         )
                         BrowserOverlay.COMPATIBILITY -> CompatibilityDiagnosticsSheet(
                             events = state.compatibilityEvents,
                             tabs = state.tabs,
                             onClear = onClearCompatibilityEvents,
                         )
                          BrowserOverlay.SETTINGS,
                         BrowserOverlay.BOOKMARKS,
                         BrowserOverlay.HISTORY,
                          BrowserOverlay.KEYBOARD_SHORTCUTS,
                         BrowserOverlay.NONE,
                        -> Unit
                    }
                }
            }
        }

        state.contentPermission?.let { prompt ->
            var rememberPermission by remember(prompt.id) { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { onResolvePermission(false, false) },
                icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                title = { Text("Allow access?") },
                text = {
                    Column {
                        Text("${prompt.origin} wants to use ${prompt.label}.")
                        Spacer(Modifier.height(12.dp))
                        SettingToggle(
                            title = "Remember for this site",
                            summary = "Reuse this decision for this origin",
                            checked = rememberPermission,
                            onCheckedChange = { rememberPermission = it },
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { onResolvePermission(true, rememberPermission) }) { Text("Allow") } },
                dismissButton = { TextButton(onClick = { onResolvePermission(false, rememberPermission) }) { Text("Block") } },
            )
        }
        extensionInstallPrompt?.let { prompt ->
            ExtensionInstallDialog(prompt, onResolveExtensionInstall)
        }
        extensionUpdatePrompt?.let { prompt ->
            ExtensionUpdateDialog(prompt, onResolveExtensionUpdate)
        }
        state.contextMenu?.let { menu ->
            BrowserContextMenuPopup(menu, onContextMenuAction, onDismissContextMenu)
        }
        extensionPopup?.let { popup ->
            ExtensionPopupView(popup, onCloseExtensionPopup)
        }
         findInPage?.let { find ->
             FindInPageBar(
                 find = find,
                 onQueryChange = onUpdateFindInPage,
                 onFindNext = onFindNext,
                 onClose = onCloseFindInPage,
             )
         }
         if (state.siteSettingsOpen) {
             SiteSettingsDialog(
                 tab = activeTab,
                 setting = state.siteSetting,
                 globalDesktopSites = state.settings.desktopSites,
                 globalHttpsOnly = state.settings.httpsOnly,
                 globalAdBlocking = state.settings.adBlockingEnabled,
                 globalUserScripts = state.settings.userScriptUrls.isNotEmpty(),
                 onSetDesktopOverride = onSetSiteDesktopOverride,
                 onSetAdBlockingOverride = onSetSiteAdBlockingOverride,
                 onSetUserScriptsOverride = onSetSiteUserScriptsOverride,
                 onSetZoomOverride = onSetSiteZoomOverride,
                 onSetHttpsOnlyOverride = onSetSiteHttpsOnly,
                 globalCookieBannerMode = state.settings.cookieBannerMode,
                 onSetCookieBannerModeOverride = onSetSiteCookieBannerMode,
                 onClear = onClearSiteSettings,
                 onDismiss = onCloseSiteSettings,
             )
         }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

@Composable
private fun WindowedBrowserLayout(
    tab: BrowserTabState?,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReloadCrashedTab: () -> Unit,
    onViewReady: (String, GeckoView) -> Unit,
    startPage: StartPageSettings = StartPageSettings(),
    modifier: Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BrowserNavButton(Icons.Outlined.ArrowBack, "Back", tab?.canGoBack == true) { onBack() }
            BrowserNavButton(Icons.Outlined.ArrowForward, "Forward", tab?.canGoForward == true, onForward)
            BrowserNavButton(Icons.Outlined.Refresh, "Reload", tab != null, onReload)
            AddressBar(
                tab = tab,
                modifier = Modifier.weight(1f),
                bookmarks = bookmarks,
                history = history,
                onNavigate = onNavigate,
                onToggleBookmark = onToggleBookmark,
                focusRequester = focusRequester,
            )
            BrowserNavButton(
                if (tab?.isBookmarked == true) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                if (tab?.isBookmarked == true) "Remove bookmark" else "Bookmark",
                tab?.hasPage == true,
                onToggleBookmark,
            )
            BrowserNavButton(Icons.Outlined.Home, "Home", true, onHome)
        }
        BrowserViewport(
            tab = tab,
            onNavigate = onNavigate,
            onReloadCrashedTab = onReloadCrashedTab,
            onViewReady = onViewReady,
            startPage = startPage,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Composable
private fun DesktopBrowserLayout(
    state: com.dwicao.dextra.browser.BrowserUiState,
    activeTab: BrowserTabState?,
    fullScreenTab: BrowserTabState?,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onCloseSplit: () -> Unit,
    onSwapSplit: () -> Unit,
    onFocusSplitPane: (Boolean) -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onReloadCrashedTab: () -> Unit,
    onRegisterViewport: (String, GeckoView) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onToggleTabAudio: (String) -> Unit,
    extensionActions: List<ExtensionToolbarAction>,
    onClickExtensionAction: (String) -> Unit,
    onNewTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenTabInSplit: (String) -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onCreateTabGroup: (String?) -> Unit,
    onMoveTabToGroup: (String, String?) -> Unit,
    onRenameTabGroup: (String, String) -> Unit,
    onToggleTabGroup: (String) -> Unit,
    onDeleteTabGroup: (String) -> Unit,
    onToggleTabSleeping: (String) -> Unit,
    onHibernateInactiveTabs: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShowDownloads: () -> Unit,
    onMenu: () -> Unit,
    addressFocusRequester: FocusRequester,
    showTabBarWithAddressBar: Boolean,
    verticalTabs: Boolean,
    startPage: StartPageSettings,
) {
    val splitPrimaryTab = state.splitPrimaryTabId?.let { id -> state.tabs.firstOrNull { it.id == id } }
    val splitSecondaryTab = state.splitSecondaryTabId?.let { id -> state.tabs.firstOrNull { it.id == id } }
    Row(Modifier.fillMaxSize()) {
        if (verticalTabs && fullScreenTab == null) {
            GroupedVerticalTabStrip(
                tabs = state.tabs,
                activeTabId = state.activeTabId,
                 onNewTab = onNewTab,
                 onSelectTab = onSelectTab,
                 onCloseTab = onCloseTab,
                 onOpenTabInSplit = onOpenTabInSplit,
                 groups = state.settings.tabGroups,
                onMoveTabBefore = onMoveTabBefore,
                onMoveTabAfter = onMoveTabAfter,
                onCreateTabGroup = onCreateTabGroup,
                onMoveTabToGroup = onMoveTabToGroup,
                onRenameTabGroup = onRenameTabGroup,
                onToggleTabGroup = onToggleTabGroup,
                onDeleteTabGroup = onDeleteTabGroup,
                onToggleTabSleeping = onToggleTabSleeping,
                onHibernateInactiveTabs = onHibernateInactiveTabs,
                 onTabContextMenu = onTabContextMenu,
                 onToggleTabAudio = onToggleTabAudio,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            if (fullScreenTab == null) {
                DesktopToolbar(
                    tabs = state.tabs,
                    activeTabId = state.activeTabId,
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab,
                     onTabContextMenu = onTabContextMenu,
                     onToggleTabAudio = onToggleTabAudio,
                    onNewTab = onNewTab,
                    showTabBar = showTabBarWithAddressBar && !verticalTabs,
                 activeTab = activeTab,
                     bookmarks = bookmarks,
                     history = history,
                     onNavigate = onNavigate,
                    onHome = onHome,
                    onBack = onBack,
                    onForward = onForward,
                    onReload = onReload,
                    onMoveTabBefore = onMoveTabBefore,
                    onMoveTabAfter = onMoveTabAfter,
                    onToggleBookmark = onToggleBookmark,
                    onShowDownloads = onShowDownloads,
                    extensionActions = extensionActions,
                    onClickExtensionAction = onClickExtensionAction,
                    onMenu = onMenu,
                    addressFocusRequester = addressFocusRequester,
                    stretchAddressBar = verticalTabs,
                )
                activeTab?.takeIf { it.isLoading }?.let {
                    LinearProgressIndicator(
                        progress = { it.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                    )
                }
            }
            if (fullScreenTab == null && splitPrimaryTab != null && splitSecondaryTab != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Split view", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    BrowserNavButton(Icons.Outlined.SwapHoriz, "Swap panes", true, onSwapSplit)
                    BrowserNavButton(Icons.Outlined.Close, "Close split view", true, onCloseSplit)
                }
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    BrowserViewport(
                        tab = splitPrimaryTab,
                        onNavigate = onNavigate,
                        onReloadCrashedTab = onReloadCrashedTab,
                        onViewReady = onRegisterViewport,
                        startPage = startPage,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onFocus = { onFocusSplitPane(false) },
                    )
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    BrowserViewport(
                        tab = splitSecondaryTab,
                        onNavigate = onNavigate,
                        onReloadCrashedTab = onReloadCrashedTab,
                        onViewReady = onRegisterViewport,
                        startPage = startPage,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onFocus = { onFocusSplitPane(true) },
                    )
                }
            } else {
                BrowserViewport(
                    tab = fullScreenTab ?: activeTab,
                    onNavigate = onNavigate,
                    onReloadCrashedTab = onReloadCrashedTab,
                    onViewReady = onRegisterViewport,
                    startPage = startPage,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompactBrowserLayout(
    state: com.dwicao.dextra.browser.BrowserUiState,
    activeTab: BrowserTabState?,
    fullScreenTab: BrowserTabState?,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onReloadCrashedTab: () -> Unit,
    onRegisterViewport: (String, GeckoView) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onToggleTabAudio: (String) -> Unit,
    extensionActions: List<ExtensionToolbarAction>,
    onClickExtensionAction: (String) -> Unit,
    onNewTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onToggleBookmark: () -> Unit,
    onMenu: () -> Unit,
    addressFocusRequester: FocusRequester,
    onShowTabs: () -> Unit,
    showTabBarWithAddressBar: Boolean,
    startPage: StartPageSettings,
) {
    Column(Modifier.fillMaxSize()) {
        if (fullScreenTab == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BrowserNavButton(Icons.Outlined.Menu, "Open menu", enabled = true, onClick = onMenu)
                AddressBar(
                    tab = activeTab,
                    modifier = Modifier.weight(1f),
                    bookmarks = bookmarks,
                    history = history,
                    onNavigate = onNavigate,
                    onToggleBookmark = onToggleBookmark,
                    focusRequester = addressFocusRequester,
                )
                extensionActions.forEach { action ->
                    ExtensionActionButton(action, onClickExtensionAction)
                }
                BrowserNavButton(Icons.Outlined.Home, "Home", enabled = true, onClick = onHome)
                BrowserNavButton(Icons.Outlined.Add, "New tab", enabled = true, onClick = onNewTab)
            }
            if (showTabBarWithAddressBar) {
                TabStrip(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    tabs = state.tabs,
                    activeTabId = state.activeTabId,
                    onNewTab = onNewTab,
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab,
                    onMoveTabBefore = onMoveTabBefore,
                    onMoveTabAfter = onMoveTabAfter,
                       onTabContextMenu = onTabContextMenu,
                       onToggleTabAudio = onToggleTabAudio,
                )
            }
            activeTab?.takeIf { it.isLoading }?.let {
                LinearProgressIndicator(
                    progress = { it.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        BrowserViewport(
            tab = fullScreenTab ?: activeTab,
            onNavigate = onNavigate,
            onReloadCrashedTab = onReloadCrashedTab,
            onViewReady = onRegisterViewport,
            startPage = startPage,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        if (fullScreenTab == null) {
            CompactBottomBar(
                tabCount = state.tabs.size,
                activeTab = activeTab,
                onBack = onBack,
                onForward = onForward,
                onReload = onReload,
                onShowTabs = onShowTabs,
                onSelectTab = onSelectTab,
                onCloseTab = onCloseTab,
            )
        }
    }
}

@Composable
private fun DesktopToolbar(
    modifier: Modifier = Modifier,
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onToggleTabAudio: (String) -> Unit,
    onNewTab: () -> Unit,
    showTabBar: Boolean,
    activeTab: BrowserTabState?,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onToggleBookmark: () -> Unit,
    onShowDownloads: () -> Unit,
    extensionActions: List<ExtensionToolbarAction>,
    onClickExtensionAction: (String) -> Unit,
    onMenu: () -> Unit,
    addressFocusRequester: FocusRequester,
    stretchAddressBar: Boolean = false,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        val addressBarWidth = (maxWidth * 0.32f).coerceIn(280.dp, 560.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BrowserNavButton(Icons.Outlined.ArrowBack, "Back", enabled = activeTab?.canGoBack == true, onClick = { onBack() })
            BrowserNavButton(Icons.Outlined.ArrowForward, "Forward", enabled = activeTab?.canGoForward == true, onClick = onForward)
            BrowserNavButton(
                if (activeTab?.isLoading == true) Icons.Outlined.Close else Icons.Outlined.Refresh,
                if (activeTab?.isLoading == true) "Stop" else "Reload",
                enabled = activeTab != null,
                onClick = onReload,
            )
            BrowserNavButton(Icons.Outlined.Home, "Home", enabled = true, onClick = onHome)
            AddressBar(
                tab = activeTab,
                modifier = if (stretchAddressBar) Modifier.weight(1f) else Modifier.width(addressBarWidth),
                bookmarks = bookmarks,
                history = history,
                onNavigate = onNavigate,
                onToggleBookmark = onToggleBookmark,
                focusRequester = addressFocusRequester,
            )
            if (showTabBar) {
                TabStrip(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onNewTab = onNewTab,
                    showNewTabButton = false,
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab,
                    onMoveTabBefore = onMoveTabBefore,
                    onMoveTabAfter = onMoveTabAfter,
                     onTabContextMenu = onTabContextMenu,
                     onToggleTabAudio = onToggleTabAudio,
                )
            }
            BrowserNavButton(Icons.Outlined.Add, "New tab", enabled = true, onClick = onNewTab)
            BrowserNavButton(
                if (activeTab?.isBookmarked == true) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                if (activeTab?.isBookmarked == true) "Remove bookmark" else "Bookmark",
                enabled = activeTab?.hasPage == true,
                onClick = onToggleBookmark,
            )
            Box {
                var allTabsExpanded by remember { mutableStateOf(false) }
                BrowserNavButton(Icons.Outlined.Tab, "All tabs", enabled = true) {
                    allTabsExpanded = true
                }
                AllTabsDropdown(
                    expanded = allTabsExpanded,
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onDismiss = { allTabsExpanded = false },
                    onSelectTab = { id ->
                        allTabsExpanded = false
                        onSelectTab(id)
                    },
                     onCloseTab = onCloseTab,
                     onToggleTabAudio = onToggleTabAudio,
                )
            }
            BrowserNavButton(Icons.Outlined.Download, "Downloads", enabled = true, onClick = onShowDownloads)
            extensionActions.forEach { action ->
                ExtensionActionButton(action, onClickExtensionAction)
            }
            BrowserNavButton(Icons.Outlined.MoreVert, "More", enabled = true, onClick = onMenu)
        }
    }
}

@Composable
private fun AllTabsDropdown(
    expanded: Boolean,
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    onDismiss: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleTabAudio: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(expanded) {
        if (!expanded) query = ""
    }
    val filteredTabs = tabs.filter { tab ->
        query.isBlank() || tab.title.contains(query, ignoreCase = true) || tab.url.contains(query, ignoreCase = true)
    }
    if (!expanded) return
    Popup(
        popupPositionProvider = BelowAnchorPositionProvider(),
        properties = PopupProperties(focusable = true),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.size(width = 360.dp, height = 560.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (query.isBlank()) {
                                Text("Search tabs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
            if (filteredTabs.isEmpty()) {
                Text(
                    "No matching tabs",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(460.dp)) {
                    items(filteredTabs, key = { it.id }) { tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (tab.id == activeTabId) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                )
                                .clickable { onSelectTab(tab.id) }
                                .padding(start = 8.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (tab.favicon != null && !tab.isPrivate) {
                                Image(
                                    bitmap = tab.favicon.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                            } else {
                                Icon(
                                    if (tab.isPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Text(tab.title.ifBlank { "New tab" }, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    tab.url.ifBlank { "Start browsing" },
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BrowserNavButton(Icons.Outlined.Close, "Close tab", enabled = true) {
                                onCloseTab(tab.id)
                            }
                            if (tab.hasActiveMedia) {
                                BrowserNavButton(
                                    if (tab.isAudioMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                    if (tab.isAudioMuted) "Unmute tab" else "Mute tab",
                                    enabled = true,
                                ) { onToggleTabAudio(tab.id) }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun GroupedVerticalTabStrip(
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    groups: List<SavedTabGroup>,
    onNewTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenTabInSplit: (String) -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onCreateTabGroup: (String?) -> Unit,
    onMoveTabToGroup: (String, String?) -> Unit,
    onRenameTabGroup: (String, String) -> Unit,
    onToggleTabGroup: (String) -> Unit,
    onDeleteTabGroup: (String) -> Unit,
    onToggleTabSleeping: (String) -> Unit,
    onHibernateInactiveTabs: () -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onToggleTabAudio: (String) -> Unit,
) {
    var collapsed by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var renameGroup by remember { mutableStateOf<SavedTabGroup?>(null) }
    var deleteGroup by remember { mutableStateOf<SavedTabGroup?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var dragDistance by remember { mutableStateOf(0f) }
    val reduceMotion = LocalDextraAccessibility.current.reduceMotion
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val orderedTabs = tabs.sortedWith(compareByDescending<BrowserTabState> { it.pinned })
    val visibleTabs = orderedTabs.filter { tab ->
        query.isBlank() || tab.title.contains(query, ignoreCase = true) || tab.url.contains(query, ignoreCase = true)
    }
    val tabIds = visibleTabs.mapTo(hashSetOf()) { it.id }
    val width = if (collapsed) 56.dp else 280.dp

    Surface(
        modifier = Modifier.fillMaxHeight().width(width),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (collapsed) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BrowserNavButton(Icons.Outlined.ChevronRight, "Expand vertical tabs", true) { collapsed = false }
                        BrowserNavButton(Icons.Outlined.Add, "New tab", true, onNewTab)
                        BrowserNavButton(Icons.Outlined.Pause, "Hibernate inactive tabs", true, onHibernateInactiveTabs)
                    }
                } else {
                    Icon(Icons.Outlined.Tab, contentDescription = null, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Tabs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${tabs.size} open",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BrowserNavButton(Icons.Outlined.Add, "New tab", true, onNewTab)
                    BrowserNavButton(Icons.Outlined.CreateNewFolder, "New tab group", true) { onCreateTabGroup(null) }
                    BrowserNavButton(Icons.Outlined.Pause, "Hibernate inactive tabs", true, onHibernateInactiveTabs)
                    BrowserNavButton(Icons.Outlined.ChevronLeft, "Collapse vertical tabs", true) { collapsed = true }
                }
            }
            if (!collapsed) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (query.isBlank()) Text("Search tabs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                innerTextField()
                            }
                        }
                    },
                )
            }
            Divider()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(listState) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (delta != 0f) scope.launch { listState.scrollBy(delta * 8f) }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                groups.forEach { group ->
                    val groupTabs = visibleTabs.filter { it.groupId == group.id }
                    if (query.isBlank() || groupTabs.isNotEmpty()) {
                        item(key = "group:${group.id}") {
                            var groupMenuExpanded by remember(group.id) { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleTabGroup(group.id) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(width = 4.dp, height = 22.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(group.color)),
                                )
                                Icon(
                                    if (group.collapsed && query.isBlank()) Icons.Outlined.ChevronRight else Icons.Outlined.ChevronLeft,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 6.dp).size(18.dp),
                                )
                                Text(
                                    group.title,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(group.color),
                                )
                                Text(groupTabs.size.toString(), style = MaterialTheme.typography.labelSmall)
                                Box {
                                    IconButton(onClick = { groupMenuExpanded = true }) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "Group actions")
                                    }
                                    DropdownMenu(
                                        expanded = groupMenuExpanded,
                                        onDismissRequest = { groupMenuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename group") },
                                            onClick = {
                                                groupMenuExpanded = false
                                                renameGroup = group
                                                renameText = group.title
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete group") },
                                            onClick = {
                                                groupMenuExpanded = false
                                                deleteGroup = group
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        if (!group.collapsed || query.isNotBlank()) {
                            items(groupTabs, key = { it.id }) { tab ->
                                VerticalTabRow(
                                    tab = tab,
                                    activeTabId = activeTabId,
                                    collapsed = collapsed,
                                    groups = groups,
                                    listState = listState,
                                    tabIds = tabIds,
                                    dragDistance = dragDistance,
                                    onDragStart = {
                                        dragDistance = 0f
                                    },
                                    onDragDistance = { dragDistance = it },
                                    onDragEnd = {
                                        dragDistance = 0f
                                    },
                                    onMoveTabBefore = onMoveTabBefore,
                                    onMoveTabAfter = onMoveTabAfter,
                                     onSelectTab = onSelectTab,
                                     onCloseTab = onCloseTab,
                                     onOpenTabInSplit = onOpenTabInSplit,
                                     onCreateTabGroup = onCreateTabGroup,
                                    onMoveTabToGroup = onMoveTabToGroup,
                                    onToggleTabSleeping = onToggleTabSleeping,
                                     onTabContextMenu = onTabContextMenu,
                                     onToggleTabAudio = onToggleTabAudio,
                                )
                            }
                        }
                    }
                }
                val ungrouped = visibleTabs.filter { tab -> tab.groupId == null || groups.none { it.id == tab.groupId } }
                if (groups.isNotEmpty() && ungrouped.isNotEmpty()) {
                    item(key = "ungrouped-header") {
                        Text(
                            "Other tabs",
                            modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(ungrouped, key = { it.id }) { tab ->
                    VerticalTabRow(
                        tab = tab,
                        activeTabId = activeTabId,
                        collapsed = collapsed,
                        groups = groups,
                        listState = listState,
                        tabIds = tabIds,
                        dragDistance = dragDistance,
                        onDragStart = {
                            dragDistance = 0f
                        },
                        onDragDistance = { dragDistance = it },
                        onDragEnd = {
                            dragDistance = 0f
                        },
                        onMoveTabBefore = onMoveTabBefore,
                        onMoveTabAfter = onMoveTabAfter,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onOpenTabInSplit = onOpenTabInSplit,
                        onCreateTabGroup = onCreateTabGroup,
                        onMoveTabToGroup = onMoveTabToGroup,
                        onToggleTabSleeping = onToggleTabSleeping,
                         onTabContextMenu = onTabContextMenu,
                         onToggleTabAudio = onToggleTabAudio,
                    )
                }
            }
        }
    }
    renameGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { renameGroup = null },
            title = { Text("Rename group") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Group name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameTabGroup(group.id, renameText)
                        renameGroup = null
                    },
                    enabled = renameText.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameGroup = null }) { Text("Cancel") } },
        )
    }
    deleteGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { deleteGroup = null },
            title = { Text("Delete ${group.title}?") },
            text = { Text("The group will be removed, but its tabs will stay open.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTabGroup(group.id)
                        deleteGroup = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteGroup = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun VerticalTabRow(
    tab: BrowserTabState,
    activeTabId: String?,
    collapsed: Boolean,
    groups: List<SavedTabGroup>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    tabIds: Set<String>,
    dragDistance: Float,
    onDragStart: () -> Unit,
    onDragDistance: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenTabInSplit: (String) -> Unit,
    onCreateTabGroup: (String?) -> Unit,
    onMoveTabToGroup: (String, String?) -> Unit,
    onToggleTabSleeping: (String) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onToggleTabAudio: (String) -> Unit,
) {
    var tabOrigin by remember(tab.id) { mutableStateOf(IntOffset.Zero) }
    var menuExpanded by remember(tab.id) { mutableStateOf(false) }
    var hovered by remember(tab.id) { mutableStateOf(false) }
    var hoverVisible by remember(tab.id) { mutableStateOf(false) }
    val reduceMotion = LocalDextraAccessibility.current.reduceMotion
    LaunchedEffect(hovered) {
        hoverVisible = false
        if (hovered) {
            delay(if (reduceMotion) 0 else 450)
            hoverVisible = hovered
        }
    }
    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (collapsed) 44.dp else 58.dp)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                tabOrigin = IntOffset(position.x.toInt(), position.y.toInt())
            }
            .clip(RoundedCornerShape(9.dp))
            .background(
                when {
                    tab.isPrivate && tab.id == activeTabId -> MaterialTheme.colorScheme.tertiaryContainer
                    tab.isPrivate -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    tab.id == activeTabId -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                },
            )
            .pointerInput(tab.id) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                            else -> Unit
                        }
                    }
                }
            }
            .pointerInput(tab.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val motionEvent = event.motionEvent
                        val buttonState = motionEvent?.buttonState ?: 0
                        if (event.type == PointerEventType.Press &&
                            buttonState and MotionEvent.BUTTON_SECONDARY != 0
                        ) {
                            val position = event.changes.firstOrNull()?.position ?: continue
                            event.changes.forEach { change -> change.consume() }
                            onTabContextMenu(tab.id, tabOrigin.x + position.x.toInt(), tabOrigin.y + position.y.toInt())
                        } else if (event.type == PointerEventType.Press &&
                            buttonState and MotionEvent.BUTTON_TERTIARY != 0
                        ) {
                            event.changes.forEach { change -> change.consume() }
                            onCloseTab(tab.id)
                        }
                    }
                }
            }
            .pointerInput(tab.id, tabIds) {
                var accumulatedDistance = 0f
                detectDragGestures(
                    onDragStart = {
                        accumulatedDistance = 0f
                        onDragStart()
                    },
                    onDragCancel = {
                        accumulatedDistance = 0f
                        onDragEnd()
                    },
                    onDragEnd = {
                        accumulatedDistance = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDistance += dragAmount.y
                        onDragDistance(accumulatedDistance)
                        val currentItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == tab.id } ?: return@detectDragGestures
                        val center = currentItem.offset + currentItem.size / 2 + accumulatedDistance.toInt()
                        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            item.key != tab.id && item.key in tabIds && center in item.offset..(item.offset + item.size)
                        }
                        if (target != null) {
                            if (accumulatedDistance > 0) {
                                onMoveTabAfter(tab.id, target.key as String)
                            } else {
                                onMoveTabBefore(tab.id, target.key as String)
                            }
                            accumulatedDistance = 0f
                            onDragDistance(0f)
                        }
                    },
                )
            }
            .clickable { onSelectTab(tab.id) }
            .padding(horizontal = if (collapsed) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tab.favicon != null && !tab.isPrivate) {
            Image(
                bitmap = tab.favicon.asImageBitmap(),
                contentDescription = tab.title.ifBlank { "Tab" },
                modifier = Modifier.size(if (collapsed) 22.dp else 18.dp).clip(RoundedCornerShape(4.dp)),
            )
        } else {
            Icon(
                if (tab.isPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Language,
                contentDescription = tab.title.ifBlank { "Tab" },
                modifier = Modifier.size(if (collapsed) 22.dp else 18.dp),
                tint = if (tab.isSleeping) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
            )
        }
        if (!collapsed) {
            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                Text(
                    tab.title.ifBlank { "New tab" },
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tab.isSleeping) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
                )
                Text(
                    if (tab.isSleeping) "Sleeping" else tab.url.ifBlank { "Start browsing" },
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tab.hasActiveMedia) {
                BrowserNavButton(
                    if (tab.isAudioMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                    if (tab.isAudioMuted) "Unmute tab" else "Mute tab",
                    enabled = true,
                ) { onToggleTabAudio(tab.id) }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Tab actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("New group from tab") },
                        onClick = {
                            menuExpanded = false
                            onCreateTabGroup(tab.id)
                        },
                    )
                    groups.forEach { group ->
                        if (group.id != tab.groupId) {
                            DropdownMenuItem(
                                text = { Text("Move to ${group.title}") },
                                onClick = {
                                    menuExpanded = false
                                    onMoveTabToGroup(tab.id, group.id)
                                },
                            )
                        }
                    }
                    if (tab.groupId != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from group") },
                            onClick = {
                                menuExpanded = false
                                onMoveTabToGroup(tab.id, null)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (tab.isSleeping) "Wake tab" else "Hibernate tab") },
                        onClick = {
                            menuExpanded = false
                            onToggleTabSleeping(tab.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Open in split view") },
                        onClick = {
                            menuExpanded = false
                            onOpenTabInSplit(tab.id)
                        },
                    )
                }
            }
            BrowserNavButton(Icons.Outlined.Close, "Close tab", true) { onCloseTab(tab.id) }
        }
    }
    if (hoverVisible) {
        Popup(
            popupPositionProvider = ContextMenuPositionProvider(tabOrigin.x + 220, tabOrigin.y),
            properties = PopupProperties(focusable = false),
        ) {
            Surface(
                modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(tab.title.ifBlank { "New tab" }, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(tab.url.ifBlank { "Start browsing" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    if (tab.isPrivate || tab.isSleeping) {
                        Text(
                            listOfNotNull(if (tab.isPrivate) "Private" else null, if (tab.isSleeping) "Sleeping" else null).joinToString("  •  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CompactBottomBar(
    tabCount: Int,
    activeTab: BrowserTabState?,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onShowTabs: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
) {
    Surface(shadowElevation = 4.dp, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onBack() }, enabled = activeTab?.canGoBack == true) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = onForward, enabled = activeTab?.canGoForward == true) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Forward")
            }
            IconButton(onClick = onReload, enabled = activeTab != null) {
                Icon(
                    if (activeTab?.isLoading == true) Icons.Outlined.Close else Icons.Outlined.Refresh,
                    contentDescription = if (activeTab?.isLoading == true) "Stop" else "Reload",
                )
            }
            IconButton(onClick = onShowTabs) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Tab, contentDescription = "Tabs")
                    Text(tabCount.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun BrowserNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accessibility = LocalDextraAccessibility.current
    Box(
        modifier = Modifier
            .size(if (accessibility.textScale >= 1.25f) 44.dp else 36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun ExtensionActionButton(
    action: ExtensionToolbarAction,
    onClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = action.enabled) { onClick(action.extensionId) }
            .semantics { contentDescription = action.title },
        contentAlignment = Alignment.Center,
    ) {
        if (action.icon != null) {
            Image(
                bitmap = action.icon.asImageBitmap(),
                contentDescription = action.title,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Icon(
                Icons.Outlined.Extension,
                contentDescription = action.title,
                modifier = Modifier.size(20.dp),
                tint = if (action.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
        action.badgeText?.takeIf(String::isNotBlank)?.let { badge ->
            Text(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
                    .padding(horizontal = 3.dp),
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AddressBar(
    tab: BrowserTabState?,
    modifier: Modifier,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onNavigate: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    focusRequester: FocusRequester,
) {
    var value by remember(tab?.id) { mutableStateOf(tab?.url?.let(BrowserUrl::displayValue).orEmpty()) }
    var isFocused by remember { mutableStateOf(false) }
    var selectedSuggestionIndex by remember(value) { mutableIntStateOf(0) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val suggestions = buildAddressSuggestions(value, bookmarks, history, tab?.isPrivate == true)

    fun selectSuggestion(index: Int) {
        suggestions.getOrNull(index)?.let { suggestion ->
            value = BrowserUrl.displayValue(suggestion.url)
            isFocused = false
            keyboardController?.hide()
            onNavigate(suggestion.url)
        }
    }

    LaunchedEffect(tab?.id, tab?.url) {
        value = BrowserUrl.displayValue(tab?.url.orEmpty())
    }

    val shape = RoundedCornerShape(18.dp)
    val isPrivate = tab?.isPrivate == true
    Box(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shape)
                .background(
                    if (isPrivate) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer,
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = when {
                        isFocused -> MaterialTheme.colorScheme.primary
                        isPrivate -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = shape,
                )
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || suggestions.isEmpty()) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            selectedSuggestionIndex = (selectedSuggestionIndex + 1) % suggestions.size
                            true
                        }
                        Key.DirectionUp -> {
                            selectedSuggestionIndex = (selectedSuggestionIndex - 1 + suggestions.size) % suggestions.size
                            true
                        }
                        Key.Enter -> {
                            selectSuggestion(selectedSuggestionIndex)
                            true
                        }
                        Key.Escape -> {
                            isFocused = false
                            true
                        }
                        else -> false
                    }
                },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (suggestions.isNotEmpty()) selectSuggestion(selectedSuggestionIndex) else onNavigate(value)
                    keyboardController?.hide()
                },
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (tab?.isSecure == true) Icons.Outlined.Lock else Icons.Outlined.Search,
                        contentDescription = if (tab?.isSecure == true) "Secure connection" else "Search",
                        modifier = Modifier.size(19.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                "Search or enter web address",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        BrowserNavButton(Icons.Outlined.Close, "Clear", enabled = true, onClick = { value = "" })
                    }
                }
            },
        )
        DropdownMenu(
            expanded = isFocused && suggestions.isNotEmpty(),
            onDismissRequest = { isFocused = false },
            modifier = Modifier.widthIn(min = 280.dp, max = 520.dp),
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                DropdownMenuItem(
                    modifier = Modifier.background(
                        if (index == selectedSuggestionIndex) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    ),
                    text = {
                        Column {
                            Text(suggestion.title, maxLines = 1)
                            Text(suggestion.url, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            if (suggestion.source == AddressSuggestionSource.BOOKMARK) Icons.Outlined.Bookmark else Icons.Outlined.History,
                            contentDescription = null,
                        )
                    },
                    onClick = { selectSuggestion(index) },
                )
            }
        }
    }
}

@Composable
private fun TabStrip(
    modifier: Modifier = Modifier,
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    onNewTab: () -> Unit,
    showNewTabButton: Boolean = true,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTabBefore: (String, String) -> Unit,
    onMoveTabAfter: (String, String) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onToggleTabAudio: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val orderedTabs = tabs.sortedWith(compareByDescending { it.pinned })
    val tabIds = orderedTabs.mapTo(hashSetOf()) { it.id }
    var dragDistance by remember { mutableStateOf(0f) }
    var hoveredTabId by remember { mutableStateOf<String?>(null) }
    var previewTabId by remember { mutableStateOf<String?>(null) }
    var hoverPosition by remember { mutableStateOf(IntOffset.Zero) }
    val reduceMotion = LocalDextraAccessibility.current.reduceMotion
    LaunchedEffect(hoveredTabId) {
        previewTabId = null
        val id = hoveredTabId ?: return@LaunchedEffect
        delay(if (reduceMotion) 0 else 450)
        if (hoveredTabId == id) previewTabId = id
    }
    Surface(
        modifier = modifier.pointerInput(listState) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (delta != 0f) scope.launch { listState.scrollBy(delta * 8f) }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        },
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowserNavButton(
                Icons.Outlined.ChevronLeft,
                "Scroll tabs left",
                enabled = listState.canScrollBackward,
                onClick = {
                    scope.launch {
                        val index = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        if (reduceMotion) listState.scrollToItem(index) else listState.animateScrollToItem(index)
                    }
                },
            )
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(orderedTabs, key = { it.id }) { tab ->
                    val tabOrigin = remember(tab.id) { mutableStateOf(IntOffset.Zero) }
                    Surface(
                        modifier = Modifier
                            .pointerInput(tab.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> {
                                                hoveredTabId = tab.id
                                                event.changes.firstOrNull()?.position?.let { position ->
                                                    hoverPosition = IntOffset(tabOrigin.value.x + position.x.toInt(), tabOrigin.value.y + position.y.toInt())
                                                }
                                            }
                                            PointerEventType.Exit -> if (hoveredTabId == tab.id) hoveredTabId = null
                                        }
                                    }
                                }
                            }
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInWindow()
                                tabOrigin.value = IntOffset(position.x.toInt(), position.y.toInt())
                            }
                            .pointerInput(tab.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val motionEvent = event.motionEvent
                                        val buttonState = motionEvent?.buttonState ?: 0
                                        if (event.type == PointerEventType.Press &&
                                            buttonState and MotionEvent.BUTTON_SECONDARY != 0
                                        ) {
                                            val position = event.changes.firstOrNull()?.position ?: continue
                                            event.changes.forEach { change -> change.consume() }
                                            val origin = tabOrigin.value
                                            onTabContextMenu(
                                                tab.id,
                                                origin.x + position.x.toInt(),
                                                origin.y + position.y.toInt(),
                                            )
                                        } else if (event.type == PointerEventType.Press &&
                                            buttonState and MotionEvent.BUTTON_TERTIARY != 0
                                        ) {
                                            event.changes.forEach { change -> change.consume() }
                                            onCloseTab(tab.id)
                                        }
                                    }
                                }
                            }
                            .pointerInput(tab.id, tabIds) {
                                var accumulatedDistance = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        accumulatedDistance = 0f
                                        dragDistance = 0f
                                    },
                                    onDragCancel = {
                                        accumulatedDistance = 0f
                                        dragDistance = 0f
                                    },
                                    onDragEnd = {
                                        accumulatedDistance = 0f
                                        dragDistance = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedDistance += dragAmount.x
                                        dragDistance = accumulatedDistance
                                        val currentItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == tab.id }
                                            ?: return@detectDragGestures
                                        val center = currentItem.offset + currentItem.size / 2 + accumulatedDistance.toInt()
                                        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                            item.key != tab.id && item.key in tabIds &&
                                                center in item.offset..(item.offset + item.size)
                                        }
                                        if (target != null) {
                                            if (accumulatedDistance > 0) {
                                                onMoveTabAfter(tab.id, target.key as String)
                                            } else {
                                                onMoveTabBefore(tab.id, target.key as String)
                                            }
                                            accumulatedDistance = 0f
                                            dragDistance = 0f
                                        }
                                    },
                                )
                            },
                        onClick = { onSelectTab(tab.id) },
                        selected = tab.id == activeTabId,
                        color = when {
                            tab.isPrivate -> if (tab.id == activeTabId) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                            }
                            tab.id == activeTabId -> MaterialTheme.colorScheme.primaryContainer
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        tonalElevation = if (tab.id == activeTabId) 2.dp else 0.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(
                                    min = if (tab.pinned) 48.dp else 150.dp,
                                    max = if (tab.pinned) 56.dp else 240.dp,
                                )
                                .padding(
                                    start = if (tab.pinned) 10.dp else 12.dp,
                                    end = if (tab.pinned) 10.dp else 4.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (tab.pinned) {
                                if (tab.hasActiveMedia) {
                                    BrowserNavButton(
                                        if (tab.isAudioMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                        if (tab.isAudioMuted) "Unmute tab" else "Mute tab",
                                        enabled = true,
                                    ) { onToggleTabAudio(tab.id) }
                                } else if (tab.favicon != null && !tab.isPrivate) {
                                    Image(
                                        bitmap = tab.favicon.asImageBitmap(),
                                        contentDescription = tab.title.ifBlank { "Pinned tab" },
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                } else {
                                    Icon(
                                        if (tab.isPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Language,
                                        contentDescription = tab.title.ifBlank { "Pinned tab" },
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                if (tab.favicon != null && !tab.isPrivate) {
                                    Image(
                                        bitmap = tab.favicon.asImageBitmap(),
                                        contentDescription = "Site favicon",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                } else {
                                    Icon(
                                        if (tab.isPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = tab.title.ifBlank { "New tab" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (tab.hasActiveMedia) {
                                    BrowserNavButton(
                                        if (tab.isAudioMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                        if (tab.isAudioMuted) "Unmute tab" else "Mute tab",
                                        enabled = true,
                                    ) { onToggleTabAudio(tab.id) }
                                }
                                BrowserNavButton(Icons.Outlined.Close, "Close tab", enabled = true, onClick = { onCloseTab(tab.id) })
                            }
                        }
                    }
                }
            }
            BrowserNavButton(
                Icons.Outlined.ChevronRight,
                "Scroll tabs right",
                enabled = listState.canScrollForward,
                onClick = {
                    scope.launch {
                        val index = listState.firstVisibleItemIndex + 1
                        if (reduceMotion) listState.scrollToItem(index) else listState.animateScrollToItem(index)
                    }
                },
            )
            if (showNewTabButton) {
            BrowserNavButton(Icons.Outlined.Add, "New tab", enabled = true, onClick = onNewTab)
            }
            previewTabId?.let { id ->
                orderedTabs.firstOrNull { it.id == id }?.let { tab ->
                    Popup(
                        popupPositionProvider = ContextMenuPositionProvider(hoverPosition.x, hoverPosition.y + 8),
                        properties = PopupProperties(focusable = false),
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(tab.title.ifBlank { "New tab" }, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(tab.url.ifBlank { "Start browsing" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                if (tab.isPrivate || tab.isSleeping) {
                                    Text(
                                        listOfNotNull(if (tab.isPrivate) "Private" else null, if (tab.isSleeping) "Sleeping" else null).joinToString("  •  "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionPopupView(
    popup: ExtensionPopupState,
    onClose: () -> Unit,
) {
    val popupBackground = MaterialTheme.colorScheme.surface.toArgb()
    val configuration = LocalConfiguration.current
    val popupWidth = (configuration.screenWidthDp - 24).coerceIn(300, 480).dp
    val popupHeight = (configuration.screenHeightDp - 96).coerceIn(360, 680).dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, end = 8.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            modifier = Modifier
                .width(popupWidth)
                .height(popupHeight),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        popup.extensionName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close extension popup")
                    }
                }
                key(popup.extensionId, popup.session) {
                    AndroidView(
                        factory = { context ->
                            GeckoView(context).apply {
                                setBackgroundColor(popupBackground)
                                setSession(popup.session)
                                popup.session.compositorController.setClearColor(popupBackground)
                            }
                        },
                        update = { view ->
                            view.setBackgroundColor(popupBackground)
                            view.session?.compositorController?.setClearColor(popupBackground)
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        onRelease = { it.releaseSession() },
                    )
                }
            }
        }
    }
}

@Composable
private fun FindInPageBar(
    find: FindInPageState,
    onQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(min = 300.dp, max = 480.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Find in page", modifier = Modifier.size(20.dp))
                BasicTextField(
                    value = find.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Enter -> {
                                    onFindNext(!event.isShiftPressed)
                                    true
                                }
                                Key.Escape -> {
                                    keyboardController?.hide()
                                    onClose()
                                    true
                                }
                                else -> false
                            }
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onFindNext(true) }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (find.query.isEmpty()) {
                                Text("Find in page", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    },
                )
                Text(
                    text = if (find.total == 0) "0/0" else "${find.current}/${find.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BrowserNavButton(Icons.Outlined.ArrowBack, "Previous match", enabled = find.total > 0) {
                    onFindNext(false)
                }
                BrowserNavButton(Icons.Outlined.ArrowForward, "Next match", enabled = find.total > 0) {
                    onFindNext(true)
                }
                BrowserNavButton(Icons.Outlined.Close, "Close find bar", enabled = true) {
                    keyboardController?.hide()
                    onClose()
                }
            }
        }
    }
}

@Composable
private fun TranslationDialog(
    translation: PageTranslationState,
    onTranslate: (String, String) -> Unit,
    onRestoreOriginal: () -> Unit,
    onNeverTranslate: () -> Unit,
    onDismiss: () -> Unit,
) {
    var targetLanguage by rememberSaveable(translation.tabId) { mutableStateOf(translation.targetLanguage) }
    var sourceLanguage by rememberSaveable(translation.tabId) { mutableStateOf(translation.sourceLanguage.orEmpty()) }
    LaunchedEffect(translation.targetLanguage) { targetLanguage = translation.targetLanguage }
    LaunchedEffect(translation.sourceLanguage) { sourceLanguage = translation.sourceLanguage.orEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
        title = { Text("Translate page") },
        text = {
            Column {
                Text(
                    translation.detectedLanguage?.let { "Detected language: $it" } ?: "Enter a target language code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = sourceLanguage,
                    onValueChange = { sourceLanguage = it },
                    label = { Text("Source language") },
                    placeholder = { Text("Auto-detected, e.g. en") },
                    singleLine = true,
                    enabled = !translation.isTranslating,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetLanguage,
                    onValueChange = { targetLanguage = it },
                    label = { Text("Target language") },
                    placeholder = { Text("en, id, ja...") },
                    singleLine = true,
                    enabled = !translation.isTranslating,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("en", "id", "de", "es", "fr", "ja", "ko", "zh").forEach { language ->
                        FilterChip(
                            selected = targetLanguage.equals(language, ignoreCase = true),
                            onClick = { targetLanguage = language },
                            label = { Text(language) },
                            enabled = !translation.isTranslating,
                        )
                    }
                }
                translation.error?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (translation.isTranslating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onTranslate(sourceLanguage, targetLanguage) },
                enabled = sourceLanguage.isNotBlank() && targetLanguage.isNotBlank() && !translation.isTranslating,
            ) { Text(if (translation.isTranslated) "Translate again" else "Translate") }
        },
        dismissButton = {
            Row {
                if (translation.isTranslated) TextButton(onClick = onRestoreOriginal) { Text("Original") }
                TextButton(onClick = onNeverTranslate) { Text("Never here") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun WebAuthnDialog(prompt: WebAuthnPromptState, onResolve: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
        title = { Text(if (prompt.isCreate) "Create a passkey?" else "Use a passkey?") },
        text = {
            Text(
                "${prompt.origin} requests a passkey for ${prompt.rpId}. Continue only if you trust this site.",
            )
        },
        confirmButton = { TextButton(onClick = { onResolve(true) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Cancel") } },
    )
}

@Composable
private fun BrowserViewport(
    tab: BrowserTabState?,
    onNavigate: (String) -> Unit,
    onReloadCrashedTab: () -> Unit,
    onViewReady: (String, GeckoView) -> Unit,
    startPage: StartPageSettings = StartPageSettings(),
    modifier: Modifier = Modifier,
    onFocus: (() -> Unit)? = null,
) {
    val pageBackground = MaterialTheme.colorScheme.surface.toArgb()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (tab?.crashed == true) {
            SiteCrashedPage(onReloadCrashedTab)
        } else if (tab == null || !tab.hasPage || tab.url.isBlank() || tab.url == "about:blank") {
            NewTabPage(onNavigate, startPage)
        } else {
            AndroidView(
                factory = { context ->
                    GeckoView(context).apply {
                        setBackgroundColor(pageBackground)
                        coverUntilFirstPaint(pageBackground)
                        setSession(tab.session)
                         setOnFocusChangeListener { _, focused -> if (focused) onFocus?.invoke() }
                         tab.session.compositorController.setClearColor(pageBackground)
                         onViewReady(tab.id, this)
                    }
                },
                    update = { view ->
                    view.setBackgroundColor(pageBackground)
                    view.session?.compositorController?.setClearColor(pageBackground)
                    if (view.session !== tab.session) {
                        view.coverUntilFirstPaint(pageBackground)
                        view.releaseSession()
                        view.setSession(tab.session)
                        view.setOnFocusChangeListener { _, focused -> if (focused) onFocus?.invoke() }
                        view.session?.compositorController?.setClearColor(pageBackground)
                    }
                    onViewReady(tab.id, view)
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { it.releaseSession() },
            )
        }
    }
}

@Composable
private fun SiteCrashedPage(onReload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("This site crashed", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "The page process stopped before the site finished loading.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onReload) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reload page")
        }
    }
}

private class ContextMenuPositionProvider(
    private val x: Int,
    private val y: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8)
        val maxY = (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8)
        return IntOffset(
            x.coerceIn(8, maxX),
            (y + 8).coerceIn(8, maxY),
        )
    }
}

private class BelowAnchorPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8)
        val maxY = (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8)
        return IntOffset(
            (anchorBounds.right - popupContentSize.width).coerceIn(8, maxX),
            (anchorBounds.bottom + 4).coerceIn(8, maxY),
        )
    }
}

@Composable
private fun BrowserContextMenuPopup(
    menu: BrowserContextMenu,
    onAction: (ContextMenuAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = ContextMenuPositionProvider(menu.x, menu.y),
        properties = PopupProperties(focusable = true),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 360.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
            if (menu.isTab) {
            DropdownMenuItem(
                text = { Text("New tab") },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.NEW_TAB) },
            )
            DropdownMenuItem(
                text = { Text("New private tab") },
                leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.NEW_PRIVATE_TAB) },
            )
            DropdownMenuItem(
                text = { Text("Duplicate tab") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.DUPLICATE_TAB) },
            )
            DropdownMenuItem(
                text = { Text("Open in split view") },
                leadingIcon = { Icon(Icons.Outlined.ViewColumn, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.OPEN_IN_SPLIT) },
            )
            DropdownMenuItem(
                text = { Text("Open in new window") },
                leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.OPEN_IN_NEW_WINDOW) },
            )
            DropdownMenuItem(
                text = { Text("Reload tab") },
                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.RELOAD_TAB) },
            )
            DropdownMenuItem(
                text = { Text(if (menu.isPinned) "Unpin tab" else "Pin tab") },
                leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.TOGGLE_TAB_PINNED) },
            )
             DropdownMenuItem(
                 text = { Text(if (menu.isSleeping) "Wake tab" else "Hibernate tab") },
                 leadingIcon = { Icon(Icons.Outlined.Pause, contentDescription = null) },
                 onClick = { onAction(ContextMenuAction.TOGGLE_TAB_SLEEPING) },
             )
             if (menu.hasActiveMedia) {
                 DropdownMenuItem(
                     text = { Text(if (menu.isAudioMuted) "Unmute tab" else "Mute tab") },
                     leadingIcon = {
                         Icon(
                             if (menu.isAudioMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                             contentDescription = null,
                         )
                     },
                     onClick = { onAction(ContextMenuAction.TOGGLE_TAB_AUDIO) },
                 )
             }
            Divider()
            DropdownMenuItem(
                text = { Text("Close tab") },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.CLOSE_TAB) },
            )
            DropdownMenuItem(
                text = { Text("Close other tabs") },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.CLOSE_OTHER_TABS) },
            )
            DropdownMenuItem(
                text = { Text("Close tabs to the left") },
                leadingIcon = { Icon(Icons.Outlined.ChevronLeft, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.CLOSE_TABS_TO_LEFT) },
            )
            DropdownMenuItem(
                text = { Text("Close tabs to the right") },
                leadingIcon = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.CLOSE_TABS_TO_RIGHT) },
            )
            } else {
                if (!menu.linkUri.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text("Open link") },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.OPEN_LINK) },
                    )
                    DropdownMenuItem(
                        text = { Text("Open link in new tab") },
                        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.OPEN_LINK_IN_NEW_TAB) },
                    )
                    DropdownMenuItem(
                        text = { Text("Open link in private tab") },
                        leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.OPEN_LINK_IN_PRIVATE_TAB) },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy link") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.COPY_LINK) },
                    )
                }
                if (!menu.resourceUri.isNullOrBlank()) {
                    val isImage = menu.resourceType == org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE
                    DropdownMenuItem(
                        text = { Text(if (isImage) "Open image in new tab" else "Open media in new tab") },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.OPEN_MEDIA_IN_NEW_TAB) },
                    )
                    DropdownMenuItem(
                        text = { Text(if (isImage) "Save image" else "Save media") },
                        leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.SAVE_MEDIA) },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy media URL") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.COPY_MEDIA_URL) },
                    )
                }
                if (!menu.textContent.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text("Copy text") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.COPY_TEXT) },
                    )
                }
                Divider()
                if (menu.canGoBack) {
                    DropdownMenuItem(
                        text = { Text("Back") },
                        leadingIcon = { Icon(Icons.Outlined.ArrowBack, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.BACK) },
                    )
                }
                if (menu.canGoForward) {
                    DropdownMenuItem(
                        text = { Text("Forward") },
                        leadingIcon = { Icon(Icons.Outlined.ArrowForward, contentDescription = null) },
                        onClick = { onAction(ContextMenuAction.FORWARD) },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Reload") },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    onClick = { onAction(ContextMenuAction.RELOAD) },
                )
                DropdownMenuItem(
                    text = { Text("Copy page URL") },
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    onClick = { onAction(ContextMenuAction.COPY_PAGE_URL) },
                )
                DropdownMenuItem(
                    text = { Text(if (menu.isBookmarked) "Remove bookmark" else "Bookmark page") },
                    leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                    onClick = { onAction(ContextMenuAction.TOGGLE_BOOKMARK) },
                )
                DropdownMenuItem(
                    text = { Text("Save page") },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    onClick = { onAction(ContextMenuAction.SAVE_PAGE) },
                )
            }
            }
        }
    }
}

@Composable
private fun CommandPaletteDialog(
    state: CommandPaletteState,
    onQueryChange: (String) -> Unit,
    onMoveSelection: (Int) -> Unit,
    onExecute: (BrowserCommandId) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val commands = BrowserCommands.all.filter {
        state.query.isBlank() || it.title.contains(state.query, true) || it.description.contains(state.query, true)
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Command palette") },
        text = {
            Column {
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> onMoveSelection(1)
                                Key.DirectionUp -> onMoveSelection(-1)
                                Key.Enter -> commands.getOrNull(state.selectedIndex)?.id?.let(onExecute)
                                Key.Escape -> onDismiss()
                                else -> return@onPreviewKeyEvent false
                            }
                            true
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.query.isBlank()) Text("Type a command", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (commands.isEmpty()) {
                    Text("No matching commands", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(commands, key = { it.id.name }) { command ->
                            val index = commands.indexOf(command)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (index == state.selectedIndex) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                    )
                                    .clickable { onExecute(command.id) }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(command.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(command.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { commands.getOrNull(state.selectedIndex)?.id?.let(onExecute) },
                enabled = commands.isNotEmpty(),
            ) { Text("Run") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun TabSwitcherDialog(
    state: TabSwitcherState,
    tabs: List<BrowserTabState>,
    onQueryChange: (String) -> Unit,
    onMoveSelection: (Int) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val visibleTabs = tabs.filter {
        state.query.isBlank() || it.title.contains(state.query, true) || it.url.contains(state.query, true)
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch tab") },
        text = {
            Column {
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> onMoveSelection(1)
                                Key.DirectionUp -> onMoveSelection(-1)
                                Key.Enter -> visibleTabs.getOrNull(state.selectedIndex)?.id?.let(onSelect)
                                Key.Escape -> onDismiss()
                                else -> return@onPreviewKeyEvent false
                            }
                            true
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.query.isBlank()) Text("Search tabs by title or URL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (visibleTabs.isEmpty()) {
                    Text("No matching tabs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(visibleTabs, key = { it.id }) { tab ->
                            val index = visibleTabs.indexOf(tab)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (index == state.selectedIndex) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                    )
                                    .clickable {
                                        onSelect(tab.id)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (tab.isPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(tab.title.ifBlank { "New tab" }, maxLines = 1)
                                    Text(tab.url.ifBlank { "Start browsing" }, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { visibleTabs.getOrNull(state.selectedIndex)?.id?.let(onSelect) },
                enabled = visibleTabs.isNotEmpty(),
            ) { Text("Switch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun QrCodeDialog(
    url: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(url) { runCatching { QrCodeGenerator.generate(url, 640) }.getOrNull() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        title = { Text("Share page") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR code for $url",
                        modifier = Modifier.size(260.dp),
                    )
                } ?: Text("This URL is too long for a QR code.")
                Spacer(Modifier.height(12.dp))
                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }
        },
        confirmButton = { TextButton(onClick = onShare) { Text("Share text") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun OfflineArticleDialog(article: OfflineArticle, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
        title = { Text(article.title.ifBlank { "Offline article" }, maxLines = 2) },
        text = {
            Text(
                article.content.ifBlank { "This offline copy has no readable text." },
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ReaderModeDialog(
    reader: ReaderModeState,
    onShare: () -> Unit,
    onSaveOffline: () -> Unit,
    onDismiss: () -> Unit,
) {
    var fontScale by rememberSaveable(reader.tabId) { mutableStateOf(1f) }
    var palette by rememberSaveable(reader.tabId) { mutableStateOf("Light") }
    val background = when (palette) {
        "Dark" -> Color(0xFF171717)
        "Sepia" -> Color(0xFFFFF8E7)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (palette) {
        "Dark" -> Color(0xFFF2F2F2)
        "Sepia" -> Color(0xFF3B3025)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(20.dp),
            color = background,
            contentColor = contentColor,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Reader mode", style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.72f))
                        Spacer(Modifier.height(4.dp))
                        Text(reader.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3)
                        Text(
                            listOfNotNull(
                                reader.language?.uppercase(),
                                reader.wordCount.takeIf { it > 0 }?.let { "$it words" },
                            ).joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.72f),
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close reader mode")
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (reader.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextButton(
                            onClick = { fontScale = (fontScale - 0.1f).coerceAtLeast(0.8f) },
                            enabled = fontScale > 0.8f,
                        ) { Text("A-") }
                        Text("${(fontScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                        TextButton(
                            onClick = { fontScale = (fontScale + 0.1f).coerceAtMost(1.5f) },
                            enabled = fontScale < 1.5f,
                        ) { Text("A+") }
                        Spacer(Modifier.width(4.dp))
                        listOf("Light", "Dark", "Sepia").forEach { option ->
                            FilterChip(
                                selected = palette == option,
                                onClick = { palette = option },
                                label = { Text(option) },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onShare) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share article")
                        }
                        IconButton(onClick = onSaveOffline) {
                            Icon(Icons.Outlined.Download, contentDescription = "Save article offline")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.18f))
                    Text(
                        reader.content.ifBlank { "This article has no readable text." },
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState()),
                        color = contentColor,
                        fontSize = (18f * fontScale).sp,
                        lineHeight = (29f * fontScale).sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyDashboardSheet(
    origins: List<PrivacyOrigin>,
    permissions: List<SitePermission>,
    totalBlocked: Int,
    currentOrigin: String?,
    onClearOrigin: (String) -> Unit,
    onForgetSite: (String) -> Unit,
    onClearAll: () -> Unit,
    onSetPermission: (String, String, String) -> Unit,
) {
    var pendingOrigin by remember { mutableStateOf<String?>(null) }
    var pendingForgetOrigin by remember { mutableStateOf<String?>(null) }
    var selectedPermissionOrigin by remember { mutableStateOf<String?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }
    SheetHeader("Privacy dashboard", "Review and clear site data managed by Dextra")
    Text(
        "Site data includes cookies, cache, DOM storage, authentication sessions, permissions, and site overrides. Bookmarks and history are kept separately.",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Blocked requests", style = MaterialTheme.typography.labelLarge)
            Text(
                "$totalBlocked third-party requests blocked this session",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    currentOrigin?.let { origin ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Current site", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(origin, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { pendingOrigin = origin }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                        Text("Clear current site data")
                    }
                    TextButton(onClick = { pendingForgetOrigin = origin }) {
                        Icon(Icons.Outlined.VisibilityOff, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Forget this site")
                    }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = { confirmClearAll = true },
            enabled = origins.isNotEmpty() || currentOrigin != null,
        ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Clear all site data")
        }
    }
    if (origins.isEmpty()) {
        EmptyLibrary(
            if (currentOrigin == null) "No saved site permissions or overrides." else "No saved decisions for other sites.",
            Icons.Outlined.Security,
        )
    } else {
        Text(
            "Saved site decisions",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(origins, key = { it.origin }) { origin ->
                ListItem(
                    modifier = Modifier.clickable { selectedPermissionOrigin = origin.origin },
                    headlineContent = { Text(origin.origin, maxLines = 1) },
                    supportingContent = {
                        Text(
                            buildList {
                                if (origin.permissionCount > 0) add("${origin.permissionCount} permission decision${if (origin.permissionCount == 1) "" else "s"}")
                                 if (origin.hasSiteOverrides) add("site overrides")
                                 if (origin.blockedCount > 0) add("${origin.blockedCount} blocked")
                            }.ifEmpty { listOf("No stored decisions") }.joinToString("  •  "),
                            maxLines = 1,
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Security, contentDescription = null) },
                     trailingContent = {
                         Row {
                             TextButton(onClick = { pendingOrigin = origin.origin }) { Text("Clear") }
                             TextButton(onClick = { pendingForgetOrigin = origin.origin }) { Text("Forget") }
                         }
                     },
                )
            }
        }
    }
    pendingForgetOrigin?.let { origin ->
        AlertDialog(
            onDismissRequest = { pendingForgetOrigin = null },
            icon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
            title = { Text("Forget this site?") },
            text = { Text("Remove site data, permissions, history, reading-list entries, downloads, and saved logins for $origin? Bookmarks are kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingForgetOrigin = null
                        onForgetSite(origin)
                    },
                ) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { pendingForgetOrigin = null }) { Text("Cancel") } },
        )
    }
    pendingOrigin?.let { origin ->
        AlertDialog(
            onDismissRequest = { pendingOrigin = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Clear site data?") },
            text = { Text("Remove cookies, storage, permissions, cache, and site overrides for $origin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingOrigin = null
                        onClearOrigin(origin)
                    },
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { pendingOrigin = null }) { Text("Cancel") } },
        )
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Clear all site data?") },
            text = { Text("This removes cookies, storage, permissions, cache, and site overrides for every site. Bookmarks and history will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = false
                        onClearAll()
                    },
                ) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") } },
        )
    }
    selectedPermissionOrigin?.let { origin ->
        val originPermissions = permissions.filter { it.origin == origin }
        AlertDialog(
            onDismissRequest = { selectedPermissionOrigin = null },
            icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
            title = { Text("Permissions for $origin") },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    if (originPermissions.isEmpty()) {
                        Text("No stored permission decisions. New requests will ask again.")
                    } else {
                        originPermissions.forEach { permission ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(sitePermissionLabel(permission.permission), style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    FilterChip(
                                        selected = permission.decision == "allow",
                                        onClick = { onSetPermission(origin, permission.permission, "allow") },
                                        label = { Text("Allow") },
                                    )
                                    FilterChip(
                                        selected = permission.decision == "block",
                                        onClick = { onSetPermission(origin, permission.permission, "block") },
                                        label = { Text("Block") },
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = { onSetPermission(origin, permission.permission, "ask") },
                                        label = { Text("Ask") },
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Ask removes the saved decision and lets the site request access again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedPermissionOrigin = null }) { Text("Done") } },
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SecurityDiagnosticsSheet(
    diagnostics: SecurityDiagnostics?,
    onSetPermission: (String, String, String) -> Unit,
) {
    SheetHeader("Security diagnostics", "Inspect the active page connection and permissions")
    if (diagnostics == null) {
        EmptyLibrary("No page is selected.", Icons.Outlined.Lock)
        return
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            diagnostics.origin ?: diagnostics.url.ifBlank { "New tab" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
        )
        Text(
            if (diagnostics.isSecure) "Secure HTTPS connection" else "Connection is not verified as secure",
            style = MaterialTheme.typography.bodyMedium,
            color = if (diagnostics.isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Connection", style = MaterialTheme.typography.titleSmall)
                Text("Host: ${diagnostics.host ?: "Unavailable"}", style = MaterialTheme.typography.bodySmall)
                Text("Port: ${diagnostics.port?.toString() ?: "Default"}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "DNS over HTTPS: ${if (diagnostics.dnsOverHttpsEnabled) diagnostics.dnsProvider else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Blocked requests: ${diagnostics.blockedRequests}", style = MaterialTheme.typography.bodySmall)
            }
        }
        diagnostics.error?.let { error ->
            Text(
                error,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        diagnostics.certificate?.let { certificate ->
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Certificate", style = MaterialTheme.typography.titleSmall)
                    Text("Subject: ${certificate.subject}", style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    Text("Issuer: ${certificate.issuer}", style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    Text("Valid: ${certificate.validFrom} - ${certificate.validTo}", style = MaterialTheme.typography.bodySmall)
                    Text("SHA-256: ${certificate.sha256}", style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
            }
        }
        if (diagnostics.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Inspecting certificate...", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Saved permissions", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleSmall)
        if (diagnostics.permissions.isEmpty()) {
            Text("No saved permissions for this origin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            diagnostics.permissions.forEach { permission ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(sitePermissionLabel(permission.permission), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = permission.decision == "allow",
                            onClick = { onSetPermission(permission.origin, permission.permission, "allow") },
                            label = { Text("Allow") },
                        )
                        FilterChip(
                            selected = permission.decision == "block",
                            onClick = { onSetPermission(permission.origin, permission.permission, "block") },
                            label = { Text("Block") },
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onSetPermission(permission.origin, permission.permission, "ask") },
                            label = { Text("Ask") },
                        )
                    }
                }
            }
        }
        diagnostics.siteSetting?.let {
            Text("This origin has custom site overrides.", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WorkspaceSheet(
    workspaces: List<TabWorkspace>,
    activeWorkspaceId: String,
    onCreate: (String) -> Unit,
    onSwitch: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var newTitle by rememberSaveable { mutableStateOf("") }
    var renameWorkspace by remember { mutableStateOf<TabWorkspace?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    SheetHeader("Workspaces & profiles", "Keep separate tab sessions and browsing containers")
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            "Each workspace keeps its own normal tabs, groups, active tab, cookies, and storage container. Private tabs are never stored in a workspace.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it.take(40) },
                modifier = Modifier.weight(1f),
                label = { Text("New workspace") },
                singleLine = true,
            )
            Button(
                onClick = {
                    onCreate(newTitle)
                    newTitle = ""
                },
                enabled = newTitle.isNotBlank(),
            ) { Text("Create") }
        }
        Spacer(Modifier.height(8.dp))
        workspaces.sortedBy { it.createdAt }.forEach { workspace ->
            ListItem(
                headlineContent = { Text(workspace.title) },
                supportingContent = {
                    Text(
                        "${workspace.tabs.size} tabs  •  ${if (workspace.contextId == null) "Shared personal container" else "Isolated container"}",
                    )
                },
                leadingContent = {
                    Icon(
                        if (workspace.id == activeWorkspaceId) Icons.Outlined.Tab else Icons.Outlined.Language,
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (workspace.id != activeWorkspaceId) {
                            TextButton(onClick = { onSwitch(workspace.id) }) { Text("Open") }
                        }
                        TextButton(onClick = {
                            renameWorkspace = workspace
                            renameText = workspace.title
                        }) { Text("Rename") }
                        if (workspaces.size > 1) {
                            IconButton(onClick = { onDelete(workspace.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete workspace")
                            }
                        }
                    }
                },
            )
        }
    }
    renameWorkspace?.let { workspace ->
        AlertDialog(
            onDismissRequest = { renameWorkspace = null },
            title = { Text("Rename workspace") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(40) },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(workspace.id, renameText)
                        renameWorkspace = null
                    },
                    enabled = renameText.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameWorkspace = null }) { Text("Cancel") } },
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SyncPreviewDialog(
    preview: SyncPreview,
    onConfirm: (SyncSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    var includeSettings by remember(preview) { mutableStateOf(preview.hasSettings) }
    var includeBookmarks by remember(preview) { mutableStateOf(preview.bookmarkCount > 0) }
    var includeHistory by remember(preview) { mutableStateOf(preview.historyCount > 0) }
    var includeReadingList by remember(preview) { mutableStateOf(preview.readingListCount > 0) }
    var includePermissions by remember(preview) { mutableStateOf(preview.permissionCount > 0) }
    var includeSiteSettings by remember(preview) { mutableStateOf(preview.siteSettingCount > 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
        title = { Text("Preview encrypted sync") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Created: ${if (preview.createdAt > 0) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(preview.createdAt)) else "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                SettingToggle("Browser settings", "${if (preview.hasSettings) "Available" else "Not included"}", includeSettings) { includeSettings = it }
                SettingToggle("Bookmarks", "${preview.bookmarkCount} entries", includeBookmarks) { includeBookmarks = it }
                SettingToggle("History", "${preview.historyCount} entries", includeHistory) { includeHistory = it }
                SettingToggle("Reading list", "${preview.readingListCount} entries", includeReadingList) { includeReadingList = it }
                SettingToggle("Site permissions", "${preview.permissionCount} decisions", includePermissions) { includePermissions = it }
                SettingToggle("Site overrides", "${preview.siteSettingCount} origins", includeSiteSettings) { includeSiteSettings = it }
                Text(
                    "Saved logins and private tabs are never part of a sync bundle.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(SyncSelection(includeSettings, includeBookmarks, includeHistory, includeReadingList, includePermissions, includeSiteSettings))
                },
            ) { Text("Import selected") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PerformanceDashboardSheet(
    metrics: PerformanceMetrics,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
) {
    SheetHeader("Performance dashboard", "Measure the current process and window")
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
            }
            OutlinedButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Copy report")
            }
        }
        PerformanceMetricCard(
            title = "Startup",
            value = metrics.startupMs?.let { "$it ms" } ?: "Waiting for first frame",
            detail = "Time from process start until the browser UI was first composed",
        )
        PerformanceMetricCard(
            title = "Memory",
            value = "${metrics.processPssMb} MB PSS",
            detail = "${metrics.availableMemoryMb} MB available${if (metrics.memoryLow) "  •  Android reports low memory" else ""}",
        )
        val jankPercent = if (metrics.frameCount == 0) 0f else metrics.jankCount * 100f / metrics.frameCount
        PerformanceMetricCard(
            title = "Frame pacing",
            value = if (metrics.frameCount == 0) "No samples" else "${metrics.averageFrameTimeMs.roundToInt()} ms average",
            detail = if (metrics.frameCount == 0) "Open this dashboard after interacting with the browser" else "${metrics.jankCount} janky frames of ${metrics.frameCount} (${String.format(java.util.Locale.US, "%.1f", jankPercent)}%)",
        )
        PerformanceMetricCard(
            title = "Window",
            value = "${metrics.windowWidthDp} x ${metrics.windowHeightDp} dp",
            detail = if (metrics.isDexLikeWindow) "Large-screen / DeX-like configuration detected" else "Compact Android window configuration",
        )
        Text(
            "Metrics are local diagnostics only and are not uploaded.",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PerformanceMetricCard(
    title: String,
    value: String,
    detail: String,
) {
    Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NetworkActivitySheet(
    activity: List<NetworkActivity>,
    tabs: List<BrowserTabState>,
    onClear: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = activity.asReversed().filter { item ->
        query.isBlank() || item.url.contains(query, ignoreCase = true) || item.kind.contains(query, ignoreCase = true) || item.status.contains(query, ignoreCase = true)
    }
    SheetHeader("Network activity", "Observable navigation, downloads, and blocking events")
    Text(
        "GeckoView does not expose every subresource request to the app. This local inspector shows the events Dextra can observe without modifying page traffic.",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            label = { Text("Filter activity") },
            singleLine = true,
        )
        TextButton(onClick = onClear, enabled = activity.isNotEmpty()) { Text("Clear") }
    }
    if (visible.isEmpty()) {
        EmptyLibrary("No observable network activity yet.", Icons.Outlined.Security)
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(visible, key = { it.id }) { item ->
                val tabTitle = tabs.firstOrNull { it.id == item.tabId }?.title
                ListItem(
                    headlineContent = { Text(item.kind.replaceFirstChar { it.uppercase() } + "  •  " + item.status, maxLines = 1) },
                    supportingContent = {
                        Column {
                            Text(item.url, maxLines = 2)
                            Text(
                                listOfNotNull(tabTitle, java.text.DateFormat.getTimeInstance().format(java.util.Date(item.timestamp))).joinToString("  •  "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingContent = { Icon(if (item.secure) Icons.Outlined.Lock else Icons.Outlined.Language, contentDescription = null) },
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun MediaSessionSheet(
    tabs: List<BrowserTabState>,
    onSelect: (String) -> Unit,
    onControl: (String, String) -> Unit,
) {
    val mediaTabs = tabs.filter { it.hasActiveMedia && !it.isPrivate }
    SheetHeader("Media sessions", "Control audio and video across open tabs")
    if (mediaTabs.isEmpty()) {
        EmptyLibrary("No active media sessions.", Icons.Outlined.PlayArrow)
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(mediaTabs, key = { it.id }) { tab ->
                ListItem(
                    modifier = Modifier.clickable { onSelect(tab.id) },
                    headlineContent = { Text(tab.title.ifBlank { "Media tab" }, maxLines = 1) },
                    supportingContent = { Text(tab.url.ifBlank { "Open page" }, maxLines = 1) },
                    leadingContent = {
                        Icon(
                            if (tab.isMediaPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onControl(tab.id, if (tab.isMediaPlaying) "pause" else "play") }) {
                                Icon(if (tab.isMediaPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = "Toggle media")
                            }
                            IconButton(onClick = { onControl(tab.id, "back") }) {
                                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Seek backward")
                            }
                            IconButton(onClick = { onControl(tab.id, "forward") }) {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = "Seek forward")
                            }
                            IconButton(onClick = { onControl(tab.id, "stop") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Stop media")
                            }
                        }
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun CompatibilityDiagnosticsSheet(
    events: List<CompatibilityEvent>,
    tabs: List<BrowserTabState>,
    onClear: () -> Unit,
) {
    SheetHeader("Compatibility diagnostics", "Observable browser warnings and page failures")
    Text(
        "The pinned GeckoView build does not expose a JavaScript console delegate. These diagnostics use official load, security, slow-script, and crash callbacks.",
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onClear, enabled = events.isNotEmpty()) { Text("Clear") }
    }
    if (events.isEmpty()) {
        EmptyLibrary("No compatibility events recorded.", Icons.Outlined.BugReport)
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(events.asReversed(), key = { it.id }) { event ->
                val tabTitle = tabs.firstOrNull { it.id == event.tabId }?.title
                ListItem(
                    headlineContent = { Text("${event.severity.uppercase()}  •  ${tabTitle ?: "Closed tab"}", maxLines = 1) },
                    supportingContent = {
                        Column {
                            Text(event.message, maxLines = 3)
                            Text(
                                java.text.DateFormat.getDateTimeInstance().format(java.util.Date(event.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingContent = {
                        Icon(
                            if (event.severity == "error") Icons.Outlined.BugReport else Icons.Outlined.Security,
                            contentDescription = null,
                            tint = if (event.severity == "error") MaterialTheme.colorScheme.error else LocalContentColor.current,
                        )
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NewTabPage(onNavigate: (String) -> Unit, settings: StartPageSettings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("D", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("The web, with room to think.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Private by default. Comfortable on a phone, expansive on DeX.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        if (settings.showQuickLinks || settings.customLinks.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (settings.showQuickLinks) {
                    QuickLink("DuckDuckGo", Icons.Outlined.Search, { onNavigate("https://duckduckgo.com") })
                    QuickLink("Wikipedia", Icons.Outlined.Public, { onNavigate("https://wikipedia.org") })
                    QuickLink("GitHub", Icons.Outlined.Language, { onNavigate("https://github.com") })
                    QuickLink("MDN", Icons.Outlined.Security, { onNavigate("https://developer.mozilla.org") })
                }
                settings.customLinks.forEach { link ->
                    QuickLink(link.label, Icons.Outlined.OpenInNew, { onNavigate(link.url) })
                }
            }
        }
        if (settings.showPrivacyTip) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Filter lists and site permissions are managed in Settings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun QuickLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) })
}

@Composable
private fun BrowserMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onReopenClosedTab: () -> Unit,
    closedTabCount: Int,
    onOpenTranslation: () -> Unit,
    onShowTabs: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowHistory: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowSiteSettings: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onOpenSecurityDiagnostics: () -> Unit,
    onOpenWorkspaces: () -> Unit,
    onOpenPerformanceDashboard: () -> Unit,
    onOpenNetworkInspector: () -> Unit,
    onShareCurrentWorkspaceTabs: () -> Unit,
    onOpenMediaManager: () -> Unit,
    onOpenCompatibilityDiagnostics: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onToggleReadingList: () -> Unit,
    onOpenQrCode: () -> Unit,
    onOpenReaderMode: () -> Unit,
    onSharePage: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPage: () -> Unit,
    onExportHtml: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onCaptureLongScreenshot: () -> Unit,
    onInstallWebApp: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onSaveOffline: () -> Unit,
    onShowSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 8.dp)
                .size(1.dp),
        ) {
            DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                DropdownMenuItem(
                    text = { Text("New private tab") },
                    leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                    onClick = onNewPrivateTab,
                )
                DropdownMenuItem(
                    text = { Text("Reopen closed tab") },
                    leadingIcon = { Icon(Icons.Outlined.Tab, contentDescription = null) },
                    enabled = closedTabCount > 0,
                    onClick = onReopenClosedTab,
                )
                DropdownMenuItem(
                    text = { Text("All tabs") },
                    leadingIcon = { Icon(Icons.Outlined.Tab, contentDescription = null) },
                    onClick = onShowTabs,
                )
                DropdownMenuItem(
                    text = { Text("Search tabs") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    onClick = onOpenTabSwitcher,
                )
                DropdownMenuItem(
                    text = { Text("Command palette") },
                    leadingIcon = { Icon(Icons.Outlined.Menu, contentDescription = null) },
                    onClick = onOpenCommandPalette,
                )
        DropdownMenuItem(
                    text = { Text("Bookmarks") },
                    leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                    onClick = onShowBookmarks,
                )
                DropdownMenuItem(
                    text = { Text("History") },
                    leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    onClick = onShowHistory,
                )
        DropdownMenuItem(
            text = { Text("Downloads") },
            leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
            onClick = onShowDownloads,
        )
                DropdownMenuItem(
                    text = { Text("Site settings") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    onClick = onShowSiteSettings,
                )
                DropdownMenuItem(
                    text = { Text("Privacy dashboard") },
                    leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    onClick = onOpenPrivacyDashboard,
                )
                DropdownMenuItem(
                    text = { Text("Site info & security") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    onClick = onOpenSecurityDiagnostics,
                )
                DropdownMenuItem(
                    text = { Text("Workspaces") },
                    leadingIcon = { Icon(Icons.Outlined.Tab, contentDescription = null) },
                    onClick = onOpenWorkspaces,
                )
                DropdownMenuItem(
                    text = { Text("Performance dashboard") },
                    leadingIcon = { Icon(Icons.Outlined.Speed, contentDescription = null) },
                    onClick = onOpenPerformanceDashboard,
                )
                DropdownMenuItem(
                    text = { Text("Network activity") },
                    leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    onClick = onOpenNetworkInspector,
                )
                DropdownMenuItem(
                    text = { Text("Share open tabs") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = onShareCurrentWorkspaceTabs,
                )
                DropdownMenuItem(
                    text = { Text("Media sessions") },
                    leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                    onClick = onOpenMediaManager,
                )
                DropdownMenuItem(
                    text = { Text("Compatibility diagnostics") },
                    leadingIcon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
                    onClick = onOpenCompatibilityDiagnostics,
                )
                DropdownMenuItem(
                    text = { Text("Save to reading list") },
                    leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                    onClick = onToggleReadingList,
                )
                DropdownMenuItem(
                    text = { Text("Show QR code") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = onOpenQrCode,
                )
                DropdownMenuItem(
                    text = { Text("Reader mode") },
                    leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                    onClick = onOpenReaderMode,
                )
                DropdownMenuItem(
                    text = { Text("Translate page") },
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    onClick = onOpenTranslation,
                )
                DropdownMenuItem(
                    text = { Text("Share page") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = onSharePage,
                )
                DropdownMenuItem(
                    text = { Text("Export as PDF") },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    onClick = onExportPdf,
                )
                DropdownMenuItem(
                    text = { Text("Print page") },
                    leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
                    onClick = onPrintPage,
                )
                DropdownMenuItem(
                    text = { Text("Save page as HTML") },
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    onClick = onExportHtml,
                )
                DropdownMenuItem(
                    text = { Text("Save screenshot") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = onCaptureScreenshot,
                )
                DropdownMenuItem(
                    text = { Text("Save long screenshot") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = onCaptureLongScreenshot,
                )
                DropdownMenuItem(
                    text = { Text("Install site as app") },
                    leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    onClick = onInstallWebApp,
                )
                DropdownMenuItem(
                    text = { Text("Picture-in-Picture") },
                    leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    onClick = onEnterPictureInPicture,
                )
                DropdownMenuItem(
                    text = { Text("Save for offline reading") },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    onClick = onSaveOffline,
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    onClick = onShowSettings,
                )
            }
        }
    }
}

@Composable
private fun FullPageScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun BookmarksPage(
    bookmarks: List<Bookmark>,
    onOpen: (String) -> Unit,
    onSetBookmarkFolder: (Bookmark, String?) -> Unit,
    onUpdateBookmark: (Bookmark, String, String?) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onExportBookmarks: () -> Unit,
    onImportBookmarks: () -> Unit,
    onBack: () -> Unit,
) {
    var selectedFolder by rememberSaveable { mutableStateOf("") }
    var folderBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var editBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editFolder by rememberSaveable { mutableStateOf("") }
    val folders = bookmarks.mapNotNull { it.folder?.takeIf(String::isNotBlank) }.distinct().sorted()
    val visibleBookmarks = bookmarks.filter { selectedFolder.isBlank() || it.folder == selectedFolder }

    FullPageScaffold("Bookmarks", "Organize saved pages and folders", onBack) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onImportBookmarks) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
                TextButton(onClick = onExportBookmarks, enabled = bookmarks.isNotEmpty()) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedFolder.isBlank(),
                    onClick = { selectedFolder = "" },
                    label = { Text("All") },
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolder == folder,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                    )
                }
            }
            if (visibleBookmarks.isEmpty()) {
                EmptyLibrary(
                    if (bookmarks.isEmpty()) "Bookmarks you save will appear here." else "No bookmarks in this folder.",
                    Icons.Outlined.BookmarkBorder,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(visibleBookmarks, key = { it.id }) { bookmark ->
                        var menuExpanded by remember(bookmark.id) { mutableStateOf(false) }
                        ListItem(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clickable { onOpen(bookmark.url) },
                            headlineContent = { Text(bookmark.title.ifBlank { BrowserUrl.displayValue(bookmark.url) }, maxLines = 1) },
                            supportingContent = {
                                Text(listOfNotNull(bookmark.folder, bookmark.url).joinToString("  •  "), maxLines = 1)
                            },
                            leadingContent = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                            trailingContent = {
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "Bookmark actions")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit bookmark") },
                                            onClick = {
                                                menuExpanded = false
                                                editBookmark = bookmark
                                                editTitle = bookmark.title
                                                editFolder = bookmark.folder.orEmpty()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("No folder") },
                                            onClick = {
                                                menuExpanded = false
                                                onSetBookmarkFolder(bookmark, null)
                                            },
                                        )
                                        folders.filter { it != bookmark.folder }.forEach { folder ->
                                            DropdownMenuItem(
                                                text = { Text(folder) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onSetBookmarkFolder(bookmark, folder)
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("New folder...") },
                                            onClick = {
                                                menuExpanded = false
                                                folderBookmark = bookmark
                                                folderName = ""
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete bookmark") },
                                            onClick = {
                                                menuExpanded = false
                                                onDeleteBookmark(bookmark)
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
    folderBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { folderBookmark = null },
            title = { Text("Move bookmark") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetBookmarkFolder(bookmark, folderName)
                        folderBookmark = null
                    },
                    enabled = folderName.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { folderBookmark = null }) { Text("Cancel") } },
        )
    }
    editBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { editBookmark = null },
            title = { Text("Edit bookmark") },
            text = {
                Column {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = editFolder, onValueChange = { editFolder = it }, label = { Text("Folder (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateBookmark(bookmark, editTitle, editFolder)
                        editBookmark = null
                    },
                    enabled = editTitle.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editBookmark = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HistoryPage(
    history: List<HistoryEntry>,
    onOpen: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleHistory = history.filter {
        query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true)
    }
    FullPageScaffold("History", "Search and manage recently visited pages", onBack) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Search history") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                )
                TextButton(onClick = onClearHistory, enabled = history.isNotEmpty()) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Clear all")
                }
            }
            if (visibleHistory.isEmpty()) {
                EmptyLibrary(
                    if (history.isEmpty()) "Pages you visit will appear here." else "No matching history.",
                    Icons.Outlined.History,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(visibleHistory, key = { it.id }) { entry ->
                        ListItem(
                            modifier = Modifier.padding(horizontal = 12.dp).clickable { onOpen(entry.url) },
                            headlineContent = { Text(entry.title.ifBlank { BrowserUrl.displayValue(entry.url) }, maxLines = 1) },
                            supportingContent = { Text(entry.url, maxLines = 1) },
                            leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                            trailingContent = {
                                IconButton(onClick = { onDeleteHistoryEntry(entry) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete history entry")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsPage(
    downloads: List<DownloadEntry>,
    onOpen: (DownloadEntry) -> Unit,
    onShare: (DownloadEntry) -> Unit,
    onToggle: (DownloadEntry) -> Unit,
    onSetPriority: (DownloadEntry, Int) -> Unit,
    onSetWifiOnly: (DownloadEntry, Boolean) -> Unit,
    onSchedule: (DownloadEntry, Long?) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onCancel: (DownloadEntry) -> Unit,
    onRemove: (DownloadEntry) -> Unit,
    onClearCompleted: () -> Unit,
    onBack: () -> Unit,
) {
    FullPageScaffold("Downloads", "Track, filter, and manage saved files", onBack) {
        DownloadsSheet(
            downloads = downloads,
            onOpen = onOpen,
            onShare = onShare,
            onToggle = onToggle,
            onSetPriority = onSetPriority,
            onSetWifiOnly = onSetWifiOnly,
            onSchedule = onSchedule,
            onPauseAll = onPauseAll,
            onResumeAll = onResumeAll,
            onCancel = onCancel,
            onRemove = onRemove,
            onClearCompleted = onClearCompleted,
            showHeader = false,
        )
    }
}

@Composable
private fun KeyboardShortcutsPage(
    shortcutBindings: Map<BrowserCommandId, KeyChord>,
    onSetKeyboardShortcut: (BrowserCommandId, KeyChord?) -> Unit,
    capturingShortcut: BrowserCommandId?,
    onBeginKeyboardShortcutCapture: (BrowserCommandId) -> Unit,
    onCancelKeyboardShortcutCapture: () -> Unit,
    onBack: () -> Unit,
) {
    FullPageScaffold("Keyboard shortcuts", "Configure keyboard and mouse-friendly browser actions", onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Use Ctrl, Alt, or Meta combinations for browser actions. Shortcuts can be changed without affecting website text fields.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            capturingShortcut?.let { command ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Press a key for ${BrowserCommands.all.firstOrNull { it.id == command }?.title ?: command.name}",
                            Modifier.weight(1f),
                        )
                        TextButton(onClick = onCancelKeyboardShortcutCapture) { Text("Cancel") }
                    }
                }
            }
            BrowserCommands.all.forEach { command ->
                val binding = shortcutBindings[command.id]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(command.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            binding?.displayName() ?: "Not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onBeginKeyboardShortcutCapture(command.id) }) { Text("Change") }
                    TextButton(
                        onClick = { onSetKeyboardShortcut(command.id, null) },
                        enabled = binding != DefaultKeyboardShortcuts.bindings[command.id],
                    ) { Text("Reset") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TabsSheet(
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
) {
    SheetHeader("Your tabs", "${tabs.size} open")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onNewTab) { Icon(Icons.Outlined.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("New tab") }
        AssistChip(onClick = onNewPrivateTab, label = { Text("Private") }, leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) })
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .padding(top = 12.dp),
    ) {
        items(tabs.sortedWith(compareByDescending { it.pinned }), key = { it.id }) { tab ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTab(tab.id) }
                    .background(
                        if (tab.isPrivate) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        else Color.Transparent,
                    )
                    .padding(horizontal = 12.dp),
                headlineContent = { Text(tab.title.ifBlank { "New tab" }, maxLines = 1) },
                supportingContent = { Text(tab.url.ifBlank { "Start browsing" }, maxLines = 1) },
                leadingContent = { Icon(if (tab.isPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Language, contentDescription = null) },
                trailingContent = { IconButton(onClick = { onCloseTab(tab.id) }) { Icon(Icons.Outlined.Close, contentDescription = "Close tab") } },
            )
            if (tab.id != tabs.lastOrNull()?.id) Divider(modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun LibrarySheet(
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    readingList: List<com.dwicao.dextra.data.ReadingListEntry>,
    onOpen: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit,
    onSetBookmarkFolder: (Bookmark, String?) -> Unit,
    onUpdateBookmark: (Bookmark, String, String?) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onExportBookmarks: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onDeleteReadingListEntry: (com.dwicao.dextra.data.ReadingListEntry) -> Unit,
    onSetReadingListRead: (com.dwicao.dextra.data.ReadingListEntry, Boolean) -> Unit,
    onOpenOffline: (com.dwicao.dextra.data.ReadingListEntry) -> Unit,
     sessionSnapshots: List<com.dwicao.dextra.data.SessionSnapshot>,
     sessionTimeline: List<com.dwicao.dextra.data.SessionSnapshot>,
     onCreateSessionSnapshot: (String) -> Unit,
     onRestoreSessionSnapshot: (com.dwicao.dextra.data.SessionSnapshot) -> Unit,
     onDeleteSessionSnapshot: (com.dwicao.dextra.data.SessionSnapshot) -> Unit,
     onDeleteSessionTimeline: (com.dwicao.dextra.data.SessionSnapshot) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var createSnapshot by remember { mutableStateOf(false) }
    var snapshotTitle by rememberSaveable { mutableStateOf("") }
    SheetHeader("Library", "Keep the useful parts close")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onImportBackup) { Text("Restore backup") }
        TextButton(onClick = onExportBackup) { Text("Backup") }
    }
    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 20.dp) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bookmarks") }, icon = { Icon(Icons.Outlined.Bookmark, null) })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("History") }, icon = { Icon(Icons.Outlined.History, null) })
        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Reading list") }, icon = { Icon(Icons.Outlined.BookmarkBorder, null) })
        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Sessions") }, icon = { Icon(Icons.Outlined.Tab, null) })
        Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Recovery") }, icon = { Icon(Icons.Outlined.History, null) })
    }
    if (selectedTab == 0) {
        var selectedFolder by rememberSaveable { mutableStateOf("") }
        var folderBookmark by remember { mutableStateOf<Bookmark?>(null) }
        var folderName by rememberSaveable { mutableStateOf("") }
        var editBookmark by remember { mutableStateOf<Bookmark?>(null) }
        var editTitle by rememberSaveable { mutableStateOf("") }
        var editFolder by rememberSaveable { mutableStateOf("") }
        val folders = bookmarks.mapNotNull { it.folder?.takeIf(String::isNotBlank) }.distinct().sorted()
        val visibleBookmarks = bookmarks.filter { selectedFolder.isBlank() || it.folder == selectedFolder }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedFolder.isBlank(),
                onClick = { selectedFolder = "" },
                label = { Text("All") },
            )
            folders.forEach { folder ->
                FilterChip(
                    selected = selectedFolder == folder,
                    onClick = { selectedFolder = folder },
                    label = { Text(folder) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onImportBookmarks) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Import")
            }
            TextButton(onClick = onExportBookmarks, enabled = bookmarks.isNotEmpty()) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Export")
            }
        }
        if (visibleBookmarks.isEmpty()) {
            EmptyLibrary("Bookmarks you save will appear here.", Icons.Outlined.BookmarkBorder)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                items(visibleBookmarks, key = { it.id }) { bookmark ->
                    var folderMenuExpanded by remember { mutableStateOf(false) }
                    ListItem(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable { onOpen(bookmark.url) },
                        headlineContent = { Text(bookmark.title.ifBlank { BrowserUrl.displayValue(bookmark.url) }, maxLines = 1) },
                        supportingContent = {
                            Text(
                                listOfNotNull(bookmark.folder, bookmark.url).joinToString("  •  "),
                                maxLines = 1,
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Bookmark, null) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { folderMenuExpanded = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "Organize bookmark")
                                }
                                DropdownMenu(
                                    expanded = folderMenuExpanded,
                                    onDismissRequest = { folderMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit bookmark") },
                                        onClick = {
                                            folderMenuExpanded = false
                                            editBookmark = bookmark
                                            editTitle = bookmark.title
                                            editFolder = bookmark.folder.orEmpty()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("No folder") },
                                        onClick = {
                                            folderMenuExpanded = false
                                            onSetBookmarkFolder(bookmark, null)
                                        },
                                    )
                                    folders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder) },
                                            onClick = {
                                                folderMenuExpanded = false
                                                onSetBookmarkFolder(bookmark, folder)
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("New folder...") },
                                        onClick = {
                                            folderMenuExpanded = false
                                            folderBookmark = bookmark
                                            folderName = ""
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete bookmark") },
                                        onClick = {
                                            folderMenuExpanded = false
                                            onDeleteBookmark(bookmark)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
        folderBookmark?.let { bookmark ->
            AlertDialog(
                onDismissRequest = { folderBookmark = null },
                title = { Text("Move bookmark") },
                text = {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSetBookmarkFolder(bookmark, folderName)
                            folderBookmark = null
                        },
                        enabled = folderName.isNotBlank(),
                    ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { folderBookmark = null }) { Text("Cancel") } },
            )
        }
        editBookmark?.let { bookmark ->
            AlertDialog(
                onDismissRequest = { editBookmark = null },
                title = { Text("Edit bookmark") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editFolder,
                            onValueChange = { editFolder = it },
                            label = { Text("Folder (optional)") },
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUpdateBookmark(bookmark, editTitle, editFolder)
                            editBookmark = null
                        },
                        enabled = editTitle.isNotBlank(),
                    ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { editBookmark = null }) { Text("Cancel") } },
            )
        }
    } else if (selectedTab == 1) {
        var historyQuery by rememberSaveable { mutableStateOf("") }
        val visibleHistory = history.filter {
            historyQuery.isBlank() || it.title.contains(historyQuery, true) || it.url.contains(historyQuery, true)
        }
        OutlinedTextField(
            value = historyQuery,
            onValueChange = { historyQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            label = { Text("Search history") },
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClearHistory, enabled = history.isNotEmpty()) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Clear history")
            }
        }
        if (visibleHistory.isEmpty()) EmptyLibrary(
            if (history.isEmpty()) "Pages you visit will appear here." else "No matching history.",
            Icons.Outlined.History,
        )
        else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                items(visibleHistory, key = { it.id }) { entry ->
                    ListItem(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable { onOpen(entry.url) },
                        headlineContent = { Text(entry.title.ifBlank { BrowserUrl.displayValue(entry.url) }, maxLines = 1) },
                        supportingContent = { Text(entry.url, maxLines = 1) },
                        leadingContent = { Icon(Icons.Outlined.History, null) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteHistoryEntry(entry) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete history entry")
                            }
                        },
                    )
                }
            }
        }
    } else if (selectedTab == 2) {
        if (readingList.isEmpty()) {
            EmptyLibrary("Pages saved for later will appear here.", Icons.Outlined.BookmarkBorder)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                items(readingList, key = { it.id }) { entry ->
                    ListItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        headlineContent = { Text(entry.title.ifBlank { BrowserUrl.displayValue(entry.url) }, maxLines = 1) },
                        supportingContent = { Text(entry.url, maxLines = 1) },
                        leadingContent = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = entry.isRead,
                                    onCheckedChange = { onSetReadingListRead(entry, it) },
                                )
                                IconButton(onClick = { onOpen(entry.url) }) {
                                    Icon(Icons.Outlined.OpenInNew, contentDescription = "Open saved page")
                                }
                                if (entry.offlinePath != null) {
                                    IconButton(onClick = { onOpenOffline(entry) }) {
                                        Icon(Icons.Outlined.Download, contentDescription = "Open offline copy")
                                    }
                                }
                                IconButton(onClick = { onDeleteReadingListEntry(entry) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove from reading list")
                                }
                            }
                        },
                    )
                }
            }
        }
    } else if (selectedTab == 3) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = { snapshotTitle = ""; createSnapshot = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Save current tabs")
            }
        }
        if (sessionSnapshots.isEmpty()) {
            EmptyLibrary("Named tab sessions will appear here.", Icons.Outlined.Tab)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                items(sessionSnapshots, key = { it.id }) { snapshot ->
                    ListItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        headlineContent = { Text(snapshot.title, maxLines = 1) },
                        supportingContent = { Text("${snapshot.tabs.size} tabs  •  ${snapshot.tabGroups.size} groups", maxLines = 1) },
                        leadingContent = { Icon(Icons.Outlined.Tab, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onRestoreSessionSnapshot(snapshot) }) { Text("Restore") }
                                IconButton(onClick = { onDeleteSessionSnapshot(snapshot) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${snapshot.title}")
                                }
                            }
                        },
                    )
                }
            }
        }
        createSnapshot.takeIf { it }?.let {
            AlertDialog(
                onDismissRequest = { createSnapshot = false },
                title = { Text("Save tab session") },
                text = {
                    OutlinedTextField(
                        value = snapshotTitle,
                        onValueChange = { snapshotTitle = it },
                        label = { Text("Session name") },
                        placeholder = { Text("For example: Work research") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onCreateSessionSnapshot(snapshotTitle)
                            createSnapshot = false
                        },
                        enabled = snapshotTitle.isNotBlank(),
                    ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { createSnapshot = false }) { Text("Cancel") } },
            )
        }
    } else {
        if (sessionTimeline.isEmpty()) {
            EmptyLibrary("Automatic recovery points will appear after the app is backgrounded.", Icons.Outlined.History)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                items(sessionTimeline, key = { it.id }) { snapshot ->
                    ListItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        headlineContent = { Text(snapshot.title, maxLines = 1) },
                        supportingContent = {
                            Text(
                                "${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(snapshot.createdAt))}  •  ${snapshot.tabs.size} tabs",
                                maxLines = 1,
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onRestoreSessionSnapshot(snapshot) }) { Text("Restore") }
                                IconButton(onClick = { onDeleteSessionTimeline(snapshot) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete recovery point")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun DownloadsSheet(
    downloads: List<DownloadEntry>,
    onOpen: (DownloadEntry) -> Unit,
    onShare: (DownloadEntry) -> Unit,
    onToggle: (DownloadEntry) -> Unit,
    onSetPriority: (DownloadEntry, Int) -> Unit,
    onSetWifiOnly: (DownloadEntry, Boolean) -> Unit,
    onSchedule: (DownloadEntry, Long?) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onCancel: (DownloadEntry) -> Unit,
    onRemove: (DownloadEntry) -> Unit,
    onClearCompleted: () -> Unit,
    showHeader: Boolean = true,
) {
    var pendingDelete by remember { mutableStateOf<DownloadEntry?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var selectedType by rememberSaveable { mutableStateOf("All") }
    var privateOnly by rememberSaveable { mutableStateOf(false) }
    val activeCount = downloads.count { it.status in setOf(DownloadStatus.QUEUED.label, DownloadStatus.DOWNLOADING.label, DownloadStatus.PAUSED.label) }
    val visibleDownloads = downloads.filter { download ->
        (selectedStatus == "All" || download.status == selectedStatus) &&
            (selectedType == "All" || downloadType(download) == selectedType) &&
            (!privateOnly || download.isPrivate) &&
            (query.isBlank() || download.fileName.contains(query, true) || download.url.contains(query, true))
    }
    if (showHeader) {
        SheetHeader("Downloads", if (activeCount == 0) "Files saved by Dextra" else "$activeCount active")
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        label = { Text("Search downloads") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("All" to null).plus(DownloadStatus.values().map { it.label to it.label }).forEach { (label, status) ->
            FilterChip(
                selected = selectedStatus == (status ?: "All"),
                onClick = { selectedStatus = status ?: "All" },
                label = { Text(label) },
            )
        }
        FilterChip(
            selected = privateOnly,
            onClick = { privateOnly = !privateOnly },
            label = { Text("Private") },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("All", "Images", "Video", "Audio", "Documents", "Other").forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { selectedType = type },
                label = { Text(type) },
            )
        }
    }
    if (downloads.any { it.status == DownloadStatus.COMPLETE.label || it.status == DownloadStatus.CANCELED.label }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClearCompleted) { Text("Clear completed") }
        }
    }
    if (activeCount > 0) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onPauseAll) { Text("Pause all") }
            TextButton(onClick = onResumeAll) { Text("Resume paused") }
        }
    }
    if (downloads.any { it.isPrivate }) {
        Text(
            "Downloads from private tabs are still saved to device storage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    if (visibleDownloads.isEmpty()) {
        EmptyLibrary("Files you download will appear here.", Icons.Outlined.Download)
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            items(visibleDownloads, key = { it.downloadId }) { download ->
                val isComplete = download.status == DownloadStatus.COMPLETE.label
                val isActive = download.status in setOf(
                    DownloadStatus.QUEUED.label,
                    DownloadStatus.DOWNLOADING.label,
                    DownloadStatus.PAUSED.label,
                )
                val progress = if (download.totalBytes > 0) {
                    (download.bytesDownloaded.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    var optionsExpanded by remember { mutableStateOf(false) }
                    ListItem(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable(enabled = isComplete) { onOpen(download) },
                        headlineContent = { Text(download.fileName, maxLines = 1) },
                        supportingContent = {
                            Text(
                                buildDownloadSummary(download),
                                maxLines = 2,
                                color = if (download.status == DownloadStatus.FAILED.label) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    IconButton(onClick = { optionsExpanded = true }) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "Download options")
                                    }
                                    DropdownMenu(
                                        expanded = optionsExpanded,
                                        onDismissRequest = { optionsExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Priority: ${downloadPriorityLabel(download.priority)}") },
                                            onClick = { optionsExpanded = false },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("High priority") },
                                            onClick = { optionsExpanded = false; onSetPriority(download, 2) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Normal priority") },
                                            onClick = { optionsExpanded = false; onSetPriority(download, 1) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Low priority") },
                                            onClick = { optionsExpanded = false; onSetPriority(download, 0) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (download.wifiOnly) "Allow metered network" else "Wi-Fi only") },
                                            onClick = { optionsExpanded = false; onSetWifiOnly(download, !download.wifiOnly) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Start now") },
                                            enabled = isActive,
                                            onClick = { optionsExpanded = false; onSchedule(download, null) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Schedule in 1 hour") },
                                            enabled = isActive,
                                            onClick = { optionsExpanded = false; onSchedule(download, System.currentTimeMillis() + 60 * 60 * 1000L) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Schedule tomorrow") },
                                            enabled = isActive,
                                            onClick = { optionsExpanded = false; onSchedule(download, System.currentTimeMillis() + 24 * 60 * 60 * 1000L) },
                                        )
                                    }
                                }
                                if (isComplete) {
                                    IconButton(onClick = { onOpen(download) }) {
                                        Icon(Icons.Outlined.OpenInNew, contentDescription = "Open ${download.fileName}")
                                    }
                                    IconButton(onClick = { onShare(download) }) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share ${download.fileName}")
                                    }
                                } else if (download.status == DownloadStatus.FAILED.label) {
                                    IconButton(onClick = { onToggle(download) }) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = "Retry ${download.fileName}")
                                    }
                                } else if (isActive) {
                                    IconButton(onClick = { onToggle(download) }) {
                                        Icon(
                                            if (download.status == DownloadStatus.PAUSED.label) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                            contentDescription = if (download.status == DownloadStatus.PAUSED.label) "Resume ${download.fileName}" else "Pause ${download.fileName}",
                                        )
                                    }
                                    IconButton(onClick = { onCancel(download) }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Cancel ${download.fileName}")
                                    }
                                }
                                IconButton(onClick = { pendingDelete = download }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${download.fileName}")
                                }
                            }
                        },
                    )
                    if (isActive && progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp),
                        )
                    }
                }
            }
        }
    }
    pendingDelete?.let { download ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = { Text("Remove ${download.fileName} and its downloaded file?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onRemove(download)
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
        )
    }
    Spacer(Modifier.height(24.dp))
}

private fun buildDownloadSummary(download: DownloadEntry): String {
    val status = if (download.isPrivate) "${download.status}  •  Private tab" else download.status
    val size = if (download.totalBytes > 0) {
        "${formatBytes(download.bytesDownloaded)} / ${formatBytes(download.totalBytes)}"
    } else if (download.bytesDownloaded > 0) {
        formatBytes(download.bytesDownloaded)
    } else {
        ""
    }
    val speed = download.speedBytesPerSecond.takeIf { it > 0 }?.let { "${formatBytes(it)}/s" }.orEmpty()
    val priority = "Priority ${downloadPriorityLabel(download.priority)}"
    val network = if (download.wifiOnly) "Wi-Fi only" else "Any network"
    val schedule = download.scheduledAt?.let { "Starts ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}" }.orEmpty()
    return listOf(status, size, speed, priority, network, schedule, download.reason.orEmpty())
        .filter(String::isNotBlank)
        .joinToString("  •  ")
}

private fun downloadPriorityLabel(priority: Int): String = when (priority.coerceIn(0, 2)) {
    2 -> "High"
    1 -> "Normal"
    else -> "Low"
}

private fun downloadType(download: DownloadEntry): String = when {
    download.mimeType?.startsWith("image/") == true -> "Images"
    download.mimeType?.startsWith("video/") == true -> "Video"
    download.mimeType?.startsWith("audio/") == true -> "Audio"
    download.mimeType in setOf(
        "application/pdf",
        "application/zip",
        "application/json",
        "text/plain",
        "text/csv",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ) -> "Documents"
    else -> "Other"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_024 * 1_024 -> "%.1f KB".format(bytes / 1_024f)
    bytes < 1_024 * 1_024 * 1_024 -> "%.1f MB".format(bytes / (1_024f * 1_024f))
    else -> "%.1f GB".format(bytes / (1_024f * 1_024f * 1_024f))
}

@Composable
private fun EmptyLibrary(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExtensionInstallDialog(
    prompt: ExtensionInstallPrompt,
    onResolve: (Boolean, Boolean, Boolean) -> Unit,
) {
    var allowPrivateBrowsing by remember(prompt.id) { mutableStateOf(false) }
    var allowDataCollection by remember(prompt.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onResolve(false, false, false) },
        icon = { Icon(Icons.Outlined.Extension, contentDescription = null) },
        title = { Text("Install ${prompt.name}?") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Version ${prompt.version} requests access to run in Dextra.")
                Spacer(Modifier.height(12.dp))
                SettingToggle(
                    title = "Allow in private tabs",
                    summary = "Run this extension while private browsing is active",
                    checked = allowPrivateBrowsing,
                    onCheckedChange = { allowPrivateBrowsing = it },
                )
                if (prompt.permissions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Extension permissions", style = MaterialTheme.typography.titleSmall)
                    prompt.permissions.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (prompt.origins.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Website access", style = MaterialTheme.typography.titleSmall)
                    prompt.origins.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (prompt.dataCollectionPermissions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SettingToggle(
                        title = "Share technical data",
                        summary = prompt.dataCollectionPermissions.joinToString(", "),
                        checked = allowDataCollection,
                        onCheckedChange = { allowDataCollection = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onResolve(true, allowPrivateBrowsing, allowDataCollection) }) {
                Text("Install")
            }
        },
        dismissButton = { TextButton(onClick = { onResolve(false, false, false) }) { Text("Cancel") } },
    )
}

@Composable
private fun CrashReportDialog(
    report: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
        title = { Text("Previous crash report") },
        text = {
            Text(
                text = report,
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = { TextButton(onClick = onCopy) { Text("Copy") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ExtensionInstallProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        icon = { CircularProgressIndicator(modifier = Modifier.size(32.dp)) },
        title = { Text("Installing extension") },
        text = { Text("Downloading and validating the .xpi package...") },
        confirmButton = {},
    )
}

@Composable
private fun MediaPermissionDialog(
    prompt: MediaPermissionPrompt,
    onResolve: (Boolean) -> Unit,
) {
    val requested = buildList {
        if (prompt.hasVideo) add("camera")
        if (prompt.hasAudio) add("microphone")
    }.joinToString(" and ")
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
        title = { Text("Allow media access?") },
        text = { Text("${prompt.origin} wants to use your $requested.") },
        confirmButton = { TextButton(onClick = { onResolve(true) }) { Text("Allow") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Block") } },
    )
}

@Composable
private fun WebPushPermissionDialog(
    origin: String,
    onResolve: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
        title = { Text("Allow Web Push?") },
        text = { Text("$origin wants to register a background notification subscription. You can revoke it later in Settings.") },
        confirmButton = { TextButton(onClick = { onResolve(true) }) { Text("Allow") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Block") } },
    )
}

@Composable
private fun ExtensionUpdateDialog(
    prompt: ExtensionUpdatePrompt,
    onResolve: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        icon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
        title = { Text("Update ${prompt.name}?") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("This extension requests additional access to update.")
                if (prompt.permissions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("New extension permissions", style = MaterialTheme.typography.titleSmall)
                    prompt.permissions.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (prompt.origins.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("New website access", style = MaterialTheme.typography.titleSmall)
                    prompt.origins.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (prompt.dataCollectionPermissions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("New data collection access", style = MaterialTheme.typography.titleSmall)
                    prompt.dataCollectionPermissions.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onResolve(true) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Cancel") } },
    )
}

@Composable
private fun SiteSettingsDialog(
    tab: BrowserTabState?,
    setting: SiteSetting?,
    globalDesktopSites: Boolean,
    globalHttpsOnly: Boolean,
    globalCookieBannerMode: Int,
    globalAdBlocking: Boolean,
    globalUserScripts: Boolean,
    onSetDesktopOverride: (Boolean?) -> Unit,
    onSetAdBlockingOverride: (Boolean?) -> Unit,
    onSetUserScriptsOverride: (Boolean?) -> Unit,
    onSetZoomOverride: (Int?) -> Unit,
    onSetHttpsOnlyOverride: (Boolean?) -> Unit,
    onSetCookieBannerModeOverride: (Int?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (tab == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text("Site settings") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(tab.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(16.dp))
                SiteOverrideToggle("Desktop site", setting?.desktopSites, globalDesktopSites, onSetDesktopOverride)
                Spacer(Modifier.height(12.dp))
                SiteOverrideToggle("HTTPS-only", setting?.httpsOnly, globalHttpsOnly, onSetHttpsOnlyOverride)
                Spacer(Modifier.height(12.dp))
                CookieBannerOverride(
                    mode = setting?.cookieBannerMode,
                    globalMode = globalCookieBannerMode,
                    onChange = onSetCookieBannerModeOverride,
                )
                Spacer(Modifier.height(12.dp))
                SiteOverrideToggle("Ad blocking", setting?.adBlockingEnabled, globalAdBlocking, onSetAdBlockingOverride)
                Spacer(Modifier.height(12.dp))
                SiteOverrideToggle("Userscripts", setting?.userScriptsEnabled, globalUserScripts, onSetUserScriptsOverride)
                Spacer(Modifier.height(16.dp))
                Text("Page zoom", style = MaterialTheme.typography.titleSmall)
                Text("Global/default: 100%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = setting?.zoomPercent == null,
                        onClick = { onSetZoomOverride(null) },
                        label = { Text("Default") },
                    )
                    listOf(80, 90, 100, 110, 125, 150, 175, 200).forEach { zoom ->
                        FilterChip(
                            selected = setting?.zoomPercent == zoom,
                            onClick = { onSetZoomOverride(zoom) },
                            label = { Text("$zoom%") },
                        )
                    }
                }
                if (setting != null) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onClear) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Clear site overrides")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun SiteOverrideToggle(
    title: String,
    override: Boolean?,
    globalValue: Boolean,
    onChange: (Boolean?) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text("Global is currently ${if (globalValue) "on" else "off"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(selected = override == null, onClick = { onChange(null) }, label = { Text("Global") })
            FilterChip(selected = override == true, onClick = { onChange(true) }, label = { Text("On") })
            FilterChip(selected = override == false, onClick = { onChange(false) }, label = { Text("Off") })
        }
    }
}

@Composable
private fun CookieBannerOverride(
    mode: Int?,
    globalMode: Int,
    onChange: (Int?) -> Unit,
) {
    Text("Cookie consent", style = MaterialTheme.typography.titleSmall)
    Text(
        "Global is currently ${cookieBannerModeLabel(globalMode)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(selected = mode == null, onClick = { onChange(null) }, label = { Text("Global") })
        FilterChip(
            selected = mode == ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED,
            onClick = { onChange(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED) },
            label = { Text("Off") },
        )
        FilterChip(
            selected = mode == ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT,
            onClick = { onChange(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT) },
            label = { Text("Reject") },
        )
        FilterChip(
            selected = mode == ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT,
            onClick = { onChange(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT) },
            label = { Text("Reject or accept") },
        )
    }
}

private fun cookieBannerModeLabel(mode: Int): String = when (mode) {
    ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT -> "Reject"
    ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT -> "Reject or accept"
    else -> "Off"
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: ThemeMode,
    searchEngine: SearchEngine,
    customSearchEngines: List<CustomSearchEngine>,
     selectedCustomSearchEngineId: String?,
     desktopSites: Boolean,
     httpsOnly: Boolean,
     downloadDirectoryUri: String?,
    onPickDownloadDirectory: () -> Unit,
    onResetDownloadDirectory: () -> Unit,
     homepage: String,
     startPage: StartPageSettings,
     onSetStartPageQuickLinks: (Boolean) -> Unit,
     onSetStartPagePrivacyTip: (Boolean) -> Unit,
     onAddStartPageLink: (String, String) -> Unit,
     onRemoveStartPageLink: (StartPageLink) -> Unit,
    tabBarWithAddressBar: Boolean,
    verticalTabs: Boolean,
    adBlockingEnabled: Boolean,
    adBlockFilters: List<AdBlockFilter>,
    onSetTheme: (ThemeMode) -> Unit,
    onSetSearchEngine: (SearchEngine) -> Unit,
    onSetCustomSearchEngine: (CustomSearchEngine) -> Unit,
    onAddCustomSearchEngine: (String, String) -> Unit,
    onRemoveCustomSearchEngine: (CustomSearchEngine) -> Unit,
     onExportSync: (String, SyncSelection) -> Unit,
    onImportSync: (String) -> Unit,
    accessibilityTextScale: Float,
    highContrast: Boolean,
    reduceMotion: Boolean,
    onSetAccessibilityTextScale: (Float) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    webDav: WebDavSettingsState,
    onSaveWebDavSettings: (String, String, String, String, String, Int) -> Unit,
     onDisableWebDav: () -> Unit,
     onRunWebDavSync: () -> Unit,
      onResolveWebDavConflict: (String) -> Unit,
      onSetDesktopSites: (Boolean) -> Unit,
      onSetHttpsOnly: (Boolean) -> Unit,
     cookieBannerMode: Int,
     onSetCookieBannerMode: (Int) -> Unit,
     historyRetentionDays: Int,
     downloadRetentionDays: Int,
     recoveryRetentionDays: Int,
     clearSiteDataOnExit: Boolean,
     privacyCleanupAllowlist: List<String>,
     onSetHistoryRetentionDays: (Int) -> Unit,
     onSetDownloadRetentionDays: (Int) -> Unit,
     onSetRecoveryRetentionDays: (Int) -> Unit,
     onSetClearSiteDataOnExit: (Boolean) -> Unit,
     onAddPrivacyCleanupAllowlist: (String) -> Unit,
     onRemovePrivacyCleanupAllowlist: (String) -> Unit,
     onRunPrivacyCleanup: () -> Unit,
     onSetHomepage: (String) -> Unit,
    onClearSitePermissions: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
     credentials: List<StoredCredential>,
     addresses: List<StoredAddress>,
     credentialCount: Int,
    credentialVaultUnlocked: Boolean,
    webPushSubscriptions: List<StoredWebPushSubscription>,
    onDeleteCredential: (StoredCredential) -> Unit,
    onRequestCredentialUnlock: () -> Unit,
    onLockCredentialVault: () -> Unit,
     onClearCredentials: () -> Unit,
     onDeleteAddress: (StoredAddress) -> Unit,
     onSaveAddress: (StoredAddress) -> Unit,
     onClearAddresses: () -> Unit,
    onCopyCredentialUsername: (StoredCredential) -> Unit,
    onCopyCredentialPassword: (StoredCredential) -> Unit,
    onRevokeWebPushSubscription: (StoredWebPushSubscription) -> Unit,
    onClearWebPushSubscriptions: () -> Unit,
    installedWebApps: List<InstalledWebApp>,
    onOpenInstalledWebApp: (InstalledWebApp) -> Unit,
    onRefreshInstalledWebApp: (InstalledWebApp) -> Unit,
    onUninstallWebApp: (InstalledWebApp) -> Unit,
    onSetTabBarWithAddressBar: (Boolean) -> Unit,
    onSetVerticalTabs: (Boolean) -> Unit,
    onOpenKeyboardShortcuts: () -> Unit,
    dnsOverHttpsEnabled: Boolean,
    onSetDnsOverHttpsEnabled: (Boolean) -> Unit,
    dnsProvider: DnsProvider,
    onSetDnsProvider: (DnsProvider) -> Unit,
    onSetAdBlockingEnabled: (Boolean) -> Unit,
    onSetAdBlockFilterEnabled: (AdBlockFilter, Boolean) -> Unit,
    onRefreshAdBlockFilters: () -> Unit,
    onAddAdBlockFilter: (String) -> Unit,
    onRemoveAdBlockFilter: (AdBlockFilter) -> Unit,
    userScriptUrls: List<String>,
    disabledUserScriptUrls: Set<String>,
    onAddUserScript: (String) -> Unit,
    onSetUserScriptEnabled: (String, Boolean) -> Unit,
    onRefreshUserScripts: () -> Unit,
    onRemoveUserScript: (String) -> Unit,
    installedExtensions: List<InstalledExtension>,
    extensionInstallInProgress: Boolean,
    onInstallExtension: (String) -> Unit,
    onOpenFirefoxAddons: () -> Unit,
    onSetExtensionEnabled: (String, Boolean) -> Unit,
     onSetExtensionPrivateBrowsing: (String, Boolean) -> Unit,
     onRevokeExtensionOptionalPermissions: (String) -> Unit,
     onUpdateExtension: (String) -> Unit,
    onOpenExtensionOptions: (String) -> Unit,
    onUninstallExtension: (String) -> Unit,
) {
    var pendingExtensionRemoval by remember { mutableStateOf<InstalledExtension?>(null) }
    var permissionExtension by remember { mutableStateOf<InstalledExtension?>(null) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tune Dextra for the way you work",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
     SettingSection("Appearance") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onSetTheme(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = {
                        Icon(
                            when (mode) {
                                ThemeMode.SYSTEM -> Icons.Outlined.LightMode
                                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                                ThemeMode.DARK -> Icons.Outlined.DarkMode
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
     }
     SettingSection("Accessibility") {
         Text(
             "Increase browser text size, improve contrast, and reduce motion without changing website content.",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
         )
         Spacer(Modifier.height(8.dp))
         Row(
             modifier = Modifier.horizontalScroll(rememberScrollState()),
             horizontalArrangement = Arrangement.spacedBy(6.dp),
         ) {
             listOf(1f, 1.1f, 1.25f, 1.5f).forEach { scale ->
                 FilterChip(
                     selected = accessibilityTextScale == scale,
                     onClick = { onSetAccessibilityTextScale(scale) },
                     label = { Text("${(scale * 100).roundToInt()}%") },
                 )
             }
         }
         Spacer(Modifier.height(8.dp))
         SettingToggle(
             title = "High contrast",
             summary = "Use stronger contrast for browser controls",
             checked = highContrast,
             onCheckedChange = onSetHighContrast,
         )
         Spacer(Modifier.height(8.dp))
         SettingToggle(
             title = "Reduce motion",
             summary = "Minimize hover and scrolling animations",
             checked = reduceMotion,
             onCheckedChange = onSetReduceMotion,
         )
         Spacer(Modifier.height(8.dp))
         Text(
             "Per-site page text size is available under Site settings > Page zoom.",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
         )
     }
      SettingSection("Search engine") {
         Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             SearchEngine.values().filter { it != SearchEngine.CUSTOM }.forEach { engine ->
                 FilterChip(selected = searchEngine == engine, onClick = { onSetSearchEngine(engine) }, label = { Text(engine.label) })
             }
         }
         if (customSearchEngines.isNotEmpty()) {
             Spacer(Modifier.height(8.dp))
             Text("Custom engines", style = MaterialTheme.typography.titleSmall)
             customSearchEngines.forEach { engine ->
                 ListItem(
                     headlineContent = { Text(engine.label, maxLines = 1) },
                     supportingContent = { Text(engine.searchUrl, maxLines = 1) },
                     leadingContent = { Icon(Icons.Outlined.Search, contentDescription = null) },
                     trailingContent = {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             RadioButton(
                                 selected = searchEngine == SearchEngine.CUSTOM && selectedCustomSearchEngineId == engine.id,
                                 onClick = { onSetCustomSearchEngine(engine) },
                             )
                             IconButton(onClick = { onRemoveCustomSearchEngine(engine) }) {
                                 Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${engine.label}")
                             }
                         }
                     },
                 )
             }
         }
         var customEngineName by rememberSaveable { mutableStateOf("") }
         var customEngineUrl by rememberSaveable { mutableStateOf("") }
         Spacer(Modifier.height(8.dp))
         Row(
             modifier = Modifier.fillMaxWidth(),
             verticalAlignment = Alignment.CenterVertically,
             horizontalArrangement = Arrangement.spacedBy(8.dp),
         ) {
             OutlinedTextField(
                 value = customEngineName,
                 onValueChange = { customEngineName = it },
                 modifier = Modifier.weight(0.38f),
                 label = { Text("Name") },
                 singleLine = true,
             )
             OutlinedTextField(
                 value = customEngineUrl,
                 onValueChange = { customEngineUrl = it },
                 modifier = Modifier.weight(0.62f),
                 label = { Text("HTTPS search URL, use %s") },
                 singleLine = true,
             )
             Button(
                 onClick = {
                     onAddCustomSearchEngine(customEngineName, customEngineUrl)
                     customEngineName = ""
                     customEngineUrl = ""
                 },
                 enabled = customEngineName.isNotBlank() && customEngineUrl.isNotBlank(),
             ) { Text("Add") }
         }
      }
      SettingSection("Browsing") {
        var homepageDraft by rememberSaveable { mutableStateOf(homepage) }
        LaunchedEffect(homepage) { homepageDraft = homepage }
        Text("Homepage", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = homepageDraft,
                onValueChange = { homepageDraft = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Web address") },
            )
            Button(
                onClick = { onSetHomepage(homepageDraft) },
                enabled = homepageDraft.isNotBlank(),
                modifier = Modifier.height(56.dp),
            ) { Text("Save") }
        }
        Spacer(Modifier.height(14.dp))
         SettingToggle(
             title = "Request desktop sites",
            summary = "Use desktop layouts for the current DeX or tablet profile",
            checked = desktopSites,
             onCheckedChange = onSetDesktopSites,
         )
         Spacer(Modifier.height(14.dp))
         SettingToggle(
             title = "HTTPS-only mode",
             summary = "Upgrade HTTP navigations to HTTPS and refuse insecure top-level loads",
             checked = httpsOnly,
             onCheckedChange = onSetHttpsOnly,
         )
         Spacer(Modifier.height(14.dp))
         Text("Cookie consent", style = MaterialTheme.typography.titleSmall)
         Text(
             "Choose how GeckoView handles supported cookie banners by default.",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
         )
         Row(
             modifier = Modifier.horizontalScroll(rememberScrollState()),
             horizontalArrangement = Arrangement.spacedBy(6.dp),
         ) {
             listOf(
                 0 to "Off",
                 1 to "Reject",
                 2 to "Reject or accept",
             ).forEach { (mode, label) ->
                 FilterChip(selected = cookieBannerMode == mode, onClick = { onSetCookieBannerMode(mode) }, label = { Text(label) })
             }
         }
        Spacer(Modifier.height(14.dp))
        SettingToggle(
            title = "Tab bar beside address bar",
            summary = "Keep tabs visible beside the address bar on wide windows",
            checked = tabBarWithAddressBar,
            onCheckedChange = onSetTabBarWithAddressBar,
        )
        Spacer(Modifier.height(14.dp))
        SettingToggle(
            title = "Vertical tab strip",
            summary = "Show tabs in a collapsible sidebar on wide windows",
            checked = verticalTabs,
            onCheckedChange = onSetVerticalTabs,
        )
        Spacer(Modifier.height(14.dp))
         TextButton(onClick = onClearSitePermissions) {
             Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
             Spacer(Modifier.width(6.dp))
             Text("Clear site permissions")
          }
          TextButton(onClick = onOpenPrivacyDashboard) {
             Icon(Icons.Outlined.Security, contentDescription = null)
             Spacer(Modifier.width(6.dp))
              Text("Open privacy dashboard")
          }
       }
        SettingSection("Privacy automation") {
            Text(
                "Automatically remove old local records. A cleanup allowlist preserves selected history and download records. Recovery snapshots follow their own retention period. Site-data-on-exit clears Gecko site storage for all profiles when the app really leaves the foreground.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            RetentionChooser("History retention", historyRetentionDays, onSetHistoryRetentionDays)
            RetentionChooser("Download retention", downloadRetentionDays, onSetDownloadRetentionDays)
            RetentionChooser("Recovery retention", recoveryRetentionDays, onSetRecoveryRetentionDays, includeNever = false)
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                title = "Clear site data on exit",
                summary = "Clear cookies and storage when Dextra leaves the foreground",
                checked = clearSiteDataOnExit,
                onCheckedChange = onSetClearSiteDataOnExit,
            )
            Spacer(Modifier.height(8.dp))
            var allowlistOrigin by rememberSaveable { mutableStateOf("") }
            Text("Cleanup allowlist", style = MaterialTheme.typography.titleSmall)
            privacyCleanupAllowlist.forEach { origin ->
                ListItem(
                    headlineContent = { Text(origin, maxLines = 1) },
                    leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { onRemovePrivacyCleanupAllowlist(origin) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove allowlist entry")
                        }
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = allowlistOrigin,
                    onValueChange = { allowlistOrigin = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("HTTP(S) origin") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onAddPrivacyCleanupAllowlist(allowlistOrigin)
                        allowlistOrigin = ""
                    },
                    enabled = allowlistOrigin.isNotBlank(),
                ) { Text("Keep") }
            }
            OutlinedButton(onClick = onRunPrivacyCleanup, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Run cleanup now")
            }
        }
        SettingSection("Start page") {
           Text(
               "Customize the page shown in new tabs without changing your homepage.",
               style = MaterialTheme.typography.bodySmall,
               color = MaterialTheme.colorScheme.onSurfaceVariant,
           )
           Spacer(Modifier.height(8.dp))
           SettingToggle(
               title = "Default quick links",
               summary = "Show DuckDuckGo, Wikipedia, GitHub, and MDN",
               checked = startPage.showQuickLinks,
               onCheckedChange = onSetStartPageQuickLinks,
           )
           Spacer(Modifier.height(8.dp))
           SettingToggle(
               title = "Privacy reminder",
               summary = "Show a reminder that filter lists and permissions are configurable",
               checked = startPage.showPrivacyTip,
               onCheckedChange = onSetStartPagePrivacyTip,
           )
           if (startPage.customLinks.isNotEmpty()) {
               Spacer(Modifier.height(10.dp))
               startPage.customLinks.forEach { link ->
                   ListItem(
                       headlineContent = { Text(link.label, maxLines = 1) },
                       supportingContent = { Text(link.url, maxLines = 1) },
                       leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                       trailingContent = {
                           IconButton(onClick = { onRemoveStartPageLink(link) }) {
                               Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${link.label}")
                           }
                       },
                   )
               }
           }
           var startLinkLabel by rememberSaveable { mutableStateOf("") }
           var startLinkUrl by rememberSaveable { mutableStateOf("") }
           Spacer(Modifier.height(8.dp))
           Row(
               modifier = Modifier.fillMaxWidth(),
               horizontalArrangement = Arrangement.spacedBy(8.dp),
               verticalAlignment = Alignment.CenterVertically,
           ) {
               OutlinedTextField(
                   value = startLinkLabel,
                   onValueChange = { startLinkLabel = it },
                   modifier = Modifier.weight(0.38f),
                   label = { Text("Name") },
                   singleLine = true,
               )
               OutlinedTextField(
                   value = startLinkUrl,
                   onValueChange = { startLinkUrl = it },
                   modifier = Modifier.weight(0.62f),
                   label = { Text("HTTPS URL") },
                   singleLine = true,
               )
               Button(
                   onClick = {
                       onAddStartPageLink(startLinkLabel, startLinkUrl)
                       startLinkLabel = ""
                       startLinkUrl = ""
                   },
                   enabled = startLinkLabel.isNotBlank() && startLinkUrl.isNotBlank(),
               ) { Text("Add") }
           }
       }
       SettingSection("Sync Center") {
           Text(
               "Move bookmarks, history, reading list, site settings, and browser preferences between devices. The bundle is encrypted with a passphrase; saved logins and private tabs are never included.",
               style = MaterialTheme.typography.bodySmall,
               color = MaterialTheme.colorScheme.onSurfaceVariant,
           )
            var syncAction by rememberSaveable { mutableStateOf<String?>(null) }
            var syncPassphrase by rememberSaveable { mutableStateOf("") }
            var syncSettings by rememberSaveable { mutableStateOf(true) }
            var syncBookmarks by rememberSaveable { mutableStateOf(true) }
            var syncHistory by rememberSaveable { mutableStateOf(true) }
            var syncReadingList by rememberSaveable { mutableStateOf(true) }
            var syncPermissions by rememberSaveable { mutableStateOf(true) }
            var syncSiteSettings by rememberSaveable { mutableStateOf(true) }
           Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               Button(onClick = { syncPassphrase = ""; syncAction = "export" }) {
                   Text("Export encrypted")
               }
               OutlinedButton(onClick = { syncPassphrase = ""; syncAction = "import" }) {
                   Text("Import encrypted")
               }
           }
           syncAction?.let { action ->
               AlertDialog(
                   onDismissRequest = { syncAction = null },
                   title = { Text(if (action == "export") "Export encrypted sync" else "Import encrypted sync") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = syncPassphrase,
                                onValueChange = { syncPassphrase = it },
                                label = { Text("Passphrase") },
                                supportingText = { Text("Use at least 8 characters") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                            )
                            if (action == "export") {
                                Text("Include in export", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall)
                                SettingToggle("Browser settings", "Theme, search, privacy, and accessibility settings", syncSettings) { syncSettings = it }
                                SettingToggle("Bookmarks", "Saved pages and folders", syncBookmarks) { syncBookmarks = it }
                                SettingToggle("History", "Recent normal browsing history", syncHistory) { syncHistory = it }
                                SettingToggle("Reading list", "Saved reading-list entries", syncReadingList) { syncReadingList = it }
                                SettingToggle("Site permissions", "Per-container permission decisions", syncPermissions) { syncPermissions = it }
                                SettingToggle("Site overrides", "Per-site desktop, blocker, script, zoom, and translation overrides", syncSiteSettings) { syncSiteSettings = it }
                            } else {
                                Text("The next step will decrypt and preview the bundle before anything is imported.", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                   },
                   confirmButton = {
                       TextButton(
                            onClick = {
                                if (action == "export") {
                                    onExportSync(
                                        syncPassphrase,
                                        SyncSelection(syncSettings, syncBookmarks, syncHistory, syncReadingList, syncPermissions, syncSiteSettings),
                                    )
                                } else onImportSync(syncPassphrase)
                               syncAction = null
                           },
                           enabled = syncPassphrase.length >= 8,
                       ) { Text(if (action == "export") "Choose file" else "Choose file") }
                   },
                   dismissButton = { TextButton(onClick = { syncAction = null }) { Text("Cancel") } },
               )
           }
        }
        SettingSection("Automatic WebDAV sync") {
            Text(
                "Sync the encrypted browser bundle to a WebDAV server on a periodic network connection. The server URL must use HTTPS; passwords and the bundle passphrase are stored in Android Keystore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            var webDavEndpoint by rememberSaveable { mutableStateOf(webDav.endpoint) }
            var webDavRemoteFile by rememberSaveable { mutableStateOf(webDav.remoteFile) }
            var webDavUsername by rememberSaveable { mutableStateOf(webDav.username) }
            var webDavPassword by rememberSaveable { mutableStateOf("") }
            var webDavPassphrase by rememberSaveable { mutableStateOf("") }
            var webDavInterval by rememberSaveable { mutableStateOf(webDav.intervalHours.toString()) }
            LaunchedEffect(webDav.endpoint, webDav.remoteFile, webDav.username, webDav.intervalHours) {
                webDavEndpoint = webDav.endpoint
                webDavRemoteFile = webDav.remoteFile
                webDavUsername = webDav.username
                webDavInterval = webDav.intervalHours.toString()
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(webDavEndpoint, { webDavEndpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("WebDAV server URL") }, placeholder = { Text("https://cloud.example/dav") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(webDavRemoteFile, { webDavRemoteFile = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Remote filename") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(webDavUsername, { webDavUsername = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Username (optional)") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(webDavPassword, { webDavPassword = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (webDav.configured) "Password (enter to replace)" else "Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(webDavPassphrase, { webDavPassphrase = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Sync passphrase") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(webDavInterval, { webDavInterval = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Interval in hours (1-168)") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSaveWebDavSettings(
                            webDavEndpoint,
                            webDavUsername,
                            webDavPassword,
                            webDavRemoteFile,
                            webDavPassphrase,
                            webDavInterval.toIntOrNull() ?: 24,
                        )
                        webDavPassword = ""
                        webDavPassphrase = ""
                    },
                    enabled = webDavEndpoint.startsWith("https://", ignoreCase = true) && webDavRemoteFile.isNotBlank() && webDavPassphrase.length >= 8,
                ) { Text(if (webDav.configured) "Update" else "Enable") }
                if (webDav.configured) {
                    TextButton(onClick = onRunWebDavSync) { Text("Sync now") }
                    TextButton(onClick = onDisableWebDav) { Text("Disable") }
                }
            }
             if (webDav.configured) {
                 Text(
                     if (webDav.lastSyncAt == null) "Waiting for the first scheduled sync" else "Last sync: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(webDav.lastSyncAt))}",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                 )
                 if (webDav.conflictPending) {
                     Card(
                         modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                     ) {
                         Column(Modifier.padding(12.dp)) {
                             Text("Sync conflict detected", style = MaterialTheme.typography.titleSmall)
                             Text(
                                 "Another device changed this bundle while Dextra was uploading. Choose how to continue.",
                                 style = MaterialTheme.typography.bodySmall,
                             )
                             Row(
                                 modifier = Modifier.horizontalScroll(rememberScrollState()),
                                 horizontalArrangement = Arrangement.spacedBy(6.dp),
                             ) {
                                 TextButton(onClick = { onResolveWebDavConflict("remote") }) { Text("Keep remote") }
                                 TextButton(onClick = { onResolveWebDavConflict("local") }) { Text("Keep local") }
                                 TextButton(onClick = { onResolveWebDavConflict("merge") }) { Text("Merge") }
                             }
                         }
                     }
                 }
                 webDav.lastError?.let { error ->
                     Text(
                         "Last sync error: $error",
                         modifier = Modifier.padding(top = 6.dp),
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.error,
                     )
                 }
             }
        }
        SettingSection("Downloads") {
          Text(
              "Choose a folder that Dextra can write to. Existing files are not moved.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(8.dp))
          Text(
              if (downloadDirectoryUri == null) "Default: device Downloads/Dextra" else "Custom folder selected",
              style = MaterialTheme.typography.bodyMedium,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = onPickDownloadDirectory) { Text("Choose folder") }
              if (downloadDirectoryUri != null) {
                  TextButton(onClick = onResetDownloadDirectory) { Text("Reset") }
              }
          }
      }
      SettingSection("Saved logins") {
          Text(
              "Credentials are encrypted with Android Keystore and are available to GeckoView autocomplete on matching origins. Private tabs never save logins.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (credentialCount == 0) {
              Text("No saved logins.", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else if (!credentialVaultUnlocked) {
              Text(
                  "$credentialCount saved login${if (credentialCount == 1) "" else "s"} are locked.",
                  modifier = Modifier.padding(top = 10.dp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(onClick = onRequestCredentialUnlock, modifier = Modifier.padding(top = 8.dp)) {
                  Icon(Icons.Outlined.Lock, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text("Unlock with biometrics")
              }
          } else {
              credentials.forEach { credential ->
                  ListItem(
                      headlineContent = { Text(credential.origin, maxLines = 1) },
                      supportingContent = { Text(credential.username.ifBlank { "Username not provided" }, maxLines = 1) },
                      leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                      trailingContent = {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                              TextButton(onClick = { onCopyCredentialUsername(credential) }) { Text("User") }
                              TextButton(onClick = { onCopyCredentialPassword(credential) }) { Text("Password") }
                              IconButton(onClick = { onDeleteCredential(credential) }) {
                                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete saved login")
                              }
                          }
                      },
                  )
              }
              TextButton(onClick = onClearCredentials) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                  Spacer(Modifier.width(6.dp))
                  Text("Delete all saved logins")
              }
              TextButton(onClick = onLockCredentialVault) {
                  Icon(Icons.Outlined.Lock, contentDescription = null)
                  Spacer(Modifier.width(6.dp))
                  Text("Lock saved logins")
              }
          }
      }
       SettingSection("Address autofill") {
           Text(
               "Addresses are encrypted at rest and are offered to compatible forms through GeckoView. Private tabs never save or receive addresses.",
               style = MaterialTheme.typography.bodySmall,
               color = MaterialTheme.colorScheme.onSurfaceVariant,
           )
           var addressId by rememberSaveable { mutableStateOf("") }
           var addressName by rememberSaveable { mutableStateOf("") }
           var addressStreet by rememberSaveable { mutableStateOf("") }
           var addressCity by rememberSaveable { mutableStateOf("") }
           var addressRegion by rememberSaveable { mutableStateOf("") }
           var addressPostal by rememberSaveable { mutableStateOf("") }
           var addressCountry by rememberSaveable { mutableStateOf("") }
           var addressPhone by rememberSaveable { mutableStateOf("") }
           var addressEmail by rememberSaveable { mutableStateOf("") }
           fun loadAddress(address: StoredAddress) {
               addressId = address.id
               addressName = address.name
               addressStreet = address.streetAddress
               addressCity = address.addressLevel2
               addressRegion = address.addressLevel1
               addressPostal = address.postalCode
               addressCountry = address.country
               addressPhone = address.tel
               addressEmail = address.email
           }
           fun resetAddress() {
               addressId = ""
               addressName = ""
               addressStreet = ""
               addressCity = ""
               addressRegion = ""
               addressPostal = ""
               addressCountry = ""
               addressPhone = ""
               addressEmail = ""
           }
           Text("Add or edit a saved address", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall)
           OutlinedTextField(addressName, { addressName = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Label / full name") }, singleLine = true)
           OutlinedTextField(addressStreet, { addressStreet = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Street address") }, singleLine = true)
           Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               OutlinedTextField(addressCity, { addressCity = it }, modifier = Modifier.weight(1f), label = { Text("City") }, singleLine = true)
               OutlinedTextField(addressRegion, { addressRegion = it }, modifier = Modifier.weight(1f), label = { Text("Region") }, singleLine = true)
           }
           Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               OutlinedTextField(addressPostal, { addressPostal = it }, modifier = Modifier.weight(1f), label = { Text("Postal code") }, singleLine = true)
               OutlinedTextField(addressCountry, { addressCountry = it }, modifier = Modifier.weight(1f), label = { Text("Country") }, singleLine = true)
           }
           Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               OutlinedTextField(addressPhone, { addressPhone = it }, modifier = Modifier.weight(1f), label = { Text("Phone") }, singleLine = true)
               OutlinedTextField(addressEmail, { addressEmail = it }, modifier = Modifier.weight(1f), label = { Text("Email") }, singleLine = true)
           }
           Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
               Button(
                    onClick = {
                        val existing = addresses.firstOrNull { it.id == addressId }
                        onSaveAddress(
                            StoredAddress(
                                id = addressId.ifBlank { UUID.randomUUID().toString() },
                                name = addressName.trim(),
                                givenName = existing?.givenName.orEmpty(),
                                additionalName = existing?.additionalName.orEmpty(),
                                familyName = existing?.familyName.orEmpty(),
                                organization = existing?.organization.orEmpty(),
                                streetAddress = addressStreet.trim(),
                                addressLevel1 = addressRegion.trim(),
                                addressLevel2 = addressCity.trim(),
                                addressLevel3 = existing?.addressLevel3.orEmpty(),
                               postalCode = addressPostal.trim(),
                               country = addressCountry.trim(),
                               tel = addressPhone.trim(),
                               email = addressEmail.trim(),
                               updatedAt = System.currentTimeMillis(),
                           ),
                       )
                       resetAddress()
                   },
                   enabled = addressName.isNotBlank(),
               ) { Text(if (addressId.isBlank()) "Save address" else "Update address") }
               if (addressId.isNotBlank()) TextButton(onClick = ::resetAddress) { Text("Cancel edit") }
           }
           if (addresses.isEmpty()) {
               Text("No saved addresses.", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
           } else {
               addresses.forEach { address ->
                   ListItem(
                       modifier = Modifier.clickable { loadAddress(address) },
                       headlineContent = { Text(address.name, maxLines = 1) },
                       supportingContent = {
                           Text(
                               listOfNotNull(address.streetAddress, address.postalCode, address.country, address.email)
                                   .filter(String::isNotBlank)
                                   .joinToString("  •  "),
                               maxLines = 2,
                           )
                       },
                       leadingContent = { Icon(Icons.Outlined.Home, contentDescription = null) },
                       trailingContent = {
                           IconButton(onClick = { onDeleteAddress(address) }) {
                               Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete saved address")
                           }
                       },
                   )
               }
               TextButton(onClick = onClearAddresses) {
                   Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                   Spacer(Modifier.width(6.dp))
                   Text("Delete all addresses")
               }
           }
       }
       SettingSection("Web Push") {
          Text(
              "Review subscriptions created by websites. Delivery is handled by the configured push provider; revoke entries here when they are no longer needed.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (webPushSubscriptions.isEmpty()) {
              Text("No Web Push subscriptions.", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
              webPushSubscriptions.forEach { subscription ->
                  ListItem(
                      headlineContent = { Text(subscription.origin, maxLines = 1) },
                      supportingContent = { Text(subscription.scope, maxLines = 1) },
                      leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                      trailingContent = {
                          TextButton(onClick = { onRevokeWebPushSubscription(subscription) }) { Text("Revoke") }
                      },
                  )
              }
              TextButton(onClick = onClearWebPushSubscriptions) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                  Spacer(Modifier.width(6.dp))
                  Text("Revoke all subscriptions")
              }
          }
      }
      SettingSection("Installed web apps") {
          Text(
              "Installed web apps open in their own Android document window and task.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (installedWebApps.isEmpty()) {
              Text("No web apps installed.", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
              installedWebApps.forEach { app ->
                  ListItem(
                      headlineContent = { Text(app.name, maxLines = 1) },
                      supportingContent = { Text(app.startUrl, maxLines = 1) },
                      leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                      trailingContent = {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                              TextButton(onClick = { onOpenInstalledWebApp(app) }) { Text("Open") }
                              IconButton(onClick = { onRefreshInstalledWebApp(app) }) {
                                  Icon(Icons.Outlined.Refresh, contentDescription = "Refresh ${app.name}")
                              }
                              IconButton(onClick = { onUninstallWebApp(app) }) {
                                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Uninstall ${app.name}")
                              }
                          }
                      },
                  )
              }
          }
      }
      SettingSection("Keyboard shortcuts") {
          Text(
              "Configure browser actions without filling the settings page with every binding.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(8.dp))
          Button(onClick = onOpenKeyboardShortcuts) {
              Icon(Icons.Outlined.Settings, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("Open keyboard shortcuts")
          }
      }
     SettingSection("Network") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DnsProvider.values().forEach { provider ->
                FilterChip(
                    selected = dnsProvider == provider,
                    onClick = { onSetDnsProvider(provider) },
                    label = { Text(provider.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingToggle(
            title = "DNS over HTTPS: ${dnsProvider.label}",
            summary = "Use the selected provider for DNS lookups. Off by default.",
            checked = dnsOverHttpsEnabled,
            onCheckedChange = onSetDnsOverHttpsEnabled,
        )
    }
    SettingSection("Privacy") {
        Text(
            "Dextra uses GeckoView tracking protection. Filter lists and userscripts are fetched over HTTPS only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        SettingToggle(
            title = "Ad blocking",
            summary = "Block third-party hosts listed by your enabled filter lists",
            checked = adBlockingEnabled,
            onCheckedChange = onSetAdBlockingEnabled,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Filter lists", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onRefreshAdBlockFilters) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Refresh")
            }
        }
        var filterUrl by rememberSaveable { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = filterUrl,
                onValueChange = { filterUrl = it },
                modifier = Modifier.weight(1f),
                label = { Text("HTTPS filter URL") },
                singleLine = true,
            )
            Button(
                onClick = {
                    onAddAdBlockFilter(filterUrl)
                    filterUrl = ""
                },
                enabled = filterUrl.startsWith("https://", ignoreCase = true),
            ) { Text("Add") }
        }
        adBlockFilters.forEach { filter ->
            ListItem(
                headlineContent = { Text(filter.name, maxLines = 1) },
                supportingContent = { Text(filter.url, maxLines = 1) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = filter.enabled,
                            onCheckedChange = { onSetAdBlockFilterEnabled(filter, it) },
                        )
                        IconButton(onClick = { onRemoveAdBlockFilter(filter) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${filter.name}")
                        }
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Userscripts", style = MaterialTheme.typography.titleSmall)
        Text(
            "Userscripts can change page content and behavior. Keep them disabled unless you trust the source.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        var userScriptUrl by rememberSaveable { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = userScriptUrl,
                onValueChange = { userScriptUrl = it },
                modifier = Modifier.weight(1f),
                label = { Text("HTTPS userscript URL") },
                singleLine = true,
            )
            Button(
                onClick = {
                    onAddUserScript(userScriptUrl)
                    userScriptUrl = ""
                },
                enabled = userScriptUrl.startsWith("https://", ignoreCase = true),
            ) { Text("Add") }
        }
        userScriptUrls.forEach { url ->
            ListItem(
                headlineContent = { Text(url, maxLines = 1) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = url !in disabledUserScriptUrls,
                            onCheckedChange = { onSetUserScriptEnabled(url, it) },
                        )
                        IconButton(onClick = { onRemoveUserScript(url) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove userscript")
                        }
                    }
                },
            )
        }
        if (userScriptUrls.isNotEmpty()) {
            TextButton(onClick = onRefreshUserScripts) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Reload userscripts")
            }
        }
    }
    SettingSection("Firefox extensions") {
        var extensionUrl by rememberSaveable { mutableStateOf("") }
        Text(
            "Install Mozilla-signed add-ons from Firefox Add-ons or a direct .xpi URL.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = extensionUrl,
                onValueChange = { extensionUrl = it },
                modifier = Modifier.weight(1f),
                label = { Text("AMO or XPI URL") },
                placeholder = { Text("https://addons.mozilla.org/.../addon/...") },
                singleLine = true,
            )
            Button(
                onClick = {
                    onInstallExtension(extensionUrl)
                    extensionUrl = ""
                },
                enabled = extensionUrl.isNotBlank() && !extensionInstallInProgress,
                modifier = Modifier.height(56.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Install extension")
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onOpenFirefoxAddons,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Extension, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Browse Firefox Add-ons")
        }
        if (extensionInstallInProgress) {
            Text(
                "Preparing extension installation...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (installedExtensions.isEmpty()) {
            Text(
                "No user extensions installed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            installedExtensions.forEach { extension ->
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    ListItem(
                        headlineContent = { Text(extension.name, maxLines = 1) },
                        supportingContent = {
                            Text(
                                listOfNotNull("v${extension.version}", extension.creatorName).joinToString("  •  "),
                                maxLines = 1,
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Extension, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = extension.enabled,
                                    onCheckedChange = { onSetExtensionEnabled(extension.id, it) },
                                )
                                 if (!extension.optionsPageUrl.isNullOrBlank()) {
                                    IconButton(onClick = { onOpenExtensionOptions(extension.id) }) {
                                        Icon(Icons.Outlined.Settings, contentDescription = "Open ${extension.name} settings")
                                    }
                                 }
                                 IconButton(onClick = { permissionExtension = extension }) {
                                     Icon(Icons.Outlined.Security, contentDescription = "View ${extension.name} permissions")
                                 }
                                 IconButton(onClick = { onUpdateExtension(extension.id) }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Update ${extension.name}")
                                }
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Allow in private tabs",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = extension.allowedInPrivateBrowsing,
                            onCheckedChange = { onSetExtensionPrivateBrowsing(extension.id, it) },
                        )
                        IconButton(onClick = { pendingExtensionRemoval = extension }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${extension.name}")
                        }
                    }
                    Divider(modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp))
                }
            }
        }
    }
    pendingExtensionRemoval?.let { extension ->
        AlertDialog(
            onDismissRequest = { pendingExtensionRemoval = null },
            title = { Text("Remove extension?") },
            text = { Text("Uninstall ${extension.name} from Dextra?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExtensionRemoval = null
                        onUninstallExtension(extension.id)
                    },
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingExtensionRemoval = null }) { Text("Keep") } },
        )
    }
    permissionExtension?.let { extension ->
        ExtensionPermissionDialog(
            extension = extension,
            onRevokeOptional = { onRevokeExtensionOptionalPermissions(extension.id) },
            onDismiss = { permissionExtension = null },
        )
    }
    Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ExtensionPermissionDialog(
    extension: InstalledExtension,
    onRevokeOptional: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
        title = { Text("${extension.name} permissions") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                ExtensionPermissionGroup("Required permissions", extension.requiredPermissions)
                ExtensionPermissionGroup("Required websites", extension.requiredOrigins)
                ExtensionPermissionGroup("Required data collection", extension.requiredDataCollectionPermissions)
                ExtensionPermissionGroup("Granted optional permissions", extension.grantedOptionalPermissions)
                ExtensionPermissionGroup("Granted optional websites", extension.grantedOptionalOrigins)
                ExtensionPermissionGroup("Granted optional data collection", extension.grantedOptionalDataCollectionPermissions)
                ExtensionPermissionGroup("Available optional permissions", extension.optionalPermissions)
                ExtensionPermissionGroup("Available optional websites", extension.optionalOrigins)
                if (extension.grantedOptionalPermissions.isNotEmpty() || extension.grantedOptionalOrigins.isNotEmpty() || extension.grantedOptionalDataCollectionPermissions.isNotEmpty()) {
                    TextButton(onClick = onRevokeOptional) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Revoke optional permissions")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ExtensionPermissionGroup(title: String, values: List<String>) {
    if (values.isEmpty()) return
    Text(title, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall)
    values.forEach { value ->
        Text("• $value", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RetentionChooser(
    title: String,
    days: Int,
    onChange: (Int) -> Unit,
    includeNever: Boolean = true,
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val values = if (includeNever) listOf(0, 1, 7, 30, 90) else listOf(1, 7, 30, 90, 365)
        values.forEach { value ->
            FilterChip(
                selected = days == value,
                onClick = { onChange(value) },
                label = { Text(if (value == 0) "Off" else "$value days") },
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
