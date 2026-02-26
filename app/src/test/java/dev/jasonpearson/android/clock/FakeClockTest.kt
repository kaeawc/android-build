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
package dev.jasonpearson.android.clock

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeClockTest {

    @Test
    fun `now returns initial epoch by default`() {
        val clock = FakeClock()
        assertEquals(Instant.fromEpochMilliseconds(0), clock.now())
    }

    @Test
    fun `now returns configured initial instant`() {
        val initial = Instant.fromEpochMilliseconds(1_000_000)
        val clock = FakeClock(initial)
        assertEquals(initial, clock.now())
    }

    @Test
    fun `advance moves clock forward`() {
        val clock = FakeClock()
        clock.advance(5.minutes)
        assertEquals(Instant.fromEpochMilliseconds(5 * 60 * 1000), clock.now())
    }

    @Test
    fun `multiple advances are cumulative`() {
        val clock = FakeClock()
        clock.advance(1.minutes)
        clock.advance(30.seconds)
        assertEquals(Instant.fromEpochMilliseconds(90_000), clock.now())
    }

    @Test
    fun `now returns same instant when not advanced`() {
        val clock = FakeClock()
        val first = clock.now()
        val second = clock.now()
        assertEquals(first, second)
    }
}
