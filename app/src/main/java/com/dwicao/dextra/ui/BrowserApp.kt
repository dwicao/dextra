package com.dwicao.dextra.ui

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ViewColumn
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
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mozilla.geckoview.GeckoSession
import com.dwicao.dextra.browser.BrowserTabState
import com.dwicao.dextra.browser.BrowserContextMenu
import com.dwicao.dextra.browser.BrowserUrl
import com.dwicao.dextra.browser.BrowserViewModel
import com.dwicao.dextra.browser.BrowserOverlay
import com.dwicao.dextra.browser.ContextMenuAction
import com.dwicao.dextra.browser.ExtensionInstallPrompt
import com.dwicao.dextra.browser.ExtensionPopupState
import com.dwicao.dextra.browser.ExtensionToolbarAction
import com.dwicao.dextra.browser.ExtensionUpdatePrompt
import com.dwicao.dextra.browser.FindInPageState
import com.dwicao.dextra.browser.InstalledExtension
import com.dwicao.dextra.browser.MediaPermissionPrompt
import com.dwicao.dextra.data.SavedTabGroup
import com.dwicao.dextra.data.AdBlockFilter
import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.HistoryEntry
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import com.dwicao.dextra.data.DnsProvider
import com.dwicao.dextra.data.SearchEngine
import com.dwicao.dextra.data.ThemeMode
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoView
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DextraApp(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
    val downloads by viewModel.downloads.collectAsStateWithLifecycle(initialValue = emptyList())
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
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val appContext = LocalContext.current

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
    LaunchedEffect(state.androidPermission?.id) {
        state.androidPermission?.let { permissionLauncher.launch(it.permissions.toTypedArray()) }
    }
    LaunchedEffect(state.snackbar) {
        if (state.snackbar == "Download started" &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DextraTheme(state.settings.themeMode) {
        Scaffold(
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
                 onSelectTab = viewModel::selectTab,
                 onCloseTab = viewModel::closeTab,
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
                 onOpenSavedPage = viewModel::openSavedPage,
                 onClearHistory = viewModel::clearHistory,
                 onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                onSetOverlay = viewModel::setOverlay,
                onDismissOverlay = viewModel::dismissOverlay,
                onSetTheme = viewModel::setThemeMode,
                onSetSearchEngine = viewModel::setSearchEngine,
                  onSetDesktopSites = viewModel::setDesktopSites,
                   onSetHomepage = viewModel::setHomepage,
                   onClearSitePermissions = viewModel::clearSitePermissions,
                  onSetTabBarWithAddressBar = viewModel::setTabBarWithAddressBar,
                  onSetVerticalTabs = viewModel::setVerticalTabs,
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
                installedExtensions = state.installedExtensions,
                extensionInstallInProgress = state.extensionInstallInProgress,
                onInstallExtension = viewModel::installExtension,
                onOpenFirefoxAddons = viewModel::openFirefoxAddons,
                 onSetExtensionEnabled = viewModel::setExtensionEnabled,
                 onSetExtensionPrivateBrowsing = viewModel::setExtensionPrivateBrowsing,
                 onUpdateExtension = viewModel::updateExtension,
                 onOpenExtensionOptions = viewModel::openExtensionOptions,
                 onUninstallExtension = viewModel::uninstallExtension,
                 extensionActions = state.extensionActions,
                 onClickExtensionAction = viewModel::clickExtensionAction,
                onOpenDownload = viewModel::openDownload,
                onShareDownload = viewModel::shareDownload,
                onToggleDownload = viewModel::toggleDownload,
                onCancelDownload = viewModel::cancelDownload,
                onRemoveDownload = viewModel::removeDownload,
                 onResolvePermission = viewModel::resolveContentPermission,
                extensionInstallPrompt = state.extensionInstallPrompt,
                onResolveExtensionInstall = viewModel::resolveExtensionInstall,
                extensionUpdatePrompt = state.extensionUpdatePrompt,
                onResolveExtensionUpdate = viewModel::resolveExtensionUpdate,
                  onContextMenuAction = viewModel::handleContextMenuAction,
                  onDismissContextMenu = viewModel::dismissContextMenu,
                  onShowContextMenu = viewModel::showContextMenu,
                  onTabContextMenu = viewModel::showTabContextMenu,
                  extensionPopup = state.extensionPopup,
                  onCloseExtensionPopup = viewModel::closeExtensionPopup,
                  findInPage = state.findInPage,
                  onUpdateFindInPage = viewModel::updateFindInPage,
                  onFindNext = viewModel::findNext,
                  onCloseFindInPage = viewModel::closeFindInPage,
             )
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
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
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
    onOpenSavedPage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit,
    onSetOverlay: (BrowserOverlay) -> Unit,
    onDismissOverlay: () -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetSearchEngine: (SearchEngine) -> Unit,
     onSetDesktopSites: (Boolean) -> Unit,
     onSetHomepage: (String) -> Unit,
     onClearSitePermissions: () -> Unit,
     onSetTabBarWithAddressBar: (Boolean) -> Unit,
     onSetVerticalTabs: (Boolean) -> Unit,
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
    installedExtensions: List<InstalledExtension>,
    extensionInstallInProgress: Boolean,
    onInstallExtension: (String) -> Unit,
    onOpenFirefoxAddons: () -> Unit,
    onSetExtensionEnabled: (String, Boolean) -> Unit,
    onSetExtensionPrivateBrowsing: (String, Boolean) -> Unit,
    onUpdateExtension: (String) -> Unit,
    onOpenExtensionOptions: (String) -> Unit,
    onUninstallExtension: (String) -> Unit,
    extensionActions: List<ExtensionToolbarAction>,
    onClickExtensionAction: (String) -> Unit,
    onOpenDownload: (DownloadEntry) -> Unit,
    onShareDownload: (DownloadEntry) -> Unit,
    onToggleDownload: (DownloadEntry) -> Unit,
    onCancelDownload: (DownloadEntry) -> Unit,
    onRemoveDownload: (DownloadEntry) -> Unit,
    onResolvePermission: (Boolean, Boolean) -> Unit,
    extensionInstallPrompt: ExtensionInstallPrompt?,
    onResolveExtensionInstall: (Boolean, Boolean, Boolean) -> Unit,
    extensionUpdatePrompt: ExtensionUpdatePrompt?,
    onResolveExtensionUpdate: (Boolean) -> Unit,
    onContextMenuAction: (ContextMenuAction) -> Unit,
    onDismissContextMenu: () -> Unit,
    onShowContextMenu: (String, Int, Int) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    extensionPopup: ExtensionPopupState?,
    onCloseExtensionPopup: () -> Unit,
    findInPage: FindInPageState?,
    onUpdateFindInPage: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFindInPage: () -> Unit,
) {
    val activeTab = state.tabs.firstOrNull { it.id == state.activeTabId }
    var menuExpanded by remember { mutableStateOf(false) }
    val addressFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.T -> {
                        onNewTab()
                        true
                    }
                    Key.R -> {
                        onReload()
                        true
                    }
                    Key.W -> {
                        activeTab?.let { onCloseTab(it.id) }
                        true
                    }
                    Key.L -> {
                        addressFocusRequester.requestFocus()
                        true
                    }
                    else -> false
                }
            },
    ) {
        LaunchedEffect(Unit) { rootFocusRequester.requestFocus() }
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
                 desktopSites = state.settings.desktopSites,
                 tabBarWithAddressBar = state.settings.tabBarWithAddressBar,
                 verticalTabs = state.settings.verticalTabs,
                  onSetDesktopSites = onSetDesktopSites,
                  homepage = state.settings.homepage,
                   onSetHomepage = onSetHomepage,
                   onClearSitePermissions = onClearSitePermissions,
                 onSetTabBarWithAddressBar = onSetTabBarWithAddressBar,
                 onSetVerticalTabs = onSetVerticalTabs,
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
                     onNavigate = onNavigate,
                     onHome = onHome,
                     onCloseSplit = onCloseSplit,
                     onSwapSplit = onSwapSplit,
                     onFocusSplitPane = onFocusSplitPane,
                    onBack = onBack,
                    onForward = onForward,
                    onReload = onReload,
                    onReloadCrashedTab = onReloadCrashedTab,
                    onShowContextMenu = onShowContextMenu,
                    onTabContextMenu = onTabContextMenu,
                    extensionActions = extensionActions,
                    onClickExtensionAction = onClickExtensionAction,
                    onNewTab = { onNewTab() },
                     onSelectTab = onSelectTab,
                     onCloseTab = onCloseTab,
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
                    addressFocusRequester = addressFocusRequester,
                     showTabBarWithAddressBar = state.settings.tabBarWithAddressBar,
                     verticalTabs = state.settings.verticalTabs,
                 )
            } else {
                CompactBrowserLayout(
                    state = state,
                    activeTab = activeTab,
                     onNavigate = onNavigate,
                     onHome = onHome,
                    onBack = onBack,
                    onForward = onForward,
                    onReload = onReload,
                    onReloadCrashedTab = onReloadCrashedTab,
                    onShowContextMenu = onShowContextMenu,
                    onTabContextMenu = onTabContextMenu,
                    extensionActions = extensionActions,
                    onClickExtensionAction = onClickExtensionAction,
                    onNewTab = { onNewTab() },
                     onSelectTab = onSelectTab,
                     onCloseTab = onCloseTab,
                     onMoveTabBefore = onMoveTabBefore,
                     onMoveTabAfter = onMoveTabAfter,
                     onToggleBookmark = onToggleBookmark,
                    onMenu = { menuExpanded = true },
                    addressFocusRequester = addressFocusRequester,
                    onShowTabs = { onSetOverlay(BrowserOverlay.TABS) },
                    showTabBarWithAddressBar = state.settings.tabBarWithAddressBar,
                )
            }

            BrowserMenu(
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                onNewPrivateTab = {
                    menuExpanded = false
                    onNewPrivateTab()
                },
                onShowTabs = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.TABS)
                },
                onShowLibrary = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.LIBRARY)
                },
                onShowDownloads = {
                    menuExpanded = false
                    onSetOverlay(BrowserOverlay.DOWNLOADS)
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
                            onOpen = onOpenSavedPage,
                            onClearHistory = onClearHistory,
                            onDeleteHistoryEntry = onDeleteHistoryEntry,
                            onSetBookmarkFolder = onSetBookmarkFolder,
                            onUpdateBookmark = onUpdateBookmark,
                            onDeleteBookmark = onDeleteBookmark,
                            onExportBookmarks = onExportBookmarks,
                            onImportBookmarks = onImportBookmarks,
                        )
                        BrowserOverlay.DOWNLOADS -> DownloadsSheet(
                            downloads = downloads,
                            onOpen = onOpenDownload,
                            onShare = onShareDownload,
                            onToggle = onToggleDownload,
                            onCancel = onCancelDownload,
                            onRemove = onRemoveDownload,
                        )
                        BrowserOverlay.SETTINGS,
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
private fun DesktopBrowserLayout(
    state: com.dwicao.dextra.browser.BrowserUiState,
    activeTab: BrowserTabState?,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onCloseSplit: () -> Unit,
    onSwapSplit: () -> Unit,
    onFocusSplitPane: (Boolean) -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onReloadCrashedTab: () -> Unit,
    onShowContextMenu: (String, Int, Int) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    extensionActions: List<ExtensionToolbarAction>,
    onClickExtensionAction: (String) -> Unit,
    onNewTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
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
) {
    val splitPrimaryTab = state.splitPrimaryTabId?.let { id -> state.tabs.firstOrNull { it.id == id } }
    val splitSecondaryTab = state.splitSecondaryTabId?.let { id -> state.tabs.firstOrNull { it.id == id } }
    Row(Modifier.fillMaxSize()) {
        if (verticalTabs) {
            GroupedVerticalTabStrip(
                tabs = state.tabs,
                activeTabId = state.activeTabId,
                onNewTab = onNewTab,
                onSelectTab = onSelectTab,
                onCloseTab = onCloseTab,
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
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            DesktopToolbar(
                tabs = state.tabs,
                activeTabId = state.activeTabId,
                onSelectTab = onSelectTab,
                onCloseTab = onCloseTab,
                onTabContextMenu = onTabContextMenu,
                onNewTab = onNewTab,
                showTabBar = showTabBarWithAddressBar && !verticalTabs,
                activeTab = activeTab,
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
            if (splitPrimaryTab != null && splitSecondaryTab != null) {
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
                        onShowContextMenu = onShowContextMenu,
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
                        onShowContextMenu = onShowContextMenu,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onFocus = { onFocusSplitPane(true) },
                    )
                }
            } else {
                BrowserViewport(
                    tab = activeTab,
                    onNavigate = onNavigate,
                    onReloadCrashedTab = onReloadCrashedTab,
                    onShowContextMenu = onShowContextMenu,
                )
            }
        }
    }
}

