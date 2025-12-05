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

import dev.jasonpearson.android.di.AppScope
import dev.zacsweers.metro.ContributesBinding

interface WidgetRepository {

    fun add(widget: Widget)

    fun getByName(name: String): Widget?

    fun getAll(): List<Widget>
}

/**
 * Thread-safe implementation of [WidgetRepository].
 *
 * Uses an object singleton which is appropriate for application-scoped state.
 * All methods are synchronized to ensure thread-safety when accessing the mutable list.
 */
@ContributesBinding(AppScope::class)
object WidgetRepositoryImpl : WidgetRepository {

    private val widgets = mutableListOf<Widget>()

    @Synchronized
    override fun add(widget: Widget) {
        widgets.add(widget)
    }

    @Synchronized
    override fun getByName(name: String): Widget? {
        return widgets.firstOrNull { it.name == name }
    }

    @Synchronized
    override fun getAll(): List<Widget> {
        return widgets.toList()
    }
}
