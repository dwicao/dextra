package com.dwicao.dextra.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dwicao.dextra.browser.BrowserCommandId
import com.dwicao.dextra.browser.DefaultKeyboardShortcuts
import com.dwicao.dextra.browser.KeyChord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AdBlockFilter(
    val name: String,
    val url: String,
    val enabled: Boolean = true,
)

private val RemovedDefaultAdBlockFilterUrls = setOf(
    "https://easylist.to/easylist/easylist.txt",
    "https://easylist.to/easylist/easyprivacy.txt",
)

private const val DefaultAdBlockFilterUrl = "https://easylist.to/easylist/easylist.txt"

data class BrowserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val homepage: String = "https://www.google.com/",
    val desktopSites: Boolean = false,
    val tabBarWithAddressBar: Boolean = true,
    val verticalTabs: Boolean = true,
    val dnsOverHttpsEnabled: Boolean = false,
    val dnsProvider: DnsProvider = DnsProvider.CLOUDFLARE,
    val adBlockingEnabled: Boolean = true,
    val adBlockFilters: List<AdBlockFilter> = emptyList(),
    val userScriptUrls: List<String> = emptyList(),
    val disabledUserScriptUrls: Set<String> = emptySet(),
    val extensionInstallRecords: Map<String, ExtensionInstallRecord> = emptyMap(),
    val openTabs: List<SavedTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val tabGroups: List<SavedTabGroup> = emptyList(),
    val shortcutBindings: Map<BrowserCommandId, KeyChord> = DefaultKeyboardShortcuts.bindings,
    val sessionSnapshots: List<SessionSnapshot> = emptyList(),
    val downloadDirectoryUri: String? = null,
)

data class ExtensionInstallRecord(
    val filePath: String,
    val allowInPrivateBrowsing: Boolean,
    val allowDataCollection: Boolean,
)

data class SavedTab(
    val url: String,
    val isPrivate: Boolean = false,
    val pinned: Boolean = false,
    val groupId: String? = null,
    val id: String? = null,
    val title: String? = null,
    val sessionState: String? = null,
)

data class SavedTabGroup(
    val id: String,
    val title: String,
    val color: Long = 0xFF4E4BB5L,
    val collapsed: Boolean = false,
)

data class SessionSnapshot(
    val id: String,
    val title: String,
    val createdAt: Long,
    val tabs: List<SavedTab>,
    val activeTabIndex: Int,
    val tabGroups: List<SavedTabGroup>,
)

enum class DnsProvider(val label: String, val dohUri: String) {
    CLOUDFLARE("Cloudflare", "https://cloudflare-dns.com/dns-query"),
    GOOGLE("Google", "https://dns.google/dns-query"),
    QUAD9("Quad9", "https://dns.quad9.net/dns-query"),
    ADGUARD("AdGuard", "https://dns.adguard-dns.com/dns-query"),
    MULLVAD("Mullvad", "https://doh.mullvad.net/dns-query"),
}

