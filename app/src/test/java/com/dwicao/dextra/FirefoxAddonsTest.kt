package com.dwicao.dextra

import com.dwicao.dextra.browser.FirefoxAddons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirefoxAddonsTest {
    @Test
    fun extractsSlugFromLocalizedAmoListing() {
        assertEquals(
            "ublock-origin",
            FirefoxAddons.listingSlug("https://addons.mozilla.org/en-US/firefox/addon/ublock-origin/"),
        )
    }

    @Test
    fun acceptsHttpsXpiPackages() {
        assertTrue(FirefoxAddons.isXpiUrl("https://addons.mozilla.org/firefox/downloads/file/123/example.xpi"))
        assertFalse(FirefoxAddons.isXpiUrl("http://addons.mozilla.org/firefox/downloads/file/123/example.xpi"))
    }

    @Test
    fun recognizesAmoInstallResponseByContentType() {
        assertTrue(FirefoxAddons.isXpiDownload("https://addons.mozilla.org/firefox/downloads/file/123/example", "application/x-xpinstall"))
        assertFalse(FirefoxAddons.isXpiDownload("https://example.com/file", "application/x-xpinstall"))
    }
}
