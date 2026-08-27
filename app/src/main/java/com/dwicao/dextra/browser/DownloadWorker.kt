package com.dwicao.dextra.browser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dwicao.dextra.MainActivity
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import java.io.File

private const val DOWNLOAD_CHANNEL_ID = "dextra_downloads"

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
        setForeground(createForegroundInfo(download))

        val engine = DownloadEngine(CoroutineScope(currentCoroutineContext())) { id, update ->
            dao.getDownload(id)?.let { current ->
                dao.upsertDownload(
                    current.copy(
                        status = update.status,
                        bytesDownloaded = update.bytesDownloaded ?: current.bytesDownloaded,
                        totalBytes = update.totalBytes ?: current.totalBytes,
                        speedBytesPerSecond = update.speedBytesPerSecond ?: current.speedBytesPerSecond,
                        filePath = update.filePath ?: current.filePath,
                        reason = update.reason,
                    ),
                )
            }
        }
        engine.execute(download)

        val completed = dao.getDownload(downloadId) ?: return Result.success()
        if (completed.status == DownloadStatus.COMPLETE.label && completed.localUri == null && completed.filePath != null) {
            val publicUri = publishDownload(completed)
            dao.upsertDownload(completed.copy(localUri = publicUri))
        }
        if (completed.status in setOf(DownloadStatus.COMPLETE.label, DownloadStatus.FAILED.label)) {
            notifyDownload(completed)
        }
        return Result.success()
    }

    private fun publishDownload(download: DownloadEntry): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val filePath = download.filePath ?: return null
        val resolver = applicationContext.contentResolver
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
    }
}
