package com.dwicao.dextra

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import com.dwicao.dextra.browser.BrowserViewModel
import com.dwicao.dextra.browser.BrowserOverlay
import com.dwicao.dextra.ui.DextraApp

class MainActivity : ComponentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        browserViewModel.handleIncomingIntent(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = browserViewModel.state.value
                if (state.overlay != BrowserOverlay.NONE) {
                    browserViewModel.dismissOverlay()
                    return
                }
                if (state.contextMenu != null || state.extensionPopup != null || state.findInPage != null ||
                    state.contentPermission != null || state.androidPermission != null || state.mediaPermission != null
                ) {
                    browserViewModel.dismissTransientUi()
                    return
                }
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
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.isCtrlPressed) {
                when {
                    event.keyCode in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> {
                        val index = event.keyCode - KeyEvent.KEYCODE_1
                        browserViewModel.state.value.tabs.getOrNull(index)?.let { browserViewModel.selectTab(it.id) }
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_TAB -> {
                        browserViewModel.cycleTab(forward = !event.isShiftPressed)
                        return true
                    }
                    event.isShiftPressed && event.keyCode == KeyEvent.KEYCODE_T -> {
                        browserViewModel.reopenClosedTab()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_T -> {
                        browserViewModel.createTab()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_W -> {
                        browserViewModel.activeTab()?.let { browserViewModel.closeTab(it.id) }
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_R -> {
                        browserViewModel.reloadOrStop()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_F -> {
                        browserViewModel.openFindInPage()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_D -> {
                        browserViewModel.toggleBookmark()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_L -> {
                        browserViewModel.focusAddressBar()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_PAGE_UP -> {
                        browserViewModel.cycleTab(forward = false)
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_PAGE_DOWN -> {
                        browserViewModel.cycleTab(forward = true)
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_0 || event.keyCode == KeyEvent.KEYCODE_NUMPAD_0 -> {
                        browserViewModel.resetPageZoom()
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_MINUS || event.keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> {
                        browserViewModel.adjustPageZoom(-10)
                        return true
                    }
                    event.keyCode == KeyEvent.KEYCODE_PLUS ||
                        event.keyCode == KeyEvent.KEYCODE_EQUALS ||
                        event.keyCode == KeyEvent.KEYCODE_NUMPAD_ADD -> {
                        browserViewModel.adjustPageZoom(10)
                        return true
                    }
                }
            }
            if (event.isAltPressed) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        browserViewModel.goBack()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        browserViewModel.goForward()
                        return true
                    }
                }
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_F5 -> {
                    browserViewModel.reloadOrStop()
                    return true
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    val state = browserViewModel.state.value
                    if (state.contextMenu != null || state.extensionPopup != null || state.findInPage != null) {
                        browserViewModel.dismissTransientUi()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
