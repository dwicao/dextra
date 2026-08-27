package com.dwicao.dextra.browser

import android.Manifest
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
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
)

enum class BrowserOverlay {
    NONE,
    TABS,
    LIBRARY,
    SETTINGS,
    DOWNLOADS,
}

enum class ContextMenuAction {
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
    val linkUri: String?,
    val linkText: String?,
    val textContent: String?,
    val resourceUri: String?,
    val resourceType: Int,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
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
    val installedExtensions: List<InstalledExtension> = emptyList(),
    val extensionInstallPrompt: ExtensionInstallPrompt? = null,
    val extensionUpdatePrompt: ExtensionUpdatePrompt? = null,
    val extensionInstallInProgress: Boolean = false,
    val contextMenu: BrowserContextMenu? = null,
    val lastCrashReport: String? = null,
    val extensionActions: List<ExtensionToolbarAction> = emptyList(),
    val extensionPopup: ExtensionPopupState? = null,
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
    private val restoringExtensionIds = ConcurrentHashMap.newKeySet<String>()
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
        val id = UUID.randomUUID().toString()
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
        if (resolvedInitialUri != null && openSession) session.loadUri(resolvedInitialUri)
        return id
    }

    fun createPrivateTab() = createTab(privateMode = true)

    fun selectTab(id: String) {
        if (_state.value.tabs.any { it.id == id }) {
            _state.update { it.copy(activeTabId = id, overlay = BrowserOverlay.NONE) }
        }
    }

    fun closeTab(id: String) {
        val current = _state.value
        val closing = current.tabs.firstOrNull { it.id == id } ?: return
        if (current.tabs.size == 1) {
            val replacementId = UUID.randomUUID().toString()
            val replacementSession = createSession(replacementId, privateMode = false)
            val replacement = BrowserTabState(id = replacementId, session = replacementSession)
            _state.update { it.copy(tabs = listOf(replacement), activeTabId = replacementId, overlay = BrowserOverlay.NONE) }
            runCatching { closing.session.close() }
            return
        }

        val remaining = current.tabs.filterNot { it.id == id }
        val newActive = if (current.activeTabId == id) {
            remaining.getOrNull(current.tabs.indexOf(closing).coerceAtMost(remaining.lastIndex))?.id
        } else {
            current.activeTabId
        }
        _state.update { it.copy(tabs = remaining, activeTabId = newActive) }
        runCatching { closing.session.close() }
    }

    fun toggleTabPinned(id: String) {
        if (_state.value.tabs.none { it.id == id }) return
        _state.update {
            it.copy(tabs = it.tabs.map { tab -> if (tab.id == id) tab.copy(pinned = !tab.pinned) else tab })
        }
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

    fun handleContextMenuAction(action: ContextMenuAction) {
        val menu = _state.value.contextMenu ?: return
        when (action) {
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

    fun setTabBarWithAddressBar(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTabBarWithAddressBar(enabled) }
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
        viewModelScope.launch { dao.deleteDownload(download.downloadId) }
    }

    private fun createSession(tabId: String, privateMode: Boolean, openSession: Boolean = true): GeckoSession {
        val settings = _state.value.settings
        val session = GeckoSession(
            GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .allowJavascript(true)
                .userAgentMode(if (settings.desktopSites) {
                    GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                } else {
                    GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                })
                .viewportMode(if (settings.desktopSites) {
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
            runCatching {
                FileProvider.getUriForFile(
                    getApplication<Application>(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    File(filePath),
                ).toString()
            }.getOrNull()
        } else {
            current.localUri
        }
        dao.upsertDownload(
            current.copy(
                status = update.status,
                bytesDownloaded = update.bytesDownloaded ?: current.bytesDownloaded,
                totalBytes = update.totalBytes ?: current.totalBytes,
                speedBytesPerSecond = update.speedBytesPerSecond ?: current.speedBytesPerSecond,
                localUri = localUri,
                filePath = filePath,
                reason = update.reason,
            ),
        )
    }

    private fun installAdBlocker() {
        runtime.webExtensionController.list().accept(
            { extensions ->
                val staleExtensions = extensions.orEmpty().filter {
                    it.id == "adblock@dextra" && it.metaData.version != "2.5.0"
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
        val session = createSession("extension-popup-${extension.id}", privateMode = false)
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
            if (_state.value.tabs.size >= 32) {
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
            val labels = permissions.orEmpty().filter { it in setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) }
            if (labels.isEmpty()) callback.grant()
            else _state.update {
                it.copy(
                    androidPermission = AndroidPermissionPrompt(
                        id = UUID.randomUUID().toString(),
                        permissions = labels,
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
            val videoSource = video.orEmpty().firstOrNull()
            val audioSource = audio.orEmpty().firstOrNull()
            if (videoSource == null && audioSource == null) {
                callback.reject()
            } else {
                callback.grant(videoSource?.id, audioSource?.id)
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
