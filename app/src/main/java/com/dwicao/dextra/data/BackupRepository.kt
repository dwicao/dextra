package com.dwicao.dextra.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/** Versioned, portable backup for local browser data. Private tabs are never persisted here. */
class BackupRepository(private val context: Context, private val dao: BrowserDao) {
    suspend fun export(uri: Uri) {
        val root = JSONObject().apply {
            put("format", "dextra-backup")
            put("version", 1)
            put("createdAt", System.currentTimeMillis())
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
                 putOpt("userScriptsEnabled", it.userScriptsEnabled); putOpt("zoomPercent", it.zoomPercent)
                 putOpt("translationTarget", it.translationTarget); put("updatedAt", it.updatedAt)
             } }))
             put("installedWebApps", JSONArray(dao.getInstalledWebApps().map { JSONObject().apply {
                 put("id", it.id); put("origin", it.origin); put("name", it.name)
                put("startUrl", it.startUrl); put("scope", it.scope); put("installedAt", it.installedAt); put("iconUrl", it.iconUrl)
             } }))
        }
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
            ?: error("Could not open backup destination")
    }

    suspend fun import(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: error("Could not open backup")
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is too large" }
        val root = JSONObject(text)
        require(root.optString("format") == "dextra-backup") { "Unsupported backup format" }
        require(root.optInt("version") == 1) { "Unsupported backup version" }
        var imported = 0
        root.optJSONArray("bookmarks")?.let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).let {
                dao.insertBookmark(Bookmark(url = it.getString("url"), title = it.getString("title"), createdAt = it.optLong("createdAt", System.currentTimeMillis()), folder = it.optString("folder").takeIf(String::isNotBlank)))
                imported++
            }
        }
        root.optJSONArray("history")?.let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).let {
                dao.insertHistory(HistoryEntry(url = it.getString("url"), title = it.getString("title"), visitedAt = it.optLong("visitedAt", System.currentTimeMillis())))
            }
        }
        root.optJSONArray("readingList")?.let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).let {
                dao.upsertReadingListEntry(ReadingListEntry(url = it.getString("url"), title = it.getString("title"), savedAt = it.optLong("savedAt", System.currentTimeMillis()), isRead = it.optBoolean("isRead")))
            }
        }
        root.optJSONArray("sitePermissions")?.let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).let {
                dao.upsertSitePermission(SitePermission(it.getString("origin"), it.getString("permission"), it.getString("decision"), it.optLong("updatedAt", System.currentTimeMillis())))
            }
        }
        root.optJSONArray("siteSettings")?.let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).let {
                 dao.upsertSiteSetting(SiteSetting(
                     origin = it.getString("origin"),
                     desktopSites = it.optBooleanOrNull("desktopSites"),
                     adBlockingEnabled = it.optBooleanOrNull("adBlockingEnabled"),
                     userScriptsEnabled = it.optBooleanOrNull("userScriptsEnabled"),
                     zoomPercent = it.optIntOrNull("zoomPercent"),
                     translationTarget = it.optString("translationTarget").takeIf(String::isNotBlank),
                     updatedAt = it.optLong("updatedAt", System.currentTimeMillis()),
                 ))
            }
        }
        root.optJSONArray("installedWebApps")?.let { array ->
            for (i in 0 until array.length()) array.getJSONObject(i).let {
                dao.upsertInstalledWebApp(
                    InstalledWebApp(
                        id = it.getString("id"),
                        origin = it.getString("origin"),
                        name = it.getString("name"),
                        startUrl = it.getString("startUrl"),
                        scope = it.getString("scope"),
                        installedAt = it.optLong("installedAt", System.currentTimeMillis()),
                        iconUrl = it.optString("iconUrl").takeIf(String::isNotBlank),
                    ),
                )
            }
        }
        return imported
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? = if (has(key) && !isNull(key)) getBoolean(key) else null
    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null

    private companion object { const val MAX_BYTES = 20 * 1024 * 1024 }
}
