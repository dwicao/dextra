package com.dwicao.dextra

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import com.dwicao.dextra.browser.BrowserViewModel
import com.dwicao.dextra.ui.DextraApp

class PwaActivity : ComponentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        browserViewModel.enterPwaMode(intent?.dataString.orEmpty())
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = browserViewModel.state.value
                if (state.webPushPrompt != null || state.webAuthnPrompt != null || state.translation != null || state.contentPermission != null || state.androidPermission != null || state.mediaPermission != null) {
                    browserViewModel.dismissTransientUi()
                    return
                }
                if (!browserViewModel.goBack()) finish()
            }
        })
        setContent { DextraApp(browserViewModel) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        browserViewModel.enterPwaMode(intent.dataString.orEmpty())
    }

    override fun onStart() {
        super.onStart()
        browserViewModel.onAppForeground()
    }

    override fun onStop() {
        browserViewModel.onAppBackground()
        super.onStop()
    }

    companion object {
        const val EXTRA_PWA_ID = "pwa_id"
    }
}
