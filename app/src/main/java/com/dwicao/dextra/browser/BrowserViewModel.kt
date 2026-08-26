package com.dwicao.dextra.browser

import android.Manifest
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dwicao.dextra.GeckoRuntimeHolder
import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.BrowserDao
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.BrowserSettings
import com.dwicao.dextra.data.HistoryEntry
import com.dwicao.dextra.data.SearchEngine
import com.dwicao.dextra.data.SettingsRepository
import com.dwicao.dextra.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse
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
)

enum class BrowserOverlay {
    NONE,
    TABS,
    LIBRARY,
    SETTINGS,
}

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

data class BrowserUiState(
    val tabs: List<BrowserTabState> = emptyList(),
    val activeTabId: String? = null,
    val settings: BrowserSettings = BrowserSettings(),
    val overlay: BrowserOverlay = BrowserOverlay.NONE,
    val snackbar: String? = null,
    val contentPermission: ContentPermissionPrompt? = null,
    val androidPermission: AndroidPermissionPrompt? = null,
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = GeckoRuntimeHolder.get(application)
    private val database = BrowserDatabase.get(application)
    private val dao: BrowserDao = database.browserDao()
    private val settingsRepository = SettingsRepository(application)
    private val _state = MutableStateFlow(BrowserUiState())

    val state: StateFlow<BrowserUiState> = _state.asStateFlow()
    val history: Flow<List<HistoryEntry>> = dao.observeHistory()
    val bookmarks: Flow<List<Bookmark>> = dao.observeBookmarks()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current -> current.copy(settings = settings) }
                _state.value.tabs.forEach { tab ->
                    tab.session.settings.setUseTrackingProtection(settings.trackingProtection)
                }
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

    fun createTab(privateMode: Boolean = false, initialUri: String? = null): String {
        val id = UUID.randomUUID().toString()
        val session = createSession(id, privateMode)
        val tab = BrowserTabState(id = id, session = session, isPrivate = privateMode)
        _state.update { current ->
            current.copy(
                tabs = current.tabs + tab,
                activeTabId = id,
                overlay = BrowserOverlay.NONE,
            )
        }
        if (initialUri != null) navigate(id, initialUri)
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
        closing.session.close()
        if (current.tabs.size == 1) {
            _state.update { it.copy(tabs = emptyList(), activeTabId = null) }
            createTab()
            return
        }

        val remaining = current.tabs.filterNot { it.id == id }
        val newActive = if (current.activeTabId == id) {
            remaining.getOrNull(current.tabs.indexOf(closing).coerceAtMost(remaining.lastIndex))?.id
        } else {
            current.activeTabId
        }
        _state.update { it.copy(tabs = remaining, activeTabId = newActive) }
    }

    fun navigateActive(input: String) {
        activeTab()?.let { navigate(it.id, input) }
    }

    fun navigate(tabId: String, input: String) {
        val url = BrowserUrl.resolve(input, _state.value.settings.searchEngine)
        if (url.isEmpty()) return
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        updateTab(tabId) {
            it.copy(url = url, title = "Loading...", hasPage = true, isLoading = true, progress = 0)
        }
        tab.session.loadUri(url)
    }

    fun goBack(): Boolean {
        val tab = activeTab() ?: return false
        if (!tab.canGoBack) return false
        tab.session.goBack()
        return true
    }

    fun goForward() {
        activeTab()?.takeIf { it.canGoForward }?.session?.goForward()
    }

    fun reloadOrStop() {
        activeTab()?.let { tab -> if (tab.isLoading) tab.session.stop() else tab.session.reload() }
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

    fun setTrackingProtection(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrackingProtection(enabled) }
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

    private fun createSession(tabId: String, privateMode: Boolean): GeckoSession {
        val session = GeckoSession(
            GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .useTrackingProtection(_state.value.settings.trackingProtection)
                .allowJavascript(true)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .build(),
        )
        session.setNavigationDelegate(NavigationDelegate(tabId))
        session.setProgressDelegate(ProgressDelegate(tabId))
        session.setContentDelegate(ContentDelegate(tabId))
        session.setPermissionDelegate(PermissionDelegate(tabId))
        session.open(runtime)
        return session
    }

    private fun updateTab(tabId: String, transform: (BrowserTabState) -> BrowserTabState) {
        _state.update { current ->
            current.copy(tabs = current.tabs.map { tab -> if (tab.id == tabId) transform(tab) else tab })
        }
    }

    private fun showSnackbar(message: String) {
        _state.update { it.copy(snackbar = message) }
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
            val request = DownloadManager.Request(Uri.parse(response.uri))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    downloadName(response),
                )
            response.headers["Content-Type"]?.let { request.setMimeType(it) }
            getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE)
                ?.let { (it as DownloadManager).enqueue(request) }
            showSnackbar("Download started")
        }.onFailure { showSnackbar("Could not start download") }
    }

    private fun downloadName(response: WebResponse): String {
        val pathName = Uri.parse(response.uri).lastPathSegment?.takeIf { it.isNotBlank() }
        if (pathName != null) return pathName
        val mimeType = response.headers["Content-Type"]?.substringBefore(';')
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return "dextra-download${extension?.let { ".${it}" } ?: ""}"
    }

    private inner class NavigationDelegate(private val tabId: String) : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            permissions: List<GeckoSession.PermissionDelegate.ContentPermission>,
            isSameDocument: Boolean,
        ) {
            val resolvedUrl = url.orEmpty()
            updateTab(tabId) { it.copy(url = resolvedUrl, isSecure = resolvedUrl.startsWith("https://")) }
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
            if (scheme != null && scheme !in setOf("http", "https", "about", "file", "data")) {
                launchExternal(request.uri)
                return GeckoResult.deny()
            }
            return GeckoResult.allow()
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession> {
            val newTabId = createTab()
            val newSession = _state.value.tabs.first { it.id == newTabId }.session
            updateTab(newTabId) { it.copy(url = uri, hasPage = true, isLoading = true) }
            return GeckoResult.fromValue(newSession)
        }
    }

    private inner class ProgressDelegate(private val tabId: String) : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            updateTab(tabId) { it.copy(url = url, hasPage = true, isLoading = true, progress = 0) }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            updateTab(tabId) { it.copy(isLoading = false, progress = if (success) 100 else 0) }
            if (success) recordHistory(tabId)
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
            download(response)
        }

        override fun onCloseRequest(session: GeckoSession) {
            closeTab(tabId)
        }

        override fun onCrash(session: GeckoSession) {
            showSnackbar("This tab stopped responding")
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
        _state.value.tabs.forEach { it.session.close() }
        super.onCleared()
    }
}
