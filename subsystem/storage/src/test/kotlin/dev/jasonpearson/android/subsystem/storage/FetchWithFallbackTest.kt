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
package dev.jasonpearson.android.subsystem.storage

import java.io.IOException
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchWithFallbackTest {

    private class MapCache(var failWrites: Boolean = false) : ContentCache {
        val entries = mutableMapOf<String, CachedEntry>()

        override suspend fun get(key: String): CachedEntry? = entries[key]

        override suspend fun put(key: String, value: String) {
            if (failWrites) throw IOException("disk full")
            entries[key] = CachedEntry(value, Instant.fromEpochMilliseconds(42))
        }

        override suspend fun clear() = entries.clear()
    }

    @Test
    fun `fresh fetch is returned and cached`() = runTest {
        val cache = MapCache()
        val result = cache.fetchWithFallback("k", { it }, { it }) { "fresh" }.getOrThrow()
        assertEquals("fresh", result.value)
        assertFalse(result.fromCache)
        assertEquals("fresh", cache.entries["k"]?.value)
    }

    @Test
    fun `failed fetch falls back to the cached copy`() = runTest {
        val cache =
            MapCache().apply { entries["k"] = CachedEntry("old", Instant.fromEpochMilliseconds(7)) }
        val result =
            cache
                .fetchWithFallback<String>("k", { it }, { it }) { throw IOException("offline") }
                .getOrThrow()
        assertEquals("old", result.value)
        assertTrue(result.fromCache)
        assertEquals(Instant.fromEpochMilliseconds(7), result.savedAt)
    }

    @Test
    fun `failed fetch without cache surfaces the error`() = runTest {
        val result =
            MapCache().fetchWithFallback<String>("k", { it }, { it }) {
                throw IOException("offline")
            }
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `cache write failure does not fail a successful fetch`() = runTest {
        val result = MapCache(failWrites = true).fetchWithFallback("k", { it }, { it }) { "fresh" }
        assertEquals("fresh", result.getOrThrow().value)
    }

    @Test
    fun `undecodable cache entry surfaces the fetch error`() = runTest {
        val cache =
            MapCache().apply {
                entries["k"] = CachedEntry("garbage", Instant.fromEpochMilliseconds(1))
            }
        val result =
            cache.fetchWithFallback<Int>("k", { it.toString() }, { it.toInt() }) {
                throw IOException("offline")
            }
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
