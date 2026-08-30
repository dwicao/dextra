package com.dwicao.dextra.browser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dwicao.dextra.MainActivity
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

private const val DOWNLOAD_CHANNEL_ID = "dextra_downloads"

private object DownloadQueueGate {
    val semaphore = Semaphore(2)
}

fun downloadWorkName(downloadId: Long): String = "dextra-download-$downloadId"

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, Long.MIN_VALUE)
        val dao = BrowserDatabase.get(applicationContext).browserDao()
        val download = dao.getDownload(downloadId) ?: return Result.success()
        if (download.status == DownloadStatus.PAUSED.label || download.status == DownloadStatus.CANCELED.label) {
            return Result.success()
        }
        if (download.status == DownloadStatus.FAILED.label) {
            if (download.attempts >= MAX_RETRY_ATTEMPTS) return Result.success()
            dao.upsertDownload(download.copy(status = DownloadStatus.QUEUED.label, reason = "Retrying"))
        }
        val engine = DownloadEngine(
            scope = CoroutineScope(currentCoroutineContext()),
            onUpdate = { id, update ->
                dao.getDownload(id)?.let { current ->
                    if (current.status == DownloadStatus.CANCELED.label ||
                        (current.status == DownloadStatus.PAUSED.label && update.status != DownloadStatus.QUEUED.label)
                    ) return@let
                    dao.upsertDownload(
                        current.copy(
                            status = update.status,
                            bytesDownloaded = update.bytesDownloaded ?: current.bytesDownloaded,
                            totalBytes = update.totalBytes ?: current.totalBytes,
                            speedBytesPerSecond = update.speedBytesPerSecond ?: current.speedBytesPerSecond,
                        filePath = update.filePath ?: current.filePath,
                        checksumSha256 = update.checksumSha256 ?: current.checksumSha256,
                        reason = update.reason,
                        ),
                    )
                }
            },
            isCancelled = {
                val current = dao.getDownload(downloadId)
                current == null || current.status in setOf(DownloadStatus.PAUSED.label, DownloadStatus.CANCELED.label)
            },
        )
        return DownloadQueueGate.semaphore.withPermit {
            val current = dao.getDownload(downloadId) ?: return@withPermit Result.success()
            if (current.status != DownloadStatus.QUEUED.label && current.status != DownloadStatus.DOWNLOADING.label) {
                return@withPermit Result.success()
            }
            val higherPriority = dao.getDownloads()
                .filter {
                    it.status == DownloadStatus.QUEUED.label &&
                        (it.scheduledAt == null || it.scheduledAt <= System.currentTimeMillis())
                }
                .maxOfOrNull { it.priority }
            if (higherPriority != null && higherPriority > current.priority) {
                return@withPermit Result.retry()
            }
            setForeground(createForegroundInfo(current))
            engine.execute(current)

            val completed = dao.getDownload(downloadId) ?: return@withPermit Result.success()
            var finalDownload = completed
            if (completed.status == DownloadStatus.COMPLETE.label && completed.localUri == null && completed.filePath != null) {
                val publicUri = publishDownload(completed)
                if (publicUri != null) {
                    if (dao.setDownloadUriIfComplete(downloadId, publicUri, DownloadStatus.COMPLETE.label) == 0) {
                        runCatching { applicationContext.contentResolver.delete(Uri.parse(publicUri), null, null) }
                        return@withPermit Result.success()
                    }
                    finalDownload = completed.copy(localUri = publicUri)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || completed.destinationTreeUri != null) {
                    val reason = "Download finished but could not be published"
                    if (dao.markDownloadFailedIfComplete(
                            downloadId,
                            DownloadStatus.FAILED.label,
                            reason,
                            DownloadStatus.COMPLETE.label,
                        ) == 0
                    ) {
                        runCatching { File(completed.filePath).delete() }
                        return@withPermit Result.success()
                    }
                    finalDownload = completed.copy(status = DownloadStatus.FAILED.label, reason = reason)
                }
            }
            if (finalDownload.status == DownloadStatus.FAILED.label &&
                finalDownload.reason != "SHA-256 checksum mismatch" &&
                finalDownload.attempts < MAX_RETRY_ATTEMPTS
            ) {
                dao.upsertDownload(finalDownload.copy(attempts = finalDownload.attempts + 1, reason = "Retry scheduled"))
                return@withPermit Result.retry()
            }
            if (finalDownload.status in setOf(DownloadStatus.COMPLETE.label, DownloadStatus.FAILED.label)) {
                notifyDownload(finalDownload)
            }
            Result.success()
        }
    }

    private fun publishDownload(download: DownloadEntry): String? {
        val filePath = download.filePath ?: return null
        val resolver = applicationContext.contentResolver
        download.destinationTreeUri?.let { treeUriString ->
            val tree = DocumentFile.fromTreeUri(applicationContext, Uri.parse(treeUriString)) ?: return@let
            val destination = tree.createFile(
                download.mimeType ?: "application/octet-stream",
                download.fileName,
            ) ?: return@let
            return try {
                resolver.openOutputStream(destination.uri)?.use { output ->
                    File(filePath).inputStream().use { input -> input.copyTo(output) }
                } ?: error("Could not open selected download folder")
                destination.uri.toString()
            } catch (_: Exception) {
                runCatching { destination.delete() }
                null
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, download.fileName)
            put(MediaStore.Downloads.MIME_TYPE, download.mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Dextra")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                File(filePath).inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open public download")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            uri.toString()
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun notifyDownload(download: DownloadEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(DOWNLOAD_CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val complete = download.status == DownloadStatus.COMPLETE.label
        manager.notify(
            download.downloadId.toInt(),
            NotificationCompat.Builder(applicationContext, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(if (complete) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                .setContentTitle(download.fileName)
                .setContentText(if (complete) "Download complete" else download.reason ?: "Download failed")
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun createForegroundInfo(download: DownloadEntry): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(DOWNLOAD_CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(applicationContext, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(download.fileName)
            .setContentText("Downloading")
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                download.downloadId.toInt(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(download.downloadId.toInt(), notification)
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
