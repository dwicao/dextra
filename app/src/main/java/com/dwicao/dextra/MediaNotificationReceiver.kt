package com.dwicao.dextra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as? DextraApplication)
            ?.mediaNotificationController
            ?.handleAction(intent.action)
    }
}
