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
package dev.jasonpearson.android.feature.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WidgetHelpersTest {
    @Test
    fun onePhotoAlwaysUsesItsOnlyIndex() {
        assertEquals(0, photoIndexFor(dayOfYear = 365, count = 1))
    }

    @Test
    fun dayLargerThanCountWrapsAround() {
        assertEquals(1, photoIndexFor(dayOfYear = 7, count = 5))
    }

    @Test
    fun invalidCountThrows() {
        assertThrows(IllegalArgumentException::class.java) { photoIndexFor(1, 0) }
    }

    @Test
    fun smallImageNeedsNoDownsampling() {
        assertEquals(1, inSampleSizeFor(width = 640, height = 480, maxEdge = 720))
    }

    @Test
    fun largeImageFitsWithinLongestEdge() {
        assertEquals(4, inSampleSizeFor(width = 2400, height = 1600, maxEdge = 720))
    }
}
