package com.dwicao.dextra

import android.app.Application
import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class DextraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { writer ->
                    error.printStackTrace(PrintWriter(writer))
                }.toString()
                File(filesDir, "last-crash.txt").writeText(
                    "Thread: ${thread.name}\n\n$trace",
                )
            }
            if (previousHandler != null) previousHandler.uncaughtException(thread, error)
            else exitProcess(10)
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
                .consoleOutput(true)
                .extensionsProcessEnabled(true)
                .build(),
        ).also { runtime = it }
    }
}
