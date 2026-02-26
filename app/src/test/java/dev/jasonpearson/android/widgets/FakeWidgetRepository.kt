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
package dev.jasonpearson.android.widgets

import kotlin.time.Clock

/**
 * Test double for [WidgetRepository]. Simple in-memory implementation with no synchronization —
 * unit tests are single-threaded.
 *
 * Pass a [FakeClock][dev.jasonpearson.android.clock.FakeClock] to control timestamps in tests.
 */
class FakeWidgetRepository(private val clock: Clock = Clock.System) : WidgetRepository {
    private val widgets = mutableListOf<Widget>()

    override fun add(name: String): Widget {
        val widget = Widget(name = name, createdAt = clock.now())
        widgets.add(widget)
        return widget
    }

    override fun getByName(name: String): Widget? = widgets.firstOrNull { it.name == name }

    override fun getAll(): List<Widget> = widgets.toList()
}
