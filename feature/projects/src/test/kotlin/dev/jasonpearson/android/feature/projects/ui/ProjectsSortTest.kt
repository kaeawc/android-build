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
package dev.jasonpearson.android.feature.projects.ui

import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.subsystem.experimentation.Treatment
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectsSortTest {

    @Test
    fun `control keeps the input order`() {
        val projects =
            listOf(
                project(1, "older", "2024-01-01T00:00:00Z"),
                project(2, "newer", "2024-03-01T00:00:00Z"),
            )

        assertEquals(projects, sortOthers(projects, Treatment.CONTROL))
    }

    @Test
    fun `variant sorts by update time descending`() {
        val oldest = project(1, "oldest", "2024-01-01T00:00:00Z")
        val newest = project(2, "newest", "2024-03-01T00:00:00Z")
        val middle = project(3, "middle", "2024-02-01T00:00:00Z")

        assertEquals(
            listOf(newest, middle, oldest),
            sortOthers(listOf(oldest, newest, middle), Treatment.VARIANT),
        )
    }

    @Test
    fun `variant puts null update times last and preserves their relative order`() {
        val nullFirst = project(1, "null-first", null)
        val datedOld = project(2, "dated-old", "2024-01-01T00:00:00Z")
        val nullSecond = project(3, "null-second", null)
        val datedNew = project(4, "dated-new", "2024-02-01T00:00:00Z")

        assertEquals(
            listOf(datedNew, datedOld, nullFirst, nullSecond),
            sortOthers(listOf(nullFirst, datedOld, nullSecond, datedNew), Treatment.VARIANT),
        )
    }

    private fun project(id: Long, name: String, updatedAt: String?): Project =
        Project(
            id = id,
            name = name,
            description = null,
            url = "https://example.com/$name",
            stars = id.toInt(),
            language = null,
            topics = emptyList(),
            updatedAt = updatedAt?.let(Instant::parse),
        )
}
