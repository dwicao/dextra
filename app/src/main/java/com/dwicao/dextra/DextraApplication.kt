package com.dwicao.dextra

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class DextraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
                .extensionsProcessEnabled(true)
                .extensionsWebAPIEnabled(true)
                .build(),
        ).also { runtime = it }
    }
}
