package com.dwicao.dextra.browser

import android.view.KeyEvent

enum class BrowserCommandId {
    NEW_TAB,
    NEW_PRIVATE_TAB,
    CLOSE_TAB,
    REOPEN_CLOSED_TAB,
    RELOAD,
    FIND_IN_PAGE,
    FOCUS_ADDRESS_BAR,
    TOGGLE_BOOKMARK,
    TOGGLE_READING_LIST,
    SHOW_QR,
    NEXT_TAB,
    PREVIOUS_TAB,
    RESET_ZOOM,
    ZOOM_OUT,
    ZOOM_IN,
    SHOW_TABS,
    SHOW_LIBRARY,
    SHOW_DOWNLOADS,
    SHOW_SETTINGS,
    SHOW_PRIVACY,
    READER_MODE,
    TOGGLE_SPLIT,
    HIBERNATE_TABS,
    COMMAND_PALETTE,
    TAB_SEARCH,
}

data class KeyChord(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false,
) {
    fun encode(): String = listOf(keyCode, ctrl, alt, shift, meta).joinToString(",")

    companion object {
        fun decode(value: String): KeyChord? {
            val parts = value.split(',')
            if (parts.size != 5) return null
            val keyCode = parts[0].toIntOrNull() ?: return null
            return KeyChord(
                keyCode = keyCode,
                ctrl = parts[1].toBooleanStrictOrNull() ?: return null,
                alt = parts[2].toBooleanStrictOrNull() ?: return null,
                shift = parts[3].toBooleanStrictOrNull() ?: return null,
                meta = parts[4].toBooleanStrictOrNull() ?: return null,
            )
        }
    }

    fun displayName(): String {
        val parts = buildList {
            if (ctrl) add("Ctrl")
            if (alt) add("Alt")
            if (shift) add("Shift")
            if (meta) add("Meta")
            add(keyName(keyCode))
        }
        return parts.joinToString("+")
    }

    private fun keyName(code: Int): String = when {
        code in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> ('A'.code + code - KeyEvent.KEYCODE_A).toChar().toString()
        code in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> ('0'.code + code - KeyEvent.KEYCODE_0).toChar().toString()
        code == KeyEvent.KEYCODE_TAB -> "Tab"
        code == KeyEvent.KEYCODE_ENTER -> "Enter"
        code == KeyEvent.KEYCODE_ESCAPE -> "Escape"
        code == KeyEvent.KEYCODE_MINUS -> "-"
        code == KeyEvent.KEYCODE_EQUALS -> "="
        code == KeyEvent.KEYCODE_COMMA -> ","
        else -> "Key $code"
    }
}

fun KeyEvent.toKeyChord(): KeyChord = KeyChord(
    keyCode = keyCode,
    ctrl = isCtrlPressed,
    alt = isAltPressed,
    shift = isShiftPressed,
    meta = isMetaPressed,
)

fun KeyChord.matches(event: KeyEvent): Boolean = this == event.toKeyChord()

object DefaultKeyboardShortcuts {
    val bindings: Map<BrowserCommandId, KeyChord> = mapOf(
        BrowserCommandId.NEW_TAB to KeyChord(KeyEvent.KEYCODE_T, ctrl = true),
        BrowserCommandId.CLOSE_TAB to KeyChord(KeyEvent.KEYCODE_W, ctrl = true),
        BrowserCommandId.REOPEN_CLOSED_TAB to KeyChord(KeyEvent.KEYCODE_T, ctrl = true, shift = true),
        BrowserCommandId.RELOAD to KeyChord(KeyEvent.KEYCODE_R, ctrl = true),
        BrowserCommandId.FIND_IN_PAGE to KeyChord(KeyEvent.KEYCODE_F, ctrl = true),
        BrowserCommandId.FOCUS_ADDRESS_BAR to KeyChord(KeyEvent.KEYCODE_L, ctrl = true),
        BrowserCommandId.TOGGLE_BOOKMARK to KeyChord(KeyEvent.KEYCODE_D, ctrl = true),
        BrowserCommandId.NEXT_TAB to KeyChord(KeyEvent.KEYCODE_TAB, ctrl = true),
        BrowserCommandId.PREVIOUS_TAB to KeyChord(KeyEvent.KEYCODE_TAB, ctrl = true, shift = true),
        BrowserCommandId.RESET_ZOOM to KeyChord(KeyEvent.KEYCODE_0, ctrl = true),
        BrowserCommandId.ZOOM_OUT to KeyChord(KeyEvent.KEYCODE_MINUS, ctrl = true),
        BrowserCommandId.ZOOM_IN to KeyChord(KeyEvent.KEYCODE_EQUALS, ctrl = true),
        BrowserCommandId.COMMAND_PALETTE to KeyChord(KeyEvent.KEYCODE_P, ctrl = true, shift = true),
        BrowserCommandId.TAB_SEARCH to KeyChord(KeyEvent.KEYCODE_A, ctrl = true, shift = true),
        BrowserCommandId.READER_MODE to KeyChord(KeyEvent.KEYCODE_R, ctrl = true, shift = true),
        BrowserCommandId.SHOW_TABS to KeyChord(KeyEvent.KEYCODE_9, ctrl = true, shift = true),
        BrowserCommandId.SHOW_LIBRARY to KeyChord(KeyEvent.KEYCODE_B, ctrl = true, shift = true),
        BrowserCommandId.SHOW_DOWNLOADS to KeyChord(KeyEvent.KEYCODE_J, ctrl = true),
        BrowserCommandId.SHOW_SETTINGS to KeyChord(KeyEvent.KEYCODE_COMMA, ctrl = true),
        BrowserCommandId.SHOW_PRIVACY to KeyChord(KeyEvent.KEYCODE_V, ctrl = true, shift = true),
        BrowserCommandId.TOGGLE_SPLIT to KeyChord(KeyEvent.KEYCODE_S, ctrl = true, shift = true),
        BrowserCommandId.HIBERNATE_TABS to KeyChord(KeyEvent.KEYCODE_H, ctrl = true, shift = true),
    )
}

