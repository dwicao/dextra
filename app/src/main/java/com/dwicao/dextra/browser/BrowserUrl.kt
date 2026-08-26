package com.dwicao.dextra.browser

import com.dwicao.dextra.data.SearchEngine
import java.net.URLEncoder
import java.net.URI
import java.nio.charset.StandardCharsets

object BrowserUrl {
    fun resolve(input: String, searchEngine: SearchEngine): String {
        val value = input.trim()
        if (value.isEmpty()) return ""

        val scheme = runCatching { URI(value).scheme?.lowercase() }.getOrNull()
        if (scheme in setOf("http", "https", "about", "file", "data")) {
            return value
        }

        if (looksLikeHost(value)) return "https://$value"

        val encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        return searchEngine.searchUrl.replace("%s", encoded)
    }

    fun displayValue(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .removeSuffix("/")

    private fun looksLikeHost(value: String): Boolean =
        !value.any(Char::isWhitespace) &&
            value.contains('.') &&
            !value.contains("/")
}
