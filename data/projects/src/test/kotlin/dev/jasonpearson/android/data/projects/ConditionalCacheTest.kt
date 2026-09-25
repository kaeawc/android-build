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
package dev.jasonpearson.android.data.projects

import dev.jasonpearson.android.client.github.Conditional
import dev.jasonpearson.android.client.github.GitHubRateLimitException
import dev.jasonpearson.android.subsystem.storage.CachedEntry
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.Fetched
import java.io.IOException
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionalCacheTest {

    private class MapCache : ContentCache {
        val entries = mutableMapOf<String, CachedEntry>()
        var writes = 0

        override suspend fun get(key: String): CachedEntry? = entries[key]

        override suspend fun put(key: String, value: String) {
            writes++
            entries[key] = CachedEntry(value, Instant.fromEpochMilliseconds(100L + writes))
        }

        override suspend fun clear() = entries.clear()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = MapCache()
    private val sentEtags = mutableListOf<String?>()

    private fun seed(etag: String?, body: String) {
        cache.entries[KEY] =
            CachedEntry(
                json.encodeToString(EtagEnvelope.serializer(), EtagEnvelope(etag, body)),
                Instant.fromEpochMilliseconds(7),
            )
    }

    private suspend fun fetch(
        forceRefresh: Boolean = false,
        response: () -> Conditional<String>,
    ): Result<Fetched<String>> =
        cache.fetchConditional(
            key = KEY,
            json = json,
            forceRefresh = forceRefresh,
            encode = { it },
            decode = { it },
            fetch = { etag ->
                sentEtags += etag
                response()
            },
        )

    private fun storedEnvelope(): EtagEnvelope =
        json.decodeFromString(EtagEnvelope.serializer(), cache.entries.getValue(KEY).value)

    @Test
    fun `200 stores the body together with its etag`() = runTest {
        val result = fetch { Conditional.Modified("fresh", "\"v1\"") }.getOrThrow()

        assertEquals("fresh", result.value)
        assertFalse(result.fromCache)
        assertEquals(listOf<String?>(null), sentEtags)
        assertEquals(EtagEnvelope("\"v1\"", "fresh"), storedEnvelope())
    }

    @Test
    fun `stored etag is sent and a 304 serves the cached body`() = runTest {
        seed("\"v1\"", "cached")

        val result = fetch { Conditional.NotModified }.getOrThrow()

        assertEquals(listOf<String?>("\"v1\""), sentEtags)
        assertEquals("cached", result.value)
        assertFalse("a 304 is a successful revalidation, not an offline fallback", result.fromCache)
        assertEquals(EtagEnvelope("\"v1\"", "cached"), storedEnvelope())
    }

    @Test
    fun `200 after a revalidation replaces body and etag`() = runTest {
        seed("\"v1\"", "old")

        val result = fetch { Conditional.Modified("new", "\"v2\"") }.getOrThrow()

        assertEquals("new", result.value)
        assertEquals(EtagEnvelope("\"v2\"", "new"), storedEnvelope())
    }

    @Test
    fun `force refresh skips If-None-Match`() = runTest {
        seed("\"v1\"", "cached")

        fetch(forceRefresh = true) { Conditional.Modified("fresh", "\"v2\"") }

        assertEquals(listOf<String?>(null), sentEtags)
    }

    @Test
    fun `legacy bare body still decodes and is fetched unconditionally`() = runTest {
        cache.entries[KEY] = CachedEntry("legacy", Instant.fromEpochMilliseconds(7))

        val result = fetch { throw IOException("offline") }.getOrThrow()

        assertEquals(listOf<String?>(null), sentEtags)
        assertEquals("legacy", result.value)
        assertTrue(result.fromCache)
    }

    @Test
    fun `undecodable cached body is not revalidated`() = runTest {
        seed("\"v1\"", "corrupt")

        val result =
            cache.fetchConditional<String>(
                key = KEY,
                json = json,
                encode = { it },
                decode = { error("bad cache") },
                fetch = { etag ->
                    sentEtags += etag
                    Conditional.Modified("fresh", "\"v2\"")
                },
            )

        assertEquals(listOf<String?>(null), sentEtags)
        assertEquals("fresh", result.getOrThrow().value)
    }

    @Test
    fun `rate limit falls back to the cached copy`() = runTest {
        seed("\"v1\"", "cached")

        val result = fetch { throw GitHubRateLimitException(null, TimeZone.UTC) }.getOrThrow()

        assertEquals("cached", result.value)
        assertTrue(result.fromCache)
        assertEquals(Instant.fromEpochMilliseconds(7), result.savedAt)
    }

    @Test
    fun `rate limit without a cache surfaces the friendly error with reset time`() = runTest {
        val limit = GitHubRateLimitException(Instant.fromEpochSeconds(1_700_000_000), TimeZone.UTC)

        val error = fetch { throw limit }.exceptionOrNull()

        assertSame(limit, error)
        assertEquals(
            "GitHub's hourly request limit was reached. Try again after 10:13 PM.",
            error?.message,
        )
        assertNull(cache.entries[KEY])
    }

    @Test
    fun `failed cache write does not fail a fresh fetch`() = runTest {
        val failing =
            object : ContentCache {
                override suspend fun get(key: String): CachedEntry? = null

                override suspend fun put(key: String, value: String) = throw IOException("full")

                override suspend fun clear() = Unit
            }

        val result =
            failing.fetchConditional<String>(
                key = KEY,
                json = json,
                encode = { it },
                decode = { it },
                fetch = { Conditional.Modified("fresh", null) },
            )

        assertEquals("fresh", result.getOrThrow().value)
    }

    private companion object {
        const val KEY = "projects:test"
    }
}
