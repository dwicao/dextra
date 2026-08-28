package com.dwicao.dextra.browser

import android.text.Html

data class ReaderModeState(
    val tabId: String,
    val url: String,
    val title: String,
    val content: String = "",
    val wordCount: Int = 0,
    val language: String? = null,
    val isLoading: Boolean = true,
)

fun readerTextFromHtml(html: String): String {
    val content = html
        .replace(Regex("(?is)<(script|style|noscript|template)[^>]*>.*?</\\1>"), "")
        .replace(Regex("(?is)<head[^>]*>.*?</head>"), "")
    return Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00a0', ' ')
        .lineSequence()
        .map(String::trimEnd)
        .joinToString("\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
