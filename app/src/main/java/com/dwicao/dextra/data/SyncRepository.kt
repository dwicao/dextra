package com.dwicao.dextra.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object SyncCrypto {
    const val FORMAT = "dextra-encrypted-sync"
    const val VERSION = 1
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private val random = SecureRandom()

    fun encrypt(payload: ByteArray, passphrase: String): EncryptedSyncPayload {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        return EncryptedSyncPayload(salt, iv, cipher.doFinal(payload))
    }

    fun decrypt(encrypted: EncryptedSyncPayload, passphrase: String): ByteArray {
        val salt = encrypted.salt
        val iv = encrypted.iv
        val data = encrypted.data
        require(salt.size == SALT_BYTES && iv.size == IV_BYTES && data.size <= MAX_BYTES) { "Invalid sync bundle" }
        return Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            doFinal(data)
        }
    }

    private fun key(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }

    private const val MAX_BYTES = 20 * 1024 * 1024
}

internal data class EncryptedSyncPayload(
    val salt: ByteArray,
    val iv: ByteArray,
    val data: ByteArray,
)

data class SyncSelection(
    val settings: Boolean = true,
    val bookmarks: Boolean = true,
    val history: Boolean = true,
    val readingList: Boolean = true,
    val sitePermissions: Boolean = true,
    val siteSettings: Boolean = true,
)

data class SyncPreview(
    val createdAt: Long,
    val bookmarkCount: Int,
    val historyCount: Int,
    val readingListCount: Int,
    val permissionCount: Int,
    val siteSettingCount: Int,
    val collectionCount: Int,
    val hasSettings: Boolean,
)

data class SyncData(
    val root: JSONObject,
    val importedBookmarks: Int,
    val importedSettings: JSONObject? = null,
)

/** Password-encrypted, portable sync bundle. Credentials, extensions, and private tabs are excluded. */
class SyncRepository(private val context: Context, private val dao: BrowserDao) {
    suspend fun export(uri: Uri, settings: BrowserSettings, passphrase: String, selection: SyncSelection = SyncSelection()) {
        val bytes = exportBytes(settings, passphrase, selection)
        context.contentResolver.openOutputStream(uri)?.use { output -> output.write(bytes) }
            ?: error("Could not open sync destination")
    }