data class BrowserCommand(
    val id: BrowserCommandId,
    val title: String,
    val description: String,
)

object BrowserCommands {
    val all = listOf(
        BrowserCommand(BrowserCommandId.NEW_TAB, "New tab", "Open a blank tab"),
        BrowserCommand(BrowserCommandId.NEW_PRIVATE_TAB, "New private tab", "Browse without saving session history"),
        BrowserCommand(BrowserCommandId.CLOSE_TAB, "Close tab", "Close the active tab"),
        BrowserCommand(BrowserCommandId.REOPEN_CLOSED_TAB, "Reopen closed tab", "Restore the last closed tab"),
        BrowserCommand(BrowserCommandId.RELOAD, "Reload page", "Reload the active page"),
        BrowserCommand(BrowserCommandId.FIND_IN_PAGE, "Find in page", "Search text on the active page"),
        BrowserCommand(BrowserCommandId.FOCUS_ADDRESS_BAR, "Focus address bar", "Start entering a URL or search"),
        BrowserCommand(BrowserCommandId.TOGGLE_BOOKMARK, "Toggle bookmark", "Save or remove the active page"),
        BrowserCommand(BrowserCommandId.TOGGLE_READING_LIST, "Toggle reading list", "Save or remove the active page from the reading list"),
        BrowserCommand(BrowserCommandId.SHOW_QR, "Show page QR code", "Display a QR code for the active URL"),
        BrowserCommand(BrowserCommandId.NEXT_TAB, "Next tab", "Move to the next tab"),
        BrowserCommand(BrowserCommandId.PREVIOUS_TAB, "Previous tab", "Move to the previous tab"),
        BrowserCommand(BrowserCommandId.RESET_ZOOM, "Reset page zoom", "Return page zoom to 100%"),
        BrowserCommand(BrowserCommandId.ZOOM_OUT, "Zoom out", "Reduce page zoom"),
        BrowserCommand(BrowserCommandId.ZOOM_IN, "Zoom in", "Increase page zoom"),
        BrowserCommand(BrowserCommandId.SHOW_TABS, "Show all tabs", "Open the tab manager"),
        BrowserCommand(BrowserCommandId.SHOW_LIBRARY, "Open library", "Open bookmarks and history"),
        BrowserCommand(BrowserCommandId.SHOW_DOWNLOADS, "Open downloads", "Open the download manager"),
        BrowserCommand(BrowserCommandId.SHOW_SETTINGS, "Open settings", "Open browser settings"),
        BrowserCommand(BrowserCommandId.SHOW_PRIVACY, "Open privacy dashboard", "Review and clear site data"),
        BrowserCommand(BrowserCommandId.READER_MODE, "Reader mode", "Read the active article without page clutter"),
        BrowserCommand(BrowserCommandId.TOGGLE_SPLIT, "Open in split view", "Open the active tab beside another tab"),
        BrowserCommand(BrowserCommandId.HIBERNATE_TABS, "Hibernate inactive tabs", "Pause inactive tabs to save resources"),
        BrowserCommand(BrowserCommandId.COMMAND_PALETTE, "Command palette", "Search and run browser actions"),
        BrowserCommand(BrowserCommandId.TAB_SEARCH, "Search tabs", "Find an open tab by title or URL"),
    )
}
