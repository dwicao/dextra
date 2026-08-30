package com.dwicao.dextra.browser

import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

data class DownloadUpdate(
    val status: String,
    val bytesDownloaded: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val filePath: String? = null,
    val reason: String? = null,
    val checksumSha256: String? = null,
)

class DownloadEngine(
    private val scope: CoroutineScope,
    private val isCancelled: suspend () -> Boolean = { false },
    private val onUpdate: suspend (Long, DownloadUpdate) -> Unit,
) {
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val cancelRequests = ConcurrentHashMap.newKeySet<Long>()
    private val removedDownloads = ConcurrentHashMap.newKeySet<Long>()

    fun start(download: DownloadEntry) {
        if (jobs.containsKey(download.downloadId)) return
        removedDownloads.remove(download.downloadId)
        cancelRequests.remove(download.downloadId)
        val job = scope.launch(Dispatchers.IO) {
            try {
                execute(download)
            } finally {
                jobs.remove(download.downloadId)
            }
        }
        jobs[download.downloadId] = job
    }

    suspend fun execute(download: DownloadEntry) {
        try {
            onUpdate(download.downloadId, DownloadUpdate(DownloadStatus.DOWNLOADING.label))
            val totalBytes = downloadFile(download)
            coroutineContext.ensureActive()
            val checksum = sha256(download.filePath)
            if (download.expectedChecksumSha256 != null &&
                !download.expectedChecksumSha256.equals(checksum, ignoreCase = true)
            ) {
                onUpdate(
                    download.downloadId,
                    DownloadUpdate(
                        status = DownloadStatus.FAILED.label,
                        checksumSha256 = checksum,
                        reason = "SHA-256 checksum mismatch",
                    ),
                )
                return
            }
            onUpdate(
                download.downloadId,
                DownloadUpdate(
                    status = DownloadStatus.COMPLETE.label,
                    bytesDownloaded = totalBytes,
                    totalBytes = totalBytes,
                    filePath = download.filePath,
                    checksumSha256 = checksum,
                ),
            )
        } catch (_: CancellationException) {
            if (download.downloadId in removedDownloads) {
                return
            } else if (download.downloadId in cancelRequests) {
                onUpdate(
                    download.downloadId,
                    DownloadUpdate(DownloadStatus.CANCELED.label, reason = "Canceled by user"),
                )
            } else {
                onUpdate(download.downloadId, DownloadUpdate(DownloadStatus.PAUSED.label))
            }
        } catch (error: Exception) {
            onUpdate(
                download.downloadId,
                DownloadUpdate(DownloadStatus.FAILED.label, reason = error.message ?: "Connection failed"),
            )
        }
    }

    fun pause(download: DownloadEntry) {
        jobs[download.downloadId]?.cancel()
            ?: scope.launch {
                onUpdate(download.downloadId, DownloadUpdate(DownloadStatus.PAUSED.label))
            }
    }

    fun cancel(download: DownloadEntry) {
        cancelRequests.add(download.downloadId)
        jobs[download.downloadId]?.cancel()
        cleanupParts(download)
        if (!jobs.containsKey(download.downloadId)) {
            scope.launch {
                onUpdate(
                    download.downloadId,
                    DownloadUpdate(DownloadStatus.CANCELED.label, reason = "Canceled by user"),
                )
            }
        }
    }

    fun remove(download: DownloadEntry) {
        removedDownloads.add(download.downloadId)
        cancelRequests.add(download.downloadId)
        jobs[download.downloadId]?.cancel()
        cleanupParts(download)
    }

    fun shutdown() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
    }

    private suspend fun downloadFile(download: DownloadEntry): Long {
        if (!NavigationPolicy.isWebUrl(download.url)) {
            throw IOException("Unsupported download URL")
        }
        val output = File(download.filePath ?: error("Download path is missing"))
        output.parentFile?.mkdirs()
        val knownTotalBytes = download.totalBytes.takeIf { it > 0 }
        val existing = output.length().let { length ->
            if (knownTotalBytes != null && length > knownTotalBytes) {
                output.delete()
                0L
            } else {
                length
            }
        }
        onUpdate(
            download.downloadId,
            DownloadUpdate(
                status = DownloadStatus.DOWNLOADING.label,
                bytesDownloaded = existing,
                totalBytes = knownTotalBytes ?: -1,
            ),
        )
        val digest = MessageDigest.getInstance("SHA-256")
        if (existing > 0L) {
            output.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    digest.update(buffer, 0, count)
                }
            }
        }
        return downloadInOneStream(download, output, knownTotalBytes, digest)
    }

    private suspend fun downloadInOneStream(
        download: DownloadEntry,
        output: File,
        knownTotalBytes: Long?,
        digest: MessageDigest,
    ): Long {
        val existing = output.length()
        if (knownTotalBytes != null && existing == knownTotalBytes) return existing

        val connection = openConnection(download.url).apply {
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val responseCode = connection.responseCode
            if (download.url.startsWith("https://", ignoreCase = true) &&
                connection.url.protocol.lowercase() != "https"
            ) {
                throw IOException("Refusing an insecure download redirect")
            }
            if (existing > 0 && responseCode == HTTP_RANGE_NOT_SATISFIABLE && knownTotalBytes != null && existing == knownTotalBytes) {
                return existing
            }
            val append = existing > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL
            if (!append && responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP $responseCode")
            }

            val startingBytes = if (append) existing else 0L
            if (!append) digest.reset()
            val responseTotal = connection.contentLengthLong.takeIf { it >= 0 }?.let { length ->
                length + startingBytes
            }
            val expectedTotal = knownTotalBytes ?: responseTotal
            if (append) validateContentRange(connection.getHeaderField("Content-Range"), existing)

            onUpdate(
                download.downloadId,
                DownloadUpdate(
                    status = DownloadStatus.DOWNLOADING.label,
                    bytesDownloaded = startingBytes,
                    totalBytes = expectedTotal ?: -1,
                ),
            )

            FileOutputStream(output, append).use { outputStream ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var position = startingBytes
                    var lastReport = startingBytes
                    var lastReportAt = System.nanoTime()
                    var smoothedSpeed = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        if (isCancelled()) throw CancellationException()
                        val read = input.read(buffer)
                        if (read == -1) break
                        outputStream.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        position += read
                        if (position - lastReport >= REPORT_STEP) {
                            val now = System.nanoTime()
                            val elapsedNanos = now - lastReportAt
                            val instantSpeed = if (elapsedNanos > 0) {
                                ((position - lastReport) * 1_000_000_000L / elapsedNanos).coerceAtLeast(0)
                            } else {
                                0L
                            }
                            if (instantSpeed > 0) {
                                smoothedSpeed = if (smoothedSpeed == 0L) instantSpeed else {
                                    (smoothedSpeed * 80L + instantSpeed * 20L) / 100L
                                }
                            }
                            lastReport = position
                            lastReportAt = now
                            onUpdate(
                                download.downloadId,
                                DownloadUpdate(
                                    status = DownloadStatus.DOWNLOADING.label,
                                    bytesDownloaded = position,
                                    totalBytes = expectedTotal ?: -1,
                                    speedBytesPerSecond = smoothedSpeed,
                                ),
                            )
                        }
                    }
                    if (expectedTotal != null && position != expectedTotal) {
                        throw IOException("Incomplete download: received $position of $expectedTotal bytes")
                    }
                    return position
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(path: String?): String? {
        val file = path?.let(::File)?.takeIf(File::isFile) ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECTION_TIMEOUT
            readTimeout = READ_TIMEOUT
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", BrowserClientIdentity.userAgent)
        }

    private fun validateContentRange(header: String?, expectedStart: Long) {
        val match = Regex("bytes\\s+(\\d+)-(\\d+)/(?:\\d+|\\*)", RegexOption.IGNORE_CASE)
            .matchEntire(header.orEmpty())
            ?: throw IOException("Server returned an invalid Content-Range")
        if (match.groupValues[1].toLongOrNull() != expectedStart) {
            throw IOException("Server returned an unexpected range start")
        }
    }

    private fun cleanupParts(download: DownloadEntry) {
        val output = download.filePath?.let(::File) ?: return
        (0 until LEGACY_PART_FILE_COUNT).forEach { index -> File("${output.path}.part$index").delete() }
        output.delete()
    }

    private companion object {
        const val BUFFER_SIZE = 32 * 1024
        const val REPORT_STEP = 256 * 1024
        const val CONNECTION_TIMEOUT = 15_000
        const val READ_TIMEOUT = 30_000
        const val LEGACY_PART_FILE_COUNT = 6
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
    }
}