@Composable
private fun CompactBrowserLayout(
    state: com.dwicao.dextra.browser.BrowserUiState,
    activeTab: BrowserTabState?,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onReloadCrashedTab: () -> Unit,
    onShowContextMenu: (String, Int, Int) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
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
) {
    Column(Modifier.fillMaxSize()) {
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
            )
        }
        activeTab?.takeIf { it.isLoading }?.let {
            LinearProgressIndicator(
                progress = { it.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        BrowserViewport(activeTab, onNavigate, onReloadCrashedTab, onShowContextMenu)
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

@Composable
private fun DesktopToolbar(
    modifier: Modifier = Modifier,
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
    onNewTab: () -> Unit,
    showTabBar: Boolean,
    activeTab: BrowserTabState?,
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
) {
    var collapsed by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var renameGroup by remember { mutableStateOf<SavedTabGroup?>(null) }
    var deleteGroup by remember { mutableStateOf<SavedTabGroup?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var dragDistance by remember { mutableStateOf(0f) }
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
                                    onCreateTabGroup = onCreateTabGroup,
                                    onMoveTabToGroup = onMoveTabToGroup,
                                    onToggleTabSleeping = onToggleTabSleeping,
                                    onTabContextMenu = onTabContextMenu,
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
                        onCreateTabGroup = onCreateTabGroup,
                        onMoveTabToGroup = onMoveTabToGroup,
                        onToggleTabSleeping = onToggleTabSleeping,
                        onTabContextMenu = onTabContextMenu,
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
    onCreateTabGroup: (String?) -> Unit,
    onMoveTabToGroup: (String, String?) -> Unit,
    onToggleTabSleeping: (String) -> Unit,
    onTabContextMenu: (String, Int, Int) -> Unit,
) {
    var tabOrigin by remember(tab.id) { mutableStateOf(IntOffset.Zero) }
    var menuExpanded by remember(tab.id) { mutableStateOf(false) }
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
                }
            }
            BrowserNavButton(Icons.Outlined.Close, "Close tab", true) { onCloseTab(tab.id) }
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
    Box(
        modifier = Modifier
            .size(36.dp)
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
    onNavigate: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    focusRequester: FocusRequester,
) {
    var value by remember(tab?.id) { mutableStateOf(tab?.url?.let(BrowserUrl::displayValue).orEmpty()) }
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(tab?.id, tab?.url) {
        value = BrowserUrl.displayValue(tab?.url.orEmpty())
    }

    val shape = RoundedCornerShape(18.dp)
    val isPrivate = tab?.isPrivate == true
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = modifier
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
            .onFocusChanged { isFocused = it.isFocused },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(
            onGo = {
                onNavigate(value)
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
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val orderedTabs = tabs.sortedWith(compareByDescending { it.pinned })
    val tabIds = orderedTabs.mapTo(hashSetOf()) { it.id }
    var dragDistance by remember { mutableStateOf(0f) }
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
                        listState.animateScrollToItem((listState.firstVisibleItemIndex - 1).coerceAtLeast(0))
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
                                if (tab.favicon != null && !tab.isPrivate) {
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
                        listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
                    }
                },
            )
            if (showNewTabButton) {
            BrowserNavButton(Icons.Outlined.Add, "New tab", enabled = true, onClick = onNewTab)
            }
        }
    }
}

private class ContextMenuGeckoView(
    context: Context,
    private var tabId: String,
    private val onSecondaryClick: (String, Int, Int) -> Unit,
    backgroundColor: Int,
) : GeckoView(context) {
    private var lastSecondaryClick = 0L
    private var currentBackgroundColor = backgroundColor

    init {
        setBackgroundColor(backgroundColor)
    }

    fun setTabId(id: String) {
        if (tabId != id) tabId = id
    }

    fun updateBackgroundColor(color: Int) {
        if (currentBackgroundColor == color) return
        currentBackgroundColor = color
        setBackgroundColor(color)
        session?.compositorController?.setClearColor(color)
    }

    private fun dispatchSecondaryClick(event: MotionEvent): Boolean {
        val now = event.eventTime
        if (now - lastSecondaryClick < 150L) return true
        lastSecondaryClick = now
        val location = IntArray(2)
        getLocationOnScreen(location)
        onSecondaryClick(tabId, location[0] + event.x.toInt(), location[1] + event.y.toInt())
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN &&
            event.buttonState and MotionEvent.BUTTON_SECONDARY != 0
        ) {
            dispatchSecondaryClick(event)
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS) &&
            event.buttonState and MotionEvent.BUTTON_SECONDARY != 0
        ) {
            dispatchSecondaryClick(event)
        }
        return super.onGenericMotionEvent(event)
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
private fun BrowserViewport(
    tab: BrowserTabState?,
    onNavigate: (String) -> Unit,
    onReloadCrashedTab: () -> Unit,
    onShowContextMenu: (String, Int, Int) -> Unit,
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
            NewTabPage(onNavigate)
        } else {
            AndroidView(
                factory = { context ->
                    ContextMenuGeckoView(
                        context = context,
                        tabId = tab.id,
                        onSecondaryClick = onShowContextMenu,
                        backgroundColor = pageBackground,
                    ).apply {
                        setBackgroundColor(pageBackground)
                        coverUntilFirstPaint(pageBackground)
                        setSession(tab.session)
                        setOnFocusChangeListener { _, focused -> if (focused) onFocus?.invoke() }
                        tab.session.compositorController.setClearColor(pageBackground)
                    }
                },
                    update = { view ->
                    view.updateBackgroundColor(pageBackground)
                    view.setTabId(tab.id)
                    if (view.session !== tab.session) {
                        view.coverUntilFirstPaint(pageBackground)
                        view.releaseSession()
                        view.setSession(tab.session)
                        view.setOnFocusChangeListener { _, focused -> if (focused) onFocus?.invoke() }
                        view.session?.compositorController?.setClearColor(pageBackground)
                    }
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
    val isImage = menu.resourceType == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE
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
                leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                onClick = { onAction(ContextMenuAction.COPY_LINK) },
            )
            }
        if (!menu.resourceUri.isNullOrBlank()) {
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
private fun NewTabPage(onNavigate: (String) -> Unit) {
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
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickLink("DuckDuckGo", Icons.Outlined.Search, { onNavigate("https://duckduckgo.com") })
            QuickLink("Wikipedia", Icons.Outlined.Public, { onNavigate("https://wikipedia.org") })
            QuickLink("GitHub", Icons.Outlined.Language, { onNavigate("https://github.com") })
            QuickLink("MDN", Icons.Outlined.Security, { onNavigate("https://developer.mozilla.org") })
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text("Filter lists are managed in Settings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
    onShowTabs: () -> Unit,
    onShowLibrary: () -> Unit,
    onShowDownloads: () -> Unit,
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
                    text = { Text("All tabs") },
                    leadingIcon = { Icon(Icons.Outlined.Tab, contentDescription = null) },
                    onClick = onShowTabs,
                )
        DropdownMenuItem(
            text = { Text("Bookmarks & history") },
            leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
            onClick = onShowLibrary,
        )
        DropdownMenuItem(
            text = { Text("Downloads") },
            leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
            onClick = onShowDownloads,
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
    onOpen: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit,
    onSetBookmarkFolder: (Bookmark, String?) -> Unit,
    onUpdateBookmark: (Bookmark, String, String?) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onExportBookmarks: () -> Unit,
    onImportBookmarks: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    SheetHeader("Library", "Keep the useful parts close")
    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 20.dp) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bookmarks") }, icon = { Icon(Icons.Outlined.Bookmark, null) })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("History") }, icon = { Icon(Icons.Outlined.History, null) })
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
    } else {
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
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun DownloadsSheet(
    downloads: List<DownloadEntry>,
    onOpen: (DownloadEntry) -> Unit,
    onShare: (DownloadEntry) -> Unit,
    onToggle: (DownloadEntry) -> Unit,
    onCancel: (DownloadEntry) -> Unit,
    onRemove: (DownloadEntry) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<DownloadEntry?>(null) }
    val activeCount = downloads.count { it.status in setOf(DownloadStatus.QUEUED.label, DownloadStatus.DOWNLOADING.label, DownloadStatus.PAUSED.label) }
    SheetHeader("Downloads", if (activeCount == 0) "Files saved by Dextra" else "$activeCount active")
    if (downloads.any { it.isPrivate }) {
        Text(
            "Downloads from private tabs are still saved to device storage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    if (downloads.isEmpty()) {
        EmptyLibrary("Files you download will appear here.", Icons.Outlined.Download)
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            items(downloads, key = { it.downloadId }) { download ->
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
                                if (isComplete) {
                                    IconButton(onClick = { onOpen(download) }) {
                                        Icon(Icons.Outlined.OpenInNew, contentDescription = "Open ${download.fileName}")
                                    }
                                    IconButton(onClick = { onShare(download) }) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share ${download.fileName}")
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
    return listOf(status, size, speed, download.reason.orEmpty())
        .filter(String::isNotBlank)
        .joinToString("  •  ")
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
                Text("${prompt.currentVersion} -> ${prompt.newVersion} requests additional access.")
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
            }
        },
        confirmButton = { TextButton(onClick = { onResolve(true) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = { onResolve(false) }) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: ThemeMode,
    searchEngine: SearchEngine,
    desktopSites: Boolean,
    homepage: String,
    tabBarWithAddressBar: Boolean,
    verticalTabs: Boolean,
    adBlockingEnabled: Boolean,
    adBlockFilters: List<AdBlockFilter>,
    onSetTheme: (ThemeMode) -> Unit,
    onSetSearchEngine: (SearchEngine) -> Unit,
    onSetDesktopSites: (Boolean) -> Unit,
    onSetHomepage: (String) -> Unit,
    onClearSitePermissions: () -> Unit,
    onSetTabBarWithAddressBar: (Boolean) -> Unit,
    onSetVerticalTabs: (Boolean) -> Unit,
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
    onUpdateExtension: (String) -> Unit,
    onOpenExtensionOptions: (String) -> Unit,
    onUninstallExtension: (String) -> Unit,
) {
    var pendingExtensionRemoval by remember { mutableStateOf<InstalledExtension?>(null) }
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
    SettingSection("Search engine") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchEngine.values().forEach { engine ->
                FilterChip(selected = searchEngine == engine, onClick = { onSetSearchEngine(engine) }, label = { Text(engine.label) })
            }
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
    Spacer(Modifier.height(28.dp))
            }
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
