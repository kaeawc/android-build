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
package dev.jasonpearson.android.resume

import dev.jasonpearson.android.timer.FakeTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResumePresenterTest {

    @Test
    fun `items are empty before coroutine runs`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val presenter = ResumePresenter(FakeTimer(), scope)

        // Before advancing, init block's launch hasn't completed
        assertEquals(emptyList<ResumeItem>(), presenter.items.value)
    }

    @Test
    fun `items populated after delay completes`() = runTest {
        val fakeTimer = FakeTimer()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val presenter = ResumePresenter(fakeTimer, scope)

        scope.advanceUntilIdle()

        assertEquals(resumeItems, presenter.items.value)
    }

    @Test
    fun `delay is requested with correct duration`() = runTest {
        val fakeTimer = FakeTimer()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        ResumePresenter(fakeTimer, scope)

        scope.advanceUntilIdle()

        assertEquals(listOf(ResumePresenter.INITIAL_DELAY_MS), fakeTimer.delays)
    }

    @Test
    fun `delay is requested exactly once`() = runTest {
        val fakeTimer = FakeTimer()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        ResumePresenter(fakeTimer, scope)

        scope.advanceUntilIdle()

        assertEquals(1, fakeTimer.delays.size)
    }
}
