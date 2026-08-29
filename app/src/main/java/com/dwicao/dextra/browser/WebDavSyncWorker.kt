package com.dwicao.dextra.browser

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.SettingsRepository
import com.dwicao.dextra.data.SyncRepository
import com.dwicao.dextra.data.SyncSelection
import com.dwicao.dextra.data.WebDavConfig
import com.dwicao.dextra.data.WebDavSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.BadPaddingException

class WebDavSyncWorker(
    appContext: Context,
    workerParams: androidx.work.WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = webDavSyncMutex.withLock {
        withContext(Dispatchers.IO) {
        val store = WebDavSettingsStore(applicationContext)
        val config = store.load() ?: return@withContext Result.success()
        if (!config.enabled) return@withContext Result.success()
        if (!isHttps(config.endpoint) || config.remoteFile.isBlank() || config.remoteFile.contains("..") ||
            !config.remoteFile.matches(Regex("[A-Za-z0-9._/-]+")) || config.passphrase.length < 8
        ) {
            return@withContext Result.failure()
        }

        runCatching {
            val dao = BrowserDatabase.get(applicationContext).browserDao()
            val settingsRepository = SettingsRepository(applicationContext)
            val syncRepository = SyncRepository(applicationContext, dao)
            val remote = WebDavClient.fetch(config)
            val remoteFingerprint = remote?.let { syncRepository.contentFingerprint(it.bytes, config.passphrase) }
            val remoteChanged = remote != null && if (config.lastRemoteFingerprint != null) {
                remoteFingerprint != config.lastRemoteFingerprint
            } else {
                config.lastEtag == null || remote.etag != config.lastEtag
            }
            val settingsBeforeApply = settingsRepository.settings.first()
            val localBundle = syncRepository.exportBytes(settingsBeforeApply, config.passphrase)
            val localFingerprint = syncRepository.contentFingerprint(localBundle, config.passphrase)
            val localChanged = config.lastLocalFingerprint?.let { it != localFingerprint }
                ?: config.lastLocalRevision?.let { it != settingsBeforeApply.openTabsRevision }
                ?: settingsBeforeApply.openTabs.isNotEmpty()
            if (config.conflictPending) {
                val resolution = config.pendingResolution
                if (resolution == null) throw WebDavConflictException()
                when (resolution) {
                    "remote" -> {
                        val remoteDocument = remote ?: throw WebDavConflictException()
                        val imported = syncRepository.importBytes(remoteDocument.bytes, config.passphrase, settingsRepository.settings.first().activeWorkspaceId)
                        imported.root.optJSONObject("settings")?.let { settingsRepository.applySyncSettings(it) }
                        if (!saveIfCurrent(store, config, config.copy(
                            lastEtag = remoteDocument.etag,
                            lastRemoteFingerprint = remoteFingerprint,
                            lastLocalRevision = settingsRepository.settings.first().openTabsRevision,
                            lastLocalFingerprint = syncRepository.contentFingerprint(
                                syncRepository.exportBytes(settingsRepository.settings.first(), config.passphrase),
                                config.passphrase,
                            ),
                            lastSyncAt = System.currentTimeMillis(),
                            conflictPending = false,
                            conflictDetectedAt = null,
                            pendingResolution = null,
                            lastError = null,
                        ))) return@runCatching
                    }
                    "local", "merge" -> {
                        if (resolution == "merge" && remote != null) {
                            val remoteDocument = remote
                            val localSettings = settingsRepository.settings.first()
                            val imported = syncRepository.importBytes(
                                remoteDocument.bytes,
                                config.passphrase,
                                localSettings.activeWorkspaceId,
                                SyncSelection(settings = false),
                                preserveSourceProfiles = true,
                                mergeRecords = true,
                            )
                            settingsRepository.applySyncSettings(
                                syncRepository.mergeSettings(imported.root, localSettings),
                            )
                        }
                        val settings = settingsRepository.settings.first()
                        val localBytes = syncRepository.exportBytes(settings, config.passphrase)
                        val uploaded = WebDavClient.putAtomically(
                            config,
                            localBytes,
                            remote?.etag,
                        )
                        if (!saveIfCurrent(store, config, config.copy(
                            lastEtag = uploaded.etag ?: remote?.etag,
                            lastRemoteFingerprint = syncRepository.contentFingerprint(uploaded.bytes, config.passphrase),
                            lastLocalFingerprint = syncRepository.contentFingerprint(uploaded.bytes, config.passphrase),
                            lastLocalRevision = settingsRepository.settings.first().openTabsRevision,
                            lastSyncAt = System.currentTimeMillis(),
                            conflictPending = false,
                            conflictDetectedAt = null,
                            pendingResolution = null,
                            lastError = null,
                        ))) return@runCatching
                    }
                    else -> throw IOException("Unknown WebDAV conflict resolution")
                }
                return@runCatching
            }
            if (SyncSessionPolicy.hasConflict(remoteChanged, localChanged)) throw WebDavConflictException()
            if (remote != null && remoteChanged) {
                val imported = syncRepository.importBytes(remote.bytes, config.passphrase, settingsRepository.settings.first().activeWorkspaceId)
                imported.root.optJSONObject("settings")?.let { importedSettings ->
                    settingsRepository.applySyncSettings(importedSettings)
                }
                if (!saveIfCurrent(store, config, config.copy(
                    lastEtag = remote.etag,
                    lastRemoteFingerprint = remoteFingerprint,
                    lastLocalRevision = settingsRepository.settings.first().openTabsRevision,
                    lastLocalFingerprint = run {
                        val currentSettings = settingsRepository.settings.first()
                        syncRepository.contentFingerprint(
                            syncRepository.exportBytes(currentSettings, config.passphrase),
                            config.passphrase,
                        )
                    },
                    lastSyncAt = System.currentTimeMillis(),
                    conflictPending = false,
                    conflictDetectedAt = null,
                    pendingResolution = null,
                    lastError = null,
                ))) return@runCatching
                return@runCatching
            }
            if (remote != null && !localChanged) {
                if (!saveIfCurrent(store, config, config.copy(
                    lastEtag = remote.etag,
                    lastRemoteFingerprint = remoteFingerprint,
                    lastSyncAt = System.currentTimeMillis(),
                    lastError = null,
                ))) return@runCatching
                return@runCatching
            }
            val settings = settingsRepository.settings.first()
            val uploadBytes = syncRepository.exportBytes(settings, config.passphrase)
            val uploaded = WebDavClient.putAtomically(config, uploadBytes, remote?.etag)
            if (!saveIfCurrent(store, config, config.copy(
                lastEtag = uploaded.etag ?: remote?.etag,
                lastRemoteFingerprint = syncRepository.contentFingerprint(uploaded.bytes, config.passphrase),
                lastLocalRevision = settings.openTabsRevision,
                lastLocalFingerprint = syncRepository.contentFingerprint(uploaded.bytes, config.passphrase),
                lastSyncAt = System.currentTimeMillis(),
                conflictPending = false,
                conflictDetectedAt = null,
                pendingResolution = null,
                lastError = null,
            ))) return@runCatching
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error is WebDavConflictException) {
                    saveIfCurrent(store, config, config.copy(
                        conflictPending = true,
                        conflictDetectedAt = System.currentTimeMillis(),
                        pendingResolution = null,
                        lastError = "Remote bundle changed while uploading",
                    ))
                    Result.failure()
                } else if (error is BadPaddingException || error is IllegalArgumentException || error is org.json.JSONException) {
                    saveIfCurrent(store, config, config.copy(lastError = error.message ?: "Invalid sync bundle"))
                    Result.failure()
                } else {
                    Result.retry()
                }
            },
        )
        }
    }

    private fun isHttps(value: String): Boolean = value.startsWith("https://", ignoreCase = true)

    private fun saveIfCurrent(store: WebDavSettingsStore, expected: WebDavConfig, updated: WebDavConfig): Boolean {
        return synchronized(store) {
            val current = store.load() ?: return@synchronized false
            if (!current.enabled || current.endpoint != expected.endpoint || current.username != expected.username ||
                current.password != expected.password || current.remoteFile != expected.remoteFile ||
                current.passphrase != expected.passphrase || current.intervalHours != expected.intervalHours ||
                current.lastEtag != expected.lastEtag || current.lastLocalRevision != expected.lastLocalRevision ||
                current.lastRemoteFingerprint != expected.lastRemoteFingerprint ||
                current.lastLocalFingerprint != expected.lastLocalFingerprint ||
                current.conflictPending != expected.conflictPending ||
                current.pendingResolution != expected.pendingResolution
            ) return@synchronized false
            store.save(updated)
            true
        }
    }

}

