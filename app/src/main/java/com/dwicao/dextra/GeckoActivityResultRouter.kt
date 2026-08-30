package com.dwicao.dextra

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import org.mozilla.geckoview.GeckoResult
import java.lang.ref.WeakReference
import java.util.LinkedHashMap

/** Routes Gecko's single runtime activity delegate to the current Android window. */
object GeckoActivityResultRouter {
    private var host: WeakReference<Activity>? = null
    private var launch: ((IntentSenderRequest) -> Unit)? = null
    private val hosts = LinkedHashMap<Activity, (IntentSenderRequest) -> Unit>()
    private var pendingIntent: PendingIntent? = null
    private var pendingResult: GeckoResult<Intent>? = null
    private var inFlight = false
    private val delegate = object : org.mozilla.geckoview.GeckoRuntime.ActivityDelegate {
        override fun onStartActivityForResult(intent: PendingIntent): GeckoResult<Intent> = start(intent)
    }

    @Synchronized
    fun register(activity: Activity, launcher: (IntentSenderRequest) -> Unit) {
        hosts[activity] = launcher
        host = WeakReference(activity)
        launch = launcher
        GeckoRuntimeHolder.get(activity).setActivityDelegate(delegate)
        if (!inFlight) launchPendingIfPossible()
    }

    @Synchronized
    fun unregister(activity: Activity) {
        hosts.remove(activity)
        if (host?.get() === activity) {
            val next = hosts.entries.lastOrNull()
            if (next == null) {
                host = null
                launch = null
                val runtime = GeckoRuntimeHolder.get(activity)
                if (runtime.activityDelegate === delegate) runtime.setActivityDelegate(null)
            } else {
                host = WeakReference(next.key)
                launch = next.value
                GeckoRuntimeHolder.get(next.key).setActivityDelegate(delegate)
            }
        }
    }

    @Synchronized
    fun onHostDestroyed(activity: Activity, willRecreate: Boolean) {
        hosts.remove(activity)
        if (willRecreate && pendingResult != null) {
            inFlight = false
            launchPendingIfPossible()
        }
    }

    fun start(intent: PendingIntent): GeckoResult<Intent> {
        val result = GeckoResult<Intent>()
        synchronized(this) {
            pendingResult?.completeExceptionally(IllegalStateException("Credential operation was replaced"))
            pendingResult = result
            pendingIntent = intent
            inFlight = false
            launchPendingIfPossible()
        }
        return result
    }

    @Synchronized
    fun complete(resultCode: Int, data: Intent?) {
        val result = pendingResult.also {
            pendingResult = null
            pendingIntent = null
            inFlight = false
        } ?: return
        if (resultCode == Activity.RESULT_OK && data != null) result.complete(data)
        else result.completeExceptionally(IllegalStateException("Credential operation was canceled"))
    }

    @Synchronized
    fun fail(error: Throwable) {
        pendingResult?.completeExceptionally(error)
        pendingResult = null
        pendingIntent = null
        inFlight = false
    }

    private fun launchPendingIfPossible() {
        val intent = pendingIntent ?: return
        val activity = host?.get() ?: return
        val launcher = launch ?: return
        inFlight = true
        activity.runOnUiThread {
            runCatching { launcher(IntentSenderRequest.Builder(intent).build()) }
                .onFailure { fail(it) }
        }
    }
}
