package com.dwicao.dextra

import com.dwicao.dextra.browser.BrowserUrl
import com.dwicao.dextra.data.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserUrlTest {
    @Test
    fun keepsExplicitWebUrls() {
        assertEquals(
            "https://example.com/docs",
            BrowserUrl.resolve("https://example.com/docs", SearchEngine.DUCKDUCKGO),
        )
    }

    @Test
    fun addsHttpsToAHost() {
        assertEquals(
            "https://example.com",
            BrowserUrl.resolve("example.com", SearchEngine.DUCKDUCKGO),
        )
    }

    @Test
    fun searchesNaturalLanguageInput() {
        assertEquals(
            "https://duckduckgo.com/?q=gecko+browser",
            BrowserUrl.resolve("gecko browser", SearchEngine.DUCKDUCKGO),
        )
    }

    @Test
    fun supportsBraveSearch() {
        assertEquals(
            "https://search.brave.com/search?q=gecko+browser",
            BrowserUrl.resolve("gecko browser", SearchEngine.BRAVE),
        )
    }

    @Test
    fun addsHttpsToHostWithPathAndPort() {
        assertEquals(
            "https://localhost:8080/docs",
            BrowserUrl.resolve("localhost:8080/docs", SearchEngine.GOOGLE),
        )
    }

    @Test
    fun preservesHttpUrlsInAddressBarInput() {
        assertEquals(
            "http://example.com/docs",
            BrowserUrl.resolve("http://example.com/docs", SearchEngine.GOOGLE),
        )
    }
}
