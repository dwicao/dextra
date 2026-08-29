package com.dwicao.dextra.browser

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.DownloadStatus
import com.dwicao.dextra.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class PrivacyCleanupWorker(
    appContext: Context,
    workerParams: androidx.work.WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val settings = SettingsRepository(context).settings.first()
        val dao = BrowserDatabase.get(context).browserDao()
        val now = System.currentTimeMillis()
        val allowlist = settings.privacyCleanupAllowlist
        if (settings.historyRetentionDays > 0) {
            val cutoff = now - TimeUnit.DAYS.toMillis(settings.historyRetentionDays.toLong())
            dao.getHistory()
                .filter { it.visitedAt < cutoff && NavigationPolicy.origin(it.url) !in allowlist }
                .forEach { dao.deleteHistory(it.id) }
        }
        if (settings.downloadRetentionDays > 0) {
            val cutoff = now - TimeUnit.DAYS.toMillis(settings.downloadRetentionDays.toLong())
            dao.getDownloads()
                .filter {
                    it.createdAt < cutoff &&
                        it.status !in setOf(DownloadStatus.QUEUED.label, DownloadStatus.DOWNLOADING.label, DownloadStatus.PAUSED.label) &&
                        NavigationPolicy.origin(it.url) !in allowlist
                }
                .forEach { download ->
                    download.localUri?.let { uri -> runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) } }
                    download.filePath?.let { path ->
                        runCatching { File(path).delete() }
                        (0 until 6).forEach { index -> runCatching { File("$path.part$index").delete() } }
                    }
                    dao.deleteDownload(download.downloadId)
                }
        }
        val recoveryCutoff = now - TimeUnit.DAYS.toMillis(settings.recoveryRetentionDays.toLong())
        SettingsRepository(context).pruneSessionTimeline(recoveryCutoff)
        Result.success()
    }
}

object PrivacyCleanupScheduler {
    private const val PERIODIC_WORK_NAME = "dextra-privacy-cleanup"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PrivacyCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$PERIODIC_WORK_NAME-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PrivacyCleanupWorker>().build(),
        )
    }
}
