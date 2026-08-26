package com.dwicao.dextra

import android.app.Application
import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class DextraApplication : Application()

object GeckoRuntimeHolder {
    @Volatile
    private var runtime: GeckoRuntime? = null

    fun get(context: Context): GeckoRuntime = runtime ?: synchronized(this) {
        runtime ?: GeckoRuntime.create(
            context.applicationContext,
            GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .globalPrivacyControlEnabled(true)
                .build(),
        ).also { runtime = it }
    }
}
