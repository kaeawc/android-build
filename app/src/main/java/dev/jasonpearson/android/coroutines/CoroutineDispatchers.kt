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
package dev.jasonpearson.android.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Provides [CoroutineDispatcher] instances for the application.
 *
 * Inject this interface instead of using [kotlinx.coroutines.Dispatchers] directly so that tests
 * can substitute [TestCoroutineDispatchers] to run coroutines eagerly.
 *
 * Production code uses [DefaultCoroutineDispatchers]. Tests use [TestCoroutineDispatchers].
 */
interface CoroutineDispatchers {
    /** Main/UI thread dispatcher. Use for Composable state updates. */
    val main: CoroutineDispatcher

    /** Optimized for disk and network I/O. */
    val io: CoroutineDispatcher

    /** Optimized for CPU-intensive work. */
    val default: CoroutineDispatcher

    /** Runs the coroutine immediately in the current thread. Useful for testing. */
    val unconfined: CoroutineDispatcher
}