    suspend fun exportBytes(settings: BrowserSettings, passphrase: String, selection: SyncSelection = SyncSelection()): ByteArray {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        val root = JSONObject().apply {
            put("format", SyncCrypto.FORMAT)
            put("version", SyncCrypto.VERSION)
            put("createdAt", System.currentTimeMillis())
            if (selection.settings) put("settings", settingsJson(settings))
            if (selection.history) put("history", JSONArray(dao.getHistory().map { JSONObject().apply {
                put("url", it.url); put("title", it.title); put("visitedAt", it.visitedAt)
            } }))
            if (selection.bookmarks) put("bookmarks", JSONArray(dao.getBookmarks().map { JSONObject().apply {
                put("url", it.url); put("title", it.title); put("createdAt", it.createdAt); put("folder", it.folder)
            } }))
            if (selection.readingList) put("readingList", JSONArray(dao.getReadingList().map { JSONObject().apply {
                put("url", it.url); put("title", it.title); put("savedAt", it.savedAt); put("isRead", it.isRead); put("annotation", it.annotation)
            } }))
            if (selection.sitePermissions) put("sitePermissions", JSONArray(dao.getSitePermissions().map { JSONObject().apply {
                put("origin", it.origin); put("permission", it.permission); put("decision", it.decision); put("updatedAt", it.updatedAt); put("profileId", it.profileId)
            } }))
            if (selection.siteSettings) put("siteSettings", JSONArray(dao.getSiteSettings().map { JSONObject().apply {
                put("origin", it.origin); putOpt("desktopSites", it.desktopSites); putOpt("adBlockingEnabled", it.adBlockingEnabled)
                putOpt("userScriptsEnabled", it.userScriptsEnabled); putOpt("zoomPercent", it.zoomPercent)
                putOpt("translationTarget", it.translationTarget); putOpt("httpsOnly", it.httpsOnly); putOpt("cookieBannerMode", it.cookieBannerMode)
                put("updatedAt", it.updatedAt); put("profileId", it.profileId)
            } }))
        }
        val payload = root.toString().toByteArray(Charsets.UTF_8)
        require(payload.size <= MAX_BYTES) { "Sync bundle is too large" }
        val encrypted = SyncCrypto.encrypt(payload, passphrase)
        val envelope = JSONObject()
            .put("format", SyncCrypto.FORMAT)
            .put("version", SyncCrypto.VERSION)
            .put("kdf", "PBKDF2WithHmacSHA256")
            .put("iterations", 120_000)
            .put("salt", Base64.getEncoder().encodeToString(encrypted.salt))
            .put("iv", Base64.getEncoder().encodeToString(encrypted.iv))
            .put("data", Base64.getEncoder().encodeToString(encrypted.data))
        return envelope.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size <= MAX_BYTES) { "Encrypted sync bundle is too large" }
        }
    }

    suspend fun import(
        uri: Uri,
        passphrase: String,
        targetProfileId: String = DEFAULT_WORKSPACE_ID,
        selection: SyncSelection = SyncSelection(),
    ): SyncData {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            readBounded(input, MAX_BYTES)
        } ?: error("Could not open sync bundle")
        return importBytes(bytes, passphrase, targetProfileId, selection)
    }

    suspend fun preview(uri: Uri, passphrase: String): SyncPreview {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input -> readBounded(input, MAX_BYTES) }
            ?: error("Could not open sync bundle")
        return previewBytes(bytes, passphrase)
    }

    suspend fun previewBytes(bytes: ByteArray, passphrase: String): SyncPreview {
        val root = decodeRoot(bytes, passphrase)
        return SyncPreview(
            createdAt = root.optLong("createdAt", 0L),
            bookmarkCount = root.optJSONArray("bookmarks")?.length() ?: 0,
            historyCount = root.optJSONArray("history")?.length() ?: 0,
            readingListCount = root.optJSONArray("readingList")?.length() ?: 0,
            permissionCount = root.optJSONArray("sitePermissions")?.length() ?: 0,
            siteSettingCount = root.optJSONArray("siteSettings")?.length() ?: 0,
            collectionCount = root.optJSONObject("settings")?.optJSONArray("tabCollections")?.length() ?: 0,
            hasSettings = root.has("settings"),
        )
    }

    suspend fun contentFingerprint(bytes: ByteArray, passphrase: String): String {
        val root = decodeRoot(bytes, passphrase)
        root.remove("createdAt")
        return MessageDigest.getInstance("SHA-256")
            .digest(root.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    suspend fun conflictingTabIds(bytes: ByteArray, passphrase: String, local: BrowserSettings): List<String> {
        val remoteSettings = decodeRoot(bytes, passphrase).optJSONObject("settings") ?: return emptyList()
        val localTabs = buildMap {
            local.openTabs.forEach { tab -> tab.id?.let { put(it, tab) } }
            local.workspaces.forEach { workspace ->
                workspace.tabs.forEach { tab -> tab.id?.let { put(it, tab) } }
            }
        }
        val remoteTabs = buildList {
            fun collect(array: JSONArray?) {
                if (array == null) return
                for (index in 0 until array.length()) {
                    val tab = array.optJSONObject(index) ?: continue
                    val id = tab.optString("id").takeIf(String::isNotBlank) ?: continue
                    add(id to tab)
                }
            }
            collect(remoteSettings.optJSONArray("openTabs"))
            remoteSettings.optJSONArray("workspaces")?.let { workspaces ->
                for (index in 0 until workspaces.length()) {
                    collect(workspaces.optJSONObject(index)?.optJSONArray("tabs"))
                }
            }
        }
        return remoteTabs.mapNotNull { (id, remoteTab) ->
            val localTab = localTabs[id] ?: return@mapNotNull null
            if (localTab.url != remoteTab.optString("url") ||
                localTab.title.orEmpty() != remoteTab.optString("title") ||
                localTab.sessionState.orEmpty() != remoteTab.optString("sessionState")
            ) id else null
        }.distinct().take(64)
    }

    suspend fun importBytes(
        bytes: ByteArray,
        passphrase: String,
        targetProfileId: String = DEFAULT_WORKSPACE_ID,
        selection: SyncSelection = SyncSelection(),
        preserveSourceProfiles: Boolean = false,
        mergeRecords: Boolean = false,
    ): SyncData {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        require(bytes.size <= MAX_BYTES) { "Sync bundle is too large" }
        val root = decodeRoot(bytes, passphrase)
        val importedWorkspaceIds = if (selection.settings || preserveSourceProfiles) {
            root.optJSONObject("settings")?.optJSONArray("workspaces")?.let { workspaces ->
                (0 until workspaces.length()).mapNotNull { index ->
                    workspaces.optJSONObject(index)?.optString("id")?.takeIf(String::isNotBlank)
                }.toSet()
            }.orEmpty()
        } else {
            emptySet()
        }
        val existingBookmarks = if (mergeRecords) dao.getBookmarks().associateBy { it.url } else emptyMap()
        val existingReadingList = if (mergeRecords) dao.getReadingList().associateBy { it.url } else emptyMap()
        var imported = 0
        if (selection.bookmarks) root.optJSONArray("bookmarks")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_BOOKMARKS)) array.getJSONObject(i).let {
                val bookmark = Bookmark(
                    url = it.getString("url"),
                    title = it.getString("title"),
                    createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                    folder = it.optString("folder").takeIf(String::isNotBlank),
                )
                if (!mergeRecords || (existingBookmarks[bookmark.url]?.createdAt ?: Long.MIN_VALUE) <= bookmark.createdAt) {
                    dao.insertBookmark(bookmark)
                    imported++
                }
            }
        }
        if (selection.history) root.optJSONArray("history")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_HISTORY)) array.getJSONObject(i).let {
                val url = it.getString("url")
                val visitedAt = it.optLong("visitedAt", System.currentTimeMillis())
                if (!dao.hasHistory(url, visitedAt)) {
                    dao.insertHistory(HistoryEntry(url = url, title = it.getString("title"), visitedAt = visitedAt))
                }
            }
            dao.trimHistory()
        }
        if (selection.readingList) root.optJSONArray("readingList")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_READING_LIST)) array.getJSONObject(i).let {
                val entry = ReadingListEntry(
                    url = it.getString("url"), title = it.getString("title"),
                    savedAt = it.optLong("savedAt", System.currentTimeMillis()), isRead = it.optBoolean("isRead"),
                    annotation = it.optString("annotation").takeIf(String::isNotBlank)?.take(2_000),
                )
                if (!mergeRecords || (existingReadingList[entry.url]?.savedAt ?: Long.MIN_VALUE) <= entry.savedAt) {
                    dao.upsertReadingListEntry(entry)
                }
            }
        }
        if (selection.sitePermissions) root.optJSONArray("sitePermissions")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_SITE_PERMISSIONS)) array.getJSONObject(i).let {
                val permission = SitePermission(
                    it.getString("origin"), it.getString("permission"), it.getString("decision"),
                    it.optLong("updatedAt", System.currentTimeMillis()),
                    profileId = it.optString("profileId").takeIf { id -> id in importedWorkspaceIds } ?: targetProfileId,
                )
                val existing = dao.getSitePermission(permission.profileId, permission.origin, permission.permission)
                if (!mergeRecords || existing == null || existing.updatedAt <= permission.updatedAt) {
                    dao.upsertSitePermission(permission)
                }
            }
        }
        if (selection.siteSettings) root.optJSONArray("siteSettings")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_SITE_SETTINGS)) array.getJSONObject(i).let {
                val setting = SiteSetting(
                    it.getString("origin"), it.optBooleanOrNull("desktopSites"),
                    it.optBooleanOrNull("adBlockingEnabled"), it.optBooleanOrNull("userScriptsEnabled"),
                    it.optIntOrNull("zoomPercent"), it.optString("translationTarget").takeIf(String::isNotBlank),
                    it.optLong("updatedAt", System.currentTimeMillis()),
                    profileId = it.optString("profileId").takeIf { id -> id in importedWorkspaceIds } ?: targetProfileId,
                    httpsOnly = it.optBooleanOrNull("httpsOnly"),
                    cookieBannerMode = it.optIntOrNull("cookieBannerMode"),
                )
                val existing = dao.getSiteSetting(setting.profileId, setting.origin)
                if (!mergeRecords || existing == null || existing.updatedAt <= setting.updatedAt) {
                    dao.upsertSiteSetting(setting)
                }
            }
        }
        return SyncData(root, imported, root.optJSONObject("settings")?.takeIf { selection.settings })
    }

    fun mergeSettings(remoteRoot: JSONObject, local: BrowserSettings): JSONObject {
        val merged = settingsJson(local)
        val remote = remoteRoot.optJSONObject("settings") ?: return merged
        val tombstones = mergeTombstones(merged.optJSONObject("tabTombstones"), remote.optJSONObject("tabTombstones"))
        val tombstoneIds = tombstones.keys().asSequence().toSet()
        merged.put("tabTombstones", tombstones)
        merged.put("openTabs", filterTombstonedTabs(
            mergeTabArrays(merged.optJSONArray("openTabs"), remote.optJSONArray("openTabs"), MAX_SYNC_TABS),
            tombstoneIds,
        ))
        merged.put("tabGroups", mergeObjectArrays(merged.optJSONArray("tabGroups"), remote.optJSONArray("tabGroups"), "id", MAX_SYNC_GROUPS))
        merged.put("workspaces", filterTombstonedWorkspaces(
            mergeWorkspaces(merged.optJSONArray("workspaces"), remote.optJSONArray("workspaces")),
            tombstoneIds,
        ))
        merged.put("tabCollections", filterTombstonedCollections(
            mergeCollections(merged.optJSONArray("tabCollections"), remote.optJSONArray("tabCollections")),
            tombstoneIds,
        ))
        return merged
    }

    private fun decodeRoot(bytes: ByteArray, passphrase: String): JSONObject {
        require(bytes.size <= MAX_BYTES) { "Sync bundle is too large" }
        val envelope = JSONObject(String(bytes, Charsets.UTF_8))
        require(envelope.optString("format") == SyncCrypto.FORMAT && envelope.optInt("version") == SyncCrypto.VERSION) {
            "Unsupported sync bundle"
        }
        require(envelope.optInt("iterations") == 120_000) { "Unsupported sync encryption" }
        val encrypted = EncryptedSyncPayload(
            salt = Base64.getDecoder().decode(envelope.getString("salt")),
            iv = Base64.getDecoder().decode(envelope.getString("iv")),
            data = Base64.getDecoder().decode(envelope.getString("data")),
        )
        return JSONObject(String(SyncCrypto.decrypt(encrypted, passphrase), Charsets.UTF_8)).also { root ->
            require(root.optString("format") == SyncCrypto.FORMAT && root.optInt("version") == SyncCrypto.VERSION) {
                "Unsupported sync bundle"
            }
        }
    }

    private fun readBounded(input: java.io.InputStream, maximum: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            if (output.size() + count > maximum) error("Sync bundle is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun settingsJson(settings: BrowserSettings): JSONObject = JSONObject().apply {
        put("theme", settings.themeMode.name)
        put("searchEngine", settings.searchEngine.name)
        put("selectedCustomSearchEngineId", settings.selectedCustomSearchEngineId)
        put("customSearchEngines", JSONArray(settings.customSearchEngines.map {
            JSONObject().put("id", it.id).put("label", it.label).put("searchUrl", it.searchUrl)
        }))
        put("activeWorkspaceId", settings.activeWorkspaceId)
        put("activeTabIndex", settings.activeTabIndex)
        put("openTabs", JSONArray(settings.openTabs.filterNot { it.isPrivate }.map(::savedTabJson)))
        put("tabGroups", JSONArray(settings.tabGroups.map(::tabGroupJson)))
        put("workspaces", JSONArray(settings.workspaces.map { workspace ->
            JSONObject().apply {
                put("id", workspace.id)
                put("title", workspace.title)
                put("color", workspace.color)
                put("contextId", workspace.contextId)
                put("createdAt", workspace.createdAt)
                put("lastUsedAt", workspace.lastUsedAt)
                put("activeTabIndex", workspace.activeTabIndex)
                put("tabs", JSONArray(workspace.tabs.filterNot { it.isPrivate }.map(::savedTabJson)))
                put("groups", JSONArray(workspace.tabGroups.map(::tabGroupJson)))
            }
        }))
        put("homepage", settings.homepage)
        put("startPage", JSONObject().apply {
            put("showQuickLinks", settings.startPage.showQuickLinks)
            put("showPrivacyTip", settings.startPage.showPrivacyTip)
            put("customLinks", JSONArray(settings.startPage.customLinks.map { link ->
                JSONObject().put("id", link.id).put("label", link.label).put("url", link.url)
            }))
        })
        put("desktopSites", settings.desktopSites)
        put("httpsOnly", settings.httpsOnly)
        put("cookieBannerMode", settings.cookieBannerMode)
        put("historyRetentionDays", settings.historyRetentionDays)
        put("downloadRetentionDays", settings.downloadRetentionDays)
        put("recoveryRetentionDays", settings.recoveryRetentionDays)
        put("clearSiteDataOnExit", settings.clearSiteDataOnExit)
        put("privacyCleanupAllowlist", JSONArray(settings.privacyCleanupAllowlist.toList()))
        put("autoSuspendMinutes", settings.autoSuspendMinutes)
        put("permissionExpiryDays", settings.permissionExpiryDays)
        put("fingerprintingProtectionEnabled", settings.fingerprintingProtectionEnabled)
        put("dexLayoutPreset", settings.dexLayoutPreset.name)
        put("tabTombstones", JSONObject(settings.tabTombstones))
        put("tabCollections", JSONArray(settings.tabCollections.map { collection ->
            JSONObject().apply {
                put("id", collection.id)
                put("title", collection.title)
                put("note", collection.note)
                put("tags", JSONArray(collection.tags))
                put("createdAt", collection.createdAt)
                put("updatedAt", collection.updatedAt)
                put("tabs", JSONArray(collection.tabs.filterNot { it.isPrivate }.map(::savedTabJson)))
            }
        }))
        put("tabBarWithAddressBar", settings.tabBarWithAddressBar)
        put("verticalTabs", settings.verticalTabs)
        put("accessibilityTextScale", settings.accessibilityTextScale)
        put("highContrast", settings.highContrast)
        put("reduceMotion", settings.reduceMotion)
        put("dnsOverHttpsEnabled", settings.dnsOverHttpsEnabled)
        put("dnsProvider", settings.dnsProvider.name)
        put("adBlockingEnabled", settings.adBlockingEnabled)
        put("adBlockFilters", JSONArray(settings.adBlockFilters.map { it.url }))
        put("disabledAdBlockFilters", JSONArray(settings.adBlockFilters.filterNot { it.enabled }.map { it.url }))
        put("userScriptUrls", JSONArray(settings.userScriptUrls))
        put("disabledUserScripts", JSONArray(settings.disabledUserScriptUrls.toList()))
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? = if (has(key) && !isNull(key)) getBoolean(key) else null
    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null

    private fun mergeWorkspaces(local: JSONArray?, remote: JSONArray?): JSONArray {
        val values = linkedMapOf<String, JSONObject>()
        fun add(value: JSONObject) {
            val id = value.optString("id").takeIf(String::isNotBlank) ?: return
            val existing = values[id]
            values[id] = if (existing == null) {
                value
            } else {
                JSONObject(existing.toString()).apply {
                    put("tabs", mergeTabArrays(existing.optJSONArray("tabs"), value.optJSONArray("tabs"), MAX_SYNC_TABS))
                    put("groups", mergeObjectArrays(existing.optJSONArray("groups"), value.optJSONArray("groups"), "id", MAX_SYNC_GROUPS))
                }
            }
        }
        local?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        remote?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        return JSONArray(values.values.take(MAX_SYNC_WORKSPACES))
    }

    private fun mergeCollections(local: JSONArray?, remote: JSONArray?): JSONArray {
        val values = linkedMapOf<String, JSONObject>()
        fun add(value: JSONObject) {
            val id = value.optString("id").takeIf(String::isNotBlank) ?: return
            val existing = values[id]
            values[id] = if (existing == null) {
                JSONObject(value.toString())
            } else {
                val newer = value.optLong("updatedAt") >= existing.optLong("updatedAt")
                JSONObject((if (newer) value else existing).toString()).apply {
                    if (newer) {
                        put("title", value.optString("title").take(60))
                        put("note", value.optString("note").take(240))
                    }
                    put("tags", mergeStringArrays(existing.optJSONArray("tags"), value.optJSONArray("tags"), MAX_COLLECTION_TAGS))
                    put("tabs", mergeTabArrays(existing.optJSONArray("tabs"), value.optJSONArray("tabs"), MAX_SYNC_COLLECTION_TABS))
                }
            }
        }
        local?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        remote?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        return JSONArray(values.values.take(MAX_SYNC_COLLECTIONS))
    }

    private fun mergeTabArrays(local: JSONArray?, remote: JSONArray?, limit: Int): JSONArray {
        val values = linkedMapOf<String, JSONObject>()
        fun add(value: JSONObject) {
            val key = value.optString("id").takeIf(String::isNotBlank)
                ?: value.optString("url").takeIf(String::isNotBlank)
                ?: return
            values.putIfAbsent(key, value)
        }
        local?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        remote?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        return JSONArray(values.values.take(limit))
    }

    private fun mergeTombstones(local: JSONObject?, remote: JSONObject?): JSONObject {
        val values = linkedMapOf<String, Long>()
        fun add(value: JSONObject?) {
            value?.keys()?.forEach { id ->
                val timestamp = value.optLong(id, Long.MIN_VALUE)
                if (timestamp != Long.MIN_VALUE) values[id] = maxOf(values[id] ?: Long.MIN_VALUE, timestamp)
            }
        }
        add(local)
        add(remote)
        return JSONObject(values.entries.sortedByDescending { it.value }.take(MAX_SYNC_TOMBSTONES).associate { it.key to it.value })
    }

    private fun filterTombstonedTabs(tabs: JSONArray, tombstoneIds: Set<String>): JSONArray = JSONArray().also { output ->
        for (index in 0 until tabs.length()) {
            val tab = tabs.optJSONObject(index) ?: continue
            if (tab.optString("id") !in tombstoneIds) output.put(tab)
        }
    }

    private fun filterTombstonedWorkspaces(workspaces: JSONArray, tombstoneIds: Set<String>): JSONArray = JSONArray().also { output ->
        for (index in 0 until workspaces.length()) {
            val workspace = workspaces.optJSONObject(index) ?: continue
            workspace.put("tabs", filterTombstonedTabs(workspace.optJSONArray("tabs") ?: JSONArray(), tombstoneIds))
            output.put(workspace)
        }
    }

    private fun filterTombstonedCollections(collections: JSONArray, tombstoneIds: Set<String>): JSONArray = JSONArray().also { output ->
        for (index in 0 until collections.length()) {
            val collection = collections.optJSONObject(index) ?: continue
            collection.put("tabs", filterTombstonedTabs(collection.optJSONArray("tabs") ?: JSONArray(), tombstoneIds))
            if (collection.optJSONArray("tabs")?.length() != 0) output.put(collection)
        }
    }

    private fun mergeStringArrays(local: JSONArray?, remote: JSONArray?, limit: Int): JSONArray = JSONArray().also { output ->
        val values = linkedSetOf<String>()
        fun add(array: JSONArray?) {
            if (array == null) return
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf(String::isNotBlank)?.let(values::add)
            }
        }
        add(local)
        add(remote)
        values.take(limit).forEach(output::put)
    }

    private fun mergeObjectArrays(local: JSONArray?, remote: JSONArray?, keyName: String, limit: Int): JSONArray {
        val values = linkedMapOf<String, JSONObject>()
        fun add(value: JSONObject) {
            val key = value.optString(keyName).takeIf(String::isNotBlank) ?: return
            values.putIfAbsent(key, value)
        }
        local?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        remote?.let { array -> for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add) }
        return JSONArray(values.values.take(limit))
    }

    private fun savedTabJson(tab: SavedTab): JSONObject = JSONObject().apply {
        put("url", tab.url)
        put("private", false)
        put("pinned", tab.pinned)
        put("groupId", tab.groupId)
        put("id", tab.id)
        put("title", tab.title)
        put("sessionState", tab.sessionState)
    }

    private fun tabGroupJson(group: SavedTabGroup): JSONObject = JSONObject().apply {
        put("id", group.id)
        put("title", group.title)
        put("color", group.color)
        put("collapsed", group.collapsed)
    }

    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        const val MAX_BOOKMARKS = 10_000
        const val MAX_HISTORY = 500
        const val MAX_READING_LIST = 5_000
        const val MAX_SITE_PERMISSIONS = 5_000
        const val MAX_SITE_SETTINGS = 5_000
        const val MAX_SYNC_TABS = 64
        const val MAX_SYNC_GROUPS = 100
        const val MAX_SYNC_WORKSPACES = 12
        const val MAX_SYNC_COLLECTIONS = 50
        const val MAX_SYNC_COLLECTION_TABS = 64
        const val MAX_COLLECTION_TAGS = 12
        const val MAX_SYNC_TOMBSTONES = 512
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}
