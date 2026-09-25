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

package dev.jasonpearson.android.data.talks

import dev.jasonpearson.android.client.ghost.GhostDataSource
import dev.jasonpearson.android.client.ghost.api.GhostContentApi
import dev.jasonpearson.android.client.ghost.dto.GhostPagesResponse
import dev.jasonpearson.android.client.ghost.dto.GhostPostDto
import dev.jasonpearson.android.client.ghost.dto.GhostPostsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSearchIndexResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSettingsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostTagsResponse
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.subsystem.storage.CachedEntry
import dev.jasonpearson.android.subsystem.storage.ContentCache
import java.io.IOException
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTalksRepositoryTest {

    private class MapCache(var failWrites: Boolean = false) : ContentCache {
        val entries = mutableMapOf<String, CachedEntry>()

        override suspend fun get(key: String): CachedEntry? = entries[key]

        override suspend fun put(key: String, value: String) {
            if (failWrites) throw IOException("disk full")
            entries[key] = CachedEntry(value, Instant.fromEpochMilliseconds(42))
        }

        override suspend fun clear() = entries.clear()
    }

    /** Serves the "talks" page from [html], or fails like an offline network when it's null. */
    private class FakeGhostApi(var html: String?) : GhostContentApi {
        var pageRequests = 0

        override suspend fun getPageBySlug(
            slug: String,
            key: String,
            formats: String,
        ): GhostPagesResponse {
            pageRequests++
            val body = html ?: throw IOException("offline")
            return GhostPagesResponse(
                listOf(
                    GhostPostDto(id = "1", uuid = "u", slug = slug, title = "Talks", html = body)
                )
            )
        }

        override suspend fun getPosts(
            key: String,
            include: String,
            limit: String,
            formats: String,
        ): GhostPostsResponse = unsupported()

        override suspend fun getPostBySlug(
            slug: String,
            key: String,
            include: String,
            formats: String,
        ): GhostPostsResponse = unsupported()

        override suspend fun getPostsPage(
            key: String,
            filter: String?,
            page: Int,
            limit: Int,
            include: String,
            formats: String,
        ): GhostPostsResponse = unsupported()

        override suspend fun getTags(
            key: String,
            include: String,
            limit: Int,
            filter: String,
        ): GhostTagsResponse = unsupported()

        override suspend fun getSettings(key: String): GhostSettingsResponse = unsupported()

        override suspend fun getSearchIndexPosts(key: String): GhostSearchIndexResponse =
            unsupported()

        private fun unsupported(): Nothing = throw UnsupportedOperationException()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = MapCache()
    private val api = FakeGhostApi(html = "<p>DroidCon 2024 - \"Fresh Talk\"</p>")
    private val repository = DefaultTalksRepository(GhostDataSource(api, "key"), cache, json)

    private fun NetworkResult<List<Talk>>.titles(): List<String> =
        (this as NetworkResult.Success).data.map(Talk::title)

    private suspend fun seedCacheWith(title: String) {
        api.html = "<p>\"$title\"</p>"
        repository.talks()
    }

    @Test
    fun `load fetches from the network and caches the result`() = runTest {
        assertEquals(listOf("Fresh Talk"), repository.talks().titles())
        assertTrue(cache.entries.getValue("talks:list").value.contains("Fresh Talk"))
    }

    @Test
    fun `load falls back to the cached copy when offline`() = runTest {
        seedCacheWith("Cached Talk")
        api.html = null

        assertEquals(listOf("Cached Talk"), repository.talks().titles())
    }

    @Test
    fun `refresh surfaces the network error instead of stale cache`() = runTest {
        seedCacheWith("Cached Talk")
        api.html = null

        val result = repository.talks(forceRefresh = true)

        assertTrue((result as NetworkResult.Failure).error is IOException)
    }

    @Test
    fun `refresh always hits the network and replaces the cached copy`() = runTest {
        seedCacheWith("Cached Talk")
        val requestsBefore = api.pageRequests
        api.html = "<p>\"Refreshed Talk\"</p>"

        assertEquals(listOf("Refreshed Talk"), repository.talks(forceRefresh = true).titles())
        assertEquals(requestsBefore + 1, api.pageRequests)

        api.html = null
        assertEquals(listOf("Refreshed Talk"), repository.talks().titles())
    }

    @Test
    fun `refresh succeeds even when the cache write fails`() = runTest {
        cache.failWrites = true

        assertEquals(listOf("Fresh Talk"), repository.talks(forceRefresh = true).titles())
    }
}
