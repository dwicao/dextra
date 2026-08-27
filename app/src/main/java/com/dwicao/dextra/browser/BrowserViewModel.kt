package com.dwicao.dextra.browser

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dwicao.dextra.GeckoRuntimeHolder
import com.dwicao.dextra.data.AdBlockFilter
import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.BrowserDao
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.BrowserSettings
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import com.dwicao.dextra.data.DnsProvider
import com.dwicao.dextra.data.ExtensionInstallRecord
import com.dwicao.dextra.data.HistoryEntry
import com.dwicao.dextra.data.SearchEngine
import com.dwicao.dextra.data.SavedTab
import com.dwicao.dextra.data.SavedTabGroup
import com.dwicao.dextra.data.SettingsRepository
import com.dwicao.dextra.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.geckoview.WebResponse
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import com.dwicao.dextra.MainActivity

data class BrowserTabState(
    val id: String,
    val session: GeckoSession,
    val title: String = "New tab",
    val url: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val isPrivate: Boolean = false,
    val hasPage: Boolean = false,
    val isBookmarked: Boolean = false,
    val favicon: Bitmap? = null,
    val crashed: Boolean = false,
    val pinned: Boolean = false,
    val groupId: String? = null,
    val isSleeping: Boolean = false,
)

enum class BrowserOverlay {
    NONE,
    TABS,
    LIBRARY,
    SETTINGS,
    DOWNLOADS,
}

enum class ContextMenuAction {
    NEW_TAB,
    NEW_PRIVATE_TAB,
    DUPLICATE_TAB,
    RELOAD_TAB,
    TOGGLE_TAB_PINNED,
    TOGGLE_TAB_SLEEPING,
    CLOSE_TAB,
    CLOSE_OTHER_TABS,
    CLOSE_TABS_TO_RIGHT,
    BACK,
    FORWARD,
    RELOAD,
    OPEN_LINK,
    OPEN_LINK_IN_NEW_TAB,
    OPEN_LINK_IN_PRIVATE_TAB,
    COPY_LINK,
    COPY_TEXT,
    OPEN_MEDIA_IN_NEW_TAB,
    COPY_MEDIA_URL,
    SAVE_MEDIA,
    COPY_PAGE_URL,
    TOGGLE_BOOKMARK,
    SAVE_PAGE,
    DISMISS,
}

data class BrowserContextMenu(
    val tabId: String,
    val x: Int,
    val y: Int,
    val pageUrl: String,
    val isBookmarked: Boolean,
    val isPinned: Boolean = false,
    val isSleeping: Boolean = false,
    val linkUri: String?,
    val linkText: String?,
    val textContent: String?,
    val resourceUri: String?,
    val resourceType: Int,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val isTab: Boolean = false,
)

data class ContentPermissionPrompt(
    val id: String,
    val origin: String,
    val label: String,
    val result: GeckoResult<Int>,
)

data class AndroidPermissionPrompt(
    val id: String,
    val permissions: List<String>,
    val callback: GeckoSession.PermissionDelegate.Callback,
)

data class MediaPermissionPrompt(
    val id: String,
    val origin: String,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val callback: GeckoSession.PermissionDelegate.MediaCallback,
    val video: Array<GeckoSession.PermissionDelegate.MediaSource>,
    val audio: Array<GeckoSession.PermissionDelegate.MediaSource>,
)

data class InstalledExtension(
    val id: String,
    val name: String,
    val version: String,
    val creatorName: String?,
    val enabled: Boolean,
    val allowedInPrivateBrowsing: Boolean,
    val amoListingUrl: String?,
    val optionsPageUrl: String?,
)

data class ExtensionToolbarAction(
    val extensionId: String,
    val title: String,
    val enabled: Boolean,
    val badgeText: String?,
    val icon: Bitmap?,
)

data class ExtensionPopupState(
    val extensionId: String,
    val extensionName: String,
    val session: GeckoSession,
)

data class FindInPageState(
    val query: String = "",
    val current: Int = 0,
    val total: Int = 0,
)

private data class ClosedTabEntry(
    val url: String,
    val isPrivate: Boolean,
    val pinned: Boolean,
    val groupId: String?,
)

data class ExtensionInstallPrompt(
    val id: String,
    val extensionId: String,
    val name: String,
    val version: String,
    val permissions: List<String>,
    val origins: List<String>,
    val dataCollectionPermissions: List<String>,
    val packageFilePath: String?,
    val result: GeckoResult<WebExtension.PermissionPromptResponse>,
)

private const val MAX_EXTENSION_PACKAGE_BYTES = 64L * 1024L * 1024L
private const val MAX_OPEN_TABS = 64
private const val DOWNLOAD_CHANNEL_ID = "dextra_downloads"

data class ExtensionUpdatePrompt(
    val id: String,
    val name: String,
    val currentVersion: String,
    val newVersion: String,
    val permissions: List<String>,
    val origins: List<String>,
    val result: GeckoResult<AllowOrDeny>,
)

