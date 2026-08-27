package com.dwicao.dextra

import com.dwicao.dextra.browser.NavigationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun acceptsWebOriginsWithPorts() {
        assertEquals("https://example.com:8443", NavigationPolicy.origin("https://example.com:8443/docs"))
    }

    @Test
    fun rejectsExecutableUserInputSchemes() {
        assertFalse(NavigationPolicy.isSafeUserInput("javascript:alert(1)"))
        assertFalse(NavigationPolicy.isSafeUserInput("file:///sdcard/private.txt"))
        assertTrue(NavigationPolicy.isSafeUserInput("https://example.com/docs"))
    }

    @Test
    fun allowsExtensionPagesOnlyWhenExplicitlyRequested() {
        assertFalse(NavigationPolicy.isAllowedTopLevel("moz-extension://addon/options.html"))
        assertTrue(NavigationPolicy.isAllowedTopLevel("moz-extension://addon/options.html", allowExtension = true))
    }
}
