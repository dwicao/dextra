package com.dwicao.dextra.browser

import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.math.min

data class DownloadUpdate(
    val status: String,
    val bytesDownloaded: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val filePath: String? = null,
    val reason: String? = null,
)

class DownloadEngine(
    private val scope: CoroutineScope,
    private val onUpdate: (Long, DownloadUpdate) -> Unit,
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
            onUpdate(
                download.downloadId,
                DownloadUpdate(
                    status = DownloadStatus.COMPLETE.label,
                    bytesDownloaded = totalBytes,
                    totalBytes = totalBytes,
                    filePath = download.filePath,
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
            ?: onUpdate(download.downloadId, DownloadUpdate(DownloadStatus.PAUSED.label))
    }

    fun cancel(download: DownloadEntry) {
        cancelRequests.add(download.downloadId)
        jobs[download.downloadId]?.cancel()
        cleanupParts(download)
        if (!jobs.containsKey(download.downloadId)) {
            onUpdate(
                download.downloadId,
                DownloadUpdate(DownloadStatus.CANCELED.label, reason = "Canceled by user"),
            )
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

    private suspend fun downloadFile(download: DownloadEntry): Long = coroutineScope {
        val output = File(download.filePath ?: error("Download path is missing"))
        output.parentFile?.mkdirs()
        val probe = probe(download.url, download.totalBytes)
        val totalBytes = probe.totalBytes
        onUpdate(
            download.downloadId,
            DownloadUpdate(
                status = DownloadStatus.DOWNLOADING.label,
                bytesDownloaded = existingBytes(download, totalBytes),
                totalBytes = totalBytes,
            ),
        )

        if (totalBytes > 0 && probe.supportsRanges) {
            try {
                downloadInParts(download, output, totalBytes)
            } catch (_: RangeUnsupportedException) {
                cleanupPartsOnly(output)
                downloadInOneStream(download, output, totalBytes)
            }
        } else {
            downloadInOneStream(download, output, totalBytes)
        }
        totalBytes.takeIf { it > 0 } ?: output.length()
    }

    private suspend fun downloadInParts(download: DownloadEntry, output: File, totalBytes: Long) {
        val partCount = min(MAX_CONNECTIONS.toLong(), totalBytes).toInt()
        val partSize = (totalBytes + partCount - 1) / partCount
        val parts = (0 until partCount).map { index ->
            val start = index * partSize
            val end = min(totalBytes, start + partSize) - 1
            Part(index, start, end, File("${output.path}.part$index"))
        }
        val progressLock = Any()
        var lastReportedBytes = existingBytes(download, totalBytes)
        var lastReportedAt = System.nanoTime()
        var smoothedSpeed = 0L
        val reportProgress = {
            val progress = synchronized(progressLock) {
                val bytes = parts.sumOf { part -> part.file.length().coerceAtMost(part.size) }
                val now = System.nanoTime()
                val elapsedNanos = now - lastReportedAt
                val instantSpeed = if (elapsedNanos > 0) {
                    ((bytes - lastReportedBytes) * 1_000_000_000L / elapsedNanos).coerceAtLeast(0)
                } else {
                    0L
                }
                if (instantSpeed > 0) {
                    smoothedSpeed = if (smoothedSpeed == 0L) instantSpeed else {
                        (smoothedSpeed * 80L + instantSpeed * 20L) / 100L
                    }
                }
                lastReportedBytes = bytes
                lastReportedAt = now
                bytes to smoothedSpeed
            }
            onUpdate(
                download.downloadId,
                DownloadUpdate(
                    status = DownloadStatus.DOWNLOADING.label,
                    bytesDownloaded = progress.first,
                    totalBytes = totalBytes,
                    speedBytesPerSecond = progress.second,
                ),
            )
        }

        coroutineScope {
            parts.map { part ->
                async(Dispatchers.IO) {
                    downloadPart(download.url, part, reportProgress)
                }
            }.awaitAll()
        }

        FileOutputStream(output, false).use { destination ->
            parts.forEach { part ->
                coroutineContext.ensureActive()
                part.file.inputStream().use { input -> input.copyTo(destination) }
                part.file.delete()
            }
        }
    }

    private suspend fun downloadPart(url: String, part: Part, reportProgress: () -> Unit) {
        val existing = part.file.takeIf { it.exists() }?.length()?.coerceAtMost(part.size) ?: 0
        if (existing >= part.size) {
            reportProgress()
            return
        }
        val connection = openConnection(url).apply {
            setRequestProperty("Range", "bytes=${part.start + existing}-${part.end}")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw RangeUnsupportedException()
            }
            val contentRange = connection.getHeaderField("Content-Range")
            val expectedStart = part.start + existing
            if (!validContentRange(contentRange, expectedStart, part.end)) {
                throw RangeUnsupportedException()
            }
            part.file.parentFile?.mkdirs()
            FileOutputStream(part.file, existing > 0).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var lastReport = existing
                    var position = existing
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        position += read
                        if (position - lastReport >= REPORT_STEP) {
                            lastReport = position
                            reportProgress()
                        }
                    }
                    if (position < part.size) throw IOException("Incomplete download")
                }
            }
            reportProgress()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadInOneStream(download: DownloadEntry, output: File, totalBytes: Long) {
        val existing = output.takeIf { it.exists() }?.length() ?: 0
        val connection = openConnection(download.url).apply {
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val append = existing > 0 && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (!append && connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP ${connection.responseCode}")
            }
            val startingBytes = if (append) existing else 0
            output.parentFile?.mkdirs()
            FileOutputStream(output, append).use { outputStream ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var position = startingBytes
                    var lastReport = startingBytes
                    var lastReportAt = System.nanoTime()
                    var smoothedSpeed = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        outputStream.write(buffer, 0, read)
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
                                    totalBytes = totalBytes,
                                    speedBytesPerSecond = smoothedSpeed,
                                ),
                            )
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun probe(url: String, knownTotalBytes: Long): Probe {
        val connection = openConnection(url).apply { requestMethod = "HEAD" }
        return try {
            val responseCode = connection.responseCode
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: knownTotalBytes
            Probe(
                totalBytes = total,
                supportsRanges = responseCode in 200..299 &&
                    connection.getHeaderField("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECTION_TIMEOUT
            readTimeout = READ_TIMEOUT
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Dextra/0.1")
        }

    private fun existingBytes(download: DownloadEntry, totalBytes: Long): Long =
        download.filePath?.let { output ->
            val file = File(output)
            if (totalBytes > 0) {
                (0 until MAX_CONNECTIONS).sumOf { File("$output.part$it").length() }.coerceAtMost(totalBytes)
            } else {
                file.length()
            }
        } ?: 0

    private fun validContentRange(header: String?, expectedStart: Long, expectedEnd: Long): Boolean {
        val match = Regex("bytes\\s+(\\d+)-(\\d+)/(?:\\d+|\\*)", RegexOption.IGNORE_CASE).matchEntire(header.orEmpty())
            ?: return false
        return match.groupValues[1].toLongOrNull() == expectedStart && match.groupValues[2].toLongOrNull() == expectedEnd
    }

    private fun cleanupParts(download: DownloadEntry) {
        val output = download.filePath?.let(::File) ?: return
        cleanupPartsOnly(output)
        output.delete()
    }

    private fun cleanupPartsOnly(output: File) {
        (0 until MAX_CONNECTIONS).forEach { File("${output.path}.part$it").delete() }
    }

    private data class Probe(val totalBytes: Long, val supportsRanges: Boolean)

    private data class Part(val index: Int, val start: Long, val end: Long, val file: File) {
        val size: Long get() = end - start + 1
    }

    private class RangeUnsupportedException : IOException("Server does not support ranged downloads")

    private companion object {
        const val MAX_CONNECTIONS = 6
        const val BUFFER_SIZE = 32 * 1024
        const val REPORT_STEP = 256 * 1024
        const val CONNECTION_TIMEOUT = 15_000
        const val READ_TIMEOUT = 30_000
    }
}
