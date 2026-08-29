package com.dwicao.dextra.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
                put("url", it.url); put("title", it.title); put("savedAt", it.savedAt); put("isRead", it.isRead)
            } }))
            if (selection.sitePermissions) put("sitePermissions", JSONArray(dao.getSitePermissions().map { JSONObject().apply {
                put("origin", it.origin); put("permission", it.permission); put("decision", it.decision); put("updatedAt", it.updatedAt); put("profileId", it.profileId)
            } }))
            if (selection.siteSettings) put("siteSettings", JSONArray(dao.getSiteSettings().map { JSONObject().apply {
                put("origin", it.origin); putOpt("desktopSites", it.desktopSites); putOpt("adBlockingEnabled", it.adBlockingEnabled)
                putOpt("userScriptsEnabled", it.userScriptsEnabled); putOpt("zoomPercent", it.zoomPercent)
                putOpt("translationTarget", it.translationTarget); putOpt("httpsOnly", it.httpsOnly)
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
        return envelope.toString().toByteArray(Charsets.UTF_8)
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
            hasSettings = root.has("settings"),
        )
    }

    suspend fun importBytes(
        bytes: ByteArray,
        passphrase: String,
        targetProfileId: String = DEFAULT_WORKSPACE_ID,
        selection: SyncSelection = SyncSelection(),
    ): SyncData {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        require(bytes.size <= MAX_BYTES) { "Sync bundle is too large" }
        val root = decodeRoot(bytes, passphrase)
        var imported = 0
        if (selection.bookmarks) root.optJSONArray("bookmarks")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_BOOKMARKS)) array.getJSONObject(i).let {
                dao.insertBookmark(Bookmark(
                    url = it.getString("url"),
                    title = it.getString("title"),
                    createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                    folder = it.optString("folder").takeIf(String::isNotBlank),
                ))
                imported++
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
                dao.upsertReadingListEntry(ReadingListEntry(
                    url = it.getString("url"), title = it.getString("title"),
                    savedAt = it.optLong("savedAt", System.currentTimeMillis()), isRead = it.optBoolean("isRead"),
                ))
            }
        }
        if (selection.sitePermissions) root.optJSONArray("sitePermissions")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_SITE_PERMISSIONS)) array.getJSONObject(i).let {
                dao.upsertSitePermission(SitePermission(
                    it.getString("origin"), it.getString("permission"), it.getString("decision"),
                    it.optLong("updatedAt", System.currentTimeMillis()), profileId = targetProfileId,
                ))
            }
        }
        if (selection.siteSettings) root.optJSONArray("siteSettings")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_SITE_SETTINGS)) array.getJSONObject(i).let {
                dao.upsertSiteSetting(SiteSetting(
                    it.getString("origin"), it.optBooleanOrNull("desktopSites"),
                    it.optBooleanOrNull("adBlockingEnabled"), it.optBooleanOrNull("userScriptsEnabled"),
                    it.optIntOrNull("zoomPercent"), it.optString("translationTarget").takeIf(String::isNotBlank),
                    it.optLong("updatedAt", System.currentTimeMillis()), profileId = targetProfileId,
                    httpsOnly = it.optBooleanOrNull("httpsOnly"),
                ))
            }
        }
        return SyncData(root, imported, root.optJSONObject("settings")?.takeIf { selection.settings })
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

    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        const val MAX_BOOKMARKS = 10_000
        const val MAX_HISTORY = 500
        const val MAX_READING_LIST = 5_000
        const val MAX_SITE_PERMISSIONS = 5_000
        const val MAX_SITE_SETTINGS = 5_000
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}
