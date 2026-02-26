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

import android.util.Log
import dev.jasonpearson.android.di.AppScope
import dev.jasonpearson.android.di.SingleIn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Production implementation of [Logger] that delegates to [android.util.Log].
 *
 * Bound in the DI graph via [@ContributesBinding]. Use [FakeLogger] in tests.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class AndroidLogger : Logger {
    override fun log(priority: Logger.Priority, tag: String, message: String, throwable: Throwable?) {
        when (priority) {
            Logger.Priority.VERBOSE -> if (throwable != null) Log.v(tag, message, throwable) else Log.v(tag, message)
            Logger.Priority.DEBUG -> if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
            Logger.Priority.INFO -> if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
            Logger.Priority.WARN -> if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
            Logger.Priority.ERROR -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        }
    }
}
