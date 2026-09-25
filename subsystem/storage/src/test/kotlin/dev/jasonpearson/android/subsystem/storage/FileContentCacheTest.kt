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

import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileContentCacheTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private class FakeClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    @Test
    fun `put and get preserve value and clock timestamp`() = runTest {
        val instant = Instant.parse("2025-01-02T03:04:05Z")
        val cache = FileContentCache(temporaryFolder.newFolder(), FakeClock(instant))
        val value = "{\"message\":\"hello\\nworld\"}\nsecond line"

        cache.put("article/1", value)

        assertEquals(CachedEntry(value, instant), cache.get("article/1"))
    }

    @Test
    fun `overwriting updates value and timestamp`() = runTest {
        val first = Instant.parse("2025-01-02T03:04:05Z")
        val second = Instant.parse("2025-01-02T03:05:06Z")
        val clock = FakeClock(first)
        val cache = FileContentCache(temporaryFolder.newFolder(), clock)

        cache.put("article", "old")
        clock.instant = second
        cache.put("article", "new")

        assertEquals(CachedEntry("new", second), cache.get("article"))
    }

    @Test
    fun `missing and corrupt entries return null`() = runTest {
        val directory = temporaryFolder.newFolder()
        val cache = FileContentCache(directory, FakeClock(Instant.parse("2025-01-02T03:04:05Z")))
        assertNull(cache.get("missing"))

        cache.put("article", "value")
        File(directory, "cache").listFiles()!!.single().writeText("not-a-timestamp\nvalue")

        assertNull(cache.get("article"))
    }

    @Test
    fun `clear removes cached entries but keeps directory`() = runTest {
        val directory = temporaryFolder.newFolder()
        val cache = FileContentCache(directory, FakeClock(Instant.parse("2025-01-02T03:04:05Z")))
        cache.put("one", "first")
        cache.put("two", "second")

        cache.clear()

        assertNull(cache.get("one"))
        assertNull(cache.get("two"))
        assertTrue(File(directory, "cache").isDirectory)
    }

    @Test
    fun `sizeBytes is zero before first put and after clear`() = runTest {
        val cache =
            FileContentCache(
                temporaryFolder.newFolder(),
                FakeClock(Instant.parse("2025-01-02T03:04:05Z")),
            )
        assertEquals(0L, cache.sizeBytes())

        cache.put("one", "first")
        cache.clear()

        assertEquals(0L, cache.sizeBytes())
    }

    @Test
    fun `sizeBytes sums every cached entry`() = runTest {
        val directory = temporaryFolder.newFolder()
        val cache = FileContentCache(directory, FakeClock(Instant.parse("2025-01-02T03:04:05Z")))

        cache.put("one", "first")
        cache.put("two", "second-value")

        val expected = File(directory, "cache").listFiles()!!.sumOf(File::length)
        assertTrue(expected > 0)
        assertEquals(expected, cache.sizeBytes())
    }

    @Test
    fun `clear leaves the key value store in the same storage directory intact`() = runTest {
        val directory = temporaryFolder.newFolder()
        val cache = FileContentCache(directory, FakeClock(Instant.parse("2025-01-02T03:04:05Z")))
        val store = DataStoreKeyValueStore(directory)
        try {
            store.put("bookmarks.v1", "[]")
            store.put("settings.theme_mode", "Dark")
            cache.put("articles:all", "cached")

            cache.clear()

            assertNull(cache.get("articles:all"))
            assertEquals("[]", store.get("bookmarks.v1"))
            assertEquals("Dark", store.get("settings.theme_mode"))
        } finally {
            store.close()
        }

        // The persisted preferences file survives too, not just the in-memory DataStore snapshot.
        val reopened = DataStoreKeyValueStore(directory)
        try {
            assertEquals("Dark", reopened.get("settings.theme_mode"))
        } finally {
            reopened.close()
        }
    }
}
