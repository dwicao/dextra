package com.dwicao.dextra.browser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dwicao.dextra.data.BackupRepository
import com.dwicao.dextra.data.BrowserDatabase
import com.dwicao.dextra.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ScheduledBackupInfo(
    val name: String,
    val uri: String,
    val modifiedAt: Long,
    val size: Long,
    val internalPath: String? = null,
)

class ScheduledBackupWorker(
    appContext: Context,
    workerParams: androidx.work.WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val settingsRepository = SettingsRepository(context)
        val settings = settingsRepository.settings.first()
        val force = inputData.getBoolean(KEY_FORCE, false)
        if (!settings.backupEnabled && !force) return@withContext Result.success()
        runCatching {
            val bytes = ScheduledBackupCrypto.encrypt(
                BackupRepository(context, BrowserDatabase.get(context).browserDao()).exportBytes(settings),
            )
            require(bytes.size <= MAX_ENCRYPTED_BACKUP_BYTES) { "Encrypted backup is too large" }
            val name = "dextra-backup-${System.currentTimeMillis()}.json"
            if (settings.backupDirectoryUri.isNullOrBlank()) {
                val directory = File(context.filesDir, "scheduled-backups").apply { mkdirs() }
                File(directory, name).writeBytes(bytes)
                rotateInternal(directory, settings.backupRetentionCount)
            } else {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(settings.backupDirectoryUri))
                    ?: error("Selected backup folder is unavailable")
                val destination = tree.createFile("application/json", name)
                    ?: error("Could not create scheduled backup")
                context.contentResolver.openOutputStream(destination.uri)?.use { it.write(bytes) }
                    ?: error("Could not write scheduled backup")
                rotateExternal(tree, settings.backupRetentionCount)
            }
            settingsRepository.recordBackupResult(System.currentTimeMillis(), null)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                runCatching { settingsRepository.recordBackupResult(settings.lastBackupAt, error.message ?: "Backup failed") }
                Result.retry()
            },
        )
    }

    companion object {
        const val KEY_FORCE = "force_backup"
    }

    private fun rotateInternal(directory: File, retention: Int) {
        directory.listFiles()
            .orEmpty()
            .filter { it.name.startsWith("dextra-backup-") && it.name.endsWith(".json") }
            .sortedByDescending(File::lastModified)
            .drop(retention.coerceIn(1, 30))
            .forEach(File::delete)
    }

    private fun rotateExternal(directory: DocumentFile, retention: Int) {
        directory.listFiles()
            .filter { it.name?.startsWith("dextra-backup-") == true && it.name?.endsWith(".json") == true }
            .sortedByDescending { it.lastModified() }
            .drop(retention.coerceIn(1, 30))
            .forEach { it.delete() }
    }
}

object BackupScheduler {
    private const val WORK_NAME = "dextra-scheduled-backup"

    fun schedule(context: Context, intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(
            intervalHours.coerceIn(1, 168).toLong(),
            TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build(),
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$WORK_NAME-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ScheduledBackupWorker>()
                .setInputData(androidx.work.workDataOf(ScheduledBackupWorker.KEY_FORCE to true))
                .build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork("$WORK_NAME-now")
    }
}

object BackupCatalog {
    fun list(context: Context, directoryUri: String?): List<ScheduledBackupInfo> {
        if (directoryUri.isNullOrBlank()) {
            return File(context.filesDir, "scheduled-backups")
                .listFiles()
                .orEmpty()
                .filter { it.name.startsWith("dextra-backup-") && it.name.endsWith(".json") }
                .sortedByDescending(File::lastModified)
                .map { ScheduledBackupInfo(it.name, Uri.fromFile(it).toString(), it.lastModified(), it.length(), it.path) }
        }
        return runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(directoryUri))
                ?.listFiles()
                .orEmpty()
                .filter { it.name?.startsWith("dextra-backup-") == true && it.name?.endsWith(".json") == true }
                .sortedByDescending { it.lastModified() }
                .map { file -> ScheduledBackupInfo(file.name.orEmpty(), file.uri.toString(), file.lastModified(), file.length()) }
        }.getOrDefault(emptyList())
    }
}

private const val MAX_ENCRYPTED_BACKUP_BYTES = 32 * 1024 * 1024

internal object ScheduledBackupCrypto {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "dextra.scheduled.backups"
    private const val TAG_BITS = 128

    fun encrypt(payload: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return JSONObject()
            .put("format", "dextra-scheduled-backup")
            .put("version", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(payload), Base64.NO_WRAP))
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decrypt(payload: ByteArray): ByteArray {
        val envelope = JSONObject(String(payload, Charsets.UTF_8))
        require(envelope.optString("format") == "dextra-scheduled-backup" && envelope.optInt("version") == 1)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(TAG_BITS, Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)),
            )
        }
        return cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.NO_WRAP))
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", KEYSTORE).apply {
            init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }
}
