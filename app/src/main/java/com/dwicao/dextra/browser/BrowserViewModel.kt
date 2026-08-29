package com.dwicao.dextra.browser

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.ContentValues
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.os.SystemClock
import android.app.ActivityManager
import android.os.Debug
import android.text.Html
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.BackoffPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dwicao.dextra.GeckoRuntimeHolder
import com.dwicao.dextra.DextraApplication
import com.dwicao.dextra.PwaActivity
import com.dwicao.dextra.BrowserWindowActivity
import com.dwicao.dextra.data.AdBlockFilter
import com.dwicao.dextra.data.AddressVault
import com.dwicao.dextra.data.BackupRepository
import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.BrowserDao
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.BrowserSettings
import com.dwicao.dextra.data.CredentialVault
import com.dwicao.dextra.data.CustomSearchEngine
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import com.dwicao.dextra.data.DnsProvider
import com.dwicao.dextra.data.ExtensionInstallRecord
import com.dwicao.dextra.data.HistoryEntry
import com.dwicao.dextra.data.InstalledWebApp
import com.dwicao.dextra.data.ReadingListEntry
import com.dwicao.dextra.data.SearchEngine
import com.dwicao.dextra.data.SavedTab
import com.dwicao.dextra.data.SavedTabGroup
import com.dwicao.dextra.data.SessionSnapshot
import com.dwicao.dextra.data.StartPageLink
import com.dwicao.dextra.data.SiteSetting
import com.dwicao.dextra.data.StoredCredential
import com.dwicao.dextra.data.StoredAddress
import com.dwicao.dextra.data.SitePermission
import com.dwicao.dextra.data.TabWorkspace
import com.dwicao.dextra.data.DEFAULT_WORKSPACE_ID
import com.dwicao.dextra.data.SettingsRepository
import com.dwicao.dextra.data.SyncRepository
import com.dwicao.dextra.data.SyncPreview
import com.dwicao.dextra.data.SyncSelection
import com.dwicao.dextra.data.WebDavConfig
import com.dwicao.dextra.data.WebDavSettingsState
import com.dwicao.dextra.data.WebDavSettingsStore
import com.dwicao.dextra.data.ThemeMode
import com.dwicao.dextra.data.StoredWebPushSubscription
import com.dwicao.dextra.data.WebPushStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.MediaSession
import org.mozilla.geckoview.PageExtractionController
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.geckoview.WebResponse
import org.mozilla.geckoview.WebPushDelegate
import org.mozilla.geckoview.WebPushSubscription
import org.mozilla.geckoview.TranslationsController
import android.print.PrintAttributes
import android.print.PrintManager
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import java.net.URL
import java.net.URI
import java.io.File
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.interfaces.ECPublicKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.DateFormat
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
    val groupId: String? = null,
    val isSleeping: Boolean = false,
    val isFullScreen: Boolean = false,
    val hasActiveMedia: Boolean = false,
    val isMediaPlaying: Boolean = false,
    val isAudioMuted: Boolean = false,
    val sessionState: String? = null,
)

enum class BrowserOverlay {
    NONE,
    TABS,
    LIBRARY,
    BOOKMARKS,
    HISTORY,
    SETTINGS,
    DOWNLOADS,
    KEYBOARD_SHORTCUTS,
    PRIVACY,
    SECURITY,
    WORKSPACES,
    PERFORMANCE,
    NETWORK,
    MEDIA,
    COMPATIBILITY,
}

enum class ContextMenuAction {
    NEW_TAB,
    NEW_PRIVATE_TAB,
    DUPLICATE_TAB,
    RELOAD_TAB,
    TOGGLE_TAB_PINNED,
    TOGGLE_TAB_SLEEPING,
    TOGGLE_TAB_AUDIO,
    CLOSE_TAB,
    CLOSE_OTHER_TABS,
    CLOSE_TABS_TO_LEFT,
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
    OPEN_IN_SPLIT,
    OPEN_IN_NEW_WINDOW,
    CLOSE_SPLIT,
    SWAP_SPLIT,
    DISMISS,
}

data class BrowserContextMenu(
    val tabId: String,
    val x: Int,
    val y: Int,
    val pageUrl: String = "",
    val isBookmarked: Boolean = false,
    val linkUri: String? = null,
    val linkText: String? = null,
    val textContent: String? = null,
    val resourceUri: String? = null,
    val resourceType: Int = GeckoSession.ContentDelegate.ContextElement.TYPE_NONE,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isTab: Boolean = false,
    val isPinned: Boolean = false,
    val isSleeping: Boolean = false,
    val hasActiveMedia: Boolean = false,
    val isAudioMuted: Boolean = false,
)

data class ContentPermissionPrompt(
    val id: String,
    val tabId: String,
    val origin: String,
    val label: String,
    val permission: Int,
    val result: GeckoResult<Int>,
)

data class AndroidPermissionPrompt(
    val id: String,
    val tabId: String,
    val permissions: List<String>,
    val callback: GeckoSession.PermissionDelegate.Callback,
)

data class MediaPermissionPrompt(
    val id: String,
    val tabId: String,
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
    val requiredPermissions: List<String> = emptyList(),
    val requiredOrigins: List<String> = emptyList(),
    val requiredDataCollectionPermissions: List<String> = emptyList(),
    val optionalPermissions: List<String> = emptyList(),
    val grantedOptionalPermissions: List<String> = emptyList(),
    val optionalOrigins: List<String> = emptyList(),
    val grantedOptionalOrigins: List<String> = emptyList(),
    val optionalDataCollectionPermissions: List<String> = emptyList(),
    val grantedOptionalDataCollectionPermissions: List<String> = emptyList(),
)

data class ExtensionToolbarAction(
    val extensionId: String,
    val title: String,
    val enabled: Boolean,
    val badgeText: String?,
    val icon: Bitmap?,
)

data class WebAppManifestInfo(
    val tabId: String,
    val name: String,
    val startUrl: String,
    val scope: String,
    val iconUrl: String? = null,
)

data class WebPushPrompt(
    val id: String,
    val origin: String,
    val scope: String,
    val appServerKey: ByteArray?,
    val result: GeckoResult<WebPushSubscription>,
)

private data class CreatedWebPushSubscription(
    val record: StoredWebPushSubscription,
    val subscription: WebPushSubscription,
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

data class CommandPaletteState(
    val query: String = "",
    val selectedIndex: Int = 0,
)

data class TabSwitcherState(
    val query: String = "",
    val selectedIndex: Int = 0,
)

data class PageTranslationState(
    val tabId: String,
    val sourceLanguage: String? = null,
    val targetLanguage: String = "en",
    val detectedLanguage: String? = null,
    val isTranslating: Boolean = false,
    val isTranslated: Boolean = false,
    val error: String? = null,
)

data class WebAuthnPromptState(
    val tabId: String,
    val origin: String,
    val rpId: String,
    val isCreate: Boolean,
    val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    val allow: () -> GeckoSession.PromptDelegate.PromptResponse,
    val dismiss: () -> GeckoSession.PromptDelegate.PromptResponse,
)

data class BlockerStats(
    val totalBlocked: Int = 0,
    val byOrigin: Map<String, Int> = emptyMap(),
)

data class NetworkActivity(
    val id: String,
    val tabId: String,
    val url: String,
    val kind: String,
    val status: String,
    val secure: Boolean,
    val timestamp: Long,
)

data class CompatibilityEvent(
    val id: String,
    val tabId: String,
    val severity: String,
    val message: String,
    val timestamp: Long,
)

data class SecurityCertificateInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val sha256: String,
)

data class SecurityDiagnostics(
    val tabId: String,
    val url: String,
    val origin: String?,
    val host: String?,
    val port: Int?,
    val isSecure: Boolean,
    val isLoading: Boolean = false,
    val certificate: SecurityCertificateInfo? = null,
    val permissions: List<SitePermission> = emptyList(),
    val siteSetting: SiteSetting? = null,
    val blockedRequests: Int = 0,
    val dnsOverHttpsEnabled: Boolean = false,
    val dnsProvider: String = "",
    val error: String? = null,
)

data class SyncPreviewState(val preview: SyncPreview)

data class PerformanceMetrics(
    val startupMs: Long? = null,
    val processPssMb: Int = 0,
    val availableMemoryMb: Int = 0,
    val memoryLow: Boolean = false,
    val frameCount: Int = 0,
    val jankCount: Int = 0,
    val averageFrameTimeMs: Float = 0f,
    val windowWidthDp: Int = 0,
    val windowHeightDp: Int = 0,
    val isDexLikeWindow: Boolean = false,
    val lastUpdatedAt: Long? = null,
)

data class OfflineArticle(
    val title: String,
    val content: String,
)

