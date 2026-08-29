package com.dwicao.dextra

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate

class DextraApplication : Application() {
    lateinit var mediaNotificationController: MediaNotificationController

    override fun onCreate() {
        super.onCreate()
        mediaNotificationController = MediaNotificationController(this)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeCrashReports(this, thread, error) }
            if (previousHandler != null) previousHandler.uncaughtException(thread, error)
            else exitProcess(10)
        }
    }

    private fun writeCrashReports(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { writer ->
            error.printStackTrace(PrintWriter(writer))
        }.toString()
        val report = buildString {
            appendLine("Dextra crash report")
            appendLine("Package: ${context.packageName}")
            appendLine("Thread: ${thread.name}")
            appendLine("Process: ${Process.myPid()}")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())}")
            appendLine()
            append(trace)
        }

        runCatching { File(context.filesDir, "last-crash.txt").writeText(report) }
        val fileName = "crash-${SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())}-${Process.myPid()}.txt"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeCrashToMediaStore(context, fileName, report)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "dextra",
                ).apply {
                    mkdirs()
                    File(this, fileName).writeText(report)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeCrashToMediaStore(context: Context, fileName: String, report: String) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/dextra")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching
            runCatching {
                resolver.openOutputStream(uri)?.use { output ->
                    output.write(report.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open crash report")
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null,
                )
            }.onFailure { runCatching { resolver.delete(uri, null, null) } }
        }
    }
}

object GeckoRuntimeHolder {
    @Volatile
    private var runtime: GeckoRuntime? = null

    fun get(context: Context): GeckoRuntime = runtime ?: synchronized(this) {
        runtime ?: GeckoRuntime.create(
            context.applicationContext,
            GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .fissionEnabled(true)
                .consoleOutput(false)
                .debugLogging(false)
                .remoteDebuggingEnabled(false)
                .aboutConfigEnabled(false)
                .globalPrivacyControlEnabled(true)
                .translationsOfferPopup(false)
                .extensionsProcessEnabled(true)
                .extensionsWebAPIEnabled(true)
                .webManifest(true)
                .build(),
        ).also {
            it.setWebNotificationDelegate(createWebNotificationDelegate(context.applicationContext))
            runtime = it
        }
    }

    private fun createWebNotificationDelegate(context: Context): WebNotificationDelegate = object : WebNotificationDelegate {
        private val notificationIds = ConcurrentHashMap<String, Int>()
        private val nextId = AtomicInteger(10_000)

        override fun onShowNotification(notification: WebNotification) {
            if (notification.privateBrowsing) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(WEB_NOTIFICATION_CHANNEL, "Website notifications", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
            val id = nextId.getAndIncrement()
            val key = notification.tag.orEmpty().ifBlank { notification.origin }
            notificationIds[key] = id
            val clickIntent = Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_WEB_NOTIFICATION, notification)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.notify(
                id,
                NotificationCompat.Builder(context, WEB_NOTIFICATION_CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(notification.title.orEmpty().ifBlank { notification.origin })
                    .setContentText(notification.text)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOngoing(notification.requireInteraction)
                    .build(),
            )
        }

        override fun onCloseNotification(notification: WebNotification) {
            val key = notification.tag.orEmpty().ifBlank { notification.origin }
            notificationIds.remove(key)?.let { id ->
                context.getSystemService(NotificationManager::class.java)?.cancel(id)
            }
        }
    }

    private const val WEB_NOTIFICATION_CHANNEL = "dextra_web_notifications"
    const val EXTRA_WEB_NOTIFICATION = "dextra_web_notification"
}
