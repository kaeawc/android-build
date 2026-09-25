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
package dev.jasonpearson.android.foundation.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InlineImageSizeTest {

    @Test
    fun `uses intrinsic badge dimensions as dp when attributes are absent`() {
        val size = resolveInlineImageSize(104f, 20f, null, null, maxWidthDp = 300f)

        assertEquals(InlineImageSize(104f, 20f), size)
    }

    @Test
    fun `explicit attributes override intrinsic dimensions`() {
        val size = resolveInlineImageSize(104f, 20f, 50, 50, maxWidthDp = 300f)

        assertEquals(InlineImageSize(50f, 50f), size)
    }

    @Test
    fun `clamps width and preserves aspect ratio`() {
        val size = resolveInlineImageSize(200f, 50f, null, null, maxWidthDp = 100f)

        assertEquals(InlineImageSize(100f, 25f), size)
    }

    @Test
    fun `returns no size until intrinsic dimensions are known`() {
        assertNull(resolveInlineImageSize(null, null, null, null, maxWidthDp = 300f))
    }
}
