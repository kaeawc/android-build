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
package dev.jasonpearson.android.data.bookmarks

import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.subsystem.storage.InMemoryKeyValueStore
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBookmarksRepositoryTest {
    private class FakeClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    @Test
    fun `toggle adds and then removes a bookmark`() = runTest {
        val repository = repository()
        val article = article("one")

        repository.toggle(article)
        assertEquals(listOf("one"), repository.bookmarks.first().map(Bookmark::slug))

        repository.toggle(article)
        assertTrue(repository.bookmarks.first().isEmpty())
    }

    @Test
    fun `bookmarks are emitted newest first`() = runTest {
        val clock = FakeClock(Instant.fromEpochMilliseconds(100))
        val repository = repository(clock = clock)

        repository.toggle(article("older"))
        clock.instant = Instant.fromEpochMilliseconds(200)
        repository.toggle(article("newer"))

        assertEquals(listOf("newer", "older"), repository.bookmarks.first().map(Bookmark::slug))
    }

    @Test
    fun `isBookmarked reflects adding and removing`() = runTest {
        val repository = repository()

        assertFalse(repository.isBookmarked("one").first())
        repository.toggle(article("one"))
        assertTrue(repository.isBookmarked("one").first())
        repository.remove("one")
        assertFalse(repository.isBookmarked("one").first())
    }

    @Test
    fun `corrupt JSON emits an empty list`() = runTest {
        val repository = repository(store = InMemoryKeyValueStore(mapOf("bookmarks.v1" to "{")))

        assertTrue(repository.bookmarks.first().isEmpty())
    }

    @Test
    fun `concurrent toggles preserve both bookmarks`() = runTest {
        val repository = repository()

        val first = async { repository.toggle(article("one")) }
        val second = async { repository.toggle(article("two")) }
        first.await()
        second.await()

        assertEquals(setOf("one", "two"), repository.bookmarks.first().map(Bookmark::slug).toSet())
    }

    private fun repository(
        store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
        clock: FakeClock = FakeClock(Instant.fromEpochMilliseconds(1_000)),
    ) = DefaultBookmarksRepository(store, clock)

    private fun article(slug: String) =
        Article(
            id = slug,
            slug = slug,
            title = "Title $slug",
            excerpt = "Excerpt $slug",
            html = null,
            featureImageUrl = "https://example.com/$slug.jpg",
            publishedAt = null,
            readingTimeMinutes = null,
            tags = emptyList(),
            author = null,
            url = "https://example.com/$slug",
        )
}
