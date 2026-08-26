package com.dwicao.dextra.browser

import java.net.URI

object FirefoxAddons {
    private const val amoHost = "addons.mozilla.org"

    private fun parse(value: String): URI? = runCatching { URI(value) }.getOrNull()

    fun isAmoUrl(value: String): Boolean {
        val uri = parse(value) ?: return false
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && (host == amoHost || host.endsWith(".$amoHost"))
    }

    fun isXpiUrl(value: String): Boolean {
        val uri = parse(value) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && uri.path.orEmpty().endsWith(".xpi", ignoreCase = true)
    }

    fun isXpiDownload(value: String, contentType: String?): Boolean =
        isXpiUrl(value) || (isAmoUrl(value) && contentType.orEmpty().contains("x-xpinstall", ignoreCase = true))

    fun listingSlug(value: String): String? {
        if (!isAmoUrl(value)) return null
        val segments = parse(value)?.path.orEmpty().split('/').filter(String::isNotBlank)
        val addonIndex = segments.indexOfLast { it.equals("addon", ignoreCase = true) }
        if (addonIndex < 0) return null
        return segments.getOrNull(addonIndex + 1)
            ?.takeIf { it.isNotBlank() && !it.contains('.') }
    }
}