private val webDavSyncMutex = Mutex()

object WebDavSyncScheduler {
    private const val UNIQUE_WORK_NAME = "dextra-webdav-sync"

    fun schedule(context: Context, intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<WebDavSyncWorker>(
            intervalHours.coerceIn(1, 168).toLong(),
            TimeUnit.HOURS,
        )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun cancelNow(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("$UNIQUE_WORK_NAME-now")
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WebDavSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$UNIQUE_WORK_NAME-now",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

private data class WebDavRemoteDocument(val bytes: ByteArray, val etag: String?)

private object WebDavClient {
    private const val MAX_BYTES = 20 * 1024 * 1024
    private const val TIMEOUT_MS = 15_000

    fun fetch(config: WebDavConfig): WebDavRemoteDocument? {
        val connection = open(remoteUrl(config), "GET", config)
        return try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> null
                in 200..299 -> WebDavRemoteDocument(readBounded(connection.inputStream), connection.getHeaderField("ETag"))
                else -> throw IOException("WebDAV GET failed: $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun putAtomically(config: WebDavConfig, bytes: ByteArray, expectedEtag: String?): WebDavRemoteDocument {
        val remote = remoteUrl(config)
        val temporary = "${remote}.dextra-${UUID.randomUUID()}"
        val put = open(temporary, "PUT", config).apply {
            doOutput = true
            setFixedLengthStreamingMode(bytes.size)
        }
        try {
            put.outputStream.use { it.write(bytes) }
            if (put.responseCode !in 200..299) throw IOException("WebDAV upload failed: ${put.responseCode}")
        } catch (error: Throwable) {
            delete(temporary, config)
            throw error
        } finally {
            put.disconnect()
        }

        val move = open(temporary, "MOVE", config).apply {
            setRequestProperty("Destination", remote)
            setRequestProperty("Overwrite", "T")
            expectedEtag?.let { setRequestProperty("If", "<$remote> ([$it])") }
        }
        var moved = false
        return try {
            if (move.responseCode !in 200..299) {
                if (move.responseCode == HttpURLConnection.HTTP_PRECON_FAILED || move.responseCode == HttpURLConnection.HTTP_CONFLICT) {
                    throw WebDavConflictException()
                }
                throw IOException("WebDAV MOVE failed: ${move.responseCode}")
            }
            moved = true
            WebDavRemoteDocument(bytes, move.getHeaderField("ETag"))
        } finally {
            move.disconnect()
            if (!moved) delete(temporary, config)
        }
    }

    private fun delete(url: String, config: WebDavConfig) {
        runCatching {
            val connection = open(url, "DELETE", config)
            try {
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun open(url: String, method: String, config: WebDavConfig): HttpURLConnection {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
            if (config.username.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString("${config.username}:${config.password}".toByteArray())
                setRequestProperty("Authorization", "Basic $credentials")
            }
        }
        return connection
    }

    private fun remoteUrl(config: WebDavConfig): String =
        "${config.endpoint.trimEnd('/')}/${config.remoteFile.trimStart('/')}"

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            if (output.size() + count > MAX_BYTES) throw IOException("WebDAV bundle is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}

private class WebDavConflictException : IOException()
