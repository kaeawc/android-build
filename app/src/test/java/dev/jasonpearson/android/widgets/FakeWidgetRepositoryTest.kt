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
package dev.jasonpearson.android.widgets

import dev.jasonpearson.android.clock.FakeClock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWidgetRepositoryTest {

    private lateinit var clock: FakeClock
    private lateinit var repository: FakeWidgetRepository

    @Before
    fun setUp() {
        clock = FakeClock()
        repository = FakeWidgetRepository(clock)
    }

    @Test
    fun `getAll returns empty list initially`() {
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `add returns the created widget`() {
        val widget = repository.add("alpha")
        assertEquals("alpha", widget.name)
    }

    @Test
    fun `add stamps createdAt from the clock`() {
        val widget = repository.add("alpha")
        assertEquals(Instant.fromEpochMilliseconds(0), widget.createdAt)
    }

    @Test
    fun `add stamps different createdAt when clock has advanced`() {
        clock.advance(10.seconds)
        val widget = repository.add("alpha")
        assertEquals(Instant.fromEpochMilliseconds(10_000), widget.createdAt)
    }

    @Test
    fun `add then getAll returns all added widgets in order`() {
        repository.add("alpha")
        repository.add("beta")
        val all = repository.getAll()
        assertEquals(2, all.size)
        assertEquals("alpha", all[0].name)
        assertEquals("beta", all[1].name)
    }

    @Test
    fun `getAll returns a snapshot not a live view`() {
        repository.add("alpha")
        val snapshot = repository.getAll()
        repository.add("beta")
        assertEquals(1, snapshot.size)
        assertEquals(2, repository.getAll().size)
    }

    @Test
    fun `getByName returns matching widget`() {
        repository.add("alpha")
        assertNotNull(repository.getByName("alpha"))
    }

    @Test
    fun `getByName returns null when name does not match`() {
        repository.add("alpha")
        assertNull(repository.getByName("beta"))
    }

    @Test
    fun `getByName returns null on empty repository`() {
        assertNull(repository.getByName("alpha"))
    }
}
