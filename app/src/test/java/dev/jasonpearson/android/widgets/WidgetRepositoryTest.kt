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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WidgetRepositoryTest {

    private lateinit var repository: WidgetRepository

    @Before
    fun setUp() {
        repository = WidgetRepositoryImpl()
    }

    @Test
    fun `getAll returns empty list initially`() {
        assertEquals(emptyList<Widget>(), repository.getAll())
    }

    @Test
    fun `add then getAll returns all added widgets in order`() {
        val widget1 = Widget("alpha")
        val widget2 = Widget("beta")

        repository.add(widget1)
        repository.add(widget2)

        assertEquals(listOf(widget1, widget2), repository.getAll())
    }

    @Test
    fun `getByName returns matching widget`() {
        val widget = Widget("alpha")
        repository.add(widget)

        assertEquals(widget, repository.getByName("alpha"))
    }

    @Test
    fun `getByName returns null when name does not match`() {
        repository.add(Widget("alpha"))

        assertNull(repository.getByName("nonexistent"))
    }

    @Test
    fun `getAll returns a snapshot not a live view`() {
        repository.add(Widget("alpha"))
        val snapshot = repository.getAll()

        repository.add(Widget("beta"))

        assertEquals(1, snapshot.size)
        assertEquals(2, repository.getAll().size)
    }
}
