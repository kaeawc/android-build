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
package dev.jasonpearson.android.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeTimerTest {

    private lateinit var timer: FakeTimer

    @Before
    fun setUp() {
        timer = FakeTimer()
    }

    @Test
    fun `delays is empty initially`() {
        assertTrue(timer.delays.isEmpty())
    }

    @Test
    fun `delay records the requested duration`() = runTest {
        timer.delay(200L)
        assertEquals(listOf(200L), timer.delays)
    }

    @Test
    fun `delay records zero duration`() = runTest {
        timer.delay(0L)
        assertEquals(listOf(0L), timer.delays)
    }

    @Test
    fun `multiple delays are recorded in call order`() = runTest {
        timer.delay(100L)
        timer.delay(300L)
        timer.delay(50L)
        assertEquals(listOf(100L, 300L, 50L), timer.delays)
    }

    @Test
    fun `delays returns a snapshot not a live view`() = runTest {
        timer.delay(100L)
        val snapshot = timer.delays

        timer.delay(200L)

        assertEquals(1, snapshot.size)
        assertEquals(2, timer.delays.size)
    }

    @Test
    fun `delay does not advance virtual time`() = runTest {
        val before = currentTime
        // A huge value that would time out any real or virtual delay
        timer.delay(Long.MAX_VALUE / 2)
        assertEquals("FakeTimer must not advance the test scheduler", before, currentTime)
    }

    @Test
    fun `delay completes immediately and does not suspend indefinitely`() = runTest {
        // This test would hang if delay() never resumed the coroutine
        timer.delay(999_999_999L)
        // Reaching here proves delay() returned
        assertEquals(1, timer.delays.size)
    }
}
