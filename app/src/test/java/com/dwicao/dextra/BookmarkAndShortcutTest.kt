package com.dwicao.dextra

import android.view.KeyEvent
import com.dwicao.dextra.browser.AddressSuggestionSource
import com.dwicao.dextra.browser.BookmarkHtmlParser
import com.dwicao.dextra.browser.buildAddressSuggestions
import com.dwicao.dextra.browser.buildPrivacyOrigins
import com.dwicao.dextra.browser.DefaultKeyboardShortcuts
import com.dwicao.dextra.browser.KeyChord
import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.HistoryEntry
import com.dwicao.dextra.data.SitePermission
import com.dwicao.dextra.data.SiteSetting
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

    @Test
    fun addressSuggestionsPreferBookmarkAndDeduplicateHistory() {
        val suggestions = buildAddressSuggestions(
            query = "docs",
            bookmarks = listOf(
                Bookmark(url = "https://docs.example/", title = "Docs", createdAt = 10),
            ),
            history = listOf(
                HistoryEntry(url = "https://docs.example/", title = "Docs", visitedAt = 100),
                HistoryEntry(url = "https://docs.example/guide", title = "Docs guide", visitedAt = 200),
            ),
            privateMode = false,
        )

        assertEquals(2, suggestions.size)
        assertEquals("https://docs.example/", suggestions[0].url)
        assertEquals(AddressSuggestionSource.BOOKMARK, suggestions[0].source)
    }

    @Test
    fun privateAddressSuggestionsExcludeHistory() {
        val suggestions = buildAddressSuggestions(
            query = "example",
            bookmarks = emptyList(),
            history = listOf(HistoryEntry(url = "https://example.com", title = "Example", visitedAt = 1)),
            privateMode = true,
        )

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun privacyOriginsMergePermissionAndSiteOverrideRecords() {
        val origins = buildPrivacyOrigins(
            permissions = listOf(
                SitePermission("https://example.com", "geo", "allow", 10),
                SitePermission("https://example.com", "notifications", "block", 20),
            ),
            settings = listOf(
                SiteSetting("https://example.com", zoomPercent = 125, updatedAt = 30),
                SiteSetting("https://other.example", adBlockingEnabled = false, updatedAt = 40),
            ),
        )

        assertEquals(listOf("https://other.example", "https://example.com"), origins.map { it.origin })
        assertEquals(2, origins[1].permissionCount)
        assertTrue(origins[1].hasSiteOverrides)
    }
}
