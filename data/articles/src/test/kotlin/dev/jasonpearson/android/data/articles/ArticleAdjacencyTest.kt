/*
 * MIT License
 *
 * Copyright (c) 2026 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
