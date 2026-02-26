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
package dev.jasonpearson.android.di

import dev.jasonpearson.android.coroutines.TestCoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineScopesTest {

    // ── BackgroundAppCoroutineScope ──────────────────────────────────────────

    @Test
    fun `BackgroundAppCoroutineScope is active on creation`() {
        val scope = BackgroundAppCoroutineScope(TestCoroutineDispatchers())
        assertTrue(scope.isActive)
    }

    @Test
    fun `BackgroundAppCoroutineScope executes launched coroutines`() {
        val dispatchers = TestCoroutineDispatchers()
        val scope = BackgroundAppCoroutineScope(dispatchers)

        var executed = false
        scope.launch { executed = true }

        // UnconfinedTestDispatcher runs the coroutine immediately inline
        assertTrue(executed)
    }

    @Test
    fun `BackgroundAppCoroutineScope failure in one child does not cancel scope`() {
        val scope = BackgroundAppCoroutineScope(TestCoroutineDispatchers())

        // SupervisorJob: a failing child must not cancel the parent scope
        scope.launch { throw RuntimeException("intentional") }

        assertTrue("Scope should remain active after child failure", scope.isActive)
    }

    @Test
    fun `BackgroundAppCoroutineScope failure in one child does not cancel siblings`() {
        val scope = BackgroundAppCoroutineScope(TestCoroutineDispatchers())

        var siblingRan = false
        scope.launch { throw RuntimeException("intentional") }
        scope.launch { siblingRan = true }

        assertTrue(siblingRan)
    }

    @Test
    fun `BackgroundAppCoroutineScope cancel stops future launches`() {
        val scope = BackgroundAppCoroutineScope(TestCoroutineDispatchers())
        scope.coroutineContext[Job]!!.cancel()

        assertFalse(scope.isActive)
    }

    // ── MainAppCoroutineScope ────────────────────────────────────────────────

    @Test
    fun `MainAppCoroutineScope is active on creation`() {
        val scope = MainAppCoroutineScope(TestCoroutineDispatchers())
        assertTrue(scope.isActive)
    }

    @Test
    fun `MainAppCoroutineScope executes launched coroutines`() {
        val dispatchers = TestCoroutineDispatchers()
        val scope = MainAppCoroutineScope(dispatchers)

        var executed = false
        scope.launch { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `MainAppCoroutineScope failure in one child does not cancel scope`() {
        val scope = MainAppCoroutineScope(TestCoroutineDispatchers())

        scope.launch { throw RuntimeException("intentional") }

        assertTrue("Scope should remain active after child failure", scope.isActive)
    }

    @Test
    fun `MainAppCoroutineScope failure in one child does not cancel siblings`() {
        val scope = MainAppCoroutineScope(TestCoroutineDispatchers())

        var siblingRan = false
        scope.launch { throw RuntimeException("intentional") }
        scope.launch { siblingRan = true }

        assertTrue(siblingRan)
    }
}
