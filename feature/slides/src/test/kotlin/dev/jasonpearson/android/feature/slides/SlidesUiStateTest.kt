/*
 * MIT License
 *
 * Copyright (c) 2026 Jason Pearson
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
package dev.jasonpearson.android.feature.slides

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SlidesUiStateTest {

    private val slides = listOf("a", "b", "c")

    @Test
    fun `starts on the first slide`() {
        val state = slidesStateFor(slides)
        assertEquals(0, state.index)
        assertEquals("a", state.current)
        assertEquals("1 / 3", state.position)
    }

    @Test
    fun `next advances forward`() {
        val state = SlidesReducer.next(slidesStateFor(slides))
        assertEquals("b", state.current)
    }

    @Test
    fun `next wraps past the end`() {
        var state = slidesStateFor(slides)
        repeat(slides.size) { state = SlidesReducer.next(state) }
        assertEquals(0, state.index)
    }

    @Test
    fun `previous wraps before the start`() {
        val state = SlidesReducer.previous(slidesStateFor(slides))
        assertEquals(slides.lastIndex, state.index)
    }

    @Test
    fun `an empty slide list is rejected`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) { slidesStateFor(emptyList()) }
        assertTrue(error.message!!.isNotBlank())
    }
}
