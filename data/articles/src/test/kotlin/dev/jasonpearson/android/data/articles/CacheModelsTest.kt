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