data class BrowserUiState(
    val tabs: List<BrowserTabState> = emptyList(),
    val activeTabId: String? = null,
    val settings: BrowserSettings = BrowserSettings(),
    val overlay: BrowserOverlay = BrowserOverlay.NONE,
    val snackbar: String? = null,
    val contentPermission: ContentPermissionPrompt? = null,
    val androidPermission: AndroidPermissionPrompt? = null,
    val mediaPermission: MediaPermissionPrompt? = null,
    val installedExtensions: List<InstalledExtension> = emptyList(),
    val extensionInstallPrompt: ExtensionInstallPrompt? = null,
    val extensionUpdatePrompt: ExtensionUpdatePrompt? = null,
    val extensionInstallInProgress: Boolean = false,
    val contextMenu: BrowserContextMenu? = null,
    val lastCrashReport: String? = null,
    val extensionActions: List<ExtensionToolbarAction> = emptyList(),
    val extensionPopup: ExtensionPopupState? = null,
    val findInPage: FindInPageState? = null,
    val addressFocusRequest: Long = 0,
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = GeckoRuntimeHolder.get(application)
    private val database = BrowserDatabase.get(application)
    private val dao: BrowserDao = database.browserDao()
    private val settingsRepository = SettingsRepository(application)
    private val _state = MutableStateFlow(BrowserUiState())
    private val removedDownloadIds = ConcurrentHashMap.newKeySet<Long>()
    private val installedExtensionObjects = mutableMapOf<String, WebExtension>()
    private val extensionActionObjects = mutableMapOf<String, WebExtension.Action>()
    private val recentlyClosedTabs = ArrayDeque<ClosedTabEntry>()
    private val pageZoomByTab = mutableMapOf<String, Int>()
    private val restoringExtensionIds = ConcurrentHashMap.newKeySet<String>()
    private var restoredSavedTabs = false
    private var pendingIncomingUri: String? = null
    @Volatile
    private var pendingExtensionPackagePath: String? = null
    @Volatile
    private var pendingExtensionPrivateBrowsing = false
    @Volatile
    private var pendingExtensionDataCollection = false
    private val downloadEngine = DownloadEngine(viewModelScope) { downloadId, update ->
        viewModelScope.launch { applyDownloadUpdate(downloadId, update) }
    }
    @Volatile
    private var adBlockPort: WebExtension.Port? = null
    @Volatile
    private var adBlockExtension: WebExtension? = null

    private val adBlockMessageDelegate = object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            adBlockPort = port
            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onDisconnect(port: WebExtension.Port) {
                    if (adBlockPort === port) adBlockPort = null
                }
            })
            syncAdBlockSettings(_state.value.settings)
            syncUserScripts(_state.value.settings)
            syncActiveTabZoom()
        }
    }

    private val adBlockContentMessageDelegate = object : WebExtension.MessageDelegate {
        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: WebExtension.MessageSender,
        ): GeckoResult<Any>? {
            if (sender.environmentType != WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT) return null
            val json = message as? JSONObject ?: return null
            val tabId = _state.value.tabs.firstOrNull { it.session === sender.session }?.id
            if (tabId == null) return null
            when (json.optString("type")) {
                "openLinkInNewTab" -> {
                    val url = json.optString("url")
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        openContextUrl(tabId, url, inNewTab = true)
                    }
                }
                "contextMenu" -> {
                    val resourceType = when (json.optString("resourceType")) {
                        "img" -> GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE
                        "video" -> GeckoSession.ContentDelegate.ContextElement.TYPE_VIDEO
                        "audio" -> GeckoSession.ContentDelegate.ContextElement.TYPE_AUDIO
                        else -> GeckoSession.ContentDelegate.ContextElement.TYPE_NONE
                    }
                    showContextMenu(
                        tabId = tabId,
                        x = json.optInt("x"),
                        y = json.optInt("y"),
                        linkUri = json.optString("linkUrl").takeIf(String::isNotBlank),
                        linkText = json.optString("textContent").takeIf(String::isNotBlank),
                        textContent = json.optString("textContent").takeIf(String::isNotBlank),
                        resourceUri = json.optString("resourceUri").takeIf(String::isNotBlank),
                        resourceType = resourceType,
                    )
                }
            }
            return null
        }
    }

    private val extensionPromptDelegate = object : WebExtensionController.PromptDelegate {
        @Suppress("DEPRECATION")
        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
        ): GeckoResult<WebExtension.PermissionPromptResponse> =
            createExtensionInstallPrompt(extension, permissions, origins, emptyArray())

        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>,
        ): GeckoResult<WebExtension.PermissionPromptResponse> =
            createExtensionInstallPrompt(extension, permissions, origins, dataCollectionPermissions)

        override fun onUpdatePrompt(
            extension: WebExtension,
            updatedExtension: WebExtension,
            newPermissions: Array<String>,
            newOrigins: Array<String>,
        ): GeckoResult<AllowOrDeny> = createExtensionUpdatePrompt(
            extension,
            updatedExtension,
            newPermissions,
            newOrigins,
        )
    }

    private val addonManagerDelegate = object : WebExtensionController.AddonManagerDelegate {
        override fun onInstalled(extension: WebExtension) = refreshInstalledExtensions()

        override fun onUninstalled(extension: WebExtension) = refreshInstalledExtensions()

        override fun onEnabled(extension: WebExtension) = refreshInstalledExtensions()

        override fun onDisabled(extension: WebExtension) = refreshInstalledExtensions()

        override fun onReady(extension: WebExtension) {
            refreshInstalledExtensions()
        }
    }

    private val extensionActionDelegate = object : WebExtension.ActionDelegate {
        override fun onBrowserAction(
            extension: WebExtension,
            session: GeckoSession?,
            action: WebExtension.Action,
        ) = updateExtensionAction(extension, action)

        override fun onPageAction(
            extension: WebExtension,
            session: GeckoSession?,
            action: WebExtension.Action,
        ) = updateExtensionAction(extension, action)

        override fun onOpenPopup(
            extension: WebExtension,
            action: WebExtension.Action,
        ): GeckoResult<GeckoSession>? = openExtensionPopup(extension)

        override fun onTogglePopup(
            extension: WebExtension,
            action: WebExtension.Action,
        ): GeckoResult<GeckoSession>? {
            if (_state.value.extensionPopup?.extensionId == extension.id) {
                closeExtensionPopup()
                return GeckoResult.fromValue(null)
            }
            return openExtensionPopup(extension)
        }
    }

    val state: StateFlow<BrowserUiState> = _state.asStateFlow()
    val history: Flow<List<HistoryEntry>> = dao.observeHistory()
    val bookmarks: Flow<List<Bookmark>> = dao.observeBookmarks()
    val downloads: Flow<List<DownloadEntry>> = dao.observeDownloads()

    init {
        readLastCrashReport()?.let { report ->
            _state.update { it.copy(lastCrashReport = report) }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val desktopSitesChanged = _state.value.settings.desktopSites != settings.desktopSites
                val adBlockingChanged = _state.value.settings.adBlockingEnabled != settings.adBlockingEnabled ||
                    _state.value.settings.adBlockFilters != settings.adBlockFilters
                val userScriptsChanged = _state.value.settings.userScriptUrls != settings.userScriptUrls ||
                    _state.value.settings.disabledUserScriptUrls != settings.disabledUserScriptUrls
                _state.update { current -> current.copy(settings = settings) }
                if (!restoredSavedTabs) {
                    restoredSavedTabs = true
                    restoreSavedTabs(settings.openTabs, settings.activeTabIndex)
                    pendingIncomingUri?.let { uri ->
                        pendingIncomingUri = null
                        openIncomingUri(uri)
                    }
                }
                _state.value.tabs.forEach { tab ->
                    applyDesktopSiteSetting(tab.session, settings.desktopSites)
                    if (desktopSitesChanged && tab.hasPage) tab.session.reload()
                    if (adBlockingChanged && tab.hasPage) tab.session.reload()
                    if (userScriptsChanged && tab.hasPage) tab.session.reload()
                }
                applyDnsOverHttps(settings.dnsOverHttpsEnabled, settings.dnsProvider)
                applyContentColorScheme(settings.themeMode)
                syncAdBlockSettings(settings)
                syncUserScripts(settings)
            }
        }
        viewModelScope.launch {
            dao.observeBookmarks().collect { savedBookmarks ->
                val bookmarkedUrls = savedBookmarks.mapTo(hashSetOf()) { it.url }
                _state.update { current ->
                    current.copy(
                        tabs = current.tabs.map { tab ->
                            tab.copy(isBookmarked = tab.url in bookmarkedUrls)
                        },
                    )
                }
            }
        }
        runtime.webExtensionController.setPromptDelegate(extensionPromptDelegate)
        runtime.webExtensionController.setAddonManagerDelegate(addonManagerDelegate)
        refreshInstalledExtensions()
        installAdBlocker()
        viewModelScope.launch {
            delay(2_000)
            restoreOfflineExtensions()
        }
        restoreDownloads()
        createTab()
    }

    fun handleIncomingIntent(intent: Intent?) {
        val uri = intent?.dataString ?: return
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) return
        if (!restoredSavedTabs) {
            pendingIncomingUri = uri
            return
        }
        openIncomingUri(uri)
    }

    private fun openIncomingUri(uri: String) {
        val active = activeTab() ?: return
        if (active.hasPage) {
            createTab(initialUri = uri)
        } else {
            navigate(active.id, uri)
        }
    }

    fun activeTab(): BrowserTabState? = _state.value.tabs.firstOrNull { it.id == _state.value.activeTabId }

    fun createTab(
        privateMode: Boolean = false,
        initialUri: String? = null,
        openSession: Boolean = true,
    ): String {
        if (_state.value.tabs.size >= MAX_OPEN_TABS) {
            showSnackbar("Tab limit reached ($MAX_OPEN_TABS)")
            return _state.value.activeTabId.orEmpty()
        }
        val id = UUID.randomUUID().toString()
        val previousActiveTabId = _state.value.activeTabId
        val session = createSession(id, privateMode, openSession)
        val resolvedInitialUri = initialUri
            ?.let { BrowserUrl.resolve(it, _state.value.settings.searchEngine) }
            ?.takeIf(String::isNotBlank)
        val tab = BrowserTabState(
            id = id,
            session = session,
            title = if (resolvedInitialUri == null) "New tab" else "Loading...",
            url = resolvedInitialUri.orEmpty(),
            isLoading = resolvedInitialUri != null,
            hasPage = resolvedInitialUri != null,
            isPrivate = privateMode,
        )
        _state.update { current ->
            current.copy(
                tabs = current.tabs + tab,
                activeTabId = id,
                overlay = BrowserOverlay.NONE,
            )
        }
        updateExtensionActiveTab(previousActiveTabId, id)
        updateSessionActivity(id)
        persistOpenTabs()
        if (resolvedInitialUri != null && openSession) session.loadUri(resolvedInitialUri)
        return id
    }

    fun createPrivateTab() = createTab(privateMode = true)

    fun selectTab(id: String) {
        val current = _state.value
        if (current.tabs.any { it.id == id }) {
            if (current.activeTabId == id) {
                _state.update { it.copy(overlay = BrowserOverlay.NONE) }
                return
            }
            closeExtensionPopup()
            closeFindInPage()
            val previousActiveTabId = current.activeTabId
            _state.update {
                it.copy(
                    activeTabId = id,
                    overlay = BrowserOverlay.NONE,
                    tabs = it.tabs.map { tab ->
                        if (tab.id == id) tab.copy(isSleeping = false) else tab
                    },
                )
            }
            updateExtensionActiveTab(previousActiveTabId, id)
            updateSessionActivity(id)
            persistOpenTabs()
        }
    }

    fun cycleTab(forward: Boolean) {
        val current = _state.value
        if (current.tabs.size < 2) return
        val currentIndex = current.tabs.indexOfFirst { it.id == current.activeTabId }
        if (currentIndex < 0) return
        val offset = if (forward) 1 else -1
        val nextIndex = (currentIndex + offset + current.tabs.size) % current.tabs.size
        selectTab(current.tabs[nextIndex].id)
    }

    fun reopenClosedTab() {
        val closed = recentlyClosedTabs.removeFirstOrNull() ?: return
        val id = createTab(privateMode = closed.isPrivate, initialUri = closed.url.takeIf(String::isNotBlank))
        if (id.isNotBlank()) {
            if (closed.pinned) toggleTabPinned(id)
            if (closed.groupId != null && _state.value.settings.tabGroups.any { it.id == closed.groupId }) {
                moveTabToGroup(id, closed.groupId)
            }
        }
    }

    fun closeTab(id: String) {
        val current = _state.value
        val closing = current.tabs.firstOrNull { it.id == id } ?: return
        pageZoomByTab.remove(id)
        rememberClosedTab(closing)
        if (closing.id == current.activeTabId) {
            closeExtensionPopup()
            closeFindInPage()
        }
        if (current.tabs.size == 1) {
            val previousActiveTabId = current.activeTabId
            val replacementId = UUID.randomUUID().toString()
            val replacementSession = createSession(replacementId, privateMode = false)
            val replacement = BrowserTabState(id = replacementId, session = replacementSession)
            setExtensionTabActive(previousActiveTabId, false)
            _state.update { it.copy(tabs = listOf(replacement), activeTabId = replacementId, overlay = BrowserOverlay.NONE) }
            setExtensionTabActive(replacementId, true)
            syncActiveTabZoom()
            persistOpenTabs()
            runCatching { closing.session.close() }
            return
        }

        val remaining = current.tabs.filterNot { it.id == id }
        val newActive = if (current.activeTabId == id) {
            remaining.getOrNull(current.tabs.indexOf(closing).coerceAtMost(remaining.lastIndex))?.id
        } else {
            current.activeTabId
        }
        if (current.activeTabId != newActive) {
            setExtensionTabActive(current.activeTabId, false)
        }
        _state.update {
            it.copy(
                tabs = remaining.map { tab -> if (tab.id == newActive) tab.copy(isSleeping = false) else tab },
                activeTabId = newActive,
            )
        }
        if (current.activeTabId != newActive) {
            setExtensionTabActive(newActive, true)
            syncActiveTabZoom()
        }
        persistOpenTabs()
        runCatching { closing.session.close() }
    }

    fun toggleTabPinned(id: String) {
        if (_state.value.tabs.none { it.id == id }) return
        _state.update {
            it.copy(
                tabs = it.tabs
                    .map { tab -> if (tab.id == id) tab.copy(pinned = !tab.pinned) else tab }
                    .sortedWith(compareByDescending { tab -> tab.pinned }),
            )
        }
        persistOpenTabs()
    }

    fun moveTabBefore(tabId: String, targetId: String) {
        val current = _state.value
        val moving = current.tabs.firstOrNull { it.id == tabId } ?: return
        val target = current.tabs.firstOrNull { it.id == targetId } ?: return
        if (moving.id == target.id || moving.pinned != target.pinned) return
        val reordered = current.tabs.toMutableList().apply {
            removeAll { it.id == moving.id }
            add(indexOfFirst { it.id == target.id }.coerceAtLeast(0), moving)
        }
        _state.update { it.copy(tabs = reordered) }
        persistOpenTabs()
    }

    fun createTabGroup(tabId: String? = null) {
        val current = _state.value
        val group = SavedTabGroup(
            id = UUID.randomUUID().toString(),
            title = "Group ${current.settings.tabGroups.size + 1}",
        )
        _state.update {
            it.copy(
                settings = it.settings.copy(tabGroups = it.settings.tabGroups + group),
                tabs = if (tabId == null) it.tabs else it.tabs.map { tab ->
                    if (tab.id == tabId) tab.copy(groupId = group.id) else tab
                },
            )
        }
        persistOpenTabs()
    }

    fun moveTabToGroup(tabId: String, groupId: String?) {
        if (groupId != null && _state.value.settings.tabGroups.none { it.id == groupId }) return
        if (_state.value.tabs.none { it.id == tabId }) return
        _state.update { state ->
            state.copy(tabs = state.tabs.map { tab -> if (tab.id == tabId) tab.copy(groupId = groupId) else tab })
        }
        persistOpenTabs()
    }

    fun renameTabGroup(groupId: String, title: String) {
        val normalized = title.trim().take(40)
        if (normalized.isBlank()) return
        _state.update { state ->
            state.copy(
                settings = state.settings.copy(
                    tabGroups = state.settings.tabGroups.map { group ->
                        if (group.id == groupId) group.copy(title = normalized) else group
                    },
                ),
            )
        }
        persistOpenTabs()
    }

    fun toggleTabGroup(groupId: String) {
        _state.update { state ->
            state.copy(
                settings = state.settings.copy(
                    tabGroups = state.settings.tabGroups.map { group ->
                        if (group.id == groupId) group.copy(collapsed = !group.collapsed) else group
                    },
                ),
            )
        }
        persistOpenTabs()
    }

    fun toggleTabSleeping(id: String) {
        val current = _state.value
        if (id == current.activeTabId) {
            showSnackbar("The active tab cannot be put to sleep")
            return
        }
        val tab = current.tabs.firstOrNull { it.id == id } ?: return
        val sleeping = !tab.isSleeping
        _state.update { state ->
            state.copy(tabs = state.tabs.map { item -> if (item.id == id) item.copy(isSleeping = sleeping) else item })
        }
        runCatching { tab.session.setActive(!sleeping) }
        persistOpenTabs()
    }

    fun hibernateInactiveTabs() {
        val activeId = _state.value.activeTabId
        val inactive = _state.value.tabs.filter { it.id != activeId && !it.isSleeping }
        if (inactive.isEmpty()) {
            showSnackbar("No inactive tabs to hibernate")
            return
        }
        _state.update { state ->
            state.copy(tabs = state.tabs.map { tab -> if (tab.id == activeId) tab else tab.copy(isSleeping = true) })
        }
        inactive.forEach { tab -> runCatching { tab.session.setActive(false) } }
        persistOpenTabs()
        showSnackbar("Hibernated ${inactive.size} tabs")
    }

    fun navigateActive(input: String) {
        activeTab()?.let { navigate(it.id, input) }
    }

    fun navigate(tabId: String, input: String) {
        val url = BrowserUrl.resolve(input, _state.value.settings.searchEngine)
        if (url.isEmpty()) return
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        updateTab(tabId) {
            it.copy(url = url, title = "Loading...", hasPage = true, isLoading = true, progress = 0, favicon = null, crashed = false)
        }
        persistOpenTabs()
        tab.session.loadUri(url)
    }

    fun goBack(): Boolean {
        val tab = activeTab() ?: return false
        if (!tab.canGoBack) return false
        tab.session.goBack(true)
        return true
    }

    fun goForward() {
        activeTab()?.takeIf { it.canGoForward }?.session?.goForward(true)
    }

    fun adjustPageZoom(step: Int) {
        val tab = activeTab() ?: return
        val current = pageZoomByTab[tab.id] ?: 100
        val next = (current + step).coerceIn(50, 200)
        if (next == current) return
        pageZoomByTab[tab.id] = next
        syncActiveTabZoom()
        showSnackbar("Zoom ${next}%")
    }

    fun resetPageZoom() {
        val tab = activeTab() ?: return
        pageZoomByTab.remove(tab.id)
        syncActiveTabZoom()
        showSnackbar("Zoom 100%")
    }

    fun openFindInPage() {
        if (activeTab()?.hasPage != true) return
        _state.update { it.copy(findInPage = it.findInPage ?: FindInPageState()) }
    }

    fun focusAddressBar() {
        _state.update { it.copy(addressFocusRequest = it.addressFocusRequest + 1) }
    }

    fun updateFindInPage(query: String) {
        val tab = activeTab() ?: return
        _state.update { it.copy(findInPage = FindInPageState(query = query)) }
        if (query.isBlank()) {
            tab.session.finder.clear()
            return
        }
        tab.session.finder.setDisplayFlags(GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL)
        tab.session.finder.find(query, 0).accept(
            { result ->
                if (_state.value.findInPage?.query == query) {
                    _state.update {
                        it.copy(
                            findInPage = it.findInPage?.copy(
                                current = result?.current?.plus(1) ?: 0,
                                total = result?.total ?: 0,
                            ),
                        )
                    }
                }
            },
            {},
        )
    }

    fun findNext(forward: Boolean) {
        val tab = activeTab() ?: return
        val query = _state.value.findInPage?.query?.takeIf(String::isNotBlank) ?: return
        tab.session.finder.find(
            query,
            if (forward) GeckoSession.FINDER_FIND_FORWARD else GeckoSession.FINDER_FIND_BACKWARDS,
        ).accept(
            { result ->
                _state.update {
                    it.copy(findInPage = it.findInPage?.copy(
                        current = result?.current?.plus(1) ?: 0,
                        total = result?.total ?: 0,
                    ))
                }
            },
            {},
        )
    }

    fun closeFindInPage() {
        activeTab()?.session?.finder?.clear()
        _state.update { it.copy(findInPage = null) }
    }

    fun dismissTransientUi() {
        dismissContextMenu()
        closeExtensionPopup()
        closeFindInPage()
    }

    fun reloadOrStop() {
        activeTab()?.let { tab ->
            if (tab.crashed) reloadCrashedTab()
            else if (tab.isLoading) tab.session.stop() else tab.session.reload()
        }
    }

    fun dismissContextMenu() {
        _state.update { it.copy(contextMenu = null) }
    }

    fun showContextMenu(
        tabId: String,
        x: Int,
        y: Int,
        linkUri: String? = null,
        linkText: String? = null,
        textContent: String? = null,
        resourceUri: String? = null,
        resourceType: Int = GeckoSession.ContentDelegate.ContextElement.TYPE_NONE,
    ) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        _state.update {
            it.copy(
                contextMenu = BrowserContextMenu(
                    tabId = tabId,
                    x = x,
                    y = y,
                    pageUrl = tab.url,
                    isBookmarked = tab.isBookmarked,
                    linkUri = linkUri,
                    linkText = linkText,
                    textContent = textContent,
                    resourceUri = resourceUri,
                    resourceType = resourceType,
                    canGoBack = tab.canGoBack,
                    canGoForward = tab.canGoForward,
                ),
            )
        }
    }

    fun showTabContextMenu(tabId: String, x: Int, y: Int) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        _state.update {
            it.copy(
                contextMenu = BrowserContextMenu(
                    tabId = tabId,
                    x = x,
                    y = y,
                    pageUrl = tab.url,
                    isBookmarked = tab.isBookmarked,
                    linkUri = null,
                    linkText = null,
                    textContent = null,
                    resourceUri = null,
                    resourceType = GeckoSession.ContentDelegate.ContextElement.TYPE_NONE,
                    canGoBack = tab.canGoBack,
                    canGoForward = tab.canGoForward,
                    isPinned = tab.pinned,
                    isSleeping = tab.isSleeping,
                    isTab = true,
                ),
            )
        }
    }

    fun handleContextMenuAction(action: ContextMenuAction) {
        val menu = _state.value.contextMenu ?: return
        when (action) {
            ContextMenuAction.NEW_TAB -> createTab()
            ContextMenuAction.NEW_PRIVATE_TAB -> createPrivateTab()
            ContextMenuAction.DUPLICATE_TAB -> {
                val tab = _state.value.tabs.firstOrNull { it.id == menu.tabId }
                if (tab != null) createTab(privateMode = tab.isPrivate, initialUri = tab.url.takeIf(String::isNotBlank))
            }
            ContextMenuAction.RELOAD_TAB -> _state.value.tabs.firstOrNull { it.id == menu.tabId }?.session?.reload()
            ContextMenuAction.TOGGLE_TAB_PINNED -> toggleTabPinned(menu.tabId)
            ContextMenuAction.TOGGLE_TAB_SLEEPING -> toggleTabSleeping(menu.tabId)
            ContextMenuAction.CLOSE_TAB -> closeTab(menu.tabId)
            ContextMenuAction.CLOSE_OTHER_TABS -> closeOtherTabs(menu.tabId)
            ContextMenuAction.CLOSE_TABS_TO_RIGHT -> closeTabsToRight(menu.tabId)
            ContextMenuAction.BACK -> if (menu.canGoBack) _state.value.tabs.firstOrNull { it.id == menu.tabId }?.session?.goBack(true)
            ContextMenuAction.FORWARD -> if (menu.canGoForward) _state.value.tabs.firstOrNull { it.id == menu.tabId }?.session?.goForward(true)
            ContextMenuAction.RELOAD -> _state.value.tabs.firstOrNull { it.id == menu.tabId }?.session?.reload()
            ContextMenuAction.OPEN_LINK -> menu.linkUri?.let { openContextUrl(menu.tabId, it, inNewTab = false) }
            ContextMenuAction.OPEN_LINK_IN_NEW_TAB -> menu.linkUri?.let { openContextUrl(menu.tabId, it, inNewTab = true) }
            ContextMenuAction.OPEN_LINK_IN_PRIVATE_TAB -> menu.linkUri?.let { openContextUrl(menu.tabId, it, inNewTab = true, privateTab = true) }
            ContextMenuAction.COPY_LINK -> menu.linkUri?.let { copyToClipboard("Link", it) }
            ContextMenuAction.COPY_TEXT -> menu.textContent?.takeIf(String::isNotBlank)?.let { copyToClipboard("Text", it) }
            ContextMenuAction.OPEN_MEDIA_IN_NEW_TAB -> menu.resourceUri?.let { openContextUrl(menu.tabId, it, inNewTab = true) }
            ContextMenuAction.COPY_MEDIA_URL -> menu.resourceUri?.let { copyToClipboard("Media URL", it) }
            ContextMenuAction.SAVE_MEDIA -> menu.resourceUri?.let(::downloadUrl)
            ContextMenuAction.COPY_PAGE_URL -> menu.pageUrl.takeIf(String::isNotBlank)?.let { copyToClipboard("Page URL", it) }
            ContextMenuAction.TOGGLE_BOOKMARK -> if (menu.tabId == _state.value.activeTabId) toggleBookmark()
            ContextMenuAction.SAVE_PAGE -> menu.pageUrl.takeIf(String::isNotBlank)?.let(::downloadUrl)
            ContextMenuAction.DISMISS -> Unit
        }
        dismissContextMenu()
    }

    private fun closeOtherTabs(id: String) {
        val current = _state.value
        val keep = current.tabs.firstOrNull { it.id == id } ?: return
        val closing = current.tabs.filterNot { it.id == id }
        _state.update {
            it.copy(
                tabs = listOf(keep.copy(isSleeping = false)),
                activeTabId = id,
                overlay = BrowserOverlay.NONE,
            )
        }
        updateExtensionActiveTab(current.activeTabId, id)
        updateSessionActivity(id)
        persistOpenTabs()
        closing.forEach { tab -> runCatching { tab.session.close() } }
    }

    private fun closeTabsToRight(id: String) {
        val current = _state.value
        val index = current.tabs.indexOfFirst { it.id == id }
        if (index < 0 || index == current.tabs.lastIndex) return
        val closing = current.tabs.drop(index + 1)
        val newActive = if (closing.any { it.id == current.activeTabId }) id else current.activeTabId
        _state.update {
            it.copy(
                tabs = current.tabs.take(index + 1),
                activeTabId = newActive,
                overlay = BrowserOverlay.NONE,
            )
        }
        if (newActive != null) {
            _state.update { state ->
                state.copy(tabs = state.tabs.map { tab -> if (tab.id == newActive) tab.copy(isSleeping = false) else tab })
            }
        }
        if (newActive != current.activeTabId) updateExtensionActiveTab(current.activeTabId, newActive)
        updateSessionActivity(newActive)
        persistOpenTabs()
        closing.forEach { tab -> runCatching { tab.session.close() } }
    }

    fun reloadCrashedTab() {
        val tab = activeTab()?.takeIf { it.crashed } ?: return
        val url = tab.url
        if (url.isBlank()) return
        tab.session.close()
        val session = createSession(tab.id, tab.isPrivate)
        updateTab(tab.id) {
            it.copy(session = session, crashed = false, hasPage = true, isLoading = true, progress = 0)
        }
        session.loadUri(url)
    }

    fun toggleBookmark() {
        val tab = activeTab() ?: return
        if (!tab.hasPage || tab.url.isBlank() || !tab.url.startsWith("http")) return
        viewModelScope.launch {
            if (dao.isBookmarked(tab.url)) {
                dao.deleteBookmark(tab.url)
                showSnackbar("Bookmark removed")
            } else {
                dao.insertBookmark(
                    Bookmark(
                        url = tab.url,
                        title = tab.title.ifBlank { BrowserUrl.displayValue(tab.url) },
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                showSnackbar("Saved to bookmarks")
            }
        }
    }

    fun setBookmarkFolder(bookmark: Bookmark, folder: String?) {
        val normalized = folder?.trim()?.take(40)?.takeIf(String::isNotBlank)
        viewModelScope.launch { dao.updateBookmarkFolder(bookmark.url, normalized) }
    }

    fun exportBookmarks(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val bookmarks = dao.getBookmarks()
                val html = buildString {
                    appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
                    appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
                    appendLine("<TITLE>Dextra bookmarks</TITLE>")
                    appendLine("<H1>Dextra bookmarks</H1>")
                    bookmarks.groupBy { it.folder }.forEach { (folder, entries) ->
                        if (folder != null) {
                            appendLine("<DT><H3>${folder.htmlEscape()}</H3>")
                            appendLine("<DL><p>")
                        }
                        entries.forEach { bookmark ->
                            appendLine(
                                "<DT><A HREF=\"${bookmark.url.htmlEscape()}\" ADD_DATE=\"${bookmark.createdAt}\">${bookmark.title.htmlEscape()}</A>",
                            )
                        }
                        if (folder != null) appendLine("</DL><p>")
                    }
                }
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    output.writer(Charsets.UTF_8).use { it.write(html) }
                } ?: error("Could not open export file")
            }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(if (result.isSuccess) "Bookmarks exported" else "Could not export bookmarks")
            }
        }
    }

    fun importBookmarks(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = runCatching {
                val html = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    input.reader(Charsets.UTF_8).readText()
                } ?: error("Could not open import file")
                val anchors = Regex(
                    "<A\\s+[^>]*HREF\\s*=\\s*\"([^\"]+)\"[^>]*>(.*?)</A>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                )
                val headings = Regex(
                    "<H3[^>]*>(.*?)</H3>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                )
                var count = 0
                anchors.findAll(html).forEach { match ->
                    val url = Html.fromHtml(match.groupValues[1], Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    if (Uri.parse(url).scheme?.lowercase() !in setOf("http", "https")) return@forEach
                    val title = Html.fromHtml(match.groupValues[2], Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val folder = headings.findAll(html.substring(0, match.range.first)).lastOrNull()?.groupValues?.get(1)
                        ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                        ?.takeIf(String::isNotBlank)
                    dao.insertBookmark(
                        Bookmark(
                            url = url,
                            title = title.ifBlank { BrowserUrl.displayValue(url) },
                            createdAt = System.currentTimeMillis(),
                            folder = folder,
                        ),
                    )
                    count++
                }
                count
            }.getOrDefault(-1)
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(if (imported >= 0) "Imported $imported bookmarks" else "Could not import bookmarks")
            }
        }
    }

    private fun downloadUrl(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: run {
            showSnackbar("This resource cannot be downloaded")
            return
        }
        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            showSnackbar("This resource cannot be downloaded")
            return
        }
        runCatching {
            val fileName = uri.lastPathSegment
                ?.takeIf { it.isNotBlank() }
                ?.sanitizeFileName()
                ?: "dextra-download"
            val downloadId = -System.nanoTime()
            val downloadDirectory = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(getApplication<Application>().filesDir, "downloads")
            val outputPath = File(downloadDirectory, "$downloadId-$fileName").path
            val download = DownloadEntry(
                downloadId = downloadId,
                fileName = fileName,
                url = url,
                mimeType = null,
                status = DownloadStatus.QUEUED.label,
                bytesDownloaded = 0,
                totalBytes = -1,
                localUri = null,
                filePath = outputPath,
                reason = null,
                speedBytesPerSecond = 0,
                createdAt = System.currentTimeMillis(),
            )
            viewModelScope.launch {
                dao.upsertDownload(download)
                downloadEngine.start(download)
            }
            showSnackbar("Download started")
        }.onFailure { showSnackbar("Could not start download") }
    }

    fun openSavedPage(url: String) {
        val tab = activeTab() ?: return
        navigate(tab.id, url)
        dismissOverlay()
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
            showSnackbar("History cleared")
        }
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch { dao.deleteHistory(entry.id) }
    }

    fun setOverlay(overlay: BrowserOverlay) {
        _state.update { it.copy(overlay = overlay) }
    }

    fun dismissOverlay() {
        setOverlay(BrowserOverlay.NONE)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { settingsRepository.setSearchEngine(engine) }
    }

    fun setAdBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAdBlockingEnabled(enabled) }
    }

    fun setAdBlockFilterEnabled(filter: AdBlockFilter, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAdBlockFilterEnabled(filter.url, enabled) }
    }

    fun refreshAdBlockFilters() {
        syncAdBlockSettings(_state.value.settings)
        _state.value.tabs.filter { it.hasPage }.forEach { it.session.reload() }
        showSnackbar("Adblock lists are being updated")
    }

    fun addAdBlockFilter(url: String) {
        val normalized = url.trim()
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            showSnackbar("Enter a valid http(s) filter URL")
            return
        }
        viewModelScope.launch { settingsRepository.addAdBlockFilter(normalized) }
    }

    fun removeAdBlockFilter(filter: AdBlockFilter) {
        viewModelScope.launch { settingsRepository.removeAdBlockFilter(filter.url) }
    }

    fun addUserScript(url: String) {
        val normalized = url.trim()
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            showSnackbar("Enter a valid http(s) userscript URL")
            return
        }
        viewModelScope.launch { settingsRepository.addUserScript(normalized) }
    }

    fun removeUserScript(url: String) {
        viewModelScope.launch { settingsRepository.removeUserScript(url) }
    }

    fun setUserScriptEnabled(url: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUserScriptEnabled(url, enabled) }
    }

    fun refreshUserScripts() {
        syncUserScripts(_state.value.settings)
        _state.value.tabs.filter { it.hasPage }.forEach { it.session.reload() }
        showSnackbar("Userscripts are being updated")
    }

    fun installExtension(input: String) {
        val normalized = input.trim()
        if (normalized.isBlank()) {
            showSnackbar("Enter an AMO listing URL or a direct .xpi URL")
            return
        }
        if (_state.value.extensionInstallInProgress) return

        pendingExtensionPrivateBrowsing = false
        pendingExtensionDataCollection = false
        _state.update { it.copy(extensionInstallInProgress = true) }
        viewModelScope.launch {
            val packageUrl = withContext(Dispatchers.IO) { resolveExtensionPackageUrl(normalized) }
            if (packageUrl == null) {
                _state.update { it.copy(extensionInstallInProgress = false) }
                showSnackbar("Could not find a signed Firefox extension package")
                return@launch
            }
            val packageFile = withContext(Dispatchers.IO) { cacheExtensionPackage(packageUrl) }
            if (packageFile == null) {
                _state.update { it.copy(extensionInstallInProgress = false) }
                showSnackbar("Could not save extension package offline")
                return@launch
            }
            installExtensionPackage(Uri.fromFile(packageFile).toString(), packageFile.path)
        }
    }

    fun openFirefoxAddons() {
        dismissOverlay()
        navigateActive("https://addons.mozilla.org/en-US/firefox/")
    }

    fun setExtensionEnabled(id: String, enabled: Boolean) {
        val extension = installedExtensionObjects[id] ?: return
        val operation = if (enabled) {
            runtime.webExtensionController.enable(extension, WebExtensionController.EnableSource.USER)
        } else {
            runtime.webExtensionController.disable(extension, WebExtensionController.EnableSource.USER)
        }
        operation.accept(
            { refreshInstalledExtensions() },
            { showSnackbar(if (enabled) "Could not enable extension" else "Could not disable extension") },
        )
    }

    fun updateExtension(id: String) {
        val extension = installedExtensionObjects[id] ?: return
        runtime.webExtensionController.update(extension).accept(
            { updated ->
                refreshInstalledExtensions()
                showSnackbar(if (updated == null) "Extension is already up to date" else "Extension updated")
            },
            { showSnackbar("Could not update extension") },
        )
    }

    fun clickExtensionAction(id: String) {
        extensionActionObjects[id]?.let { action ->
            runCatching { action.click() }
                .onFailure { showSnackbar("Could not open extension action") }
        }
    }

    fun closeExtensionPopup() {
        val popup = _state.value.extensionPopup ?: return
        _state.update { it.copy(extensionPopup = null) }
        runCatching { popup.session.close() }
    }

    fun openExtensionOptions(id: String) {
        val extension = installedExtensionObjects[id] ?: return
        val declaredUrl = extension.metaData.optionsPageUrl?.takeIf(String::isNotBlank)
        if (declaredUrl == null) {
            showSnackbar("This extension has no settings page")
            return
        }
        val optionsUrl = if (declaredUrl.startsWith("moz-extension://")) {
            declaredUrl
        } else {
            "${extension.metaData.baseUrl.trimEnd('/')}/${declaredUrl.trimStart('/')}"
        }
        val scheme = runCatching { Uri.parse(optionsUrl).scheme?.lowercase() }.getOrNull()
        if (scheme != "moz-extension" && scheme != "https") {
            showSnackbar("Extension settings URL is invalid")
            return
        }
        val tabId = createTab()
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        updateTab(tabId) { it.copy(url = optionsUrl, title = "Extension settings", hasPage = true, isLoading = true) }
        tab.session.loadUri(optionsUrl)
    }

    fun setExtensionPrivateBrowsing(id: String, allowed: Boolean) {
        val extension = installedExtensionObjects[id] ?: return
        runtime.webExtensionController.setAllowedInPrivateBrowsing(extension, allowed).accept(
            { refreshInstalledExtensions() },
            { showSnackbar("Could not update private-tab access") },
        )
    }

    fun uninstallExtension(id: String) {
        val extension = installedExtensionObjects[id] ?: return
        val installRecord = _state.value.settings.extensionInstallRecords[id]
        runtime.webExtensionController.uninstall(extension).accept(
            {
                installedExtensionObjects.remove(id)
                installRecord?.filePath?.let { path -> runCatching { File(path).delete() } }
                viewModelScope.launch { settingsRepository.removeExtensionInstallRecord(id) }
                refreshInstalledExtensions()
                showSnackbar("Extension removed")
            },
            { showSnackbar("Could not remove extension") },
        )
    }

    fun resolveExtensionInstall(
        allow: Boolean,
        allowInPrivateBrowsing: Boolean,
        allowDataCollection: Boolean,
    ) {
        val prompt = _state.value.extensionInstallPrompt ?: return
        pendingExtensionPrivateBrowsing = allow && allowInPrivateBrowsing
        pendingExtensionDataCollection = allow && allowDataCollection
        prompt.result.complete(
            WebExtension.PermissionPromptResponse(
                allow,
                allow && allowInPrivateBrowsing,
                allow && allowDataCollection,
            ),
        )
        _state.update { it.copy(extensionInstallPrompt = null) }
    }

    fun resolveExtensionUpdate(allow: Boolean) {
        val prompt = _state.value.extensionUpdatePrompt ?: return
        prompt.result.complete(if (allow) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
        _state.update { it.copy(extensionUpdatePrompt = null) }
    }

    fun setDesktopSites(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDesktopSites(enabled) }
    }

    fun setHomepage(value: String) {
        val homepage = BrowserUrl.resolve(value, _state.value.settings.searchEngine)
        val scheme = runCatching { Uri.parse(homepage).scheme?.lowercase() }.getOrNull()
        if (scheme !in setOf("http", "https", "about")) {
            showSnackbar("Homepage must be a valid web address")
            return
        }
        viewModelScope.launch { settingsRepository.setHomepage(homepage) }
    }

    fun setTabBarWithAddressBar(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTabBarWithAddressBar(enabled) }
    }

    fun setVerticalTabs(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVerticalTabs(enabled) }
    }

    fun setDnsOverHttpsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDnsOverHttpsEnabled(enabled) }
    }

    fun setDnsProvider(provider: DnsProvider) {
        viewModelScope.launch { settingsRepository.setDnsProvider(provider) }
    }

    fun resolveContentPermission(allow: Boolean) {
        val prompt = _state.value.contentPermission ?: return
        prompt.result.complete(
            if (allow) GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
            else GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY,
        )
        _state.update { it.copy(contentPermission = null) }
    }

    fun resolveAndroidPermission(granted: Boolean) {
        val prompt = _state.value.androidPermission ?: return
        if (granted) prompt.callback.grant() else prompt.callback.reject()
        _state.update { it.copy(androidPermission = null) }
    }

    fun resolveMediaPermission(granted: Boolean) {
        val prompt = _state.value.mediaPermission ?: return
        if (granted) {
            prompt.callback.grant(prompt.video.firstOrNull()?.id, prompt.audio.firstOrNull()?.id)
        } else {
            prompt.callback.reject()
        }
        _state.update { it.copy(mediaPermission = null) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    fun dismissCrashReport() {
        runCatching { File(getApplication<Application>().filesDir, "last-crash.txt").delete() }
        _state.update { it.copy(lastCrashReport = null) }
    }

    fun copyCrashReport() {
        _state.value.lastCrashReport?.let { copyToClipboard("Crash report", it) }
    }

    fun openDownload(download: DownloadEntry) {
        val uri = downloadUri(download) ?: run {
            showSnackbar("Downloaded file is no longer available")
            return
        }
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .setDataAndType(uri, download.mimeType ?: "*/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { showSnackbar("No app can open this file") }
    }

    fun shareDownload(download: DownloadEntry) {
        val uri = downloadUri(download) ?: run {
            showSnackbar("Downloaded file is no longer available")
            return
        }
        runCatching {
            getApplication<Application>().startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType(download.mimeType ?: "*/*")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    "Share download",
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { showSnackbar("Could not share this file") }
    }

    fun cancelDownload(download: DownloadEntry) {
        if (download.status == DownloadStatus.COMPLETE.label) return
        downloadEngine.cancel(download)
    }

    fun toggleDownload(download: DownloadEntry) {
        when (download.status) {
            DownloadStatus.QUEUED.label,
            DownloadStatus.DOWNLOADING.label,
            -> downloadEngine.pause(download)
            DownloadStatus.PAUSED.label -> downloadEngine.start(download)
            else -> Unit
        }
    }

    fun removeDownload(download: DownloadEntry) {
        removedDownloadIds.add(download.downloadId)
        downloadEngine.remove(download)
        download.filePath?.let { path -> runCatching { File(path).delete() } }
        download.localUri?.let { uri -> runCatching { getApplication<Application>().contentResolver.delete(Uri.parse(uri), null, null) } }
        viewModelScope.launch { dao.deleteDownload(download.downloadId) }
    }

    private fun createSession(
        tabId: String,
        privateMode: Boolean,
        openSession: Boolean = true,
        extensionPopup: Boolean = false,
    ): GeckoSession {
        val settings = _state.value.settings
        // Extension popups use a device-width viewport so their CSS is not scaled down from 980px.
        val desktopViewport = settings.desktopSites && !extensionPopup
        val session = GeckoSession(
            GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .allowJavascript(true)
                .userAgentMode(if (desktopViewport) {
                    GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                } else {
                    GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                })
                .viewportMode(if (desktopViewport) {
                    GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                } else {
                    GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                })
                .build(),
        )
        session.setNavigationDelegate(NavigationDelegate(tabId))
        session.setHistoryDelegate(HistoryDelegate(tabId))
        session.setProgressDelegate(ProgressDelegate(tabId))
        session.setContentDelegate(ContentDelegate(tabId))
        session.setPermissionDelegate(PermissionDelegate(tabId))
        if (openSession) {
            session.open(runtime)
            attachAdBlockContentDelegate(session)
            attachExtensionActionDelegates(session)
        }
        return session
    }

    private fun applyDesktopSiteSetting(session: GeckoSession, desktopSites: Boolean) {
        session.settings.setUserAgentMode(
            if (desktopSites) {
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            },
        )
        session.settings.setViewportMode(
            if (desktopSites) {
                GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            },
        )
    }

    private fun updateTab(tabId: String, transform: (BrowserTabState) -> BrowserTabState) {
        _state.update { current ->
            current.copy(tabs = current.tabs.map { tab -> if (tab.id == tabId) transform(tab) else tab })
        }
    }

    private fun rememberClosedTab(tab: BrowserTabState) {
        if (tab.url.isBlank() || !tab.hasPage) return
        recentlyClosedTabs.addFirst(ClosedTabEntry(tab.url, tab.isPrivate, tab.pinned, tab.groupId))
        while (recentlyClosedTabs.size > 10) recentlyClosedTabs.removeLast()
    }

    private fun restoreSavedTabs(savedTabs: List<SavedTab>, activeIndex: Int) {
        val tabsToRestore = savedTabs
            .filter { !it.isPrivate && it.url.isNotBlank() && it.url != "about:blank" }
            .take(MAX_OPEN_TABS)
        if (tabsToRestore.isEmpty()) return

        val current = _state.value
        setExtensionTabActive(current.activeTabId, false)
        current.tabs.forEach { tab -> runCatching { tab.session.close() } }
        val restored = tabsToRestore.map { saved ->
            val id = UUID.randomUUID().toString()
            val session = createSession(id, saved.isPrivate)
            BrowserTabState(
                id = id,
                session = session,
                title = "Loading...",
                url = saved.url,
                isLoading = true,
                isSecure = saved.url.startsWith("https://"),
                hasPage = true,
                isPrivate = saved.isPrivate,
                pinned = saved.pinned,
                groupId = saved.groupId?.takeIf { groupId -> current.settings.tabGroups.any { it.id == groupId } },
            )
        }
        val activeTab = restored.getOrNull(activeIndex.coerceIn(0, restored.lastIndex)) ?: restored.first()
        _state.update {
            it.copy(
                tabs = restored,
                activeTabId = activeTab.id,
                overlay = BrowserOverlay.NONE,
            )
        }
        setExtensionTabActive(activeTab.id, true)
        updateSessionActivity(activeTab.id)
        restored.forEach { tab -> tab.session.loadUri(tab.url) }
    }

    private fun persistOpenTabs() {
        if (!restoredSavedTabs) return
        val current = _state.value
        val pageTabs = current.tabs.filter {
            !it.isPrivate && it.hasPage && it.url.isNotBlank() && it.url != "about:blank"
        }
        val activeIndex = pageTabs.indexOfFirst { it.id == current.activeTabId }
        viewModelScope.launch {
            settingsRepository.saveOpenTabs(
                tabs = pageTabs.map { tab -> SavedTab(tab.url, tab.isPrivate, tab.pinned, tab.groupId) },
                activeTabIndex = activeIndex.coerceAtLeast(0),
                groups = current.settings.tabGroups,
            )
        }
    }

    private fun updateSessionActivity(activeId: String?) {
        _state.value.tabs.forEach { tab ->
            runCatching { tab.session.setActive(tab.id == activeId) }
        }
    }

    private fun updateExtensionActiveTab(previousId: String?, activeId: String?) {
        if (previousId == activeId) return
        setExtensionTabActive(previousId, false)
        setExtensionTabActive(activeId, true)
        syncActiveTabZoom()
    }

    private fun setExtensionTabActive(tabId: String?, active: Boolean) {
        _state.value.tabs.firstOrNull { it.id == tabId }?.session?.let { session ->
            runtime.webExtensionController.setTabActive(session, active)
        }
    }

    private fun syncActiveTabZoom() {
        val zoom = pageZoomByTab[_state.value.activeTabId] ?: 100
        adBlockPort?.postMessage(
            JSONObject()
                .put("type", "setZoom")
                .put("zoomFactor", zoom / 100.0),
        )
    }

    private fun showSnackbar(message: String) {
        _state.update { it.copy(snackbar = message) }
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
        showSnackbar("$label copied")
    }

    private fun readLastCrashReport(): String? = runCatching {
        File(getApplication<Application>().filesDir, "last-crash.txt")
            .takeIf(File::isFile)
            ?.readText()
            ?.takeIf(String::isNotBlank)
    }.getOrNull()

    private fun openContextUrl(tabId: String, url: String, inNewTab: Boolean, privateTab: Boolean = false) {
        val scheme = runCatching { Uri.parse(url).scheme?.lowercase() }.getOrNull()
        if (scheme !in setOf("http", "https", "about", "file", "data")) {
            launchExternal(url)
        } else if (inNewTab) {
            createTab(privateMode = privateTab, initialUri = url)
        } else {
            navigate(tabId, url)
        }
    }

    private val activeDownloadStatuses = setOf(
        DownloadStatus.COMPLETE.label,
        DownloadStatus.FAILED.label,
        DownloadStatus.CANCELED.label,
    )

    private fun restoreDownloads() {
        viewModelScope.launch {
            dao.getDownloads()
                .filter { it.filePath != null && it.status !in activeDownloadStatuses }
                .forEach(downloadEngine::start)
        }
    }

    private suspend fun applyDownloadUpdate(downloadId: Long, update: DownloadUpdate) {
        if (downloadId in removedDownloadIds) return
        val current = dao.getDownload(downloadId) ?: return
        val filePath = update.filePath ?: current.filePath
        val localUri = if (update.status == DownloadStatus.COMPLETE.label && filePath != null) {
            current.localUri ?: publishDownload(current, filePath) ?: runCatching {
                FileProvider.getUriForFile(
                    getApplication<Application>(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    File(filePath),
                ).toString()
            }.getOrNull()
        } else {
            current.localUri
        }
        val saved = current.copy(
            status = update.status,
            bytesDownloaded = update.bytesDownloaded ?: current.bytesDownloaded,
            totalBytes = update.totalBytes ?: current.totalBytes,
            speedBytesPerSecond = update.speedBytesPerSecond ?: current.speedBytesPerSecond,
            localUri = localUri,
            filePath = filePath,
            reason = update.reason,
        )
        dao.upsertDownload(saved)
        if (update.status in setOf(DownloadStatus.COMPLETE.label, DownloadStatus.FAILED.label)) {
            notifyDownload(saved)
        }
    }

    private suspend fun publishDownload(download: DownloadEntry, filePath: String): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val resolver = getApplication<Application>().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, download.fileName)
            put(MediaStore.Downloads.MIME_TYPE, download.mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Dextra")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            resolver.openOutputStream(uri)?.use { output ->
                File(filePath).inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open public download")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            uri.toString()
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun notifyDownload(download: DownloadEntry) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val complete = download.status == DownloadStatus.COMPLETE.label
        manager.notify(
            download.downloadId.toInt(),
            NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(if (complete) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                .setContentTitle(download.fileName)
                .setContentText(if (complete) "Download complete" else download.reason ?: "Download failed")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun installAdBlocker() {
        runtime.webExtensionController.list().accept(
            { extensions ->
                val staleExtensions = extensions.orEmpty().filter {
                    it.id == "adblock@dextra" && it.metaData.version != "2.6.0"
                }
                removeStaleAdBlockers(staleExtensions)
            },
            { ensureAdBlocker() },
        )
    }

    private fun removeStaleAdBlockers(blockers: List<WebExtension>, index: Int = 0) {
        if (index >= blockers.size) {
            ensureAdBlocker()
            return
        }
        runtime.webExtensionController.uninstall(blockers[index]).accept(
            { removeStaleAdBlockers(blockers, index + 1) },
            { removeStaleAdBlockers(blockers, index + 1) },
        )
    }

    private fun ensureAdBlocker() {
        runtime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/adblock/",
            "adblock@dextra",
        ).accept(
            { extension ->
                extension?.let(::configureAdBlocker)
                refreshInstalledExtensions()
            },
            { error -> Log.e("Dextra", "Could not start Dextra ad blocker", error) },
        )
    }

    private fun configureAdBlocker(extension: WebExtension) {
        runtime.webExtensionController.enable(extension, WebExtensionController.EnableSource.APP)
            .accept(
                { enabledExtension -> configureAdBlockerPermissions(enabledExtension ?: extension) },
                { error ->
                    Log.e("Dextra", "Could not enable Dextra ad blocker", error)
                    configureAdBlockerPermissions(extension)
                },
            )
    }

    private fun configureAdBlockerPermissions(extension: WebExtension) {
        adBlockExtension = extension
        extension.setMessageDelegate(adBlockMessageDelegate, "dextra")
        _state.value.tabs.forEach { tab -> attachAdBlockContentDelegate(tab.session) }
        runtime.webExtensionController.setAllowedInPrivateBrowsing(extension, true)
            .accept({}, { error -> Log.e("Dextra", "Could not allow ad blocking in private tabs", error) })
    }

    private fun attachAdBlockContentDelegate(session: GeckoSession) {
        if (!session.isOpen) return
        adBlockExtension?.let { extension ->
            session.webExtensionController.setMessageDelegate(
                extension,
                adBlockContentMessageDelegate,
                "dextra",
            )
        }
    }

    private fun syncAdBlockSettings(settings: BrowserSettings) {
        val port = adBlockPort ?: return
        val urls = JSONArray()
        settings.adBlockFilters.filter { it.enabled }.forEach { urls.put(it.url) }
        port.postMessage(
            JSONObject()
                .put("type", "updateAdblock")
                .put("enabled", settings.adBlockingEnabled)
                .put("urls", urls),
        )
    }

    private fun applyDnsOverHttps(enabled: Boolean, provider: DnsProvider) {
        runtime.settings.setDohAutoselectEnabled(false)
        runtime.settings.setTrustedRecursiveResolverUri(provider.dohUri)
        runtime.settings.setTrustedRecursiveResolverMode(
            if (enabled) {
                org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_ONLY
            } else {
                org.mozilla.geckoview.GeckoRuntimeSettings.TRR_MODE_OFF
            },
        )
    }

    private fun applyContentColorScheme(themeMode: ThemeMode) {
        runtime.settings.setPreferredColorScheme(
            when (themeMode) {
                ThemeMode.SYSTEM -> org.mozilla.geckoview.GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
                ThemeMode.LIGHT -> org.mozilla.geckoview.GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
                ThemeMode.DARK -> org.mozilla.geckoview.GeckoRuntimeSettings.COLOR_SCHEME_DARK
            },
        )
    }

    private fun attachExtensionActionDelegates(session: GeckoSession) {
        installedExtensionObjects.values.forEach { extension ->
            session.webExtensionController.setActionDelegate(extension, extensionActionDelegate)
        }
    }

    private fun syncUserScripts(settings: BrowserSettings) {
        val port = adBlockPort ?: return
        val urls = JSONArray()
        settings.userScriptUrls
            .filterNot { it in settings.disabledUserScriptUrls }
            .forEach(urls::put)
        port.postMessage(
            JSONObject()
                .put("type", "updateUserscripts")
                .put("urls", urls),
        )
    }

    private fun createExtensionInstallPrompt(
        extension: WebExtension,
        permissions: Array<String>,
        origins: Array<String>,
        dataCollectionPermissions: Array<String>,
    ): GeckoResult<WebExtension.PermissionPromptResponse> {
        val result = GeckoResult<WebExtension.PermissionPromptResponse>()
        val restoreRecord = _state.value.settings.extensionInstallRecords[extension.id]
        if (extension.id in restoringExtensionIds && restoreRecord != null) {
            pendingExtensionPrivateBrowsing = restoreRecord.allowInPrivateBrowsing
            pendingExtensionDataCollection = restoreRecord.allowDataCollection
            result.complete(
                WebExtension.PermissionPromptResponse(
                    true,
                    restoreRecord.allowInPrivateBrowsing,
                    restoreRecord.allowDataCollection,
                ),
            )
            return result
        }
        _state.value.extensionInstallPrompt?.result?.complete(
            WebExtension.PermissionPromptResponse(false, false, false),
        )
        _state.update {
            it.copy(
                extensionInstallPrompt = ExtensionInstallPrompt(
                    id = UUID.randomUUID().toString(),
                    extensionId = extension.id,
                    name = extension.metaData.name ?: extension.id,
                    version = extension.metaData.version,
                    permissions = permissions.toList(),
                    origins = origins.toList(),
                    dataCollectionPermissions = dataCollectionPermissions.toList(),
                    packageFilePath = pendingExtensionPackagePath,
                    result = result,
                ),
            )
        }
        return result
    }

    private fun createExtensionUpdatePrompt(
        extension: WebExtension,
        updatedExtension: WebExtension,
        permissions: Array<String>,
        origins: Array<String>,
    ): GeckoResult<AllowOrDeny> {
        _state.value.extensionUpdatePrompt?.result?.complete(AllowOrDeny.DENY)
        val result = GeckoResult<AllowOrDeny>()
        _state.update {
            it.copy(
                extensionUpdatePrompt = ExtensionUpdatePrompt(
                    id = UUID.randomUUID().toString(),
                    name = extension.metaData.name ?: extension.id,
                    currentVersion = extension.metaData.version,
                    newVersion = updatedExtension.metaData.version,
                    permissions = permissions.toList(),
                    origins = origins.toList(),
                    result = result,
                ),
            )
        }
        return result
    }

    private fun refreshInstalledExtensions() {
        runtime.webExtensionController.list().accept(
            { extensions ->
                val userExtensions = extensions.orEmpty().filterNot {
                    it.isBuiltIn || it.id == "adblock@dextra"
                }
                installedExtensionObjects.clear()
                extensionActionObjects.keys.retainAll(userExtensions.mapTo(hashSetOf()) { it.id })
                userExtensions.forEach { installedExtensionObjects[it.id] = it }
                userExtensions.forEach { it.setActionDelegate(extensionActionDelegate) }
                _state.value.tabs.filter { it.session.isOpen }.forEach { tab ->
                    attachExtensionActionDelegates(tab.session)
                }
                _state.update {
                    it.copy(
                        installedExtensions = userExtensions.map(::installedExtensionState),
                        extensionActions = it.extensionActions.filter { action -> action.extensionId in installedExtensionObjects },
                    )
                }
                setExtensionTabActive(_state.value.activeTabId, true)
            },
            { error -> Log.e("Dextra", "Could not list Firefox extensions", error) },
        )
    }

    private fun restoreOfflineExtensions() {
        if (_state.value.extensionInstallInProgress) return
        runtime.webExtensionController.list().accept(
            { extensions ->
                val installedIds = extensions.orEmpty().mapTo(hashSetOf()) { it.id }
                val missing = _state.value.settings.extensionInstallRecords.entries
                    .firstOrNull { (id, record) ->
                        id !in installedIds && File(record.filePath).isFile && restoringExtensionIds.add(id)
                    }
                    ?: return@accept
                val file = File(missing.value.filePath)
                pendingExtensionPrivateBrowsing = missing.value.allowInPrivateBrowsing
                pendingExtensionDataCollection = missing.value.allowDataCollection
                installExtensionPackage(Uri.fromFile(file).toString(), file.path)
            },
            { error -> Log.e("Dextra", "Could not restore offline extensions", error) },
        )
    }

    private fun installedExtensionState(extension: WebExtension): InstalledExtension =
        InstalledExtension(
            id = extension.id,
            name = extension.metaData.name ?: extension.id,
            version = extension.metaData.version,
            creatorName = extension.metaData.creatorName,
            enabled = extension.metaData.enabled,
            allowedInPrivateBrowsing = extension.metaData.allowedInPrivateBrowsing,
            amoListingUrl = extension.metaData.amoListingUrl,
            optionsPageUrl = extension.metaData.optionsPageUrl,
        )

    private fun updateExtensionAction(extension: WebExtension, action: WebExtension.Action) {
        extensionActionObjects[extension.id] = action
        val toolbarAction = ExtensionToolbarAction(
            extensionId = extension.id,
            title = action.title?.takeIf(String::isNotBlank) ?: extension.metaData.name ?: extension.id,
            enabled = action.enabled != false,
            badgeText = action.badgeText,
            icon = null,
        )
        _state.update { current ->
            current.copy(
                extensionActions = (current.extensionActions.filterNot { it.extensionId == extension.id } + toolbarAction)
                    .sortedBy { it.title.lowercase() },
            )
        }
        action.icon?.getBitmap(32)?.accept(
            { bitmap ->
                _state.update { current ->
                    current.copy(
                        extensionActions = current.extensionActions.map { item ->
                            if (item.extensionId == extension.id) item.copy(icon = bitmap) else item
                        },
                    )
                }
            },
            {},
        )
    }

    private fun openExtensionPopup(extension: WebExtension): GeckoResult<GeckoSession>? {
        closeExtensionPopup()
        val session = createSession("extension-popup-${extension.id}", privateMode = false, extensionPopup = true)
        _state.update {
            it.copy(
                extensionPopup = ExtensionPopupState(
                    extensionId = extension.id,
                    extensionName = extension.metaData.name ?: extension.id,
                    session = session,
                ),
            )
        }
        return GeckoResult.fromValue(session)
    }

    private fun resolveExtensionPackageUrl(input: String): String? {
        if (FirefoxAddons.isXpiUrl(input)) return input
        val slug = FirefoxAddons.listingSlug(input) ?: return null
        val apiUrl = "https://addons.mozilla.org/api/v5/addons/addon/${Uri.encode(slug)}/?lang=en-US"
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val currentVersion = JSONObject(body).optJSONObject("current_version") ?: return null
            val currentFileUrl = currentVersion.optJSONObject("file")?.optString("url").orEmpty()
            if (currentFileUrl.startsWith("https://")) return currentFileUrl
            val files = currentVersion.optJSONArray("files") ?: return null
            for (index in 0 until files.length()) {
                val downloadUrl = files.optJSONObject(index)?.optString("download_url").orEmpty()
                if (downloadUrl.startsWith("https://")) return downloadUrl
            }
            null
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun cacheExtensionPackage(packageUrl: String): File? = withContext(Dispatchers.IO) {
        val directory = File(getApplication<Application>().filesDir, "extensions")
        if (!directory.isDirectory && !directory.mkdirs()) return@withContext null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(packageUrl.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        val destination = File(directory, "$digest.xpi")
        if (destination.isFile && destination.length() > 0) return@withContext destination

        val temporary = File(directory, "$digest.xpi.part")
        val connection = (URL(packageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/x-xpinstall,application/octet-stream")
            setRequestProperty("User-Agent", "Dextra/${android.os.Build.VERSION.SDK_INT}")
        }
        try {
            if (connection.responseCode !in 200..299 || connection.contentLengthLong > MAX_EXTENSION_PACKAGE_BYTES) {
                temporary.delete()
                return@withContext null
            }
            try {
                var total = 0L
                connection.inputStream.use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            total += count
                            if (total > MAX_EXTENSION_PACKAGE_BYTES) throw IOException("Extension package is too large")
                            output.write(buffer, 0, count)
                        }
                    }
                }
                if (temporary.length() == 0L) {
                    temporary.delete()
                    return@withContext null
                }
                if (!temporary.renameTo(destination)) {
                    temporary.copyTo(destination, overwrite = true)
                    temporary.delete()
                }
                destination
            } catch (_: Exception) {
                temporary.delete()
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun installExtensionPackageFromUrl(packageUrl: String) {
        if (_state.value.extensionInstallInProgress) return
        _state.update { it.copy(extensionInstallInProgress = true) }
        viewModelScope.launch {
            val packageFile = withContext(Dispatchers.IO) { cacheExtensionPackage(packageUrl) }
            if (packageFile == null) {
                _state.update { it.copy(extensionInstallInProgress = false) }
                showSnackbar("Could not save extension package offline")
                return@launch
            }
            installExtensionPackage(Uri.fromFile(packageFile).toString(), packageFile.path)
        }
    }

    private fun installExtensionPackage(packageUri: String, packageFilePath: String?) {
        pendingExtensionPackagePath = packageFilePath
        val alreadyPreparing = _state.value.extensionInstallInProgress
        _state.update { it.copy(extensionInstallInProgress = true) }
        if (!alreadyPreparing) showSnackbar("Preparing extension installation")
        runtime.webExtensionController.install(
            packageUri,
            WebExtensionController.INSTALLATION_METHOD_FROM_FILE,
        ).accept(
            { extension ->
                val installedId = extension?.id
                val filePath = pendingExtensionPackagePath
                if (installedId != null && filePath != null) {
                    viewModelScope.launch {
                        settingsRepository.saveExtensionInstallRecord(
                            installedId,
                            ExtensionInstallRecord(
                                filePath = filePath,
                                allowInPrivateBrowsing = pendingExtensionPrivateBrowsing,
                                allowDataCollection = pendingExtensionDataCollection,
                            ),
                        )
                    }
                }
                pendingExtensionPackagePath = null
                pendingExtensionPrivateBrowsing = false
                pendingExtensionDataCollection = false
                restoringExtensionIds.clear()
                _state.update { it.copy(extensionInstallInProgress = false, extensionInstallPrompt = null) }
                refreshInstalledExtensions()
                refreshAmoPages()
                showSnackbar("${extension?.metaData?.name ?: "Extension"} installed")
            },
            { error ->
                pendingExtensionPackagePath = null
                pendingExtensionPrivateBrowsing = false
                pendingExtensionDataCollection = false
                restoringExtensionIds.clear()
                _state.update { it.copy(extensionInstallInProgress = false, extensionInstallPrompt = null) }
                showSnackbar(extensionInstallError(error))
            },
        )
    }

    private fun extensionInstallError(error: Throwable?): String {
        val message = error?.message.orEmpty().lowercase()
        return when {
            "signed" in message -> "Extension must be Mozilla-signed"
            "incompatible" in message -> "Extension is incompatible with GeckoView"
            "blocklist" in message -> "Extension is blocked by Mozilla"
            "cancel" in message || "denied" in message -> "Extension installation canceled"
            else -> "Could not install extension"
        }
    }

    private fun refreshAmoPages() {
        _state.value.tabs
            .filter { it.url.startsWith("https://addons.mozilla.org/") && !it.crashed }
            .forEach { it.session.reload() }
    }

    private fun recordHistory(tabId: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.isPrivate || !tab.url.startsWith("http")) return
        viewModelScope.launch {
            dao.insertHistory(
                HistoryEntry(
                    url = tab.url,
                    title = tab.title.ifBlank { BrowserUrl.displayValue(tab.url) },
                    visitedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun loadFavicon(tabId: String) {
        val pageUrl = _state.value.tabs.firstOrNull { it.id == tabId }?.url ?: return
        val pageUri = runCatching { Uri.parse(pageUrl) }.getOrNull() ?: return
        if (pageUri.scheme !in setOf("http", "https") || pageUri.authority.isNullOrBlank()) return

        val faviconUrl = Uri.Builder()
            .scheme(pageUri.scheme)
            .encodedAuthority(pageUri.encodedAuthority)
            .path("favicon.ico")
            .build()
            .toString()

        viewModelScope.launch(Dispatchers.IO) {
            val favicon = runCatching {
                val connection = URL(faviconUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 4_000
                connection.readTimeout = 4_000
                connection.instanceFollowRedirects = true
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull() ?: return@launch

            withContext(Dispatchers.Main.immediate) {
                val currentTab = _state.value.tabs.firstOrNull { it.id == tabId }
                if (currentTab?.url == pageUrl) updateTab(tabId) { it.copy(favicon = favicon) }
            }
        }
    }

    private fun launchExternal(uri: String) {
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { showSnackbar("No app can open this link") }
    }

    private fun download(response: WebResponse) {
        if (response.uri.isBlank()) return
        runCatching {
            val fileName = downloadName(response)
            val mimeType = response.headers["Content-Type"]?.substringBefore(';')
            val downloadId = -System.nanoTime()
            val downloadDirectory = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(getApplication<Application>().filesDir, "downloads")
            val outputPath = File(downloadDirectory, "$downloadId-$fileName").path
            val download = DownloadEntry(
                downloadId = downloadId,
                fileName = fileName,
                url = response.uri,
                mimeType = mimeType,
                status = DownloadStatus.QUEUED.label,
                bytesDownloaded = 0,
                totalBytes = -1,
                localUri = null,
                filePath = outputPath,
                reason = null,
                speedBytesPerSecond = 0,
                createdAt = System.currentTimeMillis(),
            )
            viewModelScope.launch {
                dao.upsertDownload(download)
                downloadEngine.start(download)
            }
            showSnackbar("Download started")
        }.onFailure { showSnackbar("Could not start download") }
    }

    private fun downloadUri(download: DownloadEntry): Uri? =
        download.localUri?.let(Uri::parse)
            ?: download.filePath?.let { path ->
                runCatching {
                    FileProvider.getUriForFile(
                        getApplication<Application>(),
                        "${getApplication<Application>().packageName}.fileprovider",
                        File(path),
                    )
                }.getOrNull()
            }

    private fun downloadName(response: WebResponse): String {
        val pathName = Uri.parse(response.uri).lastPathSegment?.takeIf { it.isNotBlank() }
        if (pathName != null) return pathName.sanitizeFileName()
        val mimeType = response.headers["Content-Type"]?.substringBefore(';')
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return "dextra-download${extension?.let { ".${it}" } ?: ""}"
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "dextra-download" }

    private fun String.htmlEscape(): String =
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private inner class NavigationDelegate(private val tabId: String) : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            permissions: List<GeckoSession.PermissionDelegate.ContentPermission>,
            isSameDocument: Boolean,
        ) {
            val resolvedUrl = url.orEmpty()
            if (resolvedUrl == "about:blank") {
                updateTab(tabId) { it.copy(url = "", title = "New tab", hasPage = false, isLoading = false, progress = 0) }
            } else {
                updateTab(tabId) { it.copy(url = resolvedUrl, isSecure = resolvedUrl.startsWith("https://"), crashed = false) }
            }
            persistOpenTabs()
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            updateTab(tabId) { it.copy(canGoBack = canGoBack) }
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            updateTab(tabId) { it.copy(canGoForward = canGoForward) }
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest,
        ): GeckoResult<AllowOrDeny> {
            val scheme = Uri.parse(request.uri).scheme?.lowercase()
            if (scheme != null && scheme !in setOf("http", "https", "about", "file", "data", "moz-extension")) {
                launchExternal(request.uri)
                return GeckoResult.deny()
            }
            return GeckoResult.allow()
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession> {
            val popupUri = uri.ifBlank { "about:blank" }
            val scheme = runCatching { Uri.parse(popupUri).scheme?.lowercase() }.getOrNull()
            if (scheme !in setOf("http", "https", "about")) {
                if (scheme != null) launchExternal(popupUri)
                return GeckoResult.fromValue<GeckoSession>(null)
            }
            if (_state.value.tabs.size >= MAX_OPEN_TABS) {
                showSnackbar("Popup blocked: tab limit reached")
                return GeckoResult.fromValue<GeckoSession>(null)
            }
            // GeckoView opens the returned session itself; it must still be unopened here.
            val newTabId = createTab(initialUri = popupUri, openSession = false)
            val newSession = _state.value.tabs.firstOrNull { it.id == newTabId }?.session
                ?: return GeckoResult.fromValue<GeckoSession>(null)
            return GeckoResult.fromValue(newSession)
        }
    }

    private inner class HistoryDelegate(private val tabId: String) : GeckoSession.HistoryDelegate {
        override fun onHistoryStateChange(
            session: GeckoSession,
            history: GeckoSession.HistoryDelegate.HistoryList,
        ) {
            val currentIndex = history.currentIndex
            updateTab(tabId) {
                it.copy(
                    canGoBack = currentIndex > 0,
                    canGoForward = currentIndex >= 0 && currentIndex < history.size - 1,
                )
            }
        }
    }

    private inner class ProgressDelegate(private val tabId: String) : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            attachAdBlockContentDelegate(session)
            if (url == "about:blank") {
                updateTab(tabId) { it.copy(url = "", title = "New tab", hasPage = false, isLoading = false, progress = 0, favicon = null) }
            } else {
                updateTab(tabId) { it.copy(url = url, hasPage = true, isLoading = true, progress = 0, favicon = null, crashed = false) }
            }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            updateTab(tabId) { it.copy(isLoading = false, progress = if (success) 100 else 0) }
            if (success) {
                recordHistory(tabId)
                loadFavicon(tabId)
            }
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
        }

        override fun onSecurityChange(
            session: GeckoSession,
            securityInfo: GeckoSession.ProgressDelegate.SecurityInformation,
        ) {
            updateTab(tabId) { it.copy(isSecure = securityInfo.isSecure) }
        }
    }

    private inner class ContentDelegate(private val tabId: String) : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            updateTab(tabId) { it.copy(title = title?.ifBlank { "New tab" } ?: "New tab") }
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            val contentType = response.headers["Content-Type"] ?: response.headers["content-type"]
            if (FirefoxAddons.isAmoUrl(response.uri) && FirefoxAddons.isXpiDownload(response.uri, contentType)) {
                installExtensionPackageFromUrl(response.uri)
            } else {
                download(response)
            }
        }

        override fun onCloseRequest(session: GeckoSession) {
            closeTab(tabId)
        }

        override fun onContextMenu(
            session: GeckoSession,
            screenX: Int,
            screenY: Int,
            element: GeckoSession.ContentDelegate.ContextElement,
        ) {
            showContextMenu(
                tabId = tabId,
                x = screenX,
                y = screenY,
                linkUri = element.linkUri,
                linkText = element.textContent,
                textContent = element.textContent,
                resourceUri = element.srcUri,
                resourceType = element.type,
            )
        }

        override fun onCrash(session: GeckoSession) {
            markTabCrashed("This site crashed. Reload to recover it.")
        }

        override fun onKill(session: GeckoSession) {
            markTabCrashed("This site was stopped. Reload to recover it.")
        }

        private fun markTabCrashed(message: String) {
            val url = _state.value.tabs.firstOrNull { it.id == tabId }?.url.orEmpty()
            updateTab(tabId) { it.copy(crashed = true, isLoading = false, progress = 0) }
            recordGeckoCrash(tabId, url, message)
            showSnackbar(message)
        }
    }

    private fun recordGeckoCrash(tabId: String, url: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val report = buildString {
                    appendLine("Gecko content process failure")
                    appendLine("Tab: $tabId")
                    appendLine("URL: $url")
                    appendLine("Reason: $message")
                    appendLine()
                    appendLine(Log.getStackTraceString(IllegalStateException(message)))
                }
                File(getApplication<Application>().filesDir, "last-crash.txt").writeText(report)
            }
        }
    }

    private inner class PermissionDelegate(private val tabId: String) : GeckoSession.PermissionDelegate {
        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<String>?,
            callback: GeckoSession.PermissionDelegate.Callback,
        ) {
            val requested = permissions.orEmpty().toList()
            val supported = setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            if (requested.any { it !in supported }) {
                callback.reject()
            } else if (requested.isEmpty()) {
                callback.grant()
            } else _state.update {
                it.copy(
                    androidPermission = AndroidPermissionPrompt(
                        id = UUID.randomUUID().toString(),
                        permissions = requested,
                        callback = callback,
                    ),
                )
            }
        }

        override fun onContentPermissionRequest(
            session: GeckoSession,
            permission: GeckoSession.PermissionDelegate.ContentPermission,
        ): GeckoResult<Int> {
            val automaticDecision = when (permission.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE,
                -> GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                GeckoSession.PermissionDelegate.PERMISSION_TRACKING,
                GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                -> GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION,
                GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION,
                -> null
                else -> GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
            }
            if (automaticDecision != null) return GeckoResult.fromValue(automaticDecision)

            val result = GeckoResult<Int>()
            _state.update {
                it.copy(
                    contentPermission = ContentPermissionPrompt(
                        id = UUID.randomUUID().toString(),
                        origin = permission.uri,
                        label = permissionLabel(permission.permission),
                        result = result,
                    ),
                )
            }
            return result
        }

        override fun onMediaPermissionRequest(
            session: GeckoSession,
            uri: String,
            video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback,
        ) {
            val videoSources = video.orEmpty()
            val audioSources = audio.orEmpty()
            if (videoSources.isEmpty() && audioSources.isEmpty()) {
                callback.reject()
            } else {
                _state.update {
                    it.copy(
                        mediaPermission = MediaPermissionPrompt(
                            id = UUID.randomUUID().toString(),
                            origin = uri,
                            hasVideo = videoSources.isNotEmpty(),
                            hasAudio = audioSources.isNotEmpty(),
                            callback = callback,
                            video = videoSources.map { it }.toTypedArray(),
                            audio = audioSources.map { it }.toTypedArray(),
                        ),
                    )
                }
            }
        }
    }

    private fun permissionLabel(permission: Int): String = when (permission) {
        GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "your location"
        GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "notifications"
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> "autoplay media"
        else -> "additional access"
    }

    override fun onCleared() {
        downloadEngine.shutdown()
        _state.value.extensionPopup?.session?.close()
        _state.value.tabs.forEach { it.session.close() }
        super.onCleared()
    }
}
