package com.dwicao.dextra.browser

data class ImportedBookmark(
    val url: String,
    val title: String,
    val folder: String?,
)

object BookmarkHtmlParser {
    private const val MAX_TITLE_LENGTH = 200
    private const val MAX_FOLDER_LENGTH = 80
    private val tokenPattern = Regex(
        "<H3\\b[^>]*>(.*?)</H3>|<A\\b([^>]*)>(.*?)</A>|</DL\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val hrefPattern = Regex(
        "\\bHREF\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
        RegexOption.IGNORE_CASE,
    )

    fun parse(html: String): List<ImportedBookmark> {
        val folders = ArrayDeque<String>()
        return buildList {
            tokenPattern.findAll(html).forEach { token ->
                when {
                    token.value.startsWith("</DL", ignoreCase = true) -> folders.removeLastOrNull()
                    token.groups[1]?.value != null -> {
                        val folder = decode(token.groups[1]?.value.orEmpty())
                            .take(MAX_FOLDER_LENGTH)
                            .takeIf(String::isNotBlank)
                        if (folder != null) folders.addLast(folder)
                    }
                    token.groups[2]?.value != null -> {
                        val attributes = token.groups[2]?.value.orEmpty()
                        val href = hrefPattern.find(attributes)?.let {
                            listOfNotNull(it.groups[1]?.value, it.groups[2]?.value, it.groups[3]?.value).firstOrNull()
                        } ?: return@forEach
                        val url = decode(href).trim()
                        if (!NavigationPolicy.isWebUrl(url)) return@forEach
                        val title = decode(token.groups[3]?.value.orEmpty())
                            .take(MAX_TITLE_LENGTH)
                            .ifBlank { BrowserUrl.displayValue(url) }
                        add(ImportedBookmark(url, title, folders.lastOrNull()))
                    }
                }
            }
        }
    }

    private fun decode(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }
        .trim()
}
