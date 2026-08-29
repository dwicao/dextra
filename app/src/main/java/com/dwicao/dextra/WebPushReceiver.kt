package com.dwicao.dextra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64

/** Bridge for the configured push provider to deliver a Web Push event to GeckoView. */
class WebPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scope = intent.getStringExtra(EXTRA_SCOPE)?.takeIf(String::isNotBlank) ?: return
        val encoded = intent.getStringExtra(EXTRA_DATA).orEmpty()
        val payload = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrDefault(ByteArray(0))
        GeckoRuntimeHolder.get(context).webPushController.onPushEvent(scope, payload)
    }

    companion object {
        const val ACTION = "com.dwicao.dextra.action.WEB_PUSH"
        const val EXTRA_SCOPE = "scope"
        const val EXTRA_DATA = "data_base64"
    }
}
