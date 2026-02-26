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

/**
 * Test double for [TimerProvider] that records delay calls and returns immediately.
 *
 * Use this in unit tests to verify timing behavior without real wall-clock waits.
 *
 * Example:
 * ```
 * val fakeTimer = FakeTimer()
 * myClass.doSomethingWithDelay(fakeTimer)
 * assertEquals(listOf(200L), fakeTimer.delays)
 * ```
 */
class FakeTimer : TimerProvider {
    private val _delays = mutableListOf<Long>()

    /** All delay durations (in milliseconds) that were requested, in call order. */
    val delays: List<Long>
        get() = _delays.toList()

    /** Returns immediately without suspending. Records [millis] in [delays]. */
    override suspend fun delay(millis: Long) {
        _delays.add(millis)
    }
}
