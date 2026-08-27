package com.dwicao.dextra.browser

import com.dwicao.dextra.data.SearchEngine
import java.net.URLEncoder
import java.net.URI
import java.nio.charset.StandardCharsets

object BrowserUrl {
    fun resolve(input: String, searchEngine: SearchEngine): String {
        val value = input.trim()
        if (value.isEmpty()) return ""
        if (!NavigationPolicy.isSafeUserInput(value)) return ""

        val scheme = runCatching { URI(value).scheme?.lowercase() }.getOrNull()
        if (scheme in setOf("http", "https", "about")) {
            return value
        }

        if (looksLikeHost(value)) return "https://$value"

        val encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        return searchEngine.searchUrl.replace("%s", encoded)
    }

    fun displayValue(url: String): String = url
        .removePrefix("https://")
        .removeSuffix("/")

    private fun looksLikeHost(value: String): Boolean {
        if (value.any(Char::isWhitespace)) return false
        val candidate = runCatching { URI("https://$value") }.getOrNull() ?: return false
        val host = candidate.host ?: return false
        return host.contains('.') || host.equals("localhost", ignoreCase = true) || host.all { it.isDigit() || it == '.' }
    }
}
