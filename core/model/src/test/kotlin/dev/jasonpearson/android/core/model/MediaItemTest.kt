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
package dev.jasonpearson.android.core.model

import dev.jasonpearson.android.core.common.Identifiable
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemTest {

    @Test
    fun `is addressable as an Identifiable from core-common`() {
        val item: Identifiable =
            MediaItem(
                id = "track-1",
                title = "Elephant March",
                artist = "The Herd",
                duration = 214.seconds,
                releaseDate = LocalDate(2024, 1, 1),
            )
        assertEquals("track-1", item.id)
    }

    @Test
    fun `data class equality holds by value`() {
        fun sample() =
            MediaItem("track-1", "Elephant March", "The Herd", 214.seconds, LocalDate(2024, 1, 1))
        assertTrue(sample() == sample())
    }
}
