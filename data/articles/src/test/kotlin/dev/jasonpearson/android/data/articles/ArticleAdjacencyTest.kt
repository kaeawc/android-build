package dev.jasonpearson.android.data.articles

import dev.jasonpearson.android.core.model.Article
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleAdjacencyTest {
    private val newest = article("newest", "2026-01-03T00:00:00Z")
    private val middle = article("middle", "2026-01-02T00:00:00Z")
    private val oldest = article("oldest", "2026-01-01T00:00:00Z")
    private val unordered = listOf(oldest, newest, middle)

    @Test
    fun middleArticleHasNewerAndOlderNeighbors() {
        assertEquals(newest to oldest, adjacentArticles(unordered, "middle"))
    }

    @Test
    fun newestArticleHasNoPreviousNeighbor() {
        assertEquals(null to middle, adjacentArticles(unordered, "newest"))
    }

    @Test
    fun oldestArticleHasNoNextNeighbor() {
        assertEquals(middle to null, adjacentArticles(unordered, "oldest"))
    }

    @Test
    fun unknownSlugHasNoNeighbors() {
        assertEquals(null to null, adjacentArticles(unordered, "missing"))
    }

    @Test
    fun singleArticleHasNoNeighbors() {
        assertEquals(null to null, adjacentArticles(listOf(middle), "middle"))
    }

    @Test
    fun nullPublishedDateSortsLast() {
        val undated = article("undated", null)
        assertEquals(middle to undated, adjacentArticles(unordered + undated, "oldest"))
    }

    private fun article(slug: String, publishedAt: String?) =
        Article(
            id = slug,
            slug = slug,
            title = slug,
            excerpt = null,
            html = null,
            featureImageUrl = null,
            publishedAt = publishedAt?.let { Instant.parse(it) },
            readingTimeMinutes = null,
            tags = emptyList(),
            author = null,
        )
}
