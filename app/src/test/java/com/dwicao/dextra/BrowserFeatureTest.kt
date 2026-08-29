package com.dwicao.dextra

import com.dwicao.dextra.browser.BrowserUrl
import com.dwicao.dextra.browser.buildPrivacyOrigins
import com.dwicao.dextra.browser.sitePermissionLabel
import com.dwicao.dextra.data.CustomSearchEngine
import com.dwicao.dextra.data.SearchEngine
import com.dwicao.dextra.data.SitePermission
import com.dwicao.dextra.data.SiteSetting
import org.mozilla.geckoview.GeckoSession
import com.dwicao.dextra.data.SyncCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserFeatureTest {
    @Test
    fun resolvesCustomSearchTemplateAndEncodesQuery() {
        val url = BrowserUrl.resolve("dextra browser", "https://search.example/?q=%s&source=dextra")

        assertEquals("https://search.example/?q=dextra+browser&source=dextra", url)
    }

    @Test
    fun customSearchEngineHasStableIdentity() {
        val first = CustomSearchEngine(label = "Docs", searchUrl = "https://docs.example/search?q=%s")
        val second = first.copy()

        assertEquals(first.id, second.id)
        assertEquals("Docs", first.label)
        assertTrue(first.searchUrl.contains("%s"))
    }

    @Test
    fun encryptedSyncRoundTripDoesNotExposePlaintext() {
        val payload = "dextra sync payload".toByteArray()
        val encrypted = SyncCrypto.encrypt(payload, "correct horse battery")
        val restored = SyncCrypto.decrypt(encrypted, "correct horse battery")

        assertArrayEquals(payload, restored)
        assertFalse(payload.contentEquals(encrypted.data))
        assertEquals(SearchEngine.GOOGLE, SearchEngine.valueOf("GOOGLE"))
    }

    @Test
    fun encryptedSyncRejectsWrongPassphrase() {
        val encrypted = SyncCrypto.encrypt("private data".toByteArray(), "correct horse battery")

        var rejected = false
        try {
            SyncCrypto.decrypt(encrypted, "wrong passphrase")
        } catch (_: Exception) {
            rejected = true
        }

        assertTrue(rejected)
    }

    @Test
    fun privacyCenterGroupsPermissionsOverridesAndBlockedRequests() {
        val origins = buildPrivacyOrigins(
            permissions = listOf(SitePermission("https://example.com", GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION.toString(), "allow", 10L)),
            settings = listOf(SiteSetting("https://example.com", zoomPercent = 125, updatedAt = 20L)),
            blockedByOrigin = mapOf("https://example.com" to 3),
        )

        assertEquals(1, origins.size)
        assertEquals(1, origins.single().permissionCount)
        assertTrue(origins.single().hasSiteOverrides)
        assertEquals(3, origins.single().blockedCount)
        assertEquals("Location", sitePermissionLabel(GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION.toString()))
    }
}