enum class SearchEngine(val label: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    BRAVE("Brave", "https://search.brave.com/search?q=%s"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s"),
    STARTPAGE("Startpage", "https://www.startpage.com/sp/search?query=%s"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=%s"),
    QWANT("Qwant", "https://www.qwant.com/?q=%s"),
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val searchEngine = stringPreferencesKey("search_engine")
        val homepage = stringPreferencesKey("homepage")
        val desktopSites = booleanPreferencesKey("desktop_sites")
        val tabBarWithAddressBar = booleanPreferencesKey("tab_bar_with_address_bar")
        val verticalTabs = booleanPreferencesKey("vertical_tabs")
        val dnsOverHttpsEnabled = booleanPreferencesKey("dns_over_https_enabled")
        val dnsProvider = stringPreferencesKey("dns_provider")
        val adBlockingEnabled = booleanPreferencesKey("ad_blocking_enabled")
        val adBlockFilters = stringPreferencesKey("ad_block_filters")
        val disabledAdBlockFilters = stringPreferencesKey("disabled_ad_block_filters")
        val userScriptUrls = stringPreferencesKey("user_script_urls")
        val disabledUserScripts = stringPreferencesKey("disabled_user_scripts")
        val extensionInstallRecords = stringPreferencesKey("extension_install_records")
        val openTabs = stringPreferencesKey("open_tabs")
        val activeTabIndex = intPreferencesKey("active_tab_index")
        val tabGroups = stringPreferencesKey("tab_groups")
        val shortcutBindings = stringPreferencesKey("shortcut_bindings")
        val sessionSnapshots = stringPreferencesKey("session_snapshots")
        val downloadDirectoryUri = stringPreferencesKey("download_directory_uri")
    }

    val settings: Flow<BrowserSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toBrowserSettings() }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.settingsDataStore.edit { it[Keys.searchEngine] = engine.name }
    }

    suspend fun setDesktopSites(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.desktopSites] = enabled }
    }

    suspend fun setHomepage(homepage: String) {
        context.settingsDataStore.edit { it[Keys.homepage] = homepage }
    }

    suspend fun setTabBarWithAddressBar(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.tabBarWithAddressBar] = enabled }
    }

    suspend fun setVerticalTabs(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.verticalTabs] = enabled }
    }

    suspend fun setShortcut(command: BrowserCommandId, chord: KeyChord?) {
        context.settingsDataStore.edit { preferences ->
            val bindings = preferences.shortcutBindings().toMutableMap()
            if (chord == null) bindings.remove(command) else bindings[command] = chord
            preferences[Keys.shortcutBindings] = bindings.entries.joinToString("\n") { (id, value) ->
                "${id.name}\t${value.encode()}"
            }
        }
    }

    suspend fun setDnsOverHttpsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.dnsOverHttpsEnabled] = enabled }
    }

    suspend fun setDnsProvider(provider: DnsProvider) {
        context.settingsDataStore.edit { it[Keys.dnsProvider] = provider.name }
    }

    suspend fun setAdBlockingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.adBlockingEnabled] = enabled }
    }

    suspend fun addAdBlockFilter(url: String) {
        if (!url.startsWith("https://", ignoreCase = true)) return
        context.settingsDataStore.edit { preferences ->
            val urls = preferences.filterUrls()
            preferences[Keys.adBlockFilters] = (urls + url).distinct().joinToString("\n")
            preferences[Keys.disabledAdBlockFilters] = preferences.disabledFilterUrls()
                .filterNot { it == url }
                .joinToString("\n")
        }
    }

    suspend fun setAdBlockFilterEnabled(url: String, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val disabled = preferences.disabledFilterUrls()
            preferences[Keys.disabledAdBlockFilters] = if (enabled) {
                disabled.filterNot { it == url }
            } else {
                (disabled + url).distinct()
            }.joinToString("\n")
        }
    }

    suspend fun removeAdBlockFilter(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.adBlockFilters] = preferences.filterUrls()
                .filterNot { it == url }
                .joinToString("\n")
            preferences[Keys.disabledAdBlockFilters] = preferences.disabledFilterUrls()
                .filterNot { it == url }
                .joinToString("\n")
        }
    }

    suspend fun addUserScript(url: String) {
        if (!url.startsWith("https://", ignoreCase = true)) return
        context.settingsDataStore.edit { preferences ->
            val urls = preferences.userScripts()
            preferences[Keys.userScriptUrls] = (urls + url).distinct().joinToString("\n")
            preferences[Keys.disabledUserScripts] = preferences.disabledUserScripts()
                .filterNot { it == url }
                .joinToString("\n")
        }
    }

    suspend fun setUserScriptEnabled(url: String, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val disabled = preferences.disabledUserScripts()
            preferences[Keys.disabledUserScripts] = if (enabled) {
                disabled.filterNot { it == url }
            } else {
                (disabled + url).distinct()
            }.joinToString("\n")
        }
    }

    suspend fun removeUserScript(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.userScriptUrls] = preferences.userScripts()
                .filterNot { it == url }
                .joinToString("\n")
            preferences[Keys.disabledUserScripts] = preferences.disabledUserScripts()
                .filterNot { it == url }
                .joinToString("\n")
        }
    }

    suspend fun saveExtensionInstallRecord(id: String, record: ExtensionInstallRecord) {
        context.settingsDataStore.edit { preferences ->
            val records = preferences.extensionInstallRecords().toMutableMap()
            records[id] = record
            preferences[Keys.extensionInstallRecords] = records.entries
                .joinToString("\n") { (extensionId, value) ->
                    "$extensionId\t${value.filePath}\t${value.allowInPrivateBrowsing}\t${value.allowDataCollection}"
                }
        }
    }

    suspend fun removeExtensionInstallRecord(id: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.extensionInstallRecords] = preferences.extensionInstallRecords()
                .filterKeys { it != id }
                .entries
                .joinToString("\n") { (extensionId, value) ->
                    "$extensionId\t${value.filePath}\t${value.allowInPrivateBrowsing}\t${value.allowDataCollection}"
                }
        }
    }

    suspend fun saveOpenTabs(
        tabs: List<SavedTab>,
        activeTabIndex: Int,
        groups: List<SavedTabGroup> = emptyList(),
    ) {
        context.settingsDataStore.edit { preferences ->
            val payload = JSONArray().apply {
                tabs.forEach { tab ->
                    put(
                        JSONObject()
                            .put("url", tab.url)
                            .put("private", tab.isPrivate)
                            .put("pinned", tab.pinned)
                             .put("groupId", tab.groupId)
                             .put("id", tab.id)
                             .put("title", tab.title)
                             .put("sessionState", tab.sessionState),
                    )
                }
            }
            val groupPayload = JSONArray().apply {
                groups.forEach { group ->
                    put(
                        JSONObject()
                            .put("id", group.id)
                            .put("title", group.title)
                            .put("color", group.color)
                            .put("collapsed", group.collapsed),
                    )
                }
            }
            preferences[Keys.openTabs] = payload.toString()
            preferences[Keys.tabGroups] = groupPayload.toString()
            preferences[Keys.activeTabIndex] = activeTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
        }
    }

    suspend fun saveSessionSnapshot(snapshot: SessionSnapshot) {
        context.settingsDataStore.edit { preferences ->
            val snapshots = preferences.sessionSnapshots().toMutableList()
            snapshots.removeAll { it.id == snapshot.id }
            snapshots.add(0, snapshot)
            val bounded = snapshots.take(MAX_SESSION_SNAPSHOTS).toMutableList()
            while (bounded.isNotEmpty()) {
                val payload = JSONArray(bounded.map { it.toJson() }).toString()
                if (payload.toByteArray(Charsets.UTF_8).size <= MAX_SESSION_SNAPSHOT_BYTES) {
                    preferences[Keys.sessionSnapshots] = payload
                    return@edit
                }
                bounded.removeAt(bounded.lastIndex)
            }
            preferences.remove(Keys.sessionSnapshots)
        }
    }

    suspend fun deleteSessionSnapshot(id: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.sessionSnapshots] = JSONArray(
                preferences.sessionSnapshots().filterNot { it.id == id }.map { it.toJson() },
            ).toString()
        }
    }

    suspend fun setDownloadDirectoryUri(uri: String?) {
        context.settingsDataStore.edit { preferences ->
            if (uri == null) preferences.remove(Keys.downloadDirectoryUri)
            else preferences[Keys.downloadDirectoryUri] = uri
        }
    }

    private fun Preferences.toBrowserSettings(): BrowserSettings = BrowserSettings(
        themeMode = get(Keys.theme)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        searchEngine = get(Keys.searchEngine)?.let {
            runCatching { SearchEngine.valueOf(it) }.getOrNull()
        } ?: SearchEngine.GOOGLE,
        homepage = get(Keys.homepage) ?: "https://www.google.com/",
        desktopSites = get(Keys.desktopSites) ?: defaultDesktopSites(),
        tabBarWithAddressBar = get(Keys.tabBarWithAddressBar) ?: true,
        verticalTabs = get(Keys.verticalTabs) ?: true,
        dnsOverHttpsEnabled = get(Keys.dnsOverHttpsEnabled) ?: false,
        dnsProvider = get(Keys.dnsProvider)?.let {
            runCatching { DnsProvider.valueOf(it) }.getOrNull()
        } ?: DnsProvider.CLOUDFLARE,
        adBlockingEnabled = get(Keys.adBlockingEnabled) ?: true,
        adBlockFilters = filterUrls().map { url -> filterFromUrl(url, url !in disabledFilterUrls()) },
        userScriptUrls = userScripts(),
        disabledUserScriptUrls = disabledUserScripts().intersect(userScripts().toSet()),
        extensionInstallRecords = extensionInstallRecords(),
        openTabs = savedTabs(),
        activeTabIndex = get(Keys.activeTabIndex) ?: 0,
        tabGroups = savedTabGroups(),
        shortcutBindings = DefaultKeyboardShortcuts.bindings + shortcutBindings(),
        sessionSnapshots = sessionSnapshots(),
        downloadDirectoryUri = get(Keys.downloadDirectoryUri),
    )

    private fun Preferences.filterUrls(): List<String> = get(Keys.adBlockFilters)
        ?.split('\n')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?.filter { it.startsWith("https://", ignoreCase = true) }
        ?.filterNot { it in RemovedDefaultAdBlockFilterUrls }
        ?: listOf(DefaultAdBlockFilterUrl)

    private fun Preferences.disabledFilterUrls(): List<String> = get(Keys.disabledAdBlockFilters)
        ?.split('\n')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?: emptyList()

    private fun Preferences.userScripts(): List<String> = get(Keys.userScriptUrls)
        ?.split('\n')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?.filter { it.startsWith("https://", ignoreCase = true) }
        ?: emptyList()

    private fun Preferences.disabledUserScripts(): List<String> = get(Keys.disabledUserScripts)
        ?.split('\n')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?: emptyList()

    private fun Preferences.extensionInstallRecords(): Map<String, ExtensionInstallRecord> = get(Keys.extensionInstallRecords)
        ?.split('\n')
        ?.mapNotNull { line ->
            val values = line.split('\t')
            if (values.size != 4 || values[0].isBlank() || values[1].isBlank()) null
            else values[0] to ExtensionInstallRecord(
                filePath = values[1],
                allowInPrivateBrowsing = values[2].toBoolean(),
                allowDataCollection = values[3].toBoolean(),
            )
        }
        ?.toMap()
        ?: emptyMap()

    private fun Preferences.savedTabs(): List<SavedTab> = runCatching {
        val tabs = JSONArray(get(Keys.openTabs).orEmpty())
        (0 until tabs.length()).mapNotNull { index ->
            val tab = tabs.optJSONObject(index) ?: return@mapNotNull null
            val url = tab.optString("url").takeIf(String::isNotBlank) ?: return@mapNotNull null
            SavedTab(
                url = url,
                isPrivate = tab.optBoolean("private"),
                pinned = tab.optBoolean("pinned"),
                groupId = tab.optString("groupId").takeIf(String::isNotBlank),
                id = tab.optString("id").takeIf(String::isNotBlank),
                title = tab.optString("title").takeIf(String::isNotBlank),
                sessionState = tab.optString("sessionState").takeIf(String::isNotBlank),
            )
        }
    }.getOrDefault(emptyList())

    private fun Preferences.savedTabGroups(): List<SavedTabGroup> = runCatching {
        val groups = JSONArray(get(Keys.tabGroups).orEmpty())
        (0 until groups.length()).mapNotNull { index ->
            val group = groups.optJSONObject(index) ?: return@mapNotNull null
            val id = group.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            SavedTabGroup(
                id = id,
                title = group.optString("title").ifBlank { "Tab group" },
                color = group.optLong("color", 0xFF4E4BB5L),
                collapsed = group.optBoolean("collapsed"),
            )
        }
    }.getOrDefault(emptyList())

    private fun Preferences.shortcutBindings(): Map<BrowserCommandId, KeyChord> = get(Keys.shortcutBindings)
        ?.split('\n')
        ?.mapNotNull { line ->
            val values = line.split('\t')
            val id = values.getOrNull(0)?.let { runCatching { BrowserCommandId.valueOf(it) }.getOrNull() }
            val chord = values.getOrNull(1)?.let(KeyChord::decode)
            if (id == null || chord == null) null else id to chord
        }
        ?.toMap()
        ?: emptyMap()

    private fun Preferences.sessionSnapshots(): List<SessionSnapshot> = runCatching {
        val snapshots = JSONArray(get(Keys.sessionSnapshots).orEmpty())
        (0 until snapshots.length()).mapNotNull { index ->
            val snapshot = snapshots.optJSONObject(index) ?: return@mapNotNull null
            val id = snapshot.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val tabs = snapshot.optJSONArray("tabs")?.let { array ->
                (0 until array.length()).mapNotNull { tabIndex ->
                    array.optJSONObject(tabIndex)?.toSavedTab()
                }
            }.orEmpty()
            if (tabs.isEmpty()) return@mapNotNull null
            val groups = snapshot.optJSONArray("groups")?.let { array ->
                (0 until array.length()).mapNotNull { groupIndex ->
                    array.optJSONObject(groupIndex)?.toSavedTabGroup()
                }
            }.orEmpty()
            SessionSnapshot(
                id = id,
                title = snapshot.optString("title").ifBlank { "Saved session" },
                createdAt = snapshot.optLong("createdAt", 0L),
                tabs = tabs,
                activeTabIndex = snapshot.optInt("activeTabIndex", 0),
                tabGroups = groups,
            )
        }
    }.getOrDefault(emptyList())

    private fun SessionSnapshot.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        put("activeTabIndex", activeTabIndex)
        put("tabs", JSONArray(tabs.map { it.toJson() }))
        put("groups", JSONArray(tabGroups.map { it.toJson() }))
    }

    private fun SavedTab.toJson(): JSONObject = JSONObject().apply {
        put("url", url)
        put("private", isPrivate)
        put("pinned", pinned)
        put("groupId", groupId)
        put("id", id)
        put("title", title)
        put("sessionState", sessionState)
    }

    private fun SavedTabGroup.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("color", color)
        put("collapsed", collapsed)
    }

    private fun JSONObject.toSavedTab(): SavedTab? {
        val url = optString("url").takeIf(String::isNotBlank) ?: return null
        return SavedTab(
            url = url,
            isPrivate = optBoolean("private"),
            pinned = optBoolean("pinned"),
            groupId = optString("groupId").takeIf(String::isNotBlank),
            id = optString("id").takeIf(String::isNotBlank),
            title = optString("title").takeIf(String::isNotBlank),
            sessionState = optString("sessionState").takeIf(String::isNotBlank),
        )
    }

    private fun JSONObject.toSavedTabGroup(): SavedTabGroup? {
        val id = optString("id").takeIf(String::isNotBlank) ?: return null
        return SavedTabGroup(
            id = id,
            title = optString("title").ifBlank { "Tab group" },
            color = optLong("color", 0xFF4E4BB5L),
            collapsed = optBoolean("collapsed"),
        )
    }

    private fun filterFromUrl(url: String, enabled: Boolean): AdBlockFilter =
        AdBlockFilter(url.substringAfterLast('/').ifBlank { url }, url, enabled)

    private fun defaultDesktopSites(): Boolean {
        val configuration = context.resources.configuration
        return configuration.screenWidthDp >= 600 || configuration.smallestScreenWidthDp >= 600
    }

    private companion object {
        const val MAX_SESSION_SNAPSHOTS = 20
        const val MAX_SESSION_SNAPSHOT_BYTES = 8 * 1024 * 1024
    }
}
