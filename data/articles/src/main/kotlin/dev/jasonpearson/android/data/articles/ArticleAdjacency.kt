package dev.jasonpearson.android.data.articles

import dev.jasonpearson.android.core.model.Article

/** Returns (newer, older); an unknown slug has no neighbors. */
internal fun adjacentArticles(articles: List<Article>, slug: String): Pair<Article?, Article?> {
    val ordered = articles.sortedWith(compareByDescending { it.publishedAt })
    val index = ordered.indexOfFirst { it.slug == slug }
    if (index < 0) return null to null
    return ordered.getOrNull(index - 1) to ordered.getOrNull(index + 1)
}
