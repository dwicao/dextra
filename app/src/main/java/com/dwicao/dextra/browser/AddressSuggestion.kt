package com.dwicao.dextra.browser

import com.dwicao.dextra.data.Bookmark
import com.dwicao.dextra.data.HistoryEntry

enum class AddressSuggestionSource {
    BOOKMARK,
    HISTORY,
}

data class AddressSuggestion(
    val url: String,
    val title: String,
    val source: AddressSuggestionSource,
)

private data class RankedAddressSuggestion(
    val suggestion: AddressSuggestion,
    val score: Int,
    val recency: Long,
)

fun buildAddressSuggestions(
    query: String,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    privateMode: Boolean,
    limit: Int = 8,
): List<AddressSuggestion> {
    val normalized = query.trim().lowercase()
    val candidates = linkedMapOf<String, RankedAddressSuggestion>()

    fun add(url: String, title: String, source: AddressSuggestionSource, recency: Long) {
        if (url.isBlank()) return
        val normalizedTitle = title.ifBlank { BrowserUrl.displayValue(url) }
        val lowerTitle = normalizedTitle.lowercase()
        val lowerUrl = url.lowercase()
        val matchScore = when {
            normalized.isBlank() -> 10
            lowerUrl == normalized -> 100
            lowerUrl.startsWith(normalized) -> 80
            lowerTitle.startsWith(normalized) -> 70
            lowerUrl.contains(normalized) -> 50
            lowerTitle.contains(normalized) -> 40
            else -> return
        }
        val sourceBonus = if (source == AddressSuggestionSource.BOOKMARK) 15 else 0
        val ranked = RankedAddressSuggestion(
            suggestion = AddressSuggestion(url, normalizedTitle, source),
            score = matchScore + sourceBonus,
            recency = recency,
        )
        val previous = candidates[url]
        if (previous == null || ranked.score > previous.score ||
            ranked.score == previous.score && ranked.recency > previous.recency
        ) {
            candidates[url] = ranked
        }
    }

    bookmarks.forEach { bookmark ->
        add(bookmark.url, bookmark.title, AddressSuggestionSource.BOOKMARK, bookmark.createdAt)
    }
    if (!privateMode) {
        history.forEach { entry ->
            add(entry.url, entry.title, AddressSuggestionSource.HISTORY, entry.visitedAt)
        }
    }

    return candidates.values
        .sortedWith(compareByDescending<RankedAddressSuggestion> { it.score }.thenByDescending { it.recency })
        .take(limit.coerceAtLeast(0))
        .map { it.suggestion }
}
