package com.dwicao.dextra.browser

import java.net.URI

object NavigationPolicy {
    private val webSchemes = setOf("http", "https")
    private val internalSchemes = webSchemes + setOf("about", "moz-extension")

    fun isWebUrl(value: String): Boolean = scheme(value) in webSchemes

    fun isAllowedTopLevel(value: String, allowExtension: Boolean = false): Boolean {
        val parsedScheme = scheme(value) ?: return false
        if (parsedScheme == "moz-extension") return allowExtension
        return parsedScheme in internalSchemes
    }

    fun isSafeUserInput(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank() || trimmed.contains('\u0000')) return false
        val parsedScheme = runCatching { URI(trimmed).scheme?.lowercase() }.getOrNull()
            ?: Regex("^[A-Za-z][A-Za-z0-9+.-]*:").find(trimmed)?.value?.dropLast(1)?.lowercase()
        if (parsedScheme != null && parsedScheme !in setOf("http", "https", "about") && looksLikeHostPort(trimmed)) {
            return true
        }
        return parsedScheme == null || parsedScheme in setOf("http", "https", "about")
    }

    fun origin(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (scheme !in webSchemes || host.isBlank()) return null
        return buildString {
            append(scheme)
            append("://")
            append(host)
            uri.port.takeIf { it != -1 }?.let { append(":").append(it) }
        }
    }

    private fun scheme(value: String): String? = runCatching {
        URI(value).scheme?.lowercase()
    }.getOrNull()

    private fun looksLikeHostPort(value: String): Boolean {
        val host = value.substringBefore(':')
        val port = value.substringAfter(':', "").substringBefore('/').toIntOrNull()
        return port != null && port in 1..65535 &&
            (host.equals("localhost", ignoreCase = true) || host.contains('.') || host.all(Char::isDigit))
    }
}
