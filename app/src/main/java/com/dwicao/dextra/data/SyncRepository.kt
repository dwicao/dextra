package com.dwicao.dextra.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
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

data class SyncData(
    val root: JSONObject,
    val importedBookmarks: Int,
)

/** Password-encrypted, portable sync bundle. Credentials, extensions, and private tabs are excluded. */
class SyncRepository(private val context: Context, private val dao: BrowserDao) {
    suspend fun export(uri: Uri, settings: BrowserSettings, passphrase: String) {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        val root = JSONObject().apply {
            put("format", SyncCrypto.FORMAT)
            put("version", SyncCrypto.VERSION)
            put("createdAt", System.currentTimeMillis())
            put("settings", settingsJson(settings))
            put("history", JSONArray(dao.getHistory().map { JSONObject().apply {
                put("url", it.url); put("title", it.title); put("visitedAt", it.visitedAt)
            } }))
            put("bookmarks", JSONArray(dao.getBookmarks().map { JSONObject().apply {
                put("url", it.url); put("title", it.title); put("createdAt", it.createdAt); put("folder", it.folder)
            } }))
            put("readingList", JSONArray(dao.getReadingList().map { JSONObject().apply {
                put("url", it.url); put("title", it.title); put("savedAt", it.savedAt); put("isRead", it.isRead)
            } }))
            put("sitePermissions", JSONArray(dao.getSitePermissions().map { JSONObject().apply {
                put("origin", it.origin); put("permission", it.permission); put("decision", it.decision); put("updatedAt", it.updatedAt)
            } }))
            put("siteSettings", JSONArray(dao.getSiteSettings().map { JSONObject().apply {
                put("origin", it.origin); putOpt("desktopSites", it.desktopSites); putOpt("adBlockingEnabled", it.adBlockingEnabled)
                putOpt("userScriptsEnabled", it.userScriptsEnabled); putOpt("zoomPercent", it.zoomPercent); put("updatedAt", it.updatedAt)
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
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(envelope.toString().toByteArray(Charsets.UTF_8))
        } ?: error("Could not open sync destination")
    }

    suspend fun import(uri: Uri, passphrase: String): SyncData {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) { "Sync passphrase is too short" }
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: error("Could not open sync bundle")
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Sync bundle is too large" }
        val envelope = JSONObject(text)
        require(envelope.optString("format") == SyncCrypto.FORMAT && envelope.optInt("version") == SyncCrypto.VERSION) {
            "Unsupported sync bundle"
        }
        require(envelope.optInt("iterations") == 120_000) { "Unsupported sync encryption" }
        val encrypted = EncryptedSyncPayload(
            salt = Base64.getDecoder().decode(envelope.getString("salt")),
            iv = Base64.getDecoder().decode(envelope.getString("iv")),
            data = Base64.getDecoder().decode(envelope.getString("data")),
        )
        val root = JSONObject(String(SyncCrypto.decrypt(encrypted, passphrase), Charsets.UTF_8))
        require(root.optString("format") == SyncCrypto.FORMAT && root.optInt("version") == SyncCrypto.VERSION) {
            "Unsupported sync bundle"
        }
        var imported = 0
        root.optJSONArray("bookmarks")?.let { array ->
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
        root.optJSONArray("history")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_HISTORY)) array.getJSONObject(i).let {
                dao.insertHistory(HistoryEntry(
                    url = it.getString("url"),
                    title = it.getString("title"),
                    visitedAt = it.optLong("visitedAt", System.currentTimeMillis()),
                ))
            }
            dao.trimHistory()
        }
        root.optJSONArray("readingList")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_READING_LIST)) array.getJSONObject(i).let {
                dao.upsertReadingListEntry(ReadingListEntry(
                    url = it.getString("url"), title = it.getString("title"),
                    savedAt = it.optLong("savedAt", System.currentTimeMillis()), isRead = it.optBoolean("isRead"),
                ))
            }
        }
        root.optJSONArray("sitePermissions")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_SITE_PERMISSIONS)) array.getJSONObject(i).let {
                dao.upsertSitePermission(SitePermission(
                    it.getString("origin"), it.getString("permission"), it.getString("decision"),
                    it.optLong("updatedAt", System.currentTimeMillis()),
                ))
            }
        }
        root.optJSONArray("siteSettings")?.let { array ->
            for (i in 0 until array.length().coerceAtMost(MAX_SITE_SETTINGS)) array.getJSONObject(i).let {
                dao.upsertSiteSetting(SiteSetting(
                    it.getString("origin"), it.optBooleanOrNull("desktopSites"),
                    it.optBooleanOrNull("adBlockingEnabled"), it.optBooleanOrNull("userScriptsEnabled"),
                    it.optIntOrNull("zoomPercent"), it.optLong("updatedAt", System.currentTimeMillis()),
                ))
            }
        }
        return SyncData(root, imported)
    }

    private fun settingsJson(settings: BrowserSettings): JSONObject = JSONObject().apply {
        put("theme", settings.themeMode.name)
        put("searchEngine", settings.searchEngine.name)
        put("selectedCustomSearchEngineId", settings.selectedCustomSearchEngineId)
        put("customSearchEngines", JSONArray(settings.customSearchEngines.map {
            JSONObject().put("id", it.id).put("label", it.label).put("searchUrl", it.searchUrl)
        }))
        put("homepage", settings.homepage)
        put("desktopSites", settings.desktopSites)
        put("tabBarWithAddressBar", settings.tabBarWithAddressBar)
        put("verticalTabs", settings.verticalTabs)
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
