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

import kotlin.math.max

/**
 * Returns a zero-based daily index. Throws [IllegalArgumentException] if the day or count is
 * below 1.
 */
internal fun photoIndexFor(dayOfYear: Int, count: Int): Int {
    require(dayOfYear >= 1)
    require(count >= 1)
    return (dayOfYear - 1) % count
}

/** Returns a sample size that limits the longest decoded edge to [maxEdge]. */
internal fun inSampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    require(width > 0 && height > 0 && maxEdge > 0)
    val required = (max(width, height).toLong() + maxEdge - 1) / maxEdge
    var sample = 1
    while (sample < required) {
        if (sample > Int.MAX_VALUE / 2) return required.toInt()
        sample *= 2
    }
    return sample
}
