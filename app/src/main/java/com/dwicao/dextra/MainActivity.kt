package com.dwicao.dextra

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import com.dwicao.dextra.browser.BrowserViewModel
import com.dwicao.dextra.ui.DextraApp

class MainActivity : ComponentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        browserViewModel.handleIncomingIntent(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (browserViewModel.goBack()) return
                val active = browserViewModel.activeTab()
                if (active != null && browserViewModel.state.value.tabs.size > 1) {
                    browserViewModel.closeTab(active.id)
                } else {
                    finish()
                }
            }
        })
        setContent { DextraApp(browserViewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        browserViewModel.handleIncomingIntent(intent)
    }
}
