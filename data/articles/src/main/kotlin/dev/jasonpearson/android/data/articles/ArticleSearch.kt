package dev.jasonpearson.android.data.articles

import dev.jasonpearson.android.core.model.SearchEntry

internal fun filterSearchIndex(entries: List<SearchEntry>, query: String): List<SearchEntry> {
    val tokens = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    if (tokens.isEmpty()) return emptyList()

    val matches = entries.filter { entry ->
        tokens.all { token ->
            entry.title.contains(token, ignoreCase = true) ||
                entry.excerpt.orEmpty().contains(token, ignoreCase = true)
        }
    }
    // Stable partition: title-only matches lead, with index order preserved within each group.
    val (titleMatches, excerptMatches) =
        matches.partition { entry -> tokens.all { entry.title.contains(it, ignoreCase = true) } }
    return titleMatches + excerptMatches
}
