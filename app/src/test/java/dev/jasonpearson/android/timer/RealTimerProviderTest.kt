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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealTimerProviderTest {

    private val provider = RealTimerProvider()

    @Test
    fun `delay completes without real wall-clock wait`() = runTest {
        // If this test completes quickly, delay used virtual time (not real time).
        // A 60-second delay would time out the test if it used real time.
        provider.delay(60_000L)
    }

    @Test
    fun `delay advances virtual time by the requested duration`() = runTest {
        val before = currentTime
        provider.delay(1_000L)
        assertEquals(1_000L, currentTime - before)
    }

    @Test
    fun `sequential delays advance virtual time cumulatively`() = runTest {
        provider.delay(200L)
        provider.delay(300L)
        assertEquals(500L, currentTime)
    }

    @Test
    fun `zero delay does not advance virtual time`() = runTest {
        val before = currentTime
        provider.delay(0L)
        assertEquals(before, currentTime)
    }
}
