package com.dwicao.dextra

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dwicao.dextra.browser.BrowserViewModel
import com.dwicao.dextra.ui.DextraApp

class BrowserWindowActivity : ComponentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels {
        BrowserViewModel.Factory(application, standaloneWindow = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        browserViewModel.enterWindowMode(
            startUrl = intent?.dataString.orEmpty(),
            privateMode = intent?.getBooleanExtra(EXTRA_PRIVATE, false) == true,
        )
        browserViewModel.handleMediaTabIntent(intent?.getStringExtra(MediaNotificationController.EXTRA_MEDIA_TAB_ID))
        window.decorView.setOnDragListener { _, event ->
            if (event.action == android.view.DragEvent.ACTION_DROP) browserViewModel.handleDroppedData(event.clipData)
            true
        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        browserViewModel.enterWindowMode(
            startUrl = intent.dataString.orEmpty(),
            privateMode = intent.getBooleanExtra(EXTRA_PRIVATE, false),
        )
        browserViewModel.handleMediaTabIntent(intent.getStringExtra(MediaNotificationController.EXTRA_MEDIA_TAB_ID))
    }

    override fun onStart() {
        super.onStart()
        browserViewModel.onAppForeground()
    }

    override fun onStop() {
        browserViewModel.onAppBackground()
        super.onStop()
    }

    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && browserViewModel.handleKeyShortcut(event)) return true
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_T -> {
                    browserViewModel.createTab()
                    return true
                }
                KeyEvent.KEYCODE_W -> {
                    finish()
                    return true
                }
                KeyEvent.KEYCODE_R -> {
                    browserViewModel.reloadOrStop()
                    return true
                }
                KeyEvent.KEYCODE_L -> {
                    browserViewModel.focusAddressBar()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val EXTRA_PRIVATE = "private_window"
    }
}
