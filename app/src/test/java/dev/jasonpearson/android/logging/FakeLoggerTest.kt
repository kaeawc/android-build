/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
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
package dev.jasonpearson.android.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeLoggerTest {

    private lateinit var logger: FakeLogger

    @Before
    fun setUp() {
        logger = FakeLogger()
    }

    @Test
    fun `entries are empty initially`() {
        assertTrue(logger.entries.isEmpty())
    }

    @Test
    fun `log records entry with correct priority, tag, and message`() {
        logger.log(Logger.Priority.INFO, "MyTag", "Hello world")

        assertEquals(1, logger.entries.size)
        val entry = logger.entries[0]
        assertEquals(Logger.Priority.INFO, entry.priority)
        assertEquals("MyTag", entry.tag)
        assertEquals("Hello world", entry.message)
        assertNull(entry.throwable)
    }

    @Test
    fun `log records throwable when provided`() {
        val ex = RuntimeException("boom")
        logger.log(Logger.Priority.ERROR, "Tag", "oops", ex)

        assertEquals(ex, logger.entries[0].throwable)
    }

    @Test
    fun `extension function e logs at ERROR priority`() {
        logger.e("Tag", "error message")

        assertEquals(Logger.Priority.ERROR, logger.entries[0].priority)
    }

    @Test
    fun `extension function w logs at WARN priority`() {
        logger.w("Tag", "warn message")

        assertEquals(Logger.Priority.WARN, logger.entries[0].priority)
    }

    @Test
    fun `extension function d logs at DEBUG priority`() {
        logger.d("Tag", "debug message")

        assertEquals(Logger.Priority.DEBUG, logger.entries[0].priority)
    }

    @Test
    fun `errors shorthand filters to ERROR entries only`() {
        logger.i("Tag", "info")
        logger.e("Tag", "error1")
        logger.w("Tag", "warn")
        logger.e("Tag", "error2")

        assertEquals(2, logger.errors.size)
        assertEquals("error1", logger.errors[0].message)
        assertEquals("error2", logger.errors[1].message)
    }

    @Test
    fun `warnings shorthand filters to WARN entries only`() {
        logger.e("Tag", "error")
        logger.w("Tag", "warn1")
        logger.w("Tag", "warn2")

        assertEquals(2, logger.warnings.size)
    }

    @Test
    fun `entries returns a snapshot not a live view`() {
        logger.i("Tag", "first")
        val snapshot = logger.entries

        logger.i("Tag", "second")

        assertEquals(1, snapshot.size)
        assertEquals(2, logger.entries.size)
    }
}
