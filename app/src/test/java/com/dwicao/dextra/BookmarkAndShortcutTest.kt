package com.dwicao.dextra

import android.view.KeyEvent
import com.dwicao.dextra.browser.BookmarkHtmlParser
import com.dwicao.dextra.browser.DefaultKeyboardShortcuts
import com.dwicao.dextra.browser.KeyChord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkAndShortcutTest {
    @Test
    fun parsesNestedNetscapeBookmarkFolders() {
        val html = """
            <DL><p>
            <DT><H3>Research</H3>
            <DL><p>
            <DT><A HREF="https://example.com/a">Example &amp; One</A>
            <DT><H3>Nested</H3><DL><p>
            <DT><A HREF='https://example.com/b'>Second</A>
            </DL><p>
            </DL><p>
        """.trimIndent()

        val bookmarks = BookmarkHtmlParser.parse(html)

        assertEquals(2, bookmarks.size)
        assertEquals("Research", bookmarks[0].folder)
        assertEquals("Nested", bookmarks[1].folder)
        assertEquals("Example & One", bookmarks[0].title)
    }

    @Test
    fun rejectsUnsafeBookmarkSchemes() {
        val bookmarks = BookmarkHtmlParser.parse(
            "<A HREF=\"javascript:alert(1)\">Unsafe</A><A HREF=\"https://safe.example\">Safe</A>",
        )

        assertEquals(1, bookmarks.size)
        assertEquals("https://safe.example", bookmarks.single().url)
    }

    @Test
    fun keyChordEncodingRoundTrips() {
        val chord = KeyChord(KeyEvent.KEYCODE_P, ctrl = true, shift = true)
        assertEquals(chord, KeyChord.decode(chord.encode()))
        assertEquals("Ctrl+Shift+P", chord.displayName())
    }

    @Test
    fun defaultShortcutsDoNotConflict() {
        val values = DefaultKeyboardShortcuts.bindings.values
        assertEquals(values.size, values.toSet().size)
        assertFalse(DefaultKeyboardShortcuts.bindings.containsKey(com.dwicao.dextra.browser.BrowserCommandId.NEW_PRIVATE_TAB))
        assertTrue(DefaultKeyboardShortcuts.bindings.isNotEmpty())
    }
}