private data class ClosedTabEntry(
    val url: String,
    val title: String,
    val isPrivate: Boolean,
    val pinned: Boolean,
    val groupId: String?,
    val sessionState: String?,
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
private const val MAX_WORKSPACES = 12
private const val MAX_NETWORK_ACTIVITY = 200
private const val MAX_COMPATIBILITY_EVENTS = 200
private const val TAB_TRANSFER_FORMAT = "dextra-tab-transfer"
private const val TAB_TRANSFER_PREFIX = "dextra-tab-transfer:"
private const val MAX_SHARED_TABS = 32
private const val DOWNLOAD_CHANNEL_ID = "dextra_downloads"
private const val MAX_BOOKMARK_IMPORT_BYTES = 5 * 1024 * 1024
private const val PROGRESS_UPDATE_INTERVAL_MS = 100L
private const val MAX_SESSION_STATE_BYTES = 256 * 1024
private const val MAX_OFFLINE_ARTICLE_BYTES = 2 * 1024 * 1024
private const val MAX_READER_ARTICLE_BYTES = 2 * 1024 * 1024
private const val MAX_HTML_EXPORT_BYTES = 5 * 1024 * 1024
private const val MAX_PWA_ICON_BYTES = 512 * 1024
private const val MAX_PWA_MANIFEST_BYTES = 512 * 1024
private const val WEB_PUSH_ENDPOINT_BASE = "https://updates.push.services.mozilla.com/wpush/v2/"

private fun WebDavConfig.toUiState(): WebDavSettingsState = WebDavSettingsState(
    configured = true,
    endpoint = endpoint,
    username = username,
    remoteFile = remoteFile,
    intervalHours = intervalHours,
    enabled = enabled,
    lastSyncAt = lastSyncAt,
    conflictPending = conflictPending,
    conflictDetectedAt = conflictDetectedAt,
    lastError = lastError,
)

data class ExtensionUpdatePrompt(
    val id: String,
    val name: String,
    val permissions: List<String>,
    val origins: List<String>,
    val dataCollectionPermissions: List<String>,
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
    val commandPalette: CommandPaletteState? = null,
    val tabSwitcher: TabSwitcherState? = null,
    val capturingShortcut: BrowserCommandId? = null,
    val siteSetting: SiteSetting? = null,
    val siteSettingsOpen: Boolean = false,
    val qrCodeUrl: String? = null,
    val webAppManifest: WebAppManifestInfo? = null,
    val isPictureInPictureMode: Boolean = false,
    val offlineArticle: OfflineArticle? = null,
    val readerMode: ReaderModeState? = null,
    val translation: PageTranslationState? = null,
    val addressFocusRequest: Long = 0,
    val splitPrimaryTabId: String? = null,
    val splitSecondaryTabId: String? = null,
    val splitPaneFocused: Boolean = false,
    val blockerStats: BlockerStats = BlockerStats(),
    val credentials: List<StoredCredential> = emptyList(),
    val addresses: List<StoredAddress> = emptyList(),
    val standalonePwa: Boolean = false,
    val standaloneWindow: Boolean = false,
    val webPushPrompt: WebPushPrompt? = null,
    val webPushSubscriptions: List<StoredWebPushSubscription> = emptyList(),
    val credentialCount: Int = 0,
    val credentialVaultUnlocked: Boolean = false,
    val credentialUnlockRequest: Long = 0,
    val closedTabCount: Int = 0,
    val webAuthnPrompt: WebAuthnPromptState? = null,
    val webDav: WebDavSettingsState = WebDavSettingsState(),
    val securityDiagnostics: SecurityDiagnostics? = null,
    val syncPreview: SyncPreviewState? = null,
    val syncPreviewLoading: Boolean = false,
    val cookieBannerDetected: Boolean = false,
    val cookieBannerHandled: Boolean = false,
    val performance: PerformanceMetrics = PerformanceMetrics(),
    val networkActivity: List<NetworkActivity> = emptyList(),
    val compatibilityEvents: List<CompatibilityEvent> = emptyList(),
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = GeckoRuntimeHolder.get(application)
    private val database = BrowserDatabase.get(application)
    private val dao: BrowserDao = database.browserDao()
    private val settingsRepository = SettingsRepository(application)
    private val backupRepository = BackupRepository(application, dao)
    private val syncRepository = SyncRepository(application, dao)
    private val webDavStore = WebDavSettingsStore(application)
    private val credentialVault = CredentialVault(application)
    private val addressVault = AddressVault(application)
    private val webPushStore = WebPushStore(application)
    private val performanceMonitor = PerformanceMonitor()
    private val siteHttpsOnlyByOrigin = mutableMapOf<String, Boolean>()
    private val _state = MutableStateFlow(BrowserUiState())
    private val installedExtensionObjects = mutableMapOf<String, WebExtension>()
    private val extensionActionObjects = mutableMapOf<String, WebExtension.Action>()
    private val recentlyClosedTabs = ArrayDeque<ClosedTabEntry>()
    private val mediaSessions = mutableMapOf<String, MediaSession>()
    private val translationDetectedByTab = mutableMapOf<String, String>()
    private val queuedContentPermissions = ArrayDeque<ContentPermissionPrompt>()
    private val queuedAndroidPermissions = ArrayDeque<AndroidPermissionPrompt>()
    private val queuedMediaPermissions = ArrayDeque<MediaPermissionPrompt>()
    private val queuedWebPushPrompts = ArrayDeque<WebPushPrompt>()
    private val pageZoomByTab = mutableMapOf<String, Int>()
    private val lastProgressUpdateAt = mutableMapOf<String, Long>()
    private val trackerBlockedByTab = mutableMapOf<String, Int>()
    private val restoringExtensionIds = ConcurrentHashMap.newKeySet<String>()
    private var restoredSavedTabs = false
    private var restoredClosedTabs = false
    private var firstFrameRecorded = false
    private var appInForeground = false
    private var privacyExitCleanupJob: Job? = null
    private var persistJob: Job? = null
    private var pendingIncomingUri: String? = null
    private var pendingTabTransfer: String? = null
    private var pendingPwaUri: String? = null
    private var pendingWindowUri: String? = null
    private var pendingWindowPrivate = false
    private var pendingMediaTabId: String? = null
    private var pendingSyncImport: Pair<Uri, String>? = null
    private var syncPreviewRequestId = 0L
    @Volatile
    private var pendingExtensionPackagePath: String? = null
    @Volatile
    private var pendingExtensionPrivateBrowsing = false
    @Volatile
    private var pendingExtensionDataCollection = false
    @Volatile
    private var adBlockPort: WebExtension.Port? = null
    @Volatile
    private var adBlockExtension: WebExtension? = null
    private var adBlockStats = BlockerStats()

    private val webPushDelegate = object : WebPushDelegate {
        override fun onSubscribe(scope: String, appServerKey: ByteArray?): GeckoResult<WebPushSubscription> {
            val existing = webPushStore.get(scope)
            if (existing != null) return GeckoResult.fromValue(webPushStore.run { existing.toGeckoSubscription() })
            val origin = NavigationPolicy.origin(scope) ?: return GeckoResult.fromValue<WebPushSubscription>(null)
            if (isPrivateCredentialOrigin(origin)) return GeckoResult.fromValue<WebPushSubscription>(null)
            val result = GeckoResult<WebPushSubscription>()
            val prompt = WebPushPrompt(
                id = UUID.randomUUID().toString(),
                origin = origin,
                scope = scope,
                appServerKey = appServerKey,
                result = result,
            )
            if (_state.value.webPushPrompt == null) {
                _state.update { it.copy(webPushPrompt = prompt) }
            } else {
                queuedWebPushPrompts.addLast(prompt)
            }
            return result
        }

        override fun onGetSubscription(scope: String): GeckoResult<WebPushSubscription> {
            val subscription = webPushStore.get(scope)
            return if (subscription == null) {
                GeckoResult.fromValue<WebPushSubscription>(null)
            } else {
                GeckoResult.fromValue(webPushStore.run { subscription.toGeckoSubscription() })
            }
        }

        override fun onUnsubscribe(scope: String): GeckoResult<Void> {
            val result = GeckoResult<Void>()
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { webPushStore.delete(scope) }
                    .onSuccess { result.complete(null) }
                    .onFailure { result.completeExceptionally(it) }
            }
            return result
        }
    }

    private val autocompleteStorageDelegate = object : org.mozilla.geckoview.Autocomplete.StorageDelegate {
        override fun onLoginFetch(origin: String): GeckoResult<Array<org.mozilla.geckoview.Autocomplete.LoginEntry>> =
            GeckoResult.fromValue(loginEntriesFor(origin))

        override fun onLoginFetch(): GeckoResult<Array<org.mozilla.geckoview.Autocomplete.LoginEntry>> =
            GeckoResult.fromValue(loginEntriesFor(null))

        override fun onLoginSave(login: org.mozilla.geckoview.Autocomplete.LoginEntry) {
            if (login.origin.isBlank() || isPrivateCredentialOrigin(login.origin)) return
            val credential = StoredCredential(
                id = login.guid ?: UUID.randomUUID().toString(),
                origin = login.origin,
                username = login.username,
                password = login.password,
                formActionOrigin = login.formActionOrigin,
                httpRealm = login.httpRealm,
                updatedAt = System.currentTimeMillis(),
            )
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { credentialVault.save(credential) }
                    .onFailure { Log.e("Dextra", "Could not save browser credential", it) }
            }
        }

        override fun onLoginUsed(login: org.mozilla.geckoview.Autocomplete.LoginEntry, dismissed: Int) {
            if (isPrivateCredentialOrigin(login.origin)) return
            val existing = _state.value.credentials.firstOrNull {
                it.origin == login.origin && it.username == login.username
            } ?: return
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    credentialVault.save(existing.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }

        override fun onAddressFetch(): GeckoResult<Array<org.mozilla.geckoview.Autocomplete.Address>> =
            GeckoResult.fromValue(
                if (activeTab()?.isPrivate == true) emptyArray()
                else _state.value.addresses.mapNotNull(::toGeckoAddress).toTypedArray(),
            )

        override fun onAddressSave(address: org.mozilla.geckoview.Autocomplete.Address) {
            if (activeTab()?.isPrivate == true || address.name.isNullOrBlank()) return
            val stored = StoredAddress(
                id = address.guid?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
                name = address.name,
                givenName = address.givenName.orEmpty(),
                additionalName = address.additionalName.orEmpty(),
                familyName = address.familyName.orEmpty(),
                organization = address.organization.orEmpty(),
                streetAddress = address.streetAddress.orEmpty(),
                addressLevel1 = address.addressLevel1.orEmpty(),
                addressLevel2 = address.addressLevel2.orEmpty(),
                addressLevel3 = address.addressLevel3.orEmpty(),
                postalCode = address.postalCode.orEmpty(),
                country = address.country.orEmpty(),
                tel = address.tel.orEmpty(),
                email = address.email.orEmpty(),
                updatedAt = System.currentTimeMillis(),
            )
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { addressVault.save(stored) }
                    .onFailure { Log.e("Dextra", "Could not save address autofill data", it) }
            }
        }
    }

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

        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: WebExtension.MessageSender,
        ): GeckoResult<Any>? {
            val json = message as? JSONObject ?: return null
            if (json.optString("type") != "blockedStats") return null
            val byOrigin = linkedMapOf<String, Int>()
            json.optJSONObject("byOrigin")?.let { values ->
                values.keys().forEach { origin ->
                    val count = values.optInt(origin, 0)
                    if (count > 0 && NavigationPolicy.isWebUrl(origin)) byOrigin[origin] = count
                }
            }
            adBlockStats = BlockerStats(
                totalBlocked = json.optInt("totalBlocked", 0).coerceAtLeast(0),
                byOrigin = byOrigin,
            )
            rebuildBlockerStats()
            return null
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
            }
            return null
        }
    }

    private val extensionPromptDelegate = object : WebExtensionController.PromptDelegate {
        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>,
        ): GeckoResult<WebExtension.PermissionPromptResponse> =
            createExtensionInstallPrompt(extension, permissions, origins, dataCollectionPermissions)

        override fun onUpdatePrompt(
            extension: WebExtension,
            newPermissions: Array<String>,
            newOrigins: Array<String>,
            newDataCollectionPermissions: Array<String>,
        ): GeckoResult<AllowOrDeny> = createExtensionUpdatePrompt(
            extension,
            newPermissions,
            newOrigins,
            newDataCollectionPermissions,
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
    val readingList: Flow<List<ReadingListEntry>> = dao.observeReadingList()
    val sitePermissions: Flow<List<SitePermission>> = dao.observeSitePermissions()
    val siteSettings: Flow<List<SiteSetting>> = dao.observeSiteSettings()
    val installedWebApps: Flow<List<InstalledWebApp>> = dao.observeInstalledWebApps()

    init {
        performanceMonitor.start()
        PrivacyCleanupScheduler.schedule(application)
        webDavStore.load()?.let { config ->
            _state.update { it.copy(webDav = config.toUiState()) }
            if (config.enabled) WebDavSyncScheduler.schedule(application, config.intervalHours)
        }
        readLastCrashReport()?.let { report ->
            _state.update { it.copy(lastCrashReport = report) }
        }
        viewModelScope.launch {
            credentialVault.load()
            credentialVault.credentials.collect { credentials ->
                _state.update {
                    it.copy(
                        credentials = if (credentialVault.unlocked.value) credentials else emptyList(),
                        credentialCount = credentials.size,
                    )
                }
            }
        }
        viewModelScope.launch {
            credentialVault.unlocked.collect { unlocked ->
                _state.update { state ->
                    state.copy(
                        credentialVaultUnlocked = unlocked,
                        credentials = if (unlocked) credentialVault.credentials.value else emptyList(),
                    )
                }
            }
        }
        viewModelScope.launch {
            addressVault.load()
            addressVault.addresses.collect { addresses ->
                _state.update { it.copy(addresses = addresses) }
            }
        }
        viewModelScope.launch {
            webPushStore.load()
            webPushStore.subscriptions.collect { subscriptions ->
                _state.update { it.copy(webPushSubscriptions = subscriptions) }
            }
        }
        runtime.setAutocompleteStorageDelegate(autocompleteStorageDelegate)
        runtime.webPushController.setDelegate(webPushDelegate)
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val previousSettings = _state.value.settings
                val desktopSitesChanged = _state.value.settings.desktopSites != settings.desktopSites
                val adBlockingChanged = _state.value.settings.adBlockingEnabled != settings.adBlockingEnabled ||
                    _state.value.settings.adBlockFilters != settings.adBlockFilters
                val userScriptsChanged = _state.value.settings.userScriptUrls != settings.userScriptUrls ||
                    _state.value.settings.disabledUserScriptUrls != settings.disabledUserScriptUrls
                val syncedSessionChanged = restoredSavedTabs && (
                    settings.openTabs != savedTabsFromState() ||
                        settings.tabGroups != previousSettings.tabGroups ||
                        settings.activeWorkspaceId != previousSettings.activeWorkspaceId
                    )
                _state.update { current -> current.copy(settings = settings) }
                applyDnsOverHttps(settings.dnsOverHttpsEnabled, settings.dnsProvider)
                applyContentColorScheme(settings.themeMode)
                applyCookieBannerMode(settings.cookieBannerMode)
                if (!restoredSavedTabs) {
                    restoredSavedTabs = true
                    restoreRecentlyClosedTabs(settings.recentlyClosedTabs)
                    restoreSavedTabs(settings.openTabs, settings.activeTabIndex)
                    if (_state.value.tabs.isEmpty()) createTab()
                    pendingMediaTabId?.let { tabId ->
                        pendingMediaTabId = null
                        selectTab(tabId)
                    }
                    pendingIncomingUri?.let { uri ->
                        pendingIncomingUri = null
                        openIncomingUri(uri)
                    }
                    pendingTabTransfer?.let { transfer ->
                        pendingTabTransfer = null
                        receiveTabTransfer(transfer)
                    }
                    pendingPwaUri?.let { uri ->
                        pendingPwaUri = null
                        openPwaUri(uri)
                    }
                    pendingWindowUri?.let { uri ->
                        pendingWindowUri = null
                        openWindowUri(uri, pendingWindowPrivate)
                        pendingWindowPrivate = false
                    }
                } else if (syncedSessionChanged) {
                    restoreSyncedSession(settings)
                }
                _state.value.tabs.forEach { tab ->
                    applyDesktopSiteSetting(tab.session, settings.desktopSites)
                    if (desktopSitesChanged && tab.hasPage) tab.session.reload()
                    if (adBlockingChanged && tab.hasPage) tab.session.reload()
                    if (userScriptsChanged && tab.hasPage) tab.session.reload()
                }
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
        viewModelScope.launch {
            dao.observeSiteSettings().collect { settings ->
                siteHttpsOnlyByOrigin.clear()
                settings.filter { it.httpsOnly != null }.forEach { setting ->
                    siteHttpsOnlyByOrigin[sitePolicyKey(setting.profileId, setting.origin)] = setting.httpsOnly == true
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
    }

    fun handleIncomingIntent(intent: Intent?) {
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (intent?.action == Intent.ACTION_SEND && !sharedText.isNullOrBlank()) {
            if (sharedText.startsWith(TAB_TRANSFER_PREFIX)) {
                if (!restoredSavedTabs) pendingTabTransfer = sharedText else receiveTabTransfer(sharedText)
                return
            }
            if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                openIncomingUri(sharedText)
            }
            return
        }
        val uri = intent?.dataString ?: return
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) return
        if (!restoredSavedTabs) {
            pendingIncomingUri = uri
            return
        }
        openIncomingUri(uri)
    }

    fun handleMediaTabIntent(tabId: String?) {
        if (tabId.isNullOrBlank()) return
        if (!restoredSavedTabs) pendingMediaTabId = tabId else selectTab(tabId)
    }

    fun handleDroppedData(data: android.content.ClipData?) {
        val item = data?.getItemAt(0) ?: return
        val candidate = item.uri?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: item.text?.toString()
        if (candidate.isNullOrBlank()) return
        if (candidate.startsWith(TAB_TRANSFER_PREFIX) && !restoredSavedTabs) {
            pendingTabTransfer = candidate
        } else if (!receiveTabTransfer(candidate)) {
            openIncomingUri(candidate)
        }
    }

    fun shareCurrentWorkspaceTabs() {
        val tabs = savedTabsFromState().take(MAX_SHARED_TABS)
        if (tabs.isEmpty()) {
            showSnackbar("There are no normal web tabs to share")
            return
        }
        val payload = JSONObject().apply {
            put("format", TAB_TRANSFER_FORMAT)
            put("version", 1)
            put("tabs", JSONArray(tabs.map { tab ->
                JSONObject().put("url", tab.url).put("title", tab.title)
            }))
        }
        runCatching {
            getApplication<Application>().startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_TEXT, "$TAB_TRANSFER_PREFIX${payload}")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    "Share open tabs",
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { showSnackbar("Could not share open tabs") }
    }

    private fun receiveTabTransfer(text: String): Boolean {
        if (!text.startsWith(TAB_TRANSFER_PREFIX)) return false
        val tabs = runCatching {
            val root = JSONObject(text.removePrefix(TAB_TRANSFER_PREFIX))
            require(root.optString("format") == TAB_TRANSFER_FORMAT && root.optInt("version") == 1)
            val array = root.optJSONArray("tabs") ?: return@runCatching emptyList<SavedTab>()
            (0 until array.length().coerceAtMost(MAX_SHARED_TABS)).mapNotNull { index ->
                val value = array.optJSONObject(index) ?: return@mapNotNull null
                val url = value.optString("url").takeIf(NavigationPolicy::isWebUrl) ?: return@mapNotNull null
                SavedTab(url = url, title = value.optString("title").takeIf(String::isNotBlank))
            }
        }.getOrNull() ?: return true
        if (tabs.isEmpty()) {
            showSnackbar("The shared tab bundle has no valid web tabs")
            return true
        }
        tabs.forEachIndexed { index, tab ->
            val active = activeTab()
            if (index == 0 && active != null && !active.hasPage) {
                navigate(active.id, tab.url)
            } else {
                createTab(initialUri = tab.url)
            }
        }
        showSnackbar("Received ${tabs.size} shared tab${if (tabs.size == 1) "" else "s"}")
        return true
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

    private fun searchUrl(settings: BrowserSettings): String =
        if (settings.searchEngine == SearchEngine.CUSTOM) {
            settings.customSearchEngines
                .firstOrNull { it.id == settings.selectedCustomSearchEngineId }
                ?.searchUrl
                ?: SearchEngine.GOOGLE.searchUrl
        } else {
            settings.searchEngine.searchUrl
        }

    fun createTab(
        privateMode: Boolean = false,
        initialUri: String? = null,
        openSession: Boolean = true,
        savedSessionState: String? = null,
    ): String {
        if (_state.value.tabs.size >= MAX_OPEN_TABS) {
            showSnackbar("Tab limit reached ($MAX_OPEN_TABS)")
            return ""
        }
        val id = UUID.randomUUID().toString()
        val previousActiveTabId = _state.value.activeTabId
        val session = createSession(id, privateMode, openSession, savedSessionState = savedSessionState)
        val resolvedInitialUri = initialUri
            ?.let { BrowserUrl.resolve(it, searchUrl(_state.value.settings)) }
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
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
            )
        }
        updateExtensionActiveTab(previousActiveTabId, id)
        updateSessionActivity(id)
        persistOpenTabs()
        val canRestoreState = savedSessionState?.let {
            runCatching { GeckoSession.SessionState.fromString(it) }.getOrNull() != null
        } == true
        if (resolvedInitialUri != null && openSession && !canRestoreState) session.loadUri(resolvedInitialUri)
        return id
    }

    fun createPrivateTab() = createTab(privateMode = true)

    fun createSessionSnapshot(title: String) {
        if (!restoredSavedTabs) return
        val snapshotTabs = savedTabsFromState()
        if (snapshotTabs.isEmpty()) {
            showSnackbar("There are no normal web tabs to save")
            return
        }
        val activeIndex = snapshotTabs.indexOfFirst { it.id == _state.value.activeTabId }
        val snapshot = SessionSnapshot(
            id = UUID.randomUUID().toString(),
            title = title.trim().take(60).ifBlank { "Saved session" },
            createdAt = System.currentTimeMillis(),
            tabs = snapshotTabs,
            activeTabIndex = activeIndex.coerceAtLeast(0),
            tabGroups = _state.value.settings.tabGroups,
        )
        viewModelScope.launch {
            settingsRepository.saveSessionSnapshot(snapshot)
            showSnackbar("Session snapshot saved")
        }
    }

    fun restoreSessionSnapshot(snapshot: SessionSnapshot) {
        val tabs = snapshot.tabs.filter { !it.isPrivate && NavigationPolicy.isWebUrl(it.url) }
        if (tabs.isEmpty()) {
            showSnackbar("This session has no restorable tabs")
            return
        }
        val current = _state.value
        current.tabs.forEach { tab -> runCatching { tab.session.close() } }
        _state.update {
            it.copy(
                settings = it.settings.copy(tabGroups = snapshot.tabGroups),
                tabs = emptyList(),
                activeTabId = null,
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
            )
        }
        restoreSavedTabs(tabs, snapshot.activeTabIndex)
        persistOpenTabs(immediate = true)
        showSnackbar("Restored ${snapshot.title}")
    }

    fun deleteSessionSnapshot(snapshot: SessionSnapshot) {
        viewModelScope.launch { settingsRepository.deleteSessionSnapshot(snapshot.id) }
    }

    fun deleteSessionTimeline(snapshot: SessionSnapshot) {
        viewModelScope.launch { settingsRepository.deleteSessionTimeline(snapshot.id) }
    }

    fun selectTab(id: String) {
        val current = _state.value
        if (current.tabs.any { it.id == id }) {
            if (current.activeTabId == id) {
                _state.update { it.copy(overlay = BrowserOverlay.NONE) }
                return
            }
            closeExtensionPopup()
            closeFindInPage()
            closeReaderMode()
            val previousActiveTabId = current.activeTabId
            val splitSelection = id == current.splitPrimaryTabId || id == current.splitSecondaryTabId
            _state.update {
                it.copy(
                    activeTabId = id,
                    overlay = BrowserOverlay.NONE,
                    siteSettingsOpen = false,
                    securityDiagnostics = null,
                    splitPrimaryTabId = if (splitSelection) it.splitPrimaryTabId else null,
                    splitSecondaryTabId = if (splitSelection) it.splitSecondaryTabId else null,
                    splitPaneFocused = splitSelection && id == it.splitSecondaryTabId,
                    tabs = it.tabs.map { tab ->
                        if (tab.id == id) tab.copy(isSleeping = false) else tab
                    },
                )
            }
            updateExtensionActiveTab(previousActiveTabId, id)
            updateSessionActivity(
                activeId = id,
                secondaryId = if (splitSelection) current.splitSecondaryTabId else null,
            )
            refreshSiteSetting(id)
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

    fun openCommandPalette() {
        _state.update { it.copy(commandPalette = CommandPaletteState(), tabSwitcher = null, overlay = BrowserOverlay.NONE) }
    }

    fun closeCommandPalette() {
        _state.update { it.copy(commandPalette = null) }
    }

    fun updateCommandPalette(query: String) {
        _state.update { it.copy(commandPalette = it.commandPalette?.copy(query = query, selectedIndex = 0)) }
    }

    fun moveCommandSelection(offset: Int) {
        val palette = _state.value.commandPalette ?: return
        val count = filteredCommands(palette.query).size
        if (count == 0) return
        _state.update {
            it.copy(commandPalette = palette.copy(selectedIndex = (palette.selectedIndex + offset + count) % count))
        }
    }

    fun executeSelectedCommand() {
        val palette = _state.value.commandPalette ?: return
        filteredCommands(palette.query).getOrNull(palette.selectedIndex)?.let { executeCommand(it.id) }
    }

    fun executeCommand(command: BrowserCommandId) {
        closeCommandPalette()
        when (command) {
            BrowserCommandId.NEW_TAB -> createTab()
            BrowserCommandId.NEW_PRIVATE_TAB -> createPrivateTab()
            BrowserCommandId.CLOSE_TAB -> activeTab()?.let { closeTab(it.id) }
            BrowserCommandId.REOPEN_CLOSED_TAB -> reopenClosedTab()
            BrowserCommandId.RELOAD -> reloadOrStop()
            BrowserCommandId.FIND_IN_PAGE -> openFindInPage()
            BrowserCommandId.FOCUS_ADDRESS_BAR -> focusAddressBar()
            BrowserCommandId.TOGGLE_BOOKMARK -> toggleBookmark()
            BrowserCommandId.TOGGLE_READING_LIST -> toggleReadingList()
            BrowserCommandId.SHOW_QR -> openQrCode()
            BrowserCommandId.NEXT_TAB -> cycleTab(true)
            BrowserCommandId.PREVIOUS_TAB -> cycleTab(false)
            BrowserCommandId.RESET_ZOOM -> resetPageZoom()
            BrowserCommandId.ZOOM_OUT -> adjustPageZoom(-10)
            BrowserCommandId.ZOOM_IN -> adjustPageZoom(10)
            BrowserCommandId.SHOW_TABS -> setOverlay(BrowserOverlay.TABS)
            BrowserCommandId.SHOW_LIBRARY -> setOverlay(BrowserOverlay.BOOKMARKS)
            BrowserCommandId.SHOW_DOWNLOADS -> setOverlay(BrowserOverlay.DOWNLOADS)
            BrowserCommandId.SHOW_SETTINGS -> setOverlay(BrowserOverlay.SETTINGS)
            BrowserCommandId.SHOW_PRIVACY -> setOverlay(BrowserOverlay.PRIVACY)
            BrowserCommandId.READER_MODE -> openReaderMode()
            BrowserCommandId.TRANSLATE_PAGE -> openTranslation()
            BrowserCommandId.TOGGLE_SPLIT -> activeTab()?.let { openTabInSplit(it.id) }
            BrowserCommandId.HIBERNATE_TABS -> hibernateInactiveTabs()
            BrowserCommandId.COMMAND_PALETTE -> openCommandPalette()
            BrowserCommandId.TAB_SEARCH -> openTabSwitcher()
        }
    }

    fun openTabSwitcher() {
        _state.update { it.copy(tabSwitcher = TabSwitcherState(), commandPalette = null, overlay = BrowserOverlay.NONE) }
    }

    fun closeTabSwitcher() {
        _state.update { it.copy(tabSwitcher = null) }
    }

    fun updateTabSwitcher(query: String) {
        _state.update { it.copy(tabSwitcher = it.tabSwitcher?.copy(query = query, selectedIndex = 0)) }
    }

    fun moveTabSelection(offset: Int) {
        val switcher = _state.value.tabSwitcher ?: return
        val count = filteredTabs(switcher.query).size
        if (count == 0) return
        _state.update {
            it.copy(tabSwitcher = switcher.copy(selectedIndex = (switcher.selectedIndex + offset + count) % count))
        }
    }

    fun selectTabFromSwitcher() {
        val switcher = _state.value.tabSwitcher ?: return
        filteredTabs(switcher.query).getOrNull(switcher.selectedIndex)?.let { tab ->
            selectTabFromSwitcher(tab.id)
        }
    }

    fun selectTabFromSwitcher(tabId: String) {
        if (_state.value.tabs.none { it.id == tabId }) return
        closeTabSwitcher()
        selectTab(tabId)
    }

    fun handleKeyShortcut(event: android.view.KeyEvent): Boolean {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return false
        _state.value.capturingShortcut?.let { command ->
            if (event.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                cancelKeyboardShortcutCapture()
                return true
            }
            if (event.keyCode in setOf(
                    android.view.KeyEvent.KEYCODE_CTRL_LEFT,
                    android.view.KeyEvent.KEYCODE_CTRL_RIGHT,
                    android.view.KeyEvent.KEYCODE_SHIFT_LEFT,
                    android.view.KeyEvent.KEYCODE_SHIFT_RIGHT,
                    android.view.KeyEvent.KEYCODE_ALT_LEFT,
                    android.view.KeyEvent.KEYCODE_ALT_RIGHT,
                    android.view.KeyEvent.KEYCODE_META_LEFT,
                    android.view.KeyEvent.KEYCODE_META_RIGHT,
                )
            ) return true
            if (setKeyboardShortcut(command, event.toKeyChord())) {
                _state.update { it.copy(capturingShortcut = null) }
            }
            return true
        }
        if (_state.value.commandPalette != null || _state.value.tabSwitcher != null) {
            return when {
                event.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE -> {
                    closeCommandPalette()
                    closeTabSwitcher()
                    true
                }
                event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (_state.value.commandPalette != null) moveCommandSelection(1) else moveTabSelection(1)
                    true
                }
                event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                    if (_state.value.commandPalette != null) moveCommandSelection(-1) else moveTabSelection(-1)
                    true
                }
                event.keyCode == android.view.KeyEvent.KEYCODE_ENTER -> {
                    if (_state.value.commandPalette != null) executeSelectedCommand() else selectTabFromSwitcher()
                    true
                }
                else -> false
            }
        }
        val chord = event.toKeyChord()
        val command = _state.value.settings.shortcutBindings.entries.firstOrNull { it.value == chord }?.key
            ?: return false
        executeCommand(command)
        return true
    }

    private fun filteredCommands(query: String): List<BrowserCommand> {
        val value = query.trim()
        return BrowserCommands.all.filter {
            value.isBlank() || it.title.contains(value, true) || it.description.contains(value, true)
        }
    }

    private fun filteredTabs(query: String): List<BrowserTabState> {
        val value = query.trim()
        return _state.value.tabs.filter {
            value.isBlank() || it.title.contains(value, true) || it.url.contains(value, true)
        }
    }

    fun reopenClosedTab() {
        val closed = recentlyClosedTabs.firstOrNull() ?: return
        val id = createTab(
            privateMode = closed.isPrivate,
            initialUri = closed.url.takeIf(String::isNotBlank),
            savedSessionState = closed.sessionState,
        )
        if (id.isNotBlank()) {
            recentlyClosedTabs.removeFirstOrNull()
            _state.update { it.copy(closedTabCount = recentlyClosedTabs.size) }
            persistRecentlyClosedTabs()
            updateTab(id) { it.copy(title = closed.title.ifBlank { it.title }) }
            if (closed.pinned) toggleTabPinned(id)
            if (closed.groupId != null && _state.value.settings.tabGroups.any { it.id == closed.groupId }) {
                moveTabToGroup(id, closed.groupId)
            }
        }
    }

    fun closeTab(id: String) {
        val current = _state.value
        val closing = current.tabs.firstOrNull { it.id == id } ?: return
        rejectPermissionsForTab(id)
        pageZoomByTab.remove(id)
        lastProgressUpdateAt.remove(id)
        trackerBlockedByTab.remove(id)
        mediaSessions.remove(id)
        translationDetectedByTab.remove(id)
        getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.clear(id)
        rebuildBlockerStats()
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
            _state.update {
                it.copy(
                    tabs = listOf(replacement),
                    activeTabId = replacementId,
                    overlay = BrowserOverlay.NONE,
                    splitPrimaryTabId = null,
                    splitSecondaryTabId = null,
                    splitPaneFocused = false,
                )
            }
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
                splitPrimaryTabId = if (id == it.splitPrimaryTabId || id == it.splitSecondaryTabId) null else it.splitPrimaryTabId,
                splitSecondaryTabId = if (id == it.splitPrimaryTabId || id == it.splitSecondaryTabId) null else it.splitSecondaryTabId,
                splitPaneFocused = if (id == it.splitPrimaryTabId || id == it.splitSecondaryTabId) false else it.splitPaneFocused,
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

    fun moveTabAfter(tabId: String, targetId: String) {
        val current = _state.value
        val moving = current.tabs.firstOrNull { it.id == tabId } ?: return
        val target = current.tabs.firstOrNull { it.id == targetId } ?: return
        if (moving.id == target.id || moving.pinned != target.pinned) return
        val reordered = current.tabs.toMutableList().apply {
            removeAll { it.id == moving.id }
            add(indexOfFirst { it.id == target.id }.plus(1).coerceAtMost(size), moving)
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

    fun createWorkspace(title: String) {
        val current = _state.value
        val workspaces = workspaceSnapshots(current)
        if (workspaces.size >= MAX_WORKSPACES) {
            showSnackbar("Workspace limit reached ($MAX_WORKSPACES)")
            return
        }
        val now = System.currentTimeMillis()
        val workspace = TabWorkspace(
            id = UUID.randomUUID().toString(),
            title = title.trim().take(40).ifBlank { "Workspace ${workspaces.size + 1}" },
            contextId = "dextra-${UUID.randomUUID()}",
            createdAt = now,
            lastUsedAt = now,
        )
        saveAndActivateWorkspace(workspaces + workspace, workspace)
    }

    fun switchWorkspace(workspaceId: String) {
        val current = _state.value
        val target = workspaceSnapshots(current).firstOrNull { it.id == workspaceId } ?: return
        if (target.id == current.settings.activeWorkspaceId) return
        saveAndActivateWorkspace(workspaceSnapshots(current), target)
    }

    fun renameWorkspace(workspaceId: String, title: String) {
        val normalized = title.trim().take(40)
        if (normalized.isBlank()) return
        _state.update { state ->
            state.copy(settings = state.settings.copy(
                workspaces = workspaceSnapshots(state).map { workspace ->
                    if (workspace.id == workspaceId) workspace.copy(title = normalized) else workspace
                },
            ))
        }
        persistOpenTabs(immediate = true)
    }

    fun deleteWorkspace(workspaceId: String) {
        val current = _state.value
        val workspaces = workspaceSnapshots(current)
        if (workspaces.size <= 1 || workspaces.none { it.id == workspaceId }) {
            showSnackbar("Keep at least one workspace")
            return
        }
        val remaining = workspaces.filterNot { it.id == workspaceId }
        if (workspaceId == current.settings.activeWorkspaceId) {
            saveAndActivateWorkspace(remaining, remaining.maxByOrNull(TabWorkspace::lastUsedAt) ?: remaining.first())
        } else {
            _state.update { it.copy(settings = it.settings.copy(workspaces = remaining)) }
            persistOpenTabs(immediate = true)
        }
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

    fun deleteTabGroup(groupId: String) {
        if (_state.value.settings.tabGroups.none { it.id == groupId }) return
        _state.update { state ->
            state.copy(
                settings = state.settings.copy(
                    tabGroups = state.settings.tabGroups.filterNot { it.id == groupId },
                ),
                tabs = state.tabs.map { tab ->
                    if (tab.groupId == groupId) tab.copy(groupId = null) else tab
                },
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

    fun openTabInSplit(tabId: String) {
        val current = _state.value
        val primaryId = current.activeTabId ?: return
        if (current.tabs.none { it.id == tabId }) return
        val secondaryId = if (tabId == primaryId) {
            current.splitSecondaryTabId?.takeIf { it != primaryId }
                ?: current.tabs.firstOrNull { it.id != primaryId }?.id
        } else {
            tabId
        }
        if (secondaryId == null) {
            showSnackbar("Open another tab before using split view")
            return
        }
        _state.update {
            it.copy(
                splitPrimaryTabId = primaryId,
                splitSecondaryTabId = secondaryId,
                splitPaneFocused = false,
                tabs = it.tabs.map { tab ->
                    if (tab.id == primaryId || tab.id == secondaryId) tab.copy(isSleeping = false) else tab
                },
            )
        }
        updateSessionActivity(primaryId, secondaryId)
    }

    fun openTabInNewWindow(tabId: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        if (!tab.hasPage || !NavigationPolicy.isAllowedTopLevel(tab.url)) {
            showSnackbar("This tab cannot be opened in a new window")
            return
        }
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(tab.url))
                    .setClass(getApplication(), BrowserWindowActivity::class.java)
                    .putExtra(BrowserWindowActivity.EXTRA_PRIVATE, tab.isPrivate)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
            )
        }.onFailure { showSnackbar("Could not open a new browser window") }
    }

    fun closeSplit() {
        val current = _state.value
        if (current.splitSecondaryTabId == null) return
        val previousId = current.activeTabId
        val primaryId = current.splitPrimaryTabId ?: current.activeTabId
        _state.update {
            it.copy(
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
                activeTabId = primaryId,
            )
        }
        updateSessionActivity(primaryId)
        updateExtensionActiveTab(previousId, primaryId)
    }

    fun swapSplit() {
        val current = _state.value
        val primaryId = current.splitPrimaryTabId ?: return
        val secondaryId = current.splitSecondaryTabId ?: return
        val previousActiveId = current.activeTabId
        _state.update {
            it.copy(
                splitPrimaryTabId = secondaryId,
                splitSecondaryTabId = primaryId,
                splitPaneFocused = current.splitPaneFocused,
                activeTabId = if (current.splitPaneFocused) primaryId else secondaryId,
            )
        }
        val nextActiveId = _state.value.activeTabId
        updateExtensionActiveTab(previousActiveId, nextActiveId)
        updateSessionActivity(_state.value.splitPrimaryTabId, _state.value.splitSecondaryTabId)
    }

    fun focusSplitPane(secondary: Boolean) {
        val current = _state.value
        val id = if (secondary) current.splitSecondaryTabId else current.splitPrimaryTabId
        if (id == null || id == current.activeTabId) {
            _state.update { it.copy(splitPaneFocused = secondary) }
            return
        }
        val previous = current.activeTabId
        _state.update { it.copy(activeTabId = id, splitPaneFocused = secondary) }
        updateExtensionActiveTab(previous, id)
    }

    fun navigateActive(input: String) {
        activeTab()?.let { navigate(it.id, input) }
    }

    fun navigate(tabId: String, input: String) {
        val url = BrowserUrl.resolve(input, searchUrl(_state.value.settings))
            .let(::upgradeToHttpsIfNeeded)
        if (url.isEmpty()) return
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        recordNetworkActivity(tabId, url, "navigation", "requested")
        if (tabId == _state.value.activeTabId && _state.value.findInPage != null) closeFindInPage()
        updateTab(tabId) {
            it.copy(url = url, title = "Loading...", hasPage = true, isLoading = true, progress = 0, favicon = null, crashed = false, sessionState = null)
        }
        _state.update {
            if (it.activeTabId == tabId) it.copy(
                webAppManifest = null,
                siteSettingsOpen = false,
                readerMode = null,
                translation = null,
                securityDiagnostics = null,
            )
            else it
        }
        refreshSiteSetting(tabId)
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

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun openTranslation() {
        val tab = activeTab()?.takeIf { it.hasPage && NavigationPolicy.isWebUrl(it.url) } ?: run {
            showSnackbar("Translation needs a web page")
            return
        }
        _state.update { state ->
            state.copy(
                translation = PageTranslationState(
                    tabId = tab.id,
                    sourceLanguage = translationDetectedByTab[tab.id],
                    targetLanguage = state.siteSetting?.translationTarget ?: "en",
                    isTranslated = false,
                ),
                readerMode = null,
            )
        }
    }

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun translateActivePage(sourceLanguage: String, targetLanguage: String) {
        val translation = _state.value.translation ?: return
        val tab = _state.value.tabs.firstOrNull { it.id == translation.tabId } ?: return
        val target = targetLanguage.trim().lowercase().take(12)
        if (target.isBlank() || !target.all { it.isLetter() || it == '-' }) {
            showSnackbar("Enter a valid language code, for example en or id")
            return
        }
        val source = sourceLanguage.trim().lowercase().take(12)
        if (source.isBlank() || !source.all { it.isLetter() || it == '-' }) {
            showSnackbar("Enter the source language code, for example en or ja")
            return
        }
        _state.update {
            it.copy(
                translation = translation.copy(
                    sourceLanguage = source,
                    targetLanguage = target,
                    isTranslating = true,
                    error = null,
                ),
            )
        }
        val sessionTranslation = tab.session.getSessionTranslation() ?: run {
            showSnackbar("Translation is unavailable")
            return
        }
        sessionTranslation.translate(
            source,
            target,
            TranslationsController.SessionTranslation.TranslationOptions.Builder()
                .downloadModel(true)
                .build(),
        ).accept(
            {
                _state.update {
                    it.copy(translation = it.translation?.copy(isTranslating = false, isTranslated = true, error = null))
                }
                updateCurrentSiteSetting { it.copy(translationTarget = target) }
            },
            { error ->
                _state.update {
                    it.copy(translation = it.translation?.copy(isTranslating = false, error = error?.message ?: "Translation failed"))
                }
            },
        )
    }

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun restoreOriginalPage() {
        val translation = _state.value.translation ?: return
        val sessionTranslation = _state.value.tabs.firstOrNull { it.id == translation.tabId }?.session?.getSessionTranslation() ?: return
        sessionTranslation.restoreOriginalPage().accept(
            {
                _state.update { it.copy(translation = translation.copy(isTranslated = false, isTranslating = false, error = null)) }
            },
            { error -> _state.update { it.copy(translation = translation.copy(error = error?.message ?: "Could not restore original page")) } },
        )
    }

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun neverTranslateCurrentSite() {
        val translation = _state.value.translation ?: return
        val tab = _state.value.tabs.firstOrNull { it.id == translation.tabId } ?: return
        val sessionTranslation = tab.session.getSessionTranslation() ?: return
        sessionTranslation.setNeverTranslateSiteSetting(true).accept(
            { _state.update { it.copy(translation = null) } },
            { showSnackbar("Could not update translation preference") },
        )
    }

    fun closeTranslation() {
        _state.update { it.copy(translation = null) }
    }

    fun resolveWebAuthn(allow: Boolean) {
        val prompt = _state.value.webAuthnPrompt ?: return
        prompt.result.complete(if (allow) prompt.allow() else prompt.dismiss())
        _state.update { it.copy(webAuthnPrompt = null) }
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

    fun toggleTabAudio(tabId: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        val mediaSession = mediaSessions[tabId]
        if (!tab.hasActiveMedia || mediaSession == null) {
            showSnackbar("No active audio in this tab")
            return
        }
        val muted = !tab.isAudioMuted
        runCatching { mediaSession.muteAudio(muted) }
            .onSuccess { updateTab(tabId) { it.copy(isAudioMuted = muted) } }
            .onFailure { showSnackbar("Could not change tab audio") }
    }

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun openReaderMode() {
        val tab = activeTab()?.takeIf { it.hasPage && NavigationPolicy.isWebUrl(it.url) } ?: run {
            showSnackbar("Reader mode needs a web page")
            return
        }
        _state.update {
            it.copy(
                readerMode = ReaderModeState(
                    tabId = tab.id,
                    url = tab.url,
                    title = tab.title.ifBlank { BrowserUrl.displayValue(tab.url) },
                ),
                overlay = BrowserOverlay.NONE,
                siteSettingsOpen = false,
            )
        }
        tab.session.sessionPageExtractor.getPageMetadata().accept(
            { metadata ->
                if (_state.value.readerMode?.tabId != tab.id) return@accept
                if (metadata?.isReaderable != true) {
                    closeReaderMode()
                    showSnackbar("This page does not have a readable article")
                    return@accept
                }
                tab.session.sessionPageExtractor.getPageContent(PageExtractionController.ContentParams(true)).accept(
                    { html ->
                        val rawContent = html.orEmpty()
                        if (rawContent.toByteArray(Charsets.UTF_8).size > MAX_READER_ARTICLE_BYTES) {
                            closeReaderMode()
                            showSnackbar("This article is too large for reader mode")
                            return@accept
                        }
                        viewModelScope.launch(Dispatchers.Default) {
                            val content = readerTextFromHtml(rawContent)
                            withContext(Dispatchers.Main.immediate) {
                                if (_state.value.readerMode?.tabId == tab.id) {
                                    _state.update {
                                        it.copy(
                                            readerMode = it.readerMode?.copy(
                                                content = content,
                                                wordCount = metadata.wordCount,
                                                language = metadata.language.takeIf(String::isNotBlank),
                                                isLoading = false,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    {
                        closeReaderMode()
                        showSnackbar("Could not extract this article")
                    },
                )
            },
            {
                closeReaderMode()
                showSnackbar("Reader mode is unavailable for this page")
            },
        )
    }

    fun closeReaderMode() {
        _state.update { it.copy(readerMode = null) }
    }

    fun dismissTransientUi() {
        dismissContextMenu()
        closeExtensionPopup()
        closeFindInPage()
        closeCommandPalette()
        closeTabSwitcher()
        closeReaderMode()
        _state.value.webAuthnPrompt?.let { prompt -> prompt.result.complete(prompt.dismiss()) }
        _state.value.contentPermission?.result?.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        _state.value.androidPermission?.callback?.reject()
        _state.value.mediaPermission?.callback?.reject()
        _state.value.webPushPrompt?.result?.complete(null)
        queuedWebPushPrompts.forEach { prompt -> prompt.result.complete(null) }
        queuedContentPermissions.forEach { prompt ->
            prompt.result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }
        queuedAndroidPermissions.forEach { prompt -> prompt.callback.reject() }
        queuedMediaPermissions.forEach { prompt -> prompt.callback.reject() }
        queuedContentPermissions.clear()
        queuedAndroidPermissions.clear()
        queuedMediaPermissions.clear()
        queuedWebPushPrompts.clear()
        _state.update { it.copy(contentPermission = null, androidPermission = null, mediaPermission = null, webPushPrompt = null, webAuthnPrompt = null) }
    }

    fun onAppBackground() {
        appInForeground = false
        _state.value.tabs.forEach { tab -> runCatching { tab.session.flushSessionState() } }
        persistOpenTabs(immediate = true)
        recordSessionRecovery()
        credentialVault.lock()
        if (_state.value.isPictureInPictureMode) return
        if (_state.value.settings.clearSiteDataOnExit) {
            privacyExitCleanupJob?.cancel()
            privacyExitCleanupJob = viewModelScope.launch {
                delay(5_000)
                if (!appInForeground) {
                    runtime.storageController.clearData(StorageController.ClearFlags.SITE_DATA)
                }
            }
        }
        updateSessionActivity(null, keepActiveMedia = true)
    }

    fun onAppForeground() {
        appInForeground = true
        privacyExitCleanupJob?.cancel()
        val current = _state.value
        updateSessionActivity(current.activeTabId, current.splitSecondaryTabId)
        refreshWebDavState()
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
                    hasActiveMedia = tab.hasActiveMedia,
                    isAudioMuted = tab.isAudioMuted,
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
                    isTab = true,
                    isPinned = tab.pinned,
                    isSleeping = tab.isSleeping,
                    hasActiveMedia = tab.hasActiveMedia,
                    isAudioMuted = tab.isAudioMuted,
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
                duplicateTab(menu.tabId)
            }
            ContextMenuAction.RELOAD_TAB -> _state.value.tabs.firstOrNull { it.id == menu.tabId }?.session?.reload()
            ContextMenuAction.TOGGLE_TAB_PINNED -> toggleTabPinned(menu.tabId)
            ContextMenuAction.TOGGLE_TAB_SLEEPING -> toggleTabSleeping(menu.tabId)
            ContextMenuAction.TOGGLE_TAB_AUDIO -> toggleTabAudio(menu.tabId)
            ContextMenuAction.CLOSE_TAB -> closeTab(menu.tabId)
            ContextMenuAction.CLOSE_OTHER_TABS -> closeOtherTabs(menu.tabId)
            ContextMenuAction.CLOSE_TABS_TO_LEFT -> closeTabsToLeft(menu.tabId)
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
            ContextMenuAction.SAVE_MEDIA -> menu.resourceUri?.let { downloadUrl(it, isPrivateTab(menu.tabId)) }
            ContextMenuAction.COPY_PAGE_URL -> menu.pageUrl.takeIf(String::isNotBlank)?.let { copyToClipboard("Page URL", it) }
            ContextMenuAction.TOGGLE_BOOKMARK -> if (menu.tabId == _state.value.activeTabId) toggleBookmark()
            ContextMenuAction.SAVE_PAGE -> menu.pageUrl.takeIf(String::isNotBlank)?.let { downloadUrl(it, isPrivateTab(menu.tabId)) }
            ContextMenuAction.OPEN_IN_SPLIT -> openTabInSplit(menu.tabId)
            ContextMenuAction.OPEN_IN_NEW_WINDOW -> openTabInNewWindow(menu.tabId)
            ContextMenuAction.CLOSE_SPLIT -> closeSplit()
            ContextMenuAction.SWAP_SPLIT -> swapSplit()
            ContextMenuAction.DISMISS -> Unit
        }
        dismissContextMenu()
    }

    fun duplicateTab(tabId: String) {
        val source = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        val duplicateId = createTab(
            privateMode = source.isPrivate,
            initialUri = source.url.takeIf(String::isNotBlank),
            savedSessionState = source.sessionState,
        )
        if (duplicateId.isBlank()) return
        updateTab(duplicateId) { it.copy(title = source.title, pinned = source.pinned, groupId = source.groupId) }
        if (source.pinned) {
            _state.update { state ->
                state.copy(tabs = state.tabs.sortedWith(compareByDescending<BrowserTabState> { it.pinned }))
            }
        }
        persistOpenTabs()
    }

    private fun closeOtherTabs(id: String) {
        val current = _state.value
        val keep = current.tabs.firstOrNull { it.id == id } ?: return
        val closing = current.tabs.filterNot { it.id == id }
        closing.forEach {
            rememberClosedTab(it)
            rejectPermissionsForTab(it.id)
            trackerBlockedByTab.remove(it.id)
            mediaSessions.remove(it.id)
        }
        _state.update {
            it.copy(
                tabs = listOf(keep.copy(isSleeping = false)),
                activeTabId = id,
                overlay = BrowserOverlay.NONE,
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
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
        closing.forEach {
            rememberClosedTab(it)
            rejectPermissionsForTab(it.id)
            trackerBlockedByTab.remove(it.id)
            mediaSessions.remove(it.id)
        }
        val newActive = if (closing.any { it.id == current.activeTabId }) id else current.activeTabId
        _state.update {
            it.copy(
                tabs = current.tabs.take(index + 1),
                activeTabId = newActive,
                overlay = BrowserOverlay.NONE,
                splitPrimaryTabId = if (closing.isNotEmpty()) null else it.splitPrimaryTabId,
                splitSecondaryTabId = if (closing.isNotEmpty()) null else it.splitSecondaryTabId,
                splitPaneFocused = if (closing.isNotEmpty()) false else it.splitPaneFocused,
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

    private fun closeTabsToLeft(id: String) {
        val current = _state.value
        val index = current.tabs.indexOfFirst { it.id == id }
        if (index <= 0) return
        val closing = current.tabs.take(index)
        closing.forEach {
            rememberClosedTab(it)
            rejectPermissionsForTab(it.id)
            trackerBlockedByTab.remove(it.id)
            mediaSessions.remove(it.id)
        }
        val newActive = if (closing.any { it.id == current.activeTabId }) id else current.activeTabId
        _state.update {
            it.copy(
                tabs = current.tabs.drop(index),
                activeTabId = newActive,
                overlay = BrowserOverlay.NONE,
                splitPrimaryTabId = if (closing.isNotEmpty()) null else it.splitPrimaryTabId,
                splitSecondaryTabId = if (closing.isNotEmpty()) null else it.splitSecondaryTabId,
                splitPaneFocused = if (closing.isNotEmpty()) false else it.splitPaneFocused,
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
        if (!tab.hasPage || tab.url.isBlank() || !NavigationPolicy.isWebUrl(tab.url)) return
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

    fun updateBookmark(bookmark: Bookmark, title: String, folder: String?) {
        val normalizedTitle = title.trim().take(200).ifBlank { BrowserUrl.displayValue(bookmark.url) }
        val normalizedFolder = folder?.trim()?.take(40)?.takeIf(String::isNotBlank)
        viewModelScope.launch { dao.updateBookmark(bookmark.url, normalizedTitle, normalizedFolder) }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            dao.deleteBookmark(bookmark.url)
            showSnackbar("Bookmark removed")
        }
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
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > MAX_BOOKMARK_IMPORT_BYTES) error("Bookmark file is too large")
                        output.write(buffer, 0, read)
                    }
                    output.toString(Charsets.UTF_8.name())
                } ?: error("Could not open import file")
                var count = 0
                BookmarkHtmlParser.parse(html).forEach { bookmark ->
                    dao.insertBookmark(
                        Bookmark(
                            url = bookmark.url,
                            title = bookmark.title,
                            createdAt = System.currentTimeMillis(),
                            folder = bookmark.folder,
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

    private fun isPrivateTab(tabId: String): Boolean =
        _state.value.tabs.firstOrNull { it.id == tabId }?.isPrivate == true

    private fun downloadUrl(url: String, privateMode: Boolean = false) {
        if (!NavigationPolicy.isWebUrl(url)) {
            showSnackbar("This resource cannot be downloaded")
            return
        }
        runCatching {
            val uri = Uri.parse(url)
            val fileName = uri.lastPathSegment?.takeIf(String::isNotBlank)?.sanitizeFileName() ?: "dextra-download"
            val downloadId = -System.nanoTime()
            val downloadDirectory = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(getApplication<Application>().filesDir, "downloads")
            val download = DownloadEntry(
                downloadId = downloadId,
                fileName = fileName,
                url = url,
                mimeType = null,
                status = DownloadStatus.QUEUED.label,
                bytesDownloaded = 0,
                totalBytes = -1,
                localUri = null,
                filePath = File(downloadDirectory, "$downloadId-$fileName").path,
                reason = null,
                speedBytesPerSecond = 0,
                createdAt = System.currentTimeMillis(),
                isPrivate = privateMode,
                destinationTreeUri = _state.value.settings.downloadDirectoryUri,
                workspaceId = activeProfileId(),
            )
            viewModelScope.launch {
                dao.upsertDownload(download)
                scheduleDownload(download)
            }
            showSnackbar("Download started")
        }.onFailure { showSnackbar("Could not start download") }
    }

    fun openSavedPage(url: String) {
        val tab = activeTab() ?: return
        navigate(tab.id, url)
        dismissOverlay()
    }

    fun toggleReadingList() {
        val tab = activeTab() ?: return
        if (tab.isPrivate || !tab.hasPage || !NavigationPolicy.isWebUrl(tab.url)) return
        viewModelScope.launch {
            if (dao.getReadingListEntry(tab.url) != null) {
                dao.deleteReadingListEntry(tab.url)
                showSnackbar("Removed from reading list")
            } else {
                dao.upsertReadingListEntry(
                    ReadingListEntry(
                        url = tab.url,
                        title = tab.title.ifBlank { BrowserUrl.displayValue(tab.url) },
                        savedAt = System.currentTimeMillis(),
                    ),
                )
                showSnackbar("Saved to reading list")
            }
        }
    }

    fun setDownloadDirectory(uri: Uri?) {
        if (uri != null) {
            runCatching {
                require(DocumentFile.fromTreeUri(getApplication(), uri)?.canWrite() == true) {
                    "Selected folder is not writable"
                }
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
            }.onFailure {
                showSnackbar("Could not keep access to this folder")
                return
            }
        }
        viewModelScope.launch {
            settingsRepository.setDownloadDirectoryUri(uri?.toString())
            showSnackbar(if (uri == null) "Download folder reset" else "Download folder updated")
        }
    }

    fun deleteCredential(credential: StoredCredential) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { credentialVault.delete(credential.id) }
                .onFailure { Log.e("Dextra", "Could not delete browser credential", it) }
        }
    }

    fun requestCredentialUnlock() {
        if (_state.value.credentialCount == 0) return
        _state.update { it.copy(credentialUnlockRequest = it.credentialUnlockRequest + 1) }
    }

    fun unlockCredentialVault() {
        credentialVault.unlock()
        showSnackbar("Saved logins unlocked")
    }

    fun reportCredentialUnlockFailure(message: String) {
        showSnackbar("Could not unlock saved logins: ${message.take(80)}")
    }

    fun lockCredentialVault() {
        credentialVault.lock()
        showSnackbar("Saved logins locked")
    }

    fun resolveWebPushPrompt(allow: Boolean) {
        val prompt = _state.value.webPushPrompt ?: return
        _state.update { it.copy(webPushPrompt = queuedWebPushPrompts.removeFirstOrNull()) }
        if (!allow) {
            prompt.result.complete(null)
            return
        }
        val subscription = runCatching { createWebPushSubscription(prompt) }.getOrNull()
        if (subscription == null) {
            prompt.result.complete(null)
            showSnackbar("Could not create Web Push subscription")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                webPushStore.save(subscription.record)
                withContext(Dispatchers.Main.immediate) {
                    prompt.result.complete(subscription.subscription)
                    showSnackbar("Web Push enabled for ${prompt.origin}")
                }
            }.onFailure {
                prompt.result.complete(null)
                withContext(Dispatchers.Main.immediate) { showSnackbar("Could not save Web Push subscription") }
            }
        }
    }

    fun revokeWebPushSubscription(subscription: StoredWebPushSubscription) {
        viewModelScope.launch(Dispatchers.IO) { webPushStore.delete(subscription.scope) }
    }

    fun clearWebPushSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) { webPushStore.clear() }
    }

    fun clearCredentials() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { credentialVault.clear() }
                .onFailure { Log.e("Dextra", "Could not clear browser credentials", it) }
        }
    }

    fun deleteAddress(address: StoredAddress) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { addressVault.delete(address.id) }
                .onFailure { Log.e("Dextra", "Could not delete address autofill data", it) }
        }
    }

    fun saveAddress(address: StoredAddress) {
        if (address.name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { addressVault.save(address.copy(updatedAt = System.currentTimeMillis())) }
                .onFailure { Log.e("Dextra", "Could not save address autofill data", it) }
        }
    }

    fun clearAddresses() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { addressVault.clear() }
                .onFailure { Log.e("Dextra", "Could not clear address autofill data", it) }
        }
    }

    fun copyCredentialUsername(credential: StoredCredential) {
        copyToClipboard("Username", credential.username)
    }

    fun copyCredentialPassword(credential: StoredCredential) {
        copyToClipboard("Password", credential.password)
    }

    fun deleteReadingListEntry(entry: ReadingListEntry) {
        viewModelScope.launch {
            entry.offlinePath?.let { path -> runCatching { File(path).delete() } }
            dao.deleteReadingListEntry(entry.url)
        }
    }

    fun setReadingListRead(entry: ReadingListEntry, isRead: Boolean) {
        viewModelScope.launch { dao.setReadingListRead(entry.url, isRead) }
    }

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun saveCurrentPageOffline() {
        val tab = activeTab() ?: return
        if (tab.isPrivate || !tab.hasPage || !NavigationPolicy.isWebUrl(tab.url)) {
            showSnackbar("Offline pages are unavailable in private tabs")
            return
        }
        viewModelScope.launch {
            tab.session.sessionPageExtractor.getPageContent().accept(
                { html ->
                    val content = html.orEmpty()
                    if (content.toByteArray(Charsets.UTF_8).size > MAX_OFFLINE_ARTICLE_BYTES) {
                        showSnackbar("This page is too large to save offline")
                        return@accept
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val result = runCatching {
                            val directory = File(getApplication<Application>().filesDir, "offline").apply { mkdirs() }
                            val file = File(directory, sha256(tab.url) + ".html")
                            file.writeText(content, Charsets.UTF_8)
                            val existing = dao.getReadingListEntry(tab.url)
                            dao.upsertReadingListEntry(
                                (existing ?: ReadingListEntry(
                                    url = tab.url,
                                    title = tab.title.ifBlank { BrowserUrl.displayValue(tab.url) },
                                    savedAt = System.currentTimeMillis(),
                                )).copy(offlinePath = file.path),
                            )
                        }
                        withContext(Dispatchers.Main.immediate) {
                            showSnackbar(if (result.isSuccess) "Saved for offline reading" else "Could not save page offline")
                        }
                    }
                },
                { showSnackbar("Could not extract this page for offline reading") },
            )
        }
    }

    fun openOfflineArticle(entry: ReadingListEntry) {
        val path = entry.offlinePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val article = runCatching {
                val file = File(path)
                val offlineRoot = File(getApplication<Application>().filesDir, "offline").canonicalFile
                require(file.canonicalFile.parentFile == offlineRoot)
                require(file.length() <= MAX_OFFLINE_ARTICLE_BYTES)
                OfflineArticle(
                    title = entry.title,
                    content = Html.fromHtml(file.readText(Charsets.UTF_8), Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                )
            }.getOrNull()
            withContext(Dispatchers.Main.immediate) {
                if (article != null) _state.update { it.copy(offlineArticle = article, overlay = BrowserOverlay.NONE) }
                else showSnackbar("Offline copy is no longer available")
            }
        }
    }

    fun closeOfflineArticle() {
        _state.update { it.copy(offlineArticle = null) }
    }

    fun setCurrentSiteDesktopOverride(value: Boolean?) {
        updateCurrentSiteSetting { it.copy(desktopSites = value) }
    }

    fun setCurrentSiteAdBlockingOverride(value: Boolean?) {
        updateCurrentSiteSetting { it.copy(adBlockingEnabled = value) }
    }

    fun setCurrentSiteUserScriptsOverride(value: Boolean?) {
        updateCurrentSiteSetting { it.copy(userScriptsEnabled = value) }
    }

    fun setCurrentSiteZoomOverride(value: Int?) {
        updateCurrentSiteSetting { it.copy(zoomPercent = value?.coerceIn(50, 200)) }
    }

    fun setCurrentSiteHttpsOnly(value: Boolean?) {
        updateCurrentSiteSetting { it.copy(httpsOnly = value) }
    }

    fun setCurrentSiteCookieBannerMode(value: Int?) {
        updateCurrentSiteSetting { it.copy(cookieBannerMode = value?.coerceIn(0, 2)) }
    }

    fun clearCurrentSiteSettings() {
        val origin = currentSiteOrigin() ?: return
        viewModelScope.launch {
            dao.deleteSiteSetting(activeProfileId(), origin)
            removeSiteCookieBannerMode(origin)
            _state.update { it.copy(siteSetting = null) }
            activeTab()?.let { tab ->
                applyDesktopSiteSetting(tab.session, _state.value.settings.desktopSites)
                pageZoomByTab.remove(tab.id)
                syncActiveTabZoom()
                if (tab.hasPage) tab.session.reload()
            }
        }
    }

    fun openSiteSettings() {
        if (currentSiteOrigin() == null) {
            showSnackbar("Site settings are unavailable on this page")
            return
        }
        refreshSiteSetting(_state.value.activeTabId ?: return)
        _state.update { it.copy(siteSettingsOpen = true) }
    }

    fun closeSiteSettings() {
        _state.update { it.copy(siteSettingsOpen = false) }
    }

    fun openQrCode() {
        val url = activeTab()?.url?.takeIf { NavigationPolicy.isWebUrl(it) } ?: run {
            showSnackbar("There is no web URL to share")
            return
        }
        _state.update { it.copy(qrCodeUrl = url) }
    }

    fun closeQrCode() {
        _state.update { it.copy(qrCodeUrl = null) }
    }

    fun setPictureInPictureMode(enabled: Boolean) {
        _state.update { it.copy(isPictureInPictureMode = enabled) }
        if (!enabled) onAppForeground()
    }

    fun shareActiveUrl() {
        val url = activeTab()?.url?.takeIf { NavigationPolicy.isWebUrl(it) } ?: return
        runCatching {
            getApplication<Application>().startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url),
                    "Share page",
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { showSnackbar("Could not share this page") }
    }

    fun shareQrCode() {
        val url = _state.value.qrCodeUrl ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val directory = File(getApplication<Application>().cacheDir, "shared").apply { mkdirs() }
                val file = File(directory, "dextra-qr.png")
                file.outputStream().use { output ->
                    check(QrCodeGenerator.generate(url, 1024).compress(Bitmap.CompressFormat.PNG, 100, output))
                }
                FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
            }
            withContext(Dispatchers.Main.immediate) {
                result.onSuccess { uri ->
                    getApplication<Application>().startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share QR code",
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { showSnackbar("Could not share QR code") }
            }
        }
    }

    fun installCurrentWebApp() {
        val manifest = _state.value.webAppManifest ?: run {
            showSnackbar("This page has no installable web app manifest")
            return
        }
        val context = getApplication<Application>()
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: run {
            showSnackbar("App shortcuts are unavailable on this device")
            return
        }
        runCatching {
            val appId = webAppId(manifest.startUrl)
            val app = InstalledWebApp(
                id = appId,
                origin = NavigationPolicy.origin(manifest.startUrl) ?: error("Invalid web app origin"),
                name = manifest.name,
                startUrl = manifest.startUrl,
                scope = manifest.scope,
                installedAt = System.currentTimeMillis(),
                iconUrl = manifest.iconUrl,
            )
            viewModelScope.launch {
                dao.upsertInstalledWebApp(app)
            }
            shortcutManager.addDynamicShortcuts(listOf(buildWebAppShortcut(app)))
            refreshWebAppShortcut(app)
        }.onSuccess {
            showSnackbar("${manifest.name} added to app shortcuts")
        }.onFailure {
            showSnackbar("Could not install this web app")
        }
    }

    fun uninstallWebApp(app: InstalledWebApp) {
        val context = getApplication<Application>()
        context.getSystemService(ShortcutManager::class.java)?.removeDynamicShortcuts(listOf("webapp-${app.id}"))
        viewModelScope.launch { dao.deleteInstalledWebApp(app.id) }
    }

    fun refreshInstalledWebApp(app: InstalledWebApp) {
        viewModelScope.launch {
            val refreshed = withContext(Dispatchers.IO) { fetchInstalledWebApp(app) }
            if (refreshed == null) {
                showSnackbar("Could not refresh ${app.name}")
            } else {
                dao.upsertInstalledWebApp(refreshed)
                refreshWebAppShortcut(refreshed)
                showSnackbar("${refreshed.name} updated")
            }
        }
    }

    fun openInstalledWebApp(app: InstalledWebApp) {
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(app.startUrl))
                    .setClass(getApplication(), PwaActivity::class.java)
                    .putExtra(PwaActivity.EXTRA_PWA_ID, app.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
            )
        }.onFailure { showSnackbar("Could not open ${app.name}") }
    }

    fun enterPwaMode(startUrl: String) {
        if (!NavigationPolicy.isAllowedTopLevel(startUrl)) {
            showSnackbar("This web app URL is not allowed")
            return
        }
        _state.update { it.copy(standalonePwa = true, overlay = BrowserOverlay.NONE) }
        if (!restoredSavedTabs) {
            pendingPwaUri = startUrl
        } else {
            openPwaUri(startUrl)
        }
    }

    fun enterWindowMode(startUrl: String, privateMode: Boolean = false) {
        if (!NavigationPolicy.isAllowedTopLevel(startUrl)) return
        _state.update { it.copy(standaloneWindow = true, overlay = BrowserOverlay.NONE) }
        if (!restoredSavedTabs) {
            pendingWindowUri = startUrl
            pendingWindowPrivate = privateMode
        } else {
            openWindowUri(startUrl, privateMode)
        }
    }

    private fun openPwaUri(startUrl: String) {
        val current = _state.value
        current.tabs.forEach { tab -> runCatching { tab.session.close() } }
        _state.update {
            it.copy(
                tabs = emptyList(),
                activeTabId = null,
                overlay = BrowserOverlay.NONE,
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
                standalonePwa = true,
            )
        }
        createTab(initialUri = startUrl)
    }

    private fun openWindowUri(startUrl: String, privateMode: Boolean) {
        val current = _state.value
        current.tabs.forEach { tab -> runCatching { tab.session.close() } }
        _state.update {
            it.copy(
                tabs = emptyList(),
                activeTabId = null,
                overlay = BrowserOverlay.NONE,
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
                standaloneWindow = true,
            )
        }
        createTab(privateMode = privateMode, initialUri = startUrl)
    }

    fun exportPdf(uri: Uri) {
        val tab = activeTab()?.takeIf { it.hasPage && NavigationPolicy.isAllowedTopLevel(it.url) } ?: run {
            showSnackbar("There is no page to export")
            return
        }
        tab.session.saveAsPdf().accept(
            { input ->
                viewModelScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        input?.use { source ->
                            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                                source.copyTo(output)
                            } ?: error("Could not open PDF destination")
                        } ?: error("Could not generate PDF")
                    }
                    withContext(Dispatchers.Main.immediate) {
                        showSnackbar(if (result.isSuccess) "PDF exported" else "Could not export PDF")
                    }
                }
            },
            { showSnackbar("Could not generate PDF") },
        )
    }

    fun printActivePage() {
        val tab = activeTab()?.takeIf { it.hasPage && NavigationPolicy.isAllowedTopLevel(it.url) } ?: run {
            showSnackbar("There is no page to print")
            return
        }
        tab.session.saveAsPdf().accept(
            { input ->
                viewModelScope.launch(Dispatchers.IO) {
                    val file = runCatching {
                        val directory = File(getApplication<Application>().cacheDir, "print").apply { mkdirs() }
                        File(directory, "page-${System.currentTimeMillis()}.pdf").also { target ->
                            input?.use { source -> target.outputStream().use { output -> source.copyTo(output) } }
                                ?: error("Could not generate PDF")
                        }
                    }.getOrNull()
                    withContext(Dispatchers.Main.immediate) {
                        if (file == null) {
                            showSnackbar("Could not prepare page for printing")
                        } else {
                            val printManager = getApplication<Application>().getSystemService(PrintManager::class.java)
                            if (printManager == null) {
                                file.delete()
                                showSnackbar("Printing is unavailable on this device")
                            } else {
                                printManager.print(
                                    tab.title.ifBlank { "Dextra page" },
                                    PagePrintAdapter(file),
                                    PrintAttributes.Builder().build(),
                                )
                            }
                        }
                    }
                }
            },
            { showSnackbar("Could not prepare page for printing") },
        )
    }

    @androidx.annotation.OptIn(markerClass = [org.mozilla.geckoview.ExperimentalGeckoViewApi::class])
    fun exportHtml(uri: Uri) {
        val tab = activeTab()?.takeIf { it.hasPage && NavigationPolicy.isAllowedTopLevel(it.url) } ?: run {
            showSnackbar("There is no page to save")
            return
        }
        tab.session.sessionPageExtractor.getPageContent().accept(
            { html ->
                val content = html.orEmpty()
                if (content.toByteArray(Charsets.UTF_8).size > MAX_HTML_EXPORT_BYTES) {
                    showSnackbar("This page is too large to save as HTML")
                    return@accept
                }
                viewModelScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(content.toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open HTML destination")
                    }
                    withContext(Dispatchers.Main.immediate) {
                        showSnackbar(if (result.isSuccess) "HTML page saved" else "Could not save HTML page")
                    }
                }
            },
            { showSnackbar("Could not extract page HTML") },
        )
    }

    fun saveScreenshot(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val context = getApplication<Application>()
                val fileName = "dextra-${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Dextra")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("Could not create screenshot")
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode screenshot" }
                        } ?: error("Could not open screenshot")
                        context.contentResolver.update(
                            uri,
                            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                            null,
                            null,
                        )
                    } catch (error: Exception) {
                        context.contentResolver.delete(uri, null, null)
                        throw error
                    }
                } else {
                    val directory = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                        "Dextra",
                    ).apply { mkdirs() }
                    File(directory, fileName).outputStream().use { output ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode screenshot" }
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(if (result.isSuccess) "Screenshot saved to Pictures/Dextra" else "Could not save screenshot")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
            showSnackbar("History cleared")
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { backupRepository.export(uri) }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(if (result.isSuccess) "Backup exported" else "Could not export backup")
            }
        }
    }

    fun importBackup(uri: Uri) {
        val profileId = activeProfileId()
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { backupRepository.import(uri, profileId) }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(result.fold({ "Restored ${it} bookmarks" }, { "Could not restore backup" }))
            }
        }
    }

    fun exportSync(uri: Uri, passphrase: String, selection: SyncSelection = SyncSelection()) {
        if (passphrase.length < 8) {
            showSnackbar("Sync passphrase must be at least 8 characters")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { syncRepository.export(uri, _state.value.settings, passphrase, selection) }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(if (result.isSuccess) "Encrypted sync exported" else "Could not export encrypted sync")
            }
        }
    }

    fun importSync(uri: Uri, passphrase: String) {
        if (passphrase.length < 8) {
            showSnackbar("Sync passphrase must be at least 8 characters")
            return
        }
        val profileId = activeProfileId()
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { syncRepository.import(uri, passphrase, profileId) }
            result.onSuccess { data ->
                data.importedSettings?.let { settingsRepository.applySyncSettings(it) }
            }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(result.fold({ "Imported ${it.importedBookmarks} bookmarks and synced settings" }, { "Could not import encrypted sync" }))
            }
        }
    }

    fun previewSync(uri: Uri, passphrase: String) {
        if (passphrase.length < 8) {
            showSnackbar("Sync passphrase must be at least 8 characters")
            return
        }
        val requestId = ++syncPreviewRequestId
        pendingSyncImport = uri to passphrase
        _state.update { it.copy(syncPreview = null, syncPreviewLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { syncRepository.preview(uri, passphrase) }
            withContext(Dispatchers.Main.immediate) {
                if (requestId != syncPreviewRequestId) return@withContext
                _state.update { it.copy(syncPreviewLoading = false) }
                result.onSuccess { preview ->
                    _state.update { it.copy(syncPreview = SyncPreviewState(preview)) }
                }.onFailure {
                    pendingSyncImport = null
                    showSnackbar("Could not decrypt sync preview")
                }
            }
        }
    }

    fun confirmSyncImport(selection: SyncSelection) {
        val pending = pendingSyncImport ?: return
        val profileId = activeProfileId()
        syncPreviewRequestId++
        pendingSyncImport = null
        _state.update { it.copy(syncPreview = null, syncPreviewLoading = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { syncRepository.import(pending.first, pending.second, profileId, selection) }
            result.onSuccess { data ->
                data.importedSettings?.let { settingsRepository.applySyncSettings(it) }
            }
            withContext(Dispatchers.Main.immediate) {
                showSnackbar(result.fold({ "Imported ${it.importedBookmarks} bookmarks and selected sync data" }, { "Could not import encrypted sync" }))
            }
        }
    }

    fun dismissSyncPreview() {
        syncPreviewRequestId++
        pendingSyncImport = null
        _state.update { it.copy(syncPreview = null, syncPreviewLoading = false) }
    }

    fun saveWebDavSettings(
        endpoint: String,
        username: String,
        password: String,
        remoteFile: String,
        passphrase: String,
        intervalHours: Int,
    ) {
        val normalizedEndpoint = endpoint.trim()
        val normalizedRemoteFile = remoteFile.trim()
        if (!normalizedEndpoint.startsWith("https://", ignoreCase = true) ||
            normalizedRemoteFile.isBlank() || normalizedRemoteFile.contains("..") ||
            normalizedRemoteFile.startsWith("/") || normalizedRemoteFile.length > 200 ||
            !normalizedRemoteFile.matches(Regex("[A-Za-z0-9._/-]+"))
        ) {
            showSnackbar("WebDAV server must use HTTPS and the filename must be relative")
            return
        }
        if (passphrase.length < 8) {
            showSnackbar("WebDAV sync passphrase must be at least 8 characters")
            return
        }
        val config = WebDavConfig(
            endpoint = normalizedEndpoint,
            username = username.trim().take(200),
            password = password,
            remoteFile = normalizedRemoteFile,
            passphrase = passphrase,
            intervalHours = intervalHours.coerceIn(1, 168),
            enabled = true,
            conflictPending = false,
            conflictDetectedAt = null,
            pendingResolution = null,
            lastError = null,
        )
        viewModelScope.launch(Dispatchers.IO) {
            webDavStore.save(config)
            WebDavSyncScheduler.schedule(getApplication(), config.intervalHours)
            withContext(Dispatchers.Main.immediate) {
                _state.update { it.copy(webDav = config.toUiState()) }
                showSnackbar("WebDAV sync enabled")
            }
        }
    }

    fun disableWebDavSync() {
        viewModelScope.launch(Dispatchers.IO) {
            webDavStore.clear()
            WebDavSyncScheduler.cancel(getApplication())
            withContext(Dispatchers.Main.immediate) {
                _state.update { it.copy(webDav = WebDavSettingsState()) }
                showSnackbar("WebDAV sync disabled")
            }
        }
    }

    fun runWebDavSyncNow() {
        if (!_state.value.webDav.configured) return
        WebDavSyncScheduler.runNow(getApplication())
        showSnackbar("WebDAV sync queued")
    }

    fun resolveWebDavConflict(resolution: String) {
        if (resolution !in setOf("remote", "local", "merge")) return
        viewModelScope.launch(Dispatchers.IO) {
            val config = webDavStore.load() ?: return@launch
            val updated = config.copy(pendingResolution = resolution)
            webDavStore.save(updated)
            WebDavSyncScheduler.runNow(getApplication())
            withContext(Dispatchers.Main.immediate) {
                _state.update { it.copy(webDav = updated.toUiState()) }
                showSnackbar("WebDAV conflict resolution queued")
            }
        }
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch { dao.deleteHistory(entry.id) }
    }

    fun setOverlay(overlay: BrowserOverlay) {
        _state.update { it.copy(overlay = overlay) }
        if (overlay == BrowserOverlay.SETTINGS) refreshWebDavState()
        if (overlay == BrowserOverlay.PERFORMANCE) refreshPerformanceMetrics()
    }

    fun openPerformanceDashboard() {
        _state.update { it.copy(overlay = BrowserOverlay.PERFORMANCE) }
        refreshPerformanceMetrics()
    }

    fun openNetworkInspector() {
        _state.update { it.copy(overlay = BrowserOverlay.NETWORK) }
    }

    fun clearNetworkActivity() {
        _state.update { it.copy(networkActivity = emptyList()) }
    }

    fun openMediaManager() {
        _state.update { it.copy(overlay = BrowserOverlay.MEDIA) }
    }

    fun openCompatibilityDiagnostics() {
        _state.update { it.copy(overlay = BrowserOverlay.COMPATIBILITY) }
    }

    fun clearCompatibilityEvents() {
        _state.update { it.copy(compatibilityEvents = emptyList()) }
    }

    private fun recordCompatibilityEvent(tabId: String, severity: String, message: String) {
        _state.update { state ->
            state.copy(
                compatibilityEvents = (state.compatibilityEvents + CompatibilityEvent(
                    id = UUID.randomUUID().toString(),
                    tabId = tabId,
                    severity = severity,
                    message = message.take(500),
                    timestamp = System.currentTimeMillis(),
                )).takeLast(MAX_COMPATIBILITY_EVENTS),
            )
        }
    }

    fun controlMedia(tabId: String, action: String) {
        if (_state.value.tabs.none { it.id == tabId }) return
        val media = mediaSessions[tabId] ?: return
        when (action) {
            "play" -> {
                media.play()
                updateTab(tabId) { it.copy(isMediaPlaying = true) }
            }
            "pause" -> {
                media.pause()
                updateTab(tabId) { it.copy(isMediaPlaying = false) }
            }
            "stop" -> {
                media.stop()
                updateTab(tabId) { it.copy(hasActiveMedia = false, isMediaPlaying = false) }
            }
            "back" -> media.seekBackward()
            "forward" -> media.seekForward()
        }
        if (action == "play" || action == "pause") {
            getApplication<DextraApplication>().mediaNotificationController.setPlaying(tabId, action == "play")
        }
    }

    private fun recordNetworkActivity(tabId: String, url: String, kind: String, status: String) {
        if (url.isBlank()) return
        _state.update { state ->
            state.copy(
                networkActivity = (state.networkActivity + NetworkActivity(
                    id = UUID.randomUUID().toString(),
                    tabId = tabId,
                    url = url.take(2_000),
                    kind = kind,
                    status = status.take(120),
                    secure = url.startsWith("https://", ignoreCase = true),
                    timestamp = System.currentTimeMillis(),
                )).takeLast(MAX_NETWORK_ACTIVITY),
            )
        }
    }

    fun markFirstFrame() {
        if (!firstFrameRecorded) {
            firstFrameRecorded = true
            val startupMs = (SystemClock.elapsedRealtime() - getApplication<DextraApplication>().processStartedAtElapsed).coerceAtLeast(0L)
            _state.update { it.copy(performance = it.performance.copy(startupMs = startupMs)) }
        }
        refreshPerformanceMetrics()
    }

    fun refreshPerformanceMetrics() {
        val frameMetrics = performanceMonitor.snapshot()
        val context = getApplication<Application>()
        val configuration = context.resources.configuration
        viewModelScope.launch(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)
            val performance = _state.value.performance.copy(
                processPssMb = (Debug.getPss() / 1024L).toInt(),
                availableMemoryMb = (memoryInfo.availMem / (1024L * 1024L)).toInt(),
                memoryLow = memoryInfo.lowMemory,
                frameCount = frameMetrics.frameCount,
                jankCount = frameMetrics.jankCount,
                averageFrameTimeMs = frameMetrics.averageFrameTimeMs,
                windowWidthDp = configuration.screenWidthDp,
                windowHeightDp = configuration.screenHeightDp,
                isDexLikeWindow = configuration.screenWidthDp >= 600 || configuration.smallestScreenWidthDp >= 600,
                lastUpdatedAt = System.currentTimeMillis(),
            )
            withContext(Dispatchers.Main.immediate) {
                _state.update { it.copy(performance = performance) }
            }
        }
    }

    fun copyPerformanceReport() {
        val metrics = _state.value.performance
        val report = buildString {
            appendLine("Dextra performance report")
            appendLine("Time: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}")
            appendLine("Startup: ${metrics.startupMs?.let { "$it ms" } ?: "not measured"}")
            appendLine("Process PSS: ${metrics.processPssMb} MB")
            appendLine("Available memory: ${metrics.availableMemoryMb} MB")
            appendLine("Low memory: ${metrics.memoryLow}")
            appendLine("Frames: ${metrics.frameCount}")
            appendLine("Janky frames: ${metrics.jankCount}")
            appendLine("Average frame time: ${metrics.averageFrameTimeMs} ms")
            appendLine("Window: ${metrics.windowWidthDp}x${metrics.windowHeightDp} dp")
            appendLine("DeX-like window: ${metrics.isDexLikeWindow}")
        }
        copyToClipboard("Performance report", report)
    }

    private fun refreshWebDavState() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = webDavStore.load() ?: return@launch
            withContext(Dispatchers.Main.immediate) {
                _state.update { it.copy(webDav = config.toUiState()) }
            }
        }
    }

    fun dismissOverlay() {
        _state.update { it.copy(overlay = BrowserOverlay.NONE, securityDiagnostics = null) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccessibilityTextScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setAccessibilityTextScale(scale) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHighContrast(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceMotion(enabled) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { settingsRepository.setSearchEngine(engine) }
    }

    fun setCustomSearchEngine(engine: CustomSearchEngine) {
        viewModelScope.launch { settingsRepository.setCustomSearchEngine(engine) }
    }

    fun addCustomSearchEngine(label: String, template: String) {
        val normalizedLabel = label.trim().take(40)
        val normalizedTemplate = template.trim()
        if (normalizedLabel.isBlank()) {
            showSnackbar("Enter a name for the search engine")
            return
        }
        if (!normalizedTemplate.startsWith("https://", ignoreCase = true) || !normalizedTemplate.contains("%s")) {
            showSnackbar("Search URL must use HTTPS and contain %s")
            return
        }
        if (normalizedTemplate.length > 500) {
            showSnackbar("Search URL is too long")
            return
        }
        setCustomSearchEngine(CustomSearchEngine(label = normalizedLabel, searchUrl = normalizedTemplate))
    }

    fun removeCustomSearchEngine(engine: CustomSearchEngine) {
        viewModelScope.launch { settingsRepository.removeCustomSearchEngine(engine.id) }
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
        if (!normalized.startsWith("https://", ignoreCase = true)) {
            showSnackbar("Filter lists must use HTTPS")
            return
        }
        viewModelScope.launch { settingsRepository.addAdBlockFilter(normalized) }
    }

    fun removeAdBlockFilter(filter: AdBlockFilter) {
        viewModelScope.launch { settingsRepository.removeAdBlockFilter(filter.url) }
    }

    fun addUserScript(url: String) {
        val normalized = url.trim()
        if (!normalized.startsWith("https://", ignoreCase = true)) {
            showSnackbar("Userscripts must use HTTPS")
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

    fun revokeExtensionOptionalPermissions(id: String) {
        val extension = installedExtensionObjects[id] ?: return
        val metadata = extension.metaData
        if (metadata.grantedOptionalPermissions.isEmpty() &&
            metadata.grantedOptionalOrigins.isEmpty() &&
            metadata.grantedOptionalDataCollectionPermissions.isEmpty()
        ) return
        runtime.webExtensionController.removeOptionalPermissions(
            extension.id,
            metadata.grantedOptionalPermissions,
            metadata.grantedOptionalOrigins,
            metadata.grantedOptionalDataCollectionPermissions,
        ).accept(
            { refreshInstalledExtensions(); showSnackbar("Optional extension permissions revoked") },
            { showSnackbar("Could not revoke optional extension permissions") },
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

    fun setHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHttpsOnly(enabled) }
    }

    fun setCookieBannerMode(mode: Int) {
        val normalized = mode.coerceIn(0, 2)
        viewModelScope.launch {
            settingsRepository.setCookieBannerMode(normalized)
            applyCookieBannerMode(normalized)
        }
    }

    fun setHistoryRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setHistoryRetentionDays(days) }
    }

    fun setDownloadRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setDownloadRetentionDays(days) }
    }

    fun setRecoveryRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setRecoveryRetentionDays(days) }
    }

    fun setClearSiteDataOnExit(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setClearSiteDataOnExit(enabled) }
    }

    fun addPrivacyCleanupAllowlist(origin: String) {
        val normalized = NavigationPolicy.origin(origin.trim()) ?: run {
            showSnackbar("Allowlist entries must be valid HTTP or HTTPS origins")
            return
        }
        viewModelScope.launch { settingsRepository.addPrivacyCleanupAllowlist(normalized) }
    }

    fun removePrivacyCleanupAllowlist(origin: String) {
        viewModelScope.launch { settingsRepository.removePrivacyCleanupAllowlist(origin) }
    }

    fun runPrivacyCleanupNow() {
        PrivacyCleanupScheduler.runNow(getApplication())
        showSnackbar("Privacy cleanup queued")
    }

    fun setHomepage(value: String) {
        val homepage = BrowserUrl.resolve(value, searchUrl(_state.value.settings))
        val scheme = runCatching { Uri.parse(homepage).scheme?.lowercase() }.getOrNull()
        if (scheme !in setOf("http", "https", "about")) {
            showSnackbar("Homepage must be a valid web address")
            return
        }
        viewModelScope.launch { settingsRepository.setHomepage(homepage) }
    }

    fun setStartPageQuickLinks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStartPageSettings(_state.value.settings.startPage.copy(showQuickLinks = enabled))
        }
    }

    fun setStartPagePrivacyTip(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStartPageSettings(_state.value.settings.startPage.copy(showPrivacyTip = enabled))
        }
    }

    fun addStartPageLink(label: String, url: String) {
        val normalizedLabel = label.trim().take(40)
        val normalizedUrl = url.trim()
        if (normalizedLabel.isBlank() || !normalizedUrl.startsWith("https://", ignoreCase = true) || normalizedUrl.length > 500) {
            showSnackbar("Start-page links must use HTTPS and include a name")
            return
        }
        viewModelScope.launch {
            val current = _state.value.settings.startPage
            val link = StartPageLink(label = normalizedLabel, url = normalizedUrl)
            settingsRepository.setStartPageSettings(current.copy(customLinks = (current.customLinks + link).takeLast(12)))
        }
    }

    fun removeStartPageLink(link: StartPageLink) {
        viewModelScope.launch {
            val current = _state.value.settings.startPage
            settingsRepository.setStartPageSettings(current.copy(customLinks = current.customLinks.filterNot { it.id == link.id }))
        }
    }

    fun setTabBarWithAddressBar(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTabBarWithAddressBar(enabled) }
    }

    fun setVerticalTabs(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVerticalTabs(enabled) }
    }

    fun setKeyboardShortcut(command: BrowserCommandId, chord: KeyChord?): Boolean {
        if (chord != null) {
            if (!chord.ctrl && !chord.alt && !chord.meta) {
                showSnackbar("Use Ctrl, Alt, or Meta for a browser shortcut")
                return false
            }
            val conflict = _state.value.settings.shortcutBindings.entries.firstOrNull {
                it.key != command && it.value == chord
            }
            if (conflict != null) {
                showSnackbar("Shortcut already used by ${BrowserCommands.all.firstOrNull { it.id == conflict.key }?.title ?: conflict.key.name}")
                return false
            }
        }
        viewModelScope.launch { settingsRepository.setShortcut(command, chord) }
        return true
    }

    fun beginKeyboardShortcutCapture(command: BrowserCommandId) {
        _state.update { it.copy(capturingShortcut = command) }
    }

    fun cancelKeyboardShortcutCapture() {
        _state.update { it.copy(capturingShortcut = null) }
    }

    fun setDnsOverHttpsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDnsOverHttpsEnabled(enabled) }
    }

    fun setDnsProvider(provider: DnsProvider) {
        viewModelScope.launch { settingsRepository.setDnsProvider(provider) }
    }

    fun resolveContentPermission(allow: Boolean, remember: Boolean = false) {
        val prompt = _state.value.contentPermission ?: return
        prompt.result.complete(
            if (allow) GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
            else GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY,
        )
        if (remember) {
            val profileId = activeProfileId()
            viewModelScope.launch {
                _state.value.tabs.firstOrNull { it.id == prompt.tabId }
                    ?.takeIf { !it.isPrivate }
                    ?.let {
                        dao.upsertSitePermission(
                            SitePermission(
                                origin = prompt.origin,
                                permission = prompt.permission.toString(),
                                decision = if (allow) "allow" else "block",
                                updatedAt = System.currentTimeMillis(),
                                profileId = profileId,
                            ),
                        )
                    }
            }
        }
        showNextContentPermission()
    }

    fun resolveAndroidPermission(granted: Boolean) {
        val prompt = _state.value.androidPermission ?: return
        if (granted) prompt.callback.grant() else prompt.callback.reject()
        _state.update { it.copy(androidPermission = queuedAndroidPermissions.removeFirstOrNull()) }
    }

    fun resolveMediaPermission(granted: Boolean) {
        val prompt = _state.value.mediaPermission ?: return
        if (granted) {
            prompt.callback.grant(prompt.video.firstOrNull()?.id, prompt.audio.firstOrNull()?.id)
        } else {
            prompt.callback.reject()
        }
        _state.update { it.copy(mediaPermission = queuedMediaPermissions.removeFirstOrNull()) }
    }

    fun clearSitePermissions() {
        runtime.storageController.clearData(StorageController.ClearFlags.PERMISSIONS).accept(
            {
                viewModelScope.launch {
                    dao.clearSitePermissions()
                    _state.update { it.copy(securityDiagnostics = it.securityDiagnostics?.copy(permissions = emptyList())) }
                    showSnackbar("Site permissions cleared")
                }
            },
            { showSnackbar("Could not clear site permissions") },
        )
    }

    fun setSitePermission(origin: String, permission: String, decision: String) {
        if (!NavigationPolicy.isWebUrl(origin) || permission.isBlank()) return
        if (decision !in setOf("allow", "block", "ask")) return
        val profileId = activeProfileId()
        viewModelScope.launch {
            if (decision == "ask") {
                dao.deleteSitePermission(profileId, origin, permission)
            } else {
                dao.upsertSitePermission(
                    SitePermission(
                        origin = origin,
                        permission = permission,
                        decision = decision,
                        updatedAt = System.currentTimeMillis(),
                        profileId = profileId,
                    ),
                )
            }
            _state.value.securityDiagnostics?.takeIf { it.origin == origin }?.let { diagnostics ->
                _state.update { it.copy(securityDiagnostics = diagnostics.copy(permissions = dao.getSitePermissions(profileId, origin))) }
            }
        }
    }

    fun openSecurityDiagnostics() {
        val tab = activeTab() ?: run {
            showSnackbar("There is no active page")
            return
        }
        val origin = NavigationPolicy.origin(tab.url)
        val uri = runCatching { Uri.parse(tab.url) }.getOrNull()
        val base = SecurityDiagnostics(
            tabId = tab.id,
            url = tab.url,
            origin = origin,
            host = uri?.host,
            port = uri?.port?.takeIf { it > 0 } ?: if (tab.url.startsWith("https://")) 443 else null,
            isSecure = tab.isSecure,
            isLoading = tab.url.startsWith("https://", ignoreCase = true),
            blockedRequests = origin?.let { adBlockStats.byOrigin[it] } ?: 0,
            dnsOverHttpsEnabled = _state.value.settings.dnsOverHttpsEnabled,
            dnsProvider = _state.value.settings.dnsProvider.label,
            error = if (!NavigationPolicy.isWebUrl(tab.url)) "Security information is unavailable for this page" else null,
        )
        _state.update { it.copy(securityDiagnostics = base, overlay = BrowserOverlay.SECURITY) }
        if (origin == null || !NavigationPolicy.isWebUrl(tab.url)) return
        val profileId = activeProfileId()
        viewModelScope.launch(Dispatchers.IO) {
            val certificate = if (tab.url.startsWith("https://", ignoreCase = true)) {
                runCatching { fetchCertificate(tab.url) }.getOrNull()
            } else null
            val error = if (tab.url.startsWith("https://", ignoreCase = true) && certificate == null) {
                "The certificate could not be inspected from the current network"
            } else null
            val permissions = dao.getSitePermissions(profileId, origin)
            val setting = dao.getSiteSetting(profileId, origin)
            withContext(Dispatchers.Main.immediate) {
                if (_state.value.securityDiagnostics?.tabId == tab.id) {
                    _state.update {
                        it.copy(
                            securityDiagnostics = base.copy(
                                certificate = certificate,
                                isLoading = false,
                                permissions = permissions,
                                siteSetting = setting,
                                error = error,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun clearSiteData(origin: String) {
        val host = runCatching { Uri.parse(origin).host?.takeIf(String::isNotBlank) }.getOrNull() ?: run {
            showSnackbar("This site origin is invalid")
            return
        }
        runtime.storageController.clearDataFromHost(host, StorageController.ClearFlags.SITE_DATA).accept(
            {
                viewModelScope.launch {
                    dao.deleteSitePermissions(activeProfileId(), origin)
                    dao.deleteSiteSetting(activeProfileId(), origin)
                    removeSiteCookieBannerMode(origin)
                    _state.value.tabs
                        .filter { NavigationPolicy.origin(it.url) == origin }
                        .forEach { tab ->
                            pageZoomByTab.remove(tab.id)
                            if (tab.hasPage) tab.session.reload()
                        }
                    syncActiveTabZoom()
                    syncAdBlockSettings(_state.value.settings)
                    syncUserScripts(_state.value.settings)
                    if (currentSiteOrigin() == origin) {
                        _state.update { it.copy(siteSetting = null) }
                        _state.value.activeTabId?.let(::refreshSiteSetting)
                    }
                    _state.value.securityDiagnostics?.takeIf { it.origin == origin }?.let { diagnostics ->
                        _state.update { it.copy(securityDiagnostics = diagnostics.copy(permissions = emptyList(), siteSetting = null)) }
                    }
                    showSnackbar("Cleared data for $origin")
                }
            },
            { showSnackbar("Could not clear data for $origin") },
        )
    }

    fun forgetSite(origin: String) {
        val host = runCatching { Uri.parse(origin).host?.takeIf(String::isNotBlank) }.getOrNull() ?: run {
            showSnackbar("This site origin is invalid")
            return
        }
        val blockedForOrigin = adBlockStats.byOrigin[origin] ?: 0
        adBlockStats = adBlockStats.copy(
            totalBlocked = (adBlockStats.totalBlocked - blockedForOrigin).coerceAtLeast(0),
            byOrigin = adBlockStats.byOrigin - origin,
        )
        _state.value.tabs
            .filter { NavigationPolicy.origin(it.url) == origin }
            .forEach { trackerBlockedByTab.remove(it.id) }
        rebuildBlockerStats()
        runtime.storageController.clearDataFromHost(
            host,
            StorageController.ClearFlags.SITE_DATA or StorageController.ClearFlags.PERMISSIONS,
        ).accept(
            {
                viewModelScope.launch(Dispatchers.IO) {
                    val historyToDelete = dao.getHistory().filter { NavigationPolicy.origin(it.url) == origin }
                    historyToDelete.forEach { dao.deleteHistory(it.id) }
                    dao.getReadingList()
                        .filter { NavigationPolicy.origin(it.url) == origin }
                        .forEach { entry ->
                            entry.offlinePath?.let { path -> runCatching { File(path).delete() } }
                            dao.deleteReadingListEntry(entry.url)
                        }
                    dao.getDownloads()
                        .filter { NavigationPolicy.origin(it.url) == origin }
                        .forEach { download ->
                            cleanupDownloadFiles(download)
                            download.localUri?.let { uri ->
                                runCatching { getApplication<Application>().contentResolver.delete(Uri.parse(uri), null, null) }
                            }
                            dao.deleteDownload(download.downloadId)
                        }
                    credentialVault.deleteOrigin(origin)
                    dao.deleteSitePermissions(activeProfileId(), origin)
                    dao.deleteSiteSetting(activeProfileId(), origin)
                    removeSiteCookieBannerMode(origin)
                    withContext(Dispatchers.Main.immediate) {
                        _state.update { state ->
                            state.copy(siteSetting = if (currentSiteOrigin() == origin) null else state.siteSetting)
                        }
                        _state.value.securityDiagnostics?.takeIf { it.origin == origin }?.let { diagnostics ->
                            _state.update { it.copy(securityDiagnostics = diagnostics.copy(permissions = emptyList(), siteSetting = null)) }
                        }
                        _state.value.tabs
                            .filter { NavigationPolicy.origin(it.url) == origin && it.hasPage }
                            .forEach { it.session.reload() }
                        syncAdBlockSettings(_state.value.settings)
                        syncUserScripts(_state.value.settings)
                        showSnackbar("Forgot $origin")
                    }
                }
            },
            { showSnackbar("Could not forget $origin") },
        )
    }

    fun clearAllSiteData() {
        runtime.storageController.clearData(StorageController.ClearFlags.SITE_DATA).accept(
            {
                viewModelScope.launch {
                    dao.getSiteSettings().forEach { setting -> removeSiteCookieBannerMode(setting.origin) }
                    dao.clearSitePermissions()
                    dao.clearSiteSettings()
                    adBlockStats = BlockerStats()
                    trackerBlockedByTab.clear()
                    pageZoomByTab.clear()
                    _state.update { it.copy(siteSetting = null, blockerStats = BlockerStats()) }
                    _state.update { it.copy(securityDiagnostics = it.securityDiagnostics?.copy(permissions = emptyList(), siteSetting = null)) }
                    syncActiveTabZoom()
                    syncAdBlockSettings(_state.value.settings)
                    syncUserScripts(_state.value.settings)
                    _state.value.tabs.filter { it.hasPage }.forEach { it.session.reload() }
                    showSnackbar("Cleared site data")
                }
            },
            { showSnackbar("Could not clear site data") },
        )
    }

    private fun showNextContentPermission() {
        _state.update { it.copy(contentPermission = queuedContentPermissions.removeFirstOrNull()) }
    }

    private fun enqueueAndroidPermission(prompt: AndroidPermissionPrompt) {
        if (_state.value.androidPermission == null) {
            _state.update { it.copy(androidPermission = prompt) }
        } else {
            queuedAndroidPermissions.addLast(prompt)
        }
    }

    private fun enqueueMediaPermission(prompt: MediaPermissionPrompt) {
        if (_state.value.mediaPermission == null) {
            _state.update { it.copy(mediaPermission = prompt) }
        } else {
            queuedMediaPermissions.addLast(prompt)
        }
    }

    private fun enqueueContentPermission(prompt: ContentPermissionPrompt) {
        if (_state.value.contentPermission == null) {
            _state.update { it.copy(contentPermission = prompt) }
        } else {
            queuedContentPermissions.addLast(prompt)
        }
    }

    private fun rejectPermissionsForTab(tabId: String) {
        val current = _state.value.contentPermission
        val currentAndroid = _state.value.androidPermission
        val currentMedia = _state.value.mediaPermission
        if (current?.tabId == tabId) {
            current.result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }
        if (currentAndroid?.tabId == tabId) currentAndroid.callback.reject()
        if (currentMedia?.tabId == tabId) currentMedia.callback.reject()
        queuedContentPermissions.removeAll { prompt ->
            if (prompt.tabId == tabId) {
                prompt.result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                true
            } else {
                false
            }
        }
        queuedAndroidPermissions.removeAll { prompt ->
            if (prompt.tabId == tabId) {
                prompt.callback.reject()
                true
            } else {
                false
            }
        }
        queuedMediaPermissions.removeAll { prompt ->
            if (prompt.tabId == tabId) {
                prompt.callback.reject()
                true
            } else {
                false
            }
        }
        if (current?.tabId == tabId) showNextContentPermission()
        if (currentAndroid?.tabId == tabId) {
            _state.update { it.copy(androidPermission = queuedAndroidPermissions.removeFirstOrNull()) }
        }
        if (currentMedia?.tabId == tabId) {
            _state.update { it.copy(mediaPermission = queuedMediaPermissions.removeFirstOrNull()) }
        }
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
        WorkManager.getInstance(getApplication()).cancelUniqueWork(downloadWorkName(download.downloadId))
        cleanupDownloadFiles(download)
        viewModelScope.launch {
            dao.getDownload(download.downloadId)?.let {
                dao.upsertDownload(it.copy(status = DownloadStatus.CANCELED.label, reason = "Canceled by user"))
            }
        }
    }

    fun toggleDownload(download: DownloadEntry) {
        when (download.status) {
            DownloadStatus.QUEUED.label,
            DownloadStatus.DOWNLOADING.label,
            -> {
                WorkManager.getInstance(getApplication()).cancelUniqueWork(downloadWorkName(download.downloadId))
                viewModelScope.launch {
                    dao.getDownload(download.downloadId)?.let {
                        dao.upsertDownload(it.copy(status = DownloadStatus.PAUSED.label, reason = "Paused by user"))
                    }
                }
            }
            DownloadStatus.PAUSED.label -> scheduleDownload(download)
            DownloadStatus.FAILED.label -> retryDownload(download)
            else -> Unit
        }
    }

    fun setDownloadPriority(download: DownloadEntry, priority: Int) {
        viewModelScope.launch {
            dao.getDownload(download.downloadId)?.let {
                val updated = it.copy(priority = priority.coerceIn(0, 2))
                dao.upsertDownload(updated)
                if (updated.status == DownloadStatus.QUEUED.label) scheduleDownload(updated)
            }
        }
    }

    fun setDownloadWifiOnly(download: DownloadEntry, wifiOnly: Boolean) {
        viewModelScope.launch {
            dao.getDownload(download.downloadId)?.let {
                val updated = it.copy(wifiOnly = wifiOnly)
                dao.upsertDownload(updated)
                if (updated.status == DownloadStatus.QUEUED.label) scheduleDownload(updated)
            }
        }
    }

    fun scheduleDownload(download: DownloadEntry, scheduledAt: Long?) {
        if (download.status == DownloadStatus.COMPLETE.label || download.status == DownloadStatus.CANCELED.label) return
        WorkManager.getInstance(getApplication()).cancelUniqueWork(downloadWorkName(download.downloadId))
        viewModelScope.launch {
            dao.getDownload(download.downloadId)?.let {
                val updated = it.copy(
                    status = DownloadStatus.QUEUED.label,
                    scheduledAt = scheduledAt?.takeIf { time -> time > System.currentTimeMillis() },
                    reason = null,
                )
                dao.upsertDownload(updated)
                scheduleDownload(updated)
            }
        }
    }

    fun pauseAllDownloads() {
        viewModelScope.launch {
            dao.getDownloads()
                .filter { it.workspaceId == activeProfileId() && it.status in setOf(DownloadStatus.QUEUED.label, DownloadStatus.DOWNLOADING.label) }
                .forEach { download ->
                    WorkManager.getInstance(getApplication()).cancelUniqueWork(downloadWorkName(download.downloadId))
                    dao.upsertDownload(download.copy(status = DownloadStatus.PAUSED.label, reason = "Paused by user"))
                }
        }
    }

    fun resumeAllDownloads() {
        viewModelScope.launch {
            dao.getDownloads()
                .filter { it.workspaceId == activeProfileId() && it.status == DownloadStatus.PAUSED.label }
                .sortedWith(compareByDescending<DownloadEntry> { it.priority }.thenBy { it.createdAt })
                .forEach { download ->
                    val resumed = download.copy(status = DownloadStatus.QUEUED.label, reason = null)
                    dao.upsertDownload(resumed)
                    scheduleDownload(resumed)
                }
        }
    }

    fun retryDownload(download: DownloadEntry) {
        if (download.status != DownloadStatus.FAILED.label) return
        viewModelScope.launch {
            dao.getDownload(download.downloadId)?.let {
                val retry = it.copy(status = DownloadStatus.QUEUED.label, reason = null)
                dao.upsertDownload(retry)
                scheduleDownload(retry)
            }
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            dao.getDownloads()
                .filter { it.workspaceId == activeProfileId() }
                .filter { it.status in setOf(DownloadStatus.COMPLETE.label, DownloadStatus.CANCELED.label) }
                .forEach { download ->
                    download.localUri?.let { uri -> runCatching { getApplication<Application>().contentResolver.delete(Uri.parse(uri), null, null) } }
                    cleanupDownloadFiles(download)
                    dao.deleteDownload(download.downloadId)
                }
            showSnackbar("Download history cleared")
        }
    }

    fun removeDownload(download: DownloadEntry) {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(downloadWorkName(download.downloadId))
        cleanupDownloadFiles(download)
        download.localUri?.let { uri -> runCatching { getApplication<Application>().contentResolver.delete(Uri.parse(uri), null, null) } }
        viewModelScope.launch { dao.deleteDownload(download.downloadId) }
    }

    private fun createSession(
        tabId: String,
        privateMode: Boolean,
        openSession: Boolean = true,
        extensionPopup: Boolean = false,
        savedSessionState: String? = null,
    ): GeckoSession {
        val settings = _state.value.settings
        // Extension popups use a device-width viewport so their CSS is not scaled down from 980px.
        val desktopViewport = settings.desktopSites && !extensionPopup
        val sessionSettings = GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .allowJavascript(true)
                .useTrackingProtection(true)
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
                .displayMode(
                    if (_state.value.standalonePwa) {
                        GeckoSessionSettings.DISPLAY_MODE_STANDALONE
                    } else {
                        GeckoSessionSettings.DISPLAY_MODE_BROWSER
                    },
                )
        if (!privateMode && !extensionPopup) {
            activeContextId()?.let(sessionSettings::contextId)
        }
        val session = GeckoSession(sessionSettings.build())
        savedSessionState?.let { serialized ->
            runCatching { GeckoSession.SessionState.fromString(serialized) }
                .getOrNull()
                ?.let(session::restoreState)
        }
        session.setNavigationDelegate(NavigationDelegate(tabId))
        session.setHistoryDelegate(HistoryDelegate(tabId))
        session.setProgressDelegate(ProgressDelegate(tabId))
        session.setContentDelegate(ContentDelegate(tabId))
        session.setPermissionDelegate(PermissionDelegate(tabId))
        session.setPromptDelegate(WebAuthnPromptDelegate(tabId))
        session.setTranslationsSessionDelegate(object : TranslationsController.SessionTranslation.Delegate {
            override fun onTranslationStateChange(
                session: GeckoSession,
                state: TranslationsController.SessionTranslation.TranslationState?,
            ) {
                state ?: return
                val detected = state.detectedLanguages?.docLangTag?.takeIf(String::isNotBlank)
                detected?.let { translationDetectedByTab[tabId] = it }
                _state.update { current ->
                    if (current.translation?.tabId != tabId) current else current.copy(
                        translation = current.translation.copy(
                            sourceLanguage = state.requestedTranslationPair?.fromLanguage ?: detected,
                            targetLanguage = state.requestedTranslationPair?.toLanguage ?: current.translation.targetLanguage,
                            detectedLanguage = detected,
                            isTranslated = state.hasVisibleChange == true,
                            isTranslating = false,
                            error = state.error,
                        ),
                    )
                }
            }
        })
        session.setMediaSessionDelegate(object : MediaSession.Delegate {
            override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
                mediaSessions[tabId] = mediaSession
                val tab = _state.value.tabs.firstOrNull { it.id == tabId }
                updateTab(tabId) {
                    if (it.isAudioMuted) runCatching { mediaSession.muteAudio(true) }
                    it.copy(hasActiveMedia = true, isMediaPlaying = true)
                }
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.activate(
                    tabId = tabId,
                    session = mediaSession,
                    title = tab?.title.orEmpty(),
                    artist = null,
                    album = null,
                    privateTab = tab?.isPrivate == true,
                )
                if (tab?.isPrivate != true) showSnackbar("Media controls enabled")
            }

            override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
                if (mediaSessions[tabId] === mediaSession) mediaSessions.remove(tabId)
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.clear(tabId)
                updateTab(tabId) { it.copy(hasActiveMedia = false, isMediaPlaying = false) }
            }

            override fun onMetadata(session: GeckoSession, mediaSession: MediaSession, metadata: MediaSession.Metadata) {
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.updateMetadata(
                    tabId,
                    metadata.title.orEmpty(),
                    metadata.artist,
                    metadata.album,
                )
            }

            override fun onPositionState(session: GeckoSession, mediaSession: MediaSession, positionState: MediaSession.PositionState) {
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.updatePosition(
                    tabId,
                    positionState.duration,
                    positionState.position,
                    positionState.playbackRate,
                )
            }

            override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
                updateTab(tabId) { it.copy(hasActiveMedia = true, isMediaPlaying = true) }
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.setPlaying(tabId, true)
            }

            override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
                updateTab(tabId) { it.copy(isMediaPlaying = false) }
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.setPlaying(tabId, false)
            }

            override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
                updateTab(tabId) { it.copy(hasActiveMedia = false, isMediaPlaying = false) }
                getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.clear(tabId)
            }
        })
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
        recentlyClosedTabs.addFirst(ClosedTabEntry(tab.url, tab.title, tab.isPrivate, tab.pinned, tab.groupId, tab.sessionState))
        while (recentlyClosedTabs.size > 10) recentlyClosedTabs.removeLast()
        _state.update { it.copy(closedTabCount = recentlyClosedTabs.size) }
        persistRecentlyClosedTabs()
    }

    private fun restoreRecentlyClosedTabs(savedTabs: List<SavedTab>) {
        if (restoredClosedTabs) return
        restoredClosedTabs = true
        savedTabs.asReversed().forEach { saved ->
            recentlyClosedTabs.addFirst(
                ClosedTabEntry(
                    url = saved.url,
                    title = saved.title.orEmpty(),
                    isPrivate = false,
                    pinned = saved.pinned,
                    groupId = saved.groupId,
                    sessionState = saved.sessionState,
                ),
            )
        }
        while (recentlyClosedTabs.size > 10) recentlyClosedTabs.removeLast()
        _state.update { it.copy(closedTabCount = recentlyClosedTabs.size) }
    }

    private fun persistRecentlyClosedTabs() {
        if (!restoredClosedTabs) return
        viewModelScope.launch {
            settingsRepository.saveRecentlyClosedTabs(
                recentlyClosedTabs.filterNot { it.isPrivate }.map { closed ->
                    SavedTab(
                        url = closed.url,
                        isPrivate = false,
                        pinned = closed.pinned,
                        groupId = closed.groupId,
                        title = closed.title,
                        sessionState = closed.sessionState,
                    )
                },
            )
        }
    }

    private fun restoreSavedTabs(savedTabs: List<SavedTab>, activeIndex: Int) {
        val tabsToRestore = savedTabs
            .filter { !it.isPrivate && NavigationPolicy.isWebUrl(it.url) }
            .take(MAX_OPEN_TABS)
        if (tabsToRestore.isEmpty()) return

        val current = _state.value
        setExtensionTabActive(current.activeTabId, false)
        current.tabs.forEach { tab -> runCatching { tab.session.close() } }
        val restoredIds = mutableSetOf<String>()
        val restored = tabsToRestore.map { saved ->
            val id = saved.id?.takeIf(restoredIds::add) ?: UUID.randomUUID().toString().also(restoredIds::add)
            val validSessionState = saved.sessionState?.takeIf {
                runCatching { GeckoSession.SessionState.fromString(it) }.getOrNull() != null
            }
            val session = createSession(id, saved.isPrivate, savedSessionState = validSessionState)
            BrowserTabState(
                id = id,
                session = session,
                title = saved.title?.ifBlank { null } ?: "Loading...",
                url = saved.url,
                isLoading = validSessionState == null,
                isSecure = saved.url.startsWith("https://"),
                hasPage = true,
                isPrivate = saved.isPrivate,
                pinned = saved.pinned,
                groupId = saved.groupId?.takeIf { groupId -> current.settings.tabGroups.any { it.id == groupId } },
                sessionState = validSessionState,
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
        restored.filter { it.sessionState == null }.forEach { tab -> tab.session.loadUri(tab.url) }
    }

    private fun restoreSyncedSession(settings: BrowserSettings) {
        _state.value.tabs.forEach { tab -> runCatching { tab.session.close() } }
        _state.update {
            it.copy(
                tabs = emptyList(),
                activeTabId = null,
                splitPrimaryTabId = null,
                splitSecondaryTabId = null,
                splitPaneFocused = false,
                overlay = BrowserOverlay.NONE,
            )
        }
        restoreSavedTabs(settings.openTabs, settings.activeTabIndex)
        if (_state.value.tabs.isEmpty()) createTab()
        showSnackbar("Restored open tabs from sync")
    }

    private fun persistOpenTabs(immediate: Boolean = false) {
        if (!restoredSavedTabs || _state.value.standalonePwa || _state.value.standaloneWindow) return
        val current = _state.value
        val pageTabs = savedTabsFromState()
        val activeIndex = pageTabs.indexOfFirst { it.id == current.activeTabId }
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            if (!immediate) delay(250)
            settingsRepository.saveOpenTabs(
                tabs = pageTabs.map {
                    SavedTab(
                        url = it.url,
                        isPrivate = it.isPrivate,
                        pinned = it.pinned,
                        groupId = it.groupId,
                        id = it.id,
                        title = it.title,
                        sessionState = it.sessionState?.takeIf { state -> state.toByteArray().size <= MAX_SESSION_STATE_BYTES },
                    )
                },
                activeTabIndex = activeIndex.coerceAtLeast(0),
                groups = current.settings.tabGroups,
                workspaceList = workspaceSnapshots(current),
            )
        }
    }

    private fun recordSessionRecovery() {
        if (!restoredSavedTabs || _state.value.standalonePwa || _state.value.standaloneWindow) return
        val tabs = savedTabsFromState()
        if (tabs.isEmpty()) return
        val snapshot = SessionSnapshot(
            id = "recovery-${System.currentTimeMillis()}",
            title = "Automatic recovery",
            createdAt = System.currentTimeMillis(),
            tabs = tabs,
            activeTabIndex = tabs.indexOfFirst { it.id == _state.value.activeTabId }.coerceAtLeast(0),
            tabGroups = _state.value.settings.tabGroups,
        )
        viewModelScope.launch { settingsRepository.saveSessionTimeline(snapshot) }
    }

    private fun workspaceSnapshots(state: BrowserUiState): List<TabWorkspace> {
        val stored = state.settings.workspaces
        if (stored.isNotEmpty()) return stored
        val now = System.currentTimeMillis()
        return listOf(
            TabWorkspace(
                id = state.settings.activeWorkspaceId.ifBlank { DEFAULT_WORKSPACE_ID },
                title = "Personal",
                createdAt = now,
                lastUsedAt = now,
                tabs = savedTabsFromState(),
                activeTabIndex = state.tabs.indexOfFirst { it.id == state.activeTabId }.coerceAtLeast(0),
                tabGroups = state.settings.tabGroups,
            ),
        )
    }

    private fun saveAndActivateWorkspace(workspaces: List<TabWorkspace>, target: TabWorkspace) {
        val current = _state.value
        val activatedTarget = target.copy(lastUsedAt = System.currentTimeMillis())
        val currentWorkspace = workspaceSnapshots(current).firstOrNull { it.id == current.settings.activeWorkspaceId }
            ?: workspaceSnapshots(current).first()
        val currentTabs = savedTabsFromState()
        val currentIndex = currentTabs.indexOfFirst { it.id == current.activeTabId }.coerceAtLeast(0)
        val updated = workspaces.map { workspace ->
            when (workspace.id) {
                currentWorkspace.id -> currentWorkspace.copy(
                    tabs = currentTabs,
                    activeTabIndex = currentIndex,
                    tabGroups = current.settings.tabGroups,
                    lastUsedAt = System.currentTimeMillis(),
                )
                activatedTarget.id -> activatedTarget
                else -> workspace
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveWorkspaceState(updated, activatedTarget.id, activatedTarget.tabs, activatedTarget.activeTabIndex, activatedTarget.tabGroups)
            withContext(Dispatchers.Main.immediate) {
                val state = _state.value
                state.tabs.forEach { tab -> runCatching { tab.session.close() } }
                _state.update {
                    it.copy(
                        settings = it.settings.copy(
                            workspaces = updated,
                            activeWorkspaceId = activatedTarget.id,
                            openTabs = activatedTarget.tabs,
                            activeTabIndex = activatedTarget.activeTabIndex,
                            tabGroups = activatedTarget.tabGroups,
                        ),
                        tabs = emptyList(),
                        activeTabId = null,
                        splitPrimaryTabId = null,
                        splitSecondaryTabId = null,
                        splitPaneFocused = false,
                        overlay = BrowserOverlay.NONE,
                    )
                }
                restoreSavedTabs(activatedTarget.tabs, activatedTarget.activeTabIndex)
                if (_state.value.tabs.isEmpty()) createTab()
                persistOpenTabs(immediate = true)
                showSnackbar("Switched to ${target.title}")
            }
        }
    }

    private fun savedTabsFromState(): List<SavedTab> = _state.value.tabs.filter {
        !it.isPrivate && it.hasPage && NavigationPolicy.isWebUrl(it.url)
    }.map {
        SavedTab(
            url = it.url,
            isPrivate = it.isPrivate,
            pinned = it.pinned,
            groupId = it.groupId,
            id = it.id,
            title = it.title,
            sessionState = it.sessionState?.takeIf { state -> state.toByteArray().size <= MAX_SESSION_STATE_BYTES },
        )
    }

    private fun updateSessionActivity(
        activeId: String?,
        secondaryId: String? = null,
        keepActiveMedia: Boolean = false,
    ) {
        val visibleIds = setOfNotNull(activeId, secondaryId) + if (keepActiveMedia) mediaSessions.keys else emptySet()
        _state.value.tabs.forEach { tab ->
            runCatching { tab.session.setActive(tab.id in visibleIds) }
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

    private fun rebuildBlockerStats() {
        val trackerCount = trackerBlockedByTab.values.sum()
        val trackerByOrigin = trackerBlockedByTab.mapNotNull { (tabId, count) ->
            val origin = _state.value.tabs.firstOrNull { it.id == tabId }?.let { NavigationPolicy.origin(it.url) }
            origin?.let { it to count }
        }.groupingBy { it.first }.fold(0) { total, item -> total + item.second }
        val byOrigin = (adBlockStats.byOrigin.keys + trackerByOrigin.keys).associateWith { origin ->
            (adBlockStats.byOrigin[origin] ?: 0) + (trackerByOrigin[origin] ?: 0)
        }.filterValues { it > 0 }
        _state.update {
            it.copy(
                blockerStats = BlockerStats(
                    totalBlocked = adBlockStats.totalBlocked + trackerCount,
                    byOrigin = byOrigin,
                ),
            )
        }
    }

    private fun updateTrackerStats(tabId: String, count: Int) {
        trackerBlockedByTab[tabId] = count.coerceAtLeast(0)
        rebuildBlockerStats()
    }

    private fun currentSiteOrigin(): String? = activeTab()
        ?.takeIf { !it.isPrivate }
        ?.let { NavigationPolicy.origin(it.url) }

    private fun isPrivateCredentialOrigin(origin: String): Boolean =
        _state.value.tabs.any { it.isPrivate && NavigationPolicy.origin(it.url) == origin }

    private fun loginEntriesFor(origin: String?): Array<org.mozilla.geckoview.Autocomplete.LoginEntry> {
        if (!credentialVault.unlocked.value) return emptyArray()
        if ((origin != null && isPrivateCredentialOrigin(origin)) || (origin == null && activeTab()?.isPrivate == true)) {
            return emptyArray()
        }
        return _state.value.credentials
            .asSequence()
            .filter { origin == null || it.origin == origin }
            .mapNotNull(::toLoginEntry)
            .toList()
            .toTypedArray()
    }

    // GeckoView exposes the LoginEntry bundle constructor only to its package.
    // Keep this adapter isolated so a future public factory can replace it safely.
    private fun toLoginEntry(credential: StoredCredential): org.mozilla.geckoview.Autocomplete.LoginEntry? = runCatching {
        val bundle = org.mozilla.gecko.util.GeckoBundle().apply {
            putString("guid", credential.id)
            putString("origin", credential.origin)
            putString("formActionOrigin", credential.formActionOrigin)
            putString("httpRealm", credential.httpRealm)
            putString("username", credential.username)
            putString("password", credential.password)
        }
        org.mozilla.geckoview.Autocomplete.LoginEntry::class.java
            .getDeclaredConstructor(org.mozilla.gecko.util.GeckoBundle::class.java)
            .apply { isAccessible = true }
            .newInstance(bundle)
    }.getOrNull()

    private fun toGeckoAddress(address: StoredAddress): org.mozilla.geckoview.Autocomplete.Address? = runCatching {
        val bundle = org.mozilla.gecko.util.GeckoBundle().apply {
            putString("guid", address.id)
            putString("name", address.name)
            putString("givenName", address.givenName)
            putString("additionalName", address.additionalName)
            putString("familyName", address.familyName)
            putString("organization", address.organization)
            putString("streetAddress", address.streetAddress)
            putString("addressLevel1", address.addressLevel1)
            putString("addressLevel2", address.addressLevel2)
            putString("addressLevel3", address.addressLevel3)
            putString("postalCode", address.postalCode)
            putString("country", address.country)
            putString("tel", address.tel)
            putString("email", address.email)
        }
        org.mozilla.geckoview.Autocomplete.Address::class.java
            .getDeclaredConstructor(org.mozilla.gecko.util.GeckoBundle::class.java)
            .apply { isAccessible = true }
            .newInstance(bundle)
    }.getOrNull()

    private fun createWebPushSubscription(prompt: WebPushPrompt): CreatedWebPushSubscription {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey
        val browserPublicKey = byteArrayOf(4) + fixedBytes(publicKey.w.affineX) + fixedBytes(publicKey.w.affineY)
        val authSecret = ByteArray(16).also(SecureRandom()::nextBytes)
        val endpoint = WEB_PUSH_ENDPOINT_BASE + UUID.randomUUID()
        val record = StoredWebPushSubscription(
            scope = prompt.scope,
            origin = prompt.origin,
            endpoint = endpoint,
            appServerKey = prompt.appServerKey?.clone(),
            browserPublicKey = browserPublicKey,
            authSecret = authSecret,
            createdAt = System.currentTimeMillis(),
        )
        return CreatedWebPushSubscription(
            record = record,
            subscription = WebPushSubscription(
                record.scope,
                record.endpoint,
                record.appServerKey,
                record.browserPublicKey,
                record.authSecret,
            ),
        )
    }

    private fun fixedBytes(value: java.math.BigInteger): ByteArray {
        val raw = value.toByteArray()
        return when {
            raw.size == 32 -> raw
            raw.size == 33 && raw[0] == 0.toByte() -> raw.copyOfRange(1, raw.size)
            raw.size < 32 -> ByteArray(32 - raw.size) + raw
            else -> raw.copyOfRange(raw.size - 32, raw.size)
        }
    }

    private fun refreshSiteSetting(tabId: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId }
        val origin = tab?.takeIf { !it.isPrivate }?.let { NavigationPolicy.origin(it.url) }
        if (origin == null) {
            _state.update { it.copy(siteSetting = null) }
            return
        }
        _state.update { if (it.activeTabId == tabId) it.copy(siteSetting = null) else it }
        val profileId = activeProfileId()
        viewModelScope.launch {
            val setting = dao.getSiteSetting(profileId, origin)
            if (_state.value.activeTabId == tabId && currentSiteOrigin() == origin) {
                _state.update { it.copy(siteSetting = setting) }
                applyDesktopSiteSetting(tab.session, setting?.desktopSites ?: _state.value.settings.desktopSites)
                applySiteCookieBannerMode(origin, setting?.cookieBannerMode)
                setting?.zoomPercent?.let { pageZoomByTab[tabId] = it } ?: pageZoomByTab.remove(tabId)
                syncActiveTabZoom()
                syncAdBlockSettings(_state.value.settings)
            }
        }
    }

    private fun updateCurrentSiteSetting(transform: (SiteSetting) -> SiteSetting) {
        val tab = activeTab() ?: return
        val origin = currentSiteOrigin() ?: run {
            showSnackbar("Site settings are unavailable in private tabs")
            return
        }
        viewModelScope.launch {
            val profileId = activeProfileId()
            val current = dao.getSiteSetting(profileId, origin)
                ?: SiteSetting(origin = origin, updatedAt = System.currentTimeMillis(), profileId = profileId)
            val updated = transform(current).copy(origin = origin, profileId = profileId, updatedAt = System.currentTimeMillis())
            if (updated.desktopSites == null && updated.adBlockingEnabled == null &&
                updated.userScriptsEnabled == null && updated.zoomPercent == null && updated.translationTarget == null
                && updated.httpsOnly == null && updated.cookieBannerMode == null
            ) {
                dao.deleteSiteSetting(profileId, origin)
            } else {
                dao.upsertSiteSetting(updated)
            }
            _state.update { it.copy(siteSetting = updated.takeUnless {
                it.desktopSites == null && it.adBlockingEnabled == null &&
                    it.userScriptsEnabled == null && it.zoomPercent == null && it.translationTarget == null
                    && it.httpsOnly == null
                    && it.cookieBannerMode == null
            }) }
            applyDesktopSiteSetting(tab.session, updated.desktopSites ?: _state.value.settings.desktopSites)
            updated.zoomPercent?.let { pageZoomByTab[tab.id] = it } ?: pageZoomByTab.remove(tab.id)
            syncActiveTabZoom()
            syncAdBlockSettings(_state.value.settings)
            applySiteCookieBannerMode(origin, updated.cookieBannerMode)
            if (tab.hasPage && updated.httpsOnly == true && tab.url.startsWith("http://", ignoreCase = true)) {
                tab.session.loadUri(NavigationPolicy.upgradeToHttps(tab.url))
            } else if (tab.hasPage && updated.desktopSites != current.desktopSites) {
                tab.session.reload()
            }
        }
    }

    private fun showSnackbar(message: String) {
        _state.update { it.copy(snackbar = message) }
    }

    private fun activeProfileId(): String = _state.value.settings.activeWorkspaceId.ifBlank { DEFAULT_WORKSPACE_ID }

    private fun sitePolicyKey(profileId: String, origin: String): String = "$profileId\u0000$origin"

    private fun activeContextId(): String? = _state.value.settings.workspaces
        .firstOrNull { it.id == activeProfileId() }
        ?.contextId

    private fun upgradeToHttpsIfNeeded(url: String): String = if (
        url.startsWith("http://", ignoreCase = true) &&
        (_state.value.settings.httpsOnly || NavigationPolicy.origin(url)
            ?.let { siteHttpsOnlyByOrigin[sitePolicyKey(activeProfileId(), it)] } == true)
    ) {
        NavigationPolicy.upgradeToHttps(url)
    } else url

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
        if (!NavigationPolicy.isAllowedTopLevel(url)) {
            launchExternal(url)
        } else if (inNewTab) {
            createTab(privateMode = privateTab, initialUri = url)
        } else {
            navigate(tabId, url)
        }
    }

    private fun scheduleDownload(download: DownloadEntry) {
        val networkType = if (download.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_DOWNLOAD_ID to download.downloadId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
            .setInitialDelay(
                (download.scheduledAt?.minus(System.currentTimeMillis()) ?: 0L).coerceAtLeast(0L),
                java.util.concurrent.TimeUnit.MILLISECONDS,
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            downloadWorkName(download.downloadId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun cleanupDownloadFiles(download: DownloadEntry) {
        val path = download.filePath ?: return
        runCatching { File(path).delete() }
        (0 until 6).forEach { index -> runCatching { File("$path.part$index").delete() } }
    }

    private fun restoreDownloads() {
        viewModelScope.launch {
            dao.getDownloads()
                .filter { it.filePath != null && it.status in setOf(DownloadStatus.QUEUED.label, DownloadStatus.DOWNLOADING.label) }
                .forEach(::scheduleDownload)
    }
    }

    private fun installAdBlocker() {
        runtime.webExtensionController.list().accept(
            { extensions ->
                val staleExtensions = extensions.orEmpty().filter {
                    it.id == "adblock@dextra" && it.metaData.version != "2.6.2"
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
        viewModelScope.launch(Dispatchers.IO) {
            val disabledOrigins = JSONArray()
            val enabledOrigins = JSONArray()
            val siteSettings = dao.getSiteSettings().filter { it.profileId == activeProfileId() }
            siteSettings
                .filter { it.adBlockingEnabled == false }
                .forEach { disabledOrigins.put(it.origin) }
            siteSettings
                .filter { it.adBlockingEnabled == true }
                .forEach { enabledOrigins.put(it.origin) }
            withContext(Dispatchers.Main.immediate) {
                port.postMessage(
                    JSONObject()
                        .put("type", "updateAdblock")
                        .put("enabled", settings.adBlockingEnabled)
                        .put("urls", urls)
                        .put("disabledOrigins", disabledOrigins)
                        .put("enabledOrigins", enabledOrigins),
                )
            }
        }
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

    private fun applyCookieBannerMode(mode: Int) {
        val contentBlocking = runtime.settings.getContentBlocking()
        contentBlocking.setCookieBannerMode(mode.coerceIn(0, 2))
        contentBlocking.setCookieBannerModePrivateBrowsing(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT)
        contentBlocking.setCookieBannerGlobalRulesEnabled(mode != ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED)
        contentBlocking.setCookieBannerGlobalRulesSubFramesEnabled(true)
    }

    private fun applySiteCookieBannerMode(origin: String, mode: Int?) {
        val host = runCatching { Uri.parse(origin).host?.takeIf(String::isNotBlank) }.getOrNull() ?: return
        if (mode == null || mode == ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED) {
            runtime.storageController.removeCookieBannerModeForDomain(host, false)
        } else {
            runtime.storageController.setCookieBannerModeForDomain(host, mode, false)
        }
    }

    private fun removeSiteCookieBannerMode(origin: String) {
        applySiteCookieBannerMode(origin, null)
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
        viewModelScope.launch(Dispatchers.IO) {
            val disabledOrigins = JSONArray()
            dao.getSiteSettings().filter { it.profileId == activeProfileId() }
                .filter { it.userScriptsEnabled == false }
                .forEach { disabledOrigins.put(it.origin) }
            withContext(Dispatchers.Main.immediate) {
                port.postMessage(
                    JSONObject()
                        .put("type", "updateUserscripts")
                        .put("urls", urls)
                        .put("disabledOrigins", disabledOrigins),
                )
            }
        }
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
        permissions: Array<String>,
        origins: Array<String>,
        dataCollectionPermissions: Array<String>,
    ): GeckoResult<AllowOrDeny> {
        _state.value.extensionUpdatePrompt?.result?.complete(AllowOrDeny.DENY)
        val result = GeckoResult<AllowOrDeny>()
        _state.update {
            it.copy(
                extensionUpdatePrompt = ExtensionUpdatePrompt(
                    id = UUID.randomUUID().toString(),
                    name = extension.metaData.name ?: extension.id,
                    permissions = permissions.toList(),
                    origins = origins.toList(),
                    dataCollectionPermissions = dataCollectionPermissions.toList(),
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
            requiredPermissions = extension.metaData.requiredPermissions.toList(),
            requiredOrigins = extension.metaData.requiredOrigins.toList(),
            requiredDataCollectionPermissions = extension.metaData.requiredDataCollectionPermissions.toList(),
            optionalPermissions = extension.metaData.optionalPermissions.toList(),
            grantedOptionalPermissions = extension.metaData.grantedOptionalPermissions.toList(),
            optionalOrigins = extension.metaData.optionalOrigins.toList(),
            grantedOptionalOrigins = extension.metaData.grantedOptionalOrigins.toList(),
            optionalDataCollectionPermissions = extension.metaData.optionalDataCollectionPermissions.toList(),
            grantedOptionalDataCollectionPermissions = extension.metaData.grantedOptionalDataCollectionPermissions.toList(),
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
            setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
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
            setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
        }
        try {
            if (connection.responseCode !in 200..299 || connection.contentLengthLong > MAX_EXTENSION_PACKAGE_BYTES) {
                temporary.delete()
                return@withContext null
            }
            if (connection.url.protocol.lowercase() != "https") {
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
        if (tab.isPrivate || !NavigationPolicy.isWebUrl(tab.url)) return
        viewModelScope.launch {
            dao.insertHistory(
                HistoryEntry(
                    url = tab.url,
                    title = tab.title.ifBlank { BrowserUrl.displayValue(tab.url) },
                    visitedAt = System.currentTimeMillis(),
                ),
            )
            dao.trimHistory()
        }
    }

    private fun loadFavicon(tabId: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.isPrivate) return
        val pageUrl = tab.url
        val pageUri = runCatching { Uri.parse(pageUrl) }.getOrNull() ?: return
        if (!NavigationPolicy.isWebUrl(pageUrl) || pageUri.authority.isNullOrBlank()) return

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
                connection.setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
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

    private fun download(response: WebResponse, privateMode: Boolean) {
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
                isPrivate = privateMode,
                destinationTreeUri = _state.value.settings.downloadDirectoryUri,
                workspaceId = activeProfileId(),
            )
            viewModelScope.launch {
                dao.upsertDownload(download)
                scheduleDownload(download)
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

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun fetchCertificate(url: String): SecurityCertificateInfo {
        val connection = (URL(url).openConnection() as? HttpsURLConnection)
            ?: error("HTTPS connection unavailable")
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "HEAD"
        connection.setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
        return try {
            connection.connect()
            val certificate = connection.serverCertificates.firstOrNull() as? X509Certificate
                ?: error("No X.509 certificate")
            val fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(certificate.encoded)
                .joinToString(":") { byte -> "%02X".format(byte.toInt() and 0xff) }
            SecurityCertificateInfo(
                subject = certificate.subjectX500Principal.name.take(240),
                issuer = certificate.issuerX500Principal.name.take(240),
                validFrom = DateFormat.getDateTimeInstance().format(certificate.notBefore),
                validTo = DateFormat.getDateTimeInstance().format(certificate.notAfter),
                sha256 = fingerprint,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun webAppId(startUrl: String): String = sha256(startUrl).take(24)

    private fun refreshWebAppShortcut(app: InstalledWebApp) {
        val shortcutManager = getApplication<Application>().getSystemService(ShortcutManager::class.java) ?: return
        val iconUrl = app.iconUrl ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val icon = downloadPwaIcon(iconUrl, app.origin)
            if (icon != null) {
                withContext(Dispatchers.Main.immediate) {
                    shortcutManager.updateShortcuts(listOf(buildWebAppShortcut(app, icon)))
                }
            }
        }
    }

    private fun buildWebAppShortcut(app: InstalledWebApp, icon: Bitmap? = null): ShortcutInfo {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.startUrl)).apply {
            setClass(context, PwaActivity::class.java)
            putExtra(PwaActivity.EXTRA_PWA_ID, app.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        return ShortcutInfo.Builder(context, "webapp-${app.id}")
            .setShortLabel(app.name.take(25))
            .setLongLabel(app.name.take(80))
            .setIcon(icon?.let(Icon::createWithBitmap) ?: Icon.createWithResource(context, com.dwicao.dextra.R.drawable.ic_launcher))
            .setIntent(intent)
            .build()
    }

    private suspend fun fetchInstalledWebApp(app: InstalledWebApp): InstalledWebApp? {
        val document = fetchHttpText(app.startUrl, MAX_PWA_MANIFEST_BYTES * 2) ?: return null
        val linkTag = Regex("(?is)<link\\b[^>]*>").findAll(document.first)
            .map { it.value }
            .firstOrNull { tag ->
                Regex("(?is)\\brel\\s*=\\s*[\\\"']([^\\\"']+)").find(tag)?.groupValues?.getOrNull(1)
                    ?.split(Regex("\\s+"))?.any { it.equals("manifest", ignoreCase = true) } == true
            } ?: return null
        val href = Regex("(?is)\\bhref\\s*=\\s*[\\\"']([^\\\"']+)").find(linkTag)?.groupValues?.getOrNull(1) ?: return null
        val manifestUrl = runCatching { URI(document.second).resolve(href).toString() }.getOrNull() ?: return null
        if (NavigationPolicy.origin(manifestUrl) != app.origin) return null
        val manifestDocument = fetchHttpText(manifestUrl, MAX_PWA_MANIFEST_BYTES) ?: return null
        val manifest = runCatching { JSONObject(manifestDocument.first) }.getOrNull() ?: return null
        val startUrl = runCatching {
            URI(manifestDocument.second).resolve(manifest.optString("start_url").ifBlank { app.startUrl }).toString()
        }.getOrNull() ?: return null
        if (!NavigationPolicy.isWebUrl(startUrl) || NavigationPolicy.origin(startUrl) != app.origin) return null
        val scope = runCatching {
            URI(startUrl).resolve(manifest.optString("scope").ifBlank { "/" }).toString()
        }.getOrNull() ?: return null
        if (NavigationPolicy.origin(scope) != app.origin || !startUrl.startsWith(scope.trimEnd('/') + "/")) return null
        val name = manifest.optString("name").ifBlank { manifest.optString("short_name") }.trim().take(80)
        if (name.isBlank()) return null
        return app.copy(
            name = name,
            startUrl = startUrl,
            scope = scope,
            iconUrl = resolveManifestIcon(manifest, manifestDocument.second, app.origin),
        )
    }

    private fun resolveManifestIcon(manifest: JSONObject, baseUrl: String, origin: String): String? =
        manifest.optJSONArray("icons")?.let { icons ->
            (0 until icons.length()).mapNotNull { index ->
                val source = icons.optJSONObject(index)?.optString("src").orEmpty()
                val resolved = runCatching { URI(baseUrl).resolve(source).toString() }.getOrNull() ?: return@mapNotNull null
                if (NavigationPolicy.isWebUrl(resolved) && NavigationPolicy.origin(resolved) == origin) resolved else null
            }.firstOrNull()
        }

    private suspend fun fetchHttpText(url: String, maxBytes: Int): Pair<String, String>? = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as? HttpURLConnection)?.apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "text/html,application/manifest+json,application/json")
            setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
        } ?: return@withContext null
        try {
            if (connection.responseCode !in 200..299 || !NavigationPolicy.isWebUrl(connection.url.toString())) return@withContext null
            if (connection.contentLengthLong > maxBytes) return@withContext null
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > maxBytes) return@withContext null
                    output.write(buffer, 0, count)
                }
            }
            output.toString(Charsets.UTF_8.name()) to connection.url.toString()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadPwaIcon(url: String, expectedOrigin: String): Bitmap? = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as? HttpURLConnection)?.apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "image/*")
            setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
        } ?: return@withContext null
        try {
            if (connection.responseCode !in 200..299 ||
                !NavigationPolicy.isWebUrl(connection.url.toString()) ||
                NavigationPolicy.origin(connection.url.toString()) != expectedOrigin
            ) return@withContext null
            if (connection.contentLengthLong > MAX_PWA_ICON_BYTES) return@withContext null
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > MAX_PWA_ICON_BYTES) return@withContext null
                    output.write(buffer, 0, count)
                }
            }
            BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
        } finally {
            connection.disconnect()
        }
    }

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
            recordNetworkActivity(tabId, resolvedUrl, "navigation", "loaded")
            if (resolvedUrl == "about:blank") {
                updateTab(tabId) { it.copy(url = "", title = "New tab", hasPage = false, isLoading = false, progress = 0) }
            } else {
                updateTab(tabId) { it.copy(url = resolvedUrl, isSecure = resolvedUrl.startsWith("https://"), crashed = false) }
            }
            if (_state.value.activeTabId == tabId) refreshSiteSetting(tabId)
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
            if (!NavigationPolicy.isAllowedTopLevel(request.uri, allowExtension = true)) {
                recordNetworkActivity(tabId, request.uri, "navigation", "blocked by policy")
                launchExternal(request.uri)
                return GeckoResult.deny()
            }
            val upgraded = upgradeToHttpsIfNeeded(request.uri)
            if (upgraded != request.uri) {
                recordNetworkActivity(tabId, request.uri, "navigation", "upgraded to HTTPS")
                session.loadUri(upgraded)
                return GeckoResult.deny()
            }
            recordNetworkActivity(tabId, request.uri, "navigation", "allowed")
            return GeckoResult.allow()
        }

        override fun onLoadError(
            session: GeckoSession,
            uri: String?,
            error: org.mozilla.geckoview.WebRequestError,
        ): GeckoResult<String> {
            recordCompatibilityEvent(tabId, "error", "Load error ${error.code} for $uri")
            return GeckoResult.fromValue(null)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession> {
            val popupUri = upgradeToHttpsIfNeeded(uri.ifBlank { "about:blank" })
            if (!NavigationPolicy.isAllowedTopLevel(popupUri)) {
                if (Uri.parse(popupUri).scheme != null) launchExternal(popupUri)
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
            mediaSessions.remove(tabId)
            _state.update {
                if (it.securityDiagnostics?.tabId == tabId) {
                    it.copy(securityDiagnostics = null, overlay = BrowserOverlay.NONE, cookieBannerDetected = false, cookieBannerHandled = false)
                } else if (it.activeTabId == tabId) {
                    it.copy(cookieBannerDetected = false, cookieBannerHandled = false)
                } else it
            }
            if (_state.value.activeTabId == tabId && _state.value.findInPage != null) closeFindInPage()
            if (_state.value.activeTabId == tabId) _state.update { it.copy(translation = null) }
            lastProgressUpdateAt.remove(tabId)
            if (url == "about:blank") {
                updateTab(tabId) { it.copy(url = "", title = "New tab", hasPage = false, isLoading = false, progress = 0, favicon = null) }
            } else {
                updateTab(tabId) { it.copy(url = url, hasPage = true, isLoading = true, progress = 0, favicon = null, crashed = false) }
            }
            _state.update { if (it.activeTabId == tabId) it.copy(webAppManifest = null, readerMode = null) else it }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            updateTab(tabId) { it.copy(isLoading = false, progress = if (success) 100 else 0) }
            if (!success) recordCompatibilityEvent(tabId, "warning", "Page load did not complete")
            if (success) {
                recordHistory(tabId)
                loadFavicon(tabId)
                runtime.contentBlockingController.getLog(session).accept(
                    { entries ->
                        val blocked = entries.orEmpty().sumOf { entry ->
                            entry.blockingData.orEmpty().filter { data -> data.blocked }.sumOf { data -> data.count }
                        }
                        updateTrackerStats(tabId, blocked)
                        if (blocked > 0) {
                            recordNetworkActivity(
                                tabId,
                                _state.value.tabs.firstOrNull { it.id == tabId }?.url.orEmpty(),
                                "content blocking",
                                "$blocked blocked",
                            )
                        }
                    },
                    {},
                )
            }
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            val now = SystemClock.uptimeMillis()
            if (progress < 100 && now - (lastProgressUpdateAt[tabId] ?: 0L) < PROGRESS_UPDATE_INTERVAL_MS) return
            lastProgressUpdateAt[tabId] = now
            updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
        }

        override fun onSecurityChange(
            session: GeckoSession,
            securityInfo: GeckoSession.ProgressDelegate.SecurityInformation,
        ) {
            updateTab(tabId) { it.copy(isSecure = securityInfo.isSecure) }
            if (!securityInfo.isSecure && _state.value.tabs.firstOrNull { it.id == tabId }?.url?.startsWith("https://", ignoreCase = true) == true) {
                recordCompatibilityEvent(tabId, "error", "HTTPS page is not currently reported as secure")
            }
        }

        override fun onSessionStateChange(session: GeckoSession, state: GeckoSession.SessionState) {
            val serialized = state.toString().takeIf {
                it.toByteArray(Charsets.UTF_8).size <= MAX_SESSION_STATE_BYTES
            }
            updateTab(tabId) { it.copy(sessionState = serialized) }
            persistOpenTabs()
        }
    }

    private inner class ContentDelegate(private val tabId: String) : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            updateTab(tabId) { it.copy(title = title?.ifBlank { "New tab" } ?: "New tab") }
        }

        override fun onCookieBannerDetected(session: GeckoSession) {
            if (_state.value.activeTabId == tabId) {
                _state.update { it.copy(cookieBannerDetected = true, cookieBannerHandled = false) }
            }
        }

        override fun onCookieBannerHandled(session: GeckoSession) {
            if (_state.value.activeTabId == tabId) {
                _state.update { it.copy(cookieBannerDetected = true, cookieBannerHandled = true) }
            }
        }

        override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
            val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
            val pageOrigin = NavigationPolicy.origin(tab.url) ?: return
            val rawStartUrl = manifest.optString("start_url").ifBlank { tab.url }
            val startUrl = runCatching { URI(tab.url).resolve(rawStartUrl).toString() }.getOrNull() ?: return
            if (!NavigationPolicy.isWebUrl(startUrl) || NavigationPolicy.origin(startUrl) != pageOrigin) return
            val rawScope = manifest.optString("scope").ifBlank { "/" }
            val scope = runCatching { URI(startUrl).resolve(rawScope).toString() }.getOrNull() ?: return
            if (NavigationPolicy.origin(scope) != pageOrigin || !startUrl.startsWith(scope.trimEnd('/') + "/")) return
            val name = manifest.optString("name").ifBlank { manifest.optString("short_name") }.trim().take(80)
            if (name.isBlank()) return
            val iconUrl = manifest.optJSONArray("icons")?.let { icons ->
                (0 until icons.length()).mapNotNull { index ->
                    val rawIcon = icons.optJSONObject(index)?.optString("src").orEmpty()
                    if (rawIcon.isBlank()) return@mapNotNull null
                    val resolved = runCatching { URI(tab.url).resolve(rawIcon).toString() }.getOrNull() ?: return@mapNotNull null
                    if (!NavigationPolicy.isWebUrl(resolved) || NavigationPolicy.origin(resolved) != pageOrigin) return@mapNotNull null
                    resolved
                }.firstOrNull()
            }
            _state.update {
                if (it.activeTabId == tabId) {
                    it.copy(webAppManifest = WebAppManifestInfo(tabId, name, startUrl, scope, iconUrl))
                } else it
            }
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            recordNetworkActivity(tabId, response.uri, "response", "HTTP ${response.statusCode}")
            val contentType = response.headers["Content-Type"] ?: response.headers["content-type"]
            if (FirefoxAddons.isAmoUrl(response.uri) && FirefoxAddons.isXpiDownload(response.uri, contentType)) {
                installExtensionPackageFromUrl(response.uri)
            } else {
                download(response, isPrivateTab(tabId))
            }
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
                linkText = element.linkText,
                textContent = element.title ?: element.altText,
                resourceUri = element.srcUri,
                resourceType = element.type,
            )
        }

        override fun onCloseRequest(session: GeckoSession) {
            closeTab(tabId)
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            val current = _state.value
            if (fullScreen) {
                _state.update { state ->
                    state.copy(
                        activeTabId = tabId,
                        tabs = state.tabs.map { tab ->
                            if (tab.id == tabId) tab.copy(isFullScreen = true) else tab.copy(isFullScreen = false)
                        },
                    )
                }
                updateSessionActivity(tabId)
            } else {
                _state.update { state ->
                    state.copy(tabs = state.tabs.map { tab -> if (tab.id == tabId) tab.copy(isFullScreen = false) else tab })
                }
                updateSessionActivity(current.splitPrimaryTabId ?: current.activeTabId, current.splitSecondaryTabId)
            }
        }

        override fun onCrash(session: GeckoSession) {
            markTabCrashed("This site crashed. Reload to recover it.")
        }

        override fun onKill(session: GeckoSession) {
            markTabCrashed("This site was stopped. Reload to recover it.")
        }

        override fun onSlowScript(session: GeckoSession, scriptFileName: String): GeckoResult<org.mozilla.geckoview.SlowScriptResponse> {
            recordCompatibilityEvent(tabId, "warning", "Slow script: $scriptFileName")
            return GeckoResult.fromValue(org.mozilla.geckoview.SlowScriptResponse.CONTINUE)
        }

        private fun markTabCrashed(message: String) {
            val tab = _state.value.tabs.firstOrNull { it.id == tabId }
            val url = tab?.takeIf { !it.isPrivate }?.url.orEmpty()
            updateTab(tabId) { it.copy(crashed = true, isLoading = false, progress = 0, isFullScreen = false) }
            recordGeckoCrash(tabId, url, message)
            recordCompatibilityEvent(tabId, "error", message)
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

    private inner class WebAuthnPromptDelegate(private val tabId: String) : GeckoSession.PromptDelegate {
        override fun onWebAuthnRelatedOriginPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.WebAuthnRelatedOriginPrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val tab = _state.value.tabs.firstOrNull { it.id == tabId }
            val origin = prompt.origin?.takeIf { NavigationPolicy.isWebUrl(it) }
            if (tab?.isPrivate == true || origin == null) return GeckoResult.fromValue(prompt.dismiss())
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            val state = WebAuthnPromptState(
                tabId = tabId,
                origin = origin,
                rpId = prompt.rpId.orEmpty(),
                isCreate = prompt.isCreate,
                result = result,
                allow = { prompt.confirm(AllowOrDeny.ALLOW) },
                dismiss = { prompt.dismiss() },
            )
            if (_state.value.webAuthnPrompt == null) {
                _state.update { it.copy(webAuthnPrompt = state) }
            } else {
                result.complete(prompt.dismiss())
            }
            return result
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
            } else enqueueAndroidPermission(
                AndroidPermissionPrompt(
                    id = UUID.randomUUID().toString(),
                    tabId = tabId,
                    permissions = requested,
                    callback = callback,
                ),
            )
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
            val origin = NavigationPolicy.origin(permission.uri) ?: permission.uri
            val profileId = activeProfileId()
            viewModelScope.launch {
                val saved = dao.getSitePermission(profileId, origin, permission.permission.toString())
                when (saved?.decision) {
                    "allow" -> result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    "block" -> result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                    else -> enqueueContentPermission(
                        ContentPermissionPrompt(
                            id = UUID.randomUUID().toString(),
                            tabId = tabId,
                            origin = origin,
                            label = permissionLabel(permission.permission),
                            permission = permission.permission,
                            result = result,
                        ),
                    )
                }
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
                enqueueMediaPermission(
                    MediaPermissionPrompt(
                        id = UUID.randomUUID().toString(),
                        tabId = tabId,
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

    private fun permissionLabel(permission: Int): String = when (permission) {
        GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "your location"
        GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "notifications"
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> "autoplay media"
        else -> "additional access"
    }

    override fun onCleared() {
        performanceMonitor.stop()
        _state.value.webAuthnPrompt?.let { prompt -> prompt.result.complete(prompt.dismiss()) }
        _state.value.extensionPopup?.session?.close()
        _state.value.tabs.forEach {
            getApplication<com.dwicao.dextra.DextraApplication>().mediaNotificationController.clear(it.id)
            it.session.close()
        }
        mediaSessions.clear()
        translationDetectedByTab.clear()
        super.onCleared()
    }
}
