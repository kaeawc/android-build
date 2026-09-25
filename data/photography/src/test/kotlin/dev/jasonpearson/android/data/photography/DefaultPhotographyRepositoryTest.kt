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

package dev.jasonpearson.android.data.photography

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPhotographyRepositoryTest {

    private class MapCache(var failWrites: Boolean = false) : ContentCache {
        val entries = mutableMapOf<String, CachedEntry>()

        override suspend fun get(key: String): CachedEntry? = entries[key]

        override suspend fun put(key: String, value: String) {
            if (failWrites) throw IOException("disk full")
            entries[key] = CachedEntry(value, Instant.fromEpochMilliseconds(42))
        }

        override suspend fun clear() = entries.clear()
    }

    /** Serves the "photography" page from [html], or fails like an offline network when null. */
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
                    GhostPostDto(id = "1", uuid = "u", slug = slug, title = "Photos", html = body)
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

    private fun page(vararg urls: String): String =
        urls.joinToString("\n") { url ->
            """<figure class="kg-card kg-image-card"><img src="$url"></figure>"""
        }

    private val cache = MapCache()
    private val api = FakeGhostApi(html = page("https://example.com/fresh.jpg"))
    private val repository = DefaultPhotographyRepository(GhostDataSource(api, "key"), cache)

    private fun NetworkResult<List<GalleryPhoto>>.urls(): List<String> =
        (this as NetworkResult.Success).data.map(GalleryPhoto::fullUrl)

    private suspend fun seedCacheWith(url: String) {
        api.html = page(url)
        repository.photos()
    }

    @Test
    fun `load fetches from the network and caches the result`() = runTest {
        assertEquals(listOf("https://example.com/fresh.jpg"), repository.photos().urls())
        assertTrue(cache.entries.getValue("photography:gallery").value.contains("fresh.jpg"))
    }

    @Test
    fun `load falls back to the cached copy when offline`() = runTest {
        seedCacheWith("https://example.com/cached.jpg")
        api.html = null

        assertEquals(listOf("https://example.com/cached.jpg"), repository.photos().urls())
    }

    @Test
    fun `refresh surfaces the network error instead of stale cache`() = runTest {
        seedCacheWith("https://example.com/cached.jpg")
        api.html = null

        val result = repository.photos(forceRefresh = true)

        assertTrue((result as NetworkResult.Failure).error is IOException)
    }

    @Test
    fun `refresh always hits the network and replaces the cached copy`() = runTest {
        seedCacheWith("https://example.com/cached.jpg")
        val requestsBefore = api.pageRequests
        api.html = page("https://example.com/refreshed.jpg")

        assertEquals(
            listOf("https://example.com/refreshed.jpg"),
            repository.photos(forceRefresh = true).urls(),
        )
        assertEquals(requestsBefore + 1, api.pageRequests)

        api.html = null
        assertEquals(listOf("https://example.com/refreshed.jpg"), repository.photos().urls())
    }

    @Test
    fun `refresh succeeds even when the cache write fails`() = runTest {
        cache.failWrites = true

        assertEquals(
            listOf("https://example.com/fresh.jpg"),
            repository.photos(forceRefresh = true).urls(),
        )
    }

    @Test
    fun `cache written before captions existed still decodes`() = runTest {
        cache.entries["photography:gallery"] =
            CachedEntry(
                """[{"thumbUrl":"t","fullUrl":"f","width":1,"height":2,"takenOn":null}]""",
                Instant.fromEpochMilliseconds(1),
            )
        api.html = null

        val photo = (repository.photos() as NetworkResult.Success).data.single()

        assertEquals("f", photo.fullUrl)
        assertNull(photo.caption)
        assertNull(photo.altText)
    }
}
