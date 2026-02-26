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
 * Test double for [Logger] that records all log calls without writing to logcat.
 *
 * Example:
 * ```
 * val logger = FakeLogger()
 * myService.doWork(logger)
 * assertEquals(1, logger.errors.size)
 * assertEquals("Expected error message", logger.errors[0].message)
 * ```
 */
class FakeLogger : Logger {

    /** A recorded log entry. */
    data class Entry(
        val priority: Logger.Priority,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )

    private val _entries = mutableListOf<Entry>()

    /** All recorded log entries in call order. */
    val entries: List<Entry>
        get() = _entries.toList()

    /** Shorthand for entries with [Logger.Priority.ERROR]. */
    val errors: List<Entry>
        get() = _entries.filter { it.priority == Logger.Priority.ERROR }

    /** Shorthand for entries with [Logger.Priority.WARN]. */
    val warnings: List<Entry>
        get() = _entries.filter { it.priority == Logger.Priority.WARN }

    override fun log(
        priority: Logger.Priority,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        _entries.add(Entry(priority, tag, message, throwable))
    }
}
