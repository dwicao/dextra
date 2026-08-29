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
import com.dwicao.dextra.data.WebDavConfig
import com.dwicao.dextra.data.WebDavSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
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
            if (remote != null && (config.lastEtag == null || remote.etag != config.lastEtag)) {
                val imported = syncRepository.importBytes(remote.bytes, config.passphrase)
                imported.root.optJSONObject("settings")?.let { importedSettings ->
                    settingsRepository.applySyncSettings(importedSettings)
                }
            }
            val settings = settingsRepository.settings.first()
            val uploaded = WebDavClient.putAtomically(config, syncRepository.exportBytes(settings, config.passphrase), remote?.etag)
            store.save(config.copy(lastEtag = uploaded.etag ?: remote?.etag, lastSyncAt = System.currentTimeMillis()))
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error is BadPaddingException || error is IllegalArgumentException || error is org.json.JSONException) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            },
        )
    }

    private fun isHttps(value: String): Boolean = value.startsWith("https://", ignoreCase = true)
}

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
            expectedEtag?.let { setRequestProperty("If-Match", it) }
        }
        var moved = false
        return try {
            if (move.responseCode !in 200..299) {
                if (move.responseCode == HttpURLConnection.HTTP_PRECON_FAILED) throw WebDavConflictException()
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
