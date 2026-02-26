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

/**
 * Abstraction over Android's [android.util.Log], enabling deterministic testing without real logcat
 * output.
 *
 * Production code uses [AndroidLogger]. Tests use [FakeLogger].
 *
 * Use the extension functions ([Logger.v], [Logger.d], etc.) rather than calling [log] directly.
 */
interface Logger {

    /** Log priorities matching [android.util.Log] constants. */
    enum class Priority {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }

    fun log(priority: Priority, tag: String, message: String, throwable: Throwable? = null)
}

/** Log a verbose message. */
fun Logger.v(tag: String, message: String, throwable: Throwable? = null) =
    log(Logger.Priority.VERBOSE, tag, message, throwable)

/** Log a debug message. */
fun Logger.d(tag: String, message: String, throwable: Throwable? = null) =
    log(Logger.Priority.DEBUG, tag, message, throwable)

/** Log an informational message. */
fun Logger.i(tag: String, message: String, throwable: Throwable? = null) =
    log(Logger.Priority.INFO, tag, message, throwable)

/** Log a warning message. */
fun Logger.w(tag: String, message: String, throwable: Throwable? = null) =
    log(Logger.Priority.WARN, tag, message, throwable)

/** Log an error message. */
fun Logger.e(tag: String, message: String, throwable: Throwable? = null) =
    log(Logger.Priority.ERROR, tag, message, throwable)
