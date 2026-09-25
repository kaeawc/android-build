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
import dev.jasonpearson.android.core.model.Author
import dev.jasonpearson.android.core.model.Tag
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheModelsTest {
    @Test
    fun articleJsonRoundTripPreservesInstantAndNestedFields() {
        val tag = Tag("tag-id", "android", "Android", 7)
        val article =
            Article(
                id = "article-id",
                slug = "reader",
                title = "Reader",
                excerpt = "An excerpt",
                html = "<p>Body</p>",
                featureImageUrl = "https://example.com/image.jpg",
                publishedAt = Instant.parse("2026-09-24T12:34:56Z"),
                readingTimeMinutes = 4,
                tags = listOf(tag),
                author = Author("author-id", "Jason", null, "https://example.com/author"),
                featured = true,
                primaryTag = tag,
                url = "https://example.com/reader",
            )
        val json = Json { ignoreUnknownKeys = true }

        val encoded = json.encodeToString(article.toCacheModel())
        val decoded = json.decodeFromString<CachedArticle>(encoded).toArticle()

        assertEquals(article, decoded)
        assertEquals(article.publishedAt, decoded.publishedAt)
    }
}
