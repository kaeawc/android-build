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

import dev.jasonpearson.android.client.ghost.GhostDataSource
import dev.jasonpearson.android.client.ghost.api.GhostContentApi
import dev.jasonpearson.android.client.ghost.dto.GhostMetaDto
import dev.jasonpearson.android.client.ghost.dto.GhostPagesResponse
import dev.jasonpearson.android.client.ghost.dto.GhostPaginationDto
import dev.jasonpearson.android.client.ghost.dto.GhostPostDto
import dev.jasonpearson.android.client.ghost.dto.GhostPostsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSearchIndexPostDto
import dev.jasonpearson.android.client.ghost.dto.GhostSearchIndexResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSettingsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostTagDto
import dev.jasonpearson.android.client.ghost.dto.GhostTagsResponse
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.articles.DefaultArticlesRepository.Companion.PAGE_SIZE
import dev.jasonpearson.android.subsystem.storage.CachedEntry
import dev.jasonpearson.android.subsystem.storage.ContentCache
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultArticlesRepositoryTest {
    private val api = FakeGhostApi(posts = (1..40).map { post("p$it") })
    private val cache = InMemoryContentCache()
    private val repository =
        DefaultArticlesRepository(
            ghost = GhostDataSource(api, CONTENT_KEY),
            api = api,
            contentKey = CONTENT_KEY,
            contentCache = cache,
            json = Json { ignoreUnknownKeys = true },
        )

    @Test
    fun pagesWalkTheListInPageSizeChunks() = runTest {
        val first = repository.articlesPage(1).success()
        assertEquals((1..PAGE_SIZE).map { "p$it" }, first.articles.map { it.id })
        assertEquals(2, first.nextPage)
        assertEquals(40, first.total)
        assertFalse(first.fromCache)

        val last = repository.articlesPage(3).success()
        assertEquals((2 * PAGE_SIZE + 1..40).map { "p$it" }, last.articles.map { it.id })
        assertNull(last.nextPage)
        assertEquals(listOf(CONTENT_KEY), api.pageRequests.map { it.key }.distinct())
        assertEquals(listOf(PAGE_SIZE), api.pageRequests.map { it.limit }.distinct())
    }

    @Test
    fun tagPagesFilterByTag() = runTest {
        api.posts = listOf(post("k1", tag = "kotlin"), post("x1"), post("k2", tag = "kotlin"))

        val page = repository.articlesPage(1, tagSlug = "kotlin").success()

        assertEquals(listOf("k1", "k2"), page.articles.map { it.id })
        assertEquals("tag:kotlin", api.pageRequests.single().filter)
    }

    @Test
    fun anOrdinaryLoadFallsBackToTheCacheWhenOffline() = runTest {
        repository.articlesPage(1).success()
        api.failing = true

        val offline = repository.articlesPage(1).success()

        assertTrue(offline.fromCache)
        assertEquals((1..PAGE_SIZE).map { "p$it" }, offline.articles.map { it.id })
        assertEquals(2, offline.nextPage)
    }

    @Test
    fun aRefreshBypassesTheCacheAndReportsNetworkFailure() = runTest {
        repository.articlesPage(1).success()
        api.failing = true

        val result = repository.articlesPage(1, refresh = true)

        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun aRefreshHitsTheNetworkAndUpdatesTheCache() = runTest {
        repository.articlesPage(1).success()
        api.posts = listOf(post("fresh")) + api.posts

        val refreshed = repository.articlesPage(1, refresh = true).success()
        assertEquals("fresh", refreshed.articles.first().id)
        assertEquals(2, api.pageRequests.size)

        api.failing = true
        val offline = repository.articlesPage(1).success()
        assertTrue(offline.fromCache)
        assertEquals("fresh", offline.articles.first().id)
    }

    @Test
    fun aRefreshInvalidatesTheInMemorySearchIndex() = runTest {
        repository.search("p1").success()
        repository.search("p2").success()
        assertEquals(1, api.searchIndexRequests)

        repository.articlesPage(1, refresh = true).success()
        repository.search("p1").success()

        assertEquals(2, api.searchIndexRequests)
    }

    @Test
    fun pagedArticlesCarryReadingTime() = runTest {
        val longHtml = (1..WORDS_PER_MINUTE * 2).joinToString(" ", "<p>", "</p>") { "w$it" }
        api.posts =
            listOf(
                post("ghost", readingTime = 9, html = longHtml),
                post("estimated", readingTime = null, html = longHtml),
            )

        val page = repository.articlesPage(1).success()

        assertEquals(listOf(9, 2), page.articles.map { it.readingTimeMinutes })
        api.failing = true
        assertEquals(
            listOf(9, 2),
            repository.articlesPage(1).success().articles.map { it.readingTimeMinutes },
        )
    }

    private fun <T> NetworkResult<T>.success(): T =
        when (this) {
            is NetworkResult.Success -> data
            is NetworkResult.Failure -> throw AssertionError("Expected success", error)
        }

    private data class PageRequest(
        val key: String,
        val filter: String?,
        val page: Int,
        val limit: Int,
    )

    private class FakeGhostApi(var posts: List<GhostPostDto>) : GhostContentApi {
        var failing = false
        val pageRequests = mutableListOf<PageRequest>()
        var searchIndexRequests = 0

        override suspend fun getPostsPage(
            key: String,
            filter: String?,
            page: Int,
            limit: Int,
            include: String,
            formats: String,
        ): GhostPostsResponse {
            pageRequests += PageRequest(key, filter, page, limit)
            if (failing) throw IOException("offline")
            val tag = filter?.removePrefix("tag:")
            val matching = posts.filter {
                tag == null || it.tags.orEmpty().any { t -> t.slug == tag }
            }
            val pages = (matching.size + limit - 1) / limit
            return GhostPostsResponse(
                posts = matching.drop((page - 1) * limit).take(limit),
                meta =
                    GhostMetaDto(
                        GhostPaginationDto(
                            page = page,
                            limit = limit,
                            pages = pages,
                            total = matching.size,
                            next = (page + 1).takeIf { it <= pages },
                            prev = (page - 1).takeIf { it >= 1 },
                        )
                    ),
            )
        }

        override suspend fun getSearchIndexPosts(key: String): GhostSearchIndexResponse {
            searchIndexRequests++
            if (failing) throw IOException("offline")
            return GhostSearchIndexResponse(
                posts.map { GhostSearchIndexPostDto(it.id, it.slug, it.title) }
            )
        }

        override suspend fun getPosts(
            key: String,
            include: String,
            limit: String,
            formats: String,
        ): GhostPostsResponse = unused()

        override suspend fun getPostBySlug(
            slug: String,
            key: String,
            include: String,
            formats: String,
        ): GhostPostsResponse = unused()

        override suspend fun getPageBySlug(
            slug: String,
            key: String,
            formats: String,
        ): GhostPagesResponse = unused()

        override suspend fun getTags(
            key: String,
            include: String,
            limit: Int,
            filter: String,
        ): GhostTagsResponse = unused()

        override suspend fun getSettings(key: String): GhostSettingsResponse = unused()

        private fun unused(): Nothing = throw UnsupportedOperationException()
    }

    private class InMemoryContentCache : ContentCache {
        private val entries = mutableMapOf<String, CachedEntry>()

        override suspend fun get(key: String): CachedEntry? = entries[key]

        override suspend fun put(key: String, value: String) {
            entries[key] = CachedEntry(value, Instant.fromEpochMilliseconds(0))
        }

        override suspend fun clear() = entries.clear()
    }

    private companion object {
        const val CONTENT_KEY = "content-key"

        fun post(id: String, tag: String? = null, readingTime: Int? = 3, html: String? = null) =
            GhostPostDto(
                id = id,
                uuid = "uuid-$id",
                slug = id,
                title = id,
                html = html,
                readingTime = readingTime,
                tags = tag?.let { listOf(GhostTagDto(it, it, it)) },
            )
    }
}
