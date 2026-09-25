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
package dev.jasonpearson.android.data.articles

import dev.jasonpearson.android.core.model.SearchEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleSearchTest {
    private val entries =
        listOf(
            entry("excerpt", "Notes", "Kotlin Compose patterns"),
            entry("title", "Kotlin Compose guide", null),
            entry("split", "Kotlin basics", "Compose tips"),
            entry("other", "Android", "Compose only"),
            entry("title-later", "Compose and Kotlin", null),
        )

    @Test
    fun multiTokenQueryMatchesAcrossTitleAndExcerpt() {
        assertEquals(
            listOf("title", "title-later", "excerpt", "split"),
            filterSearchIndex(entries, "Kotlin Compose").map { it.id },
        )
    }

    @Test
    fun titleMatchesRankFirstAndPreserveIndexOrder() {
        assertEquals(
            listOf("title", "title-later", "excerpt", "split"),
            filterSearchIndex(entries, "compose kotlin").map { it.id },
        )
    }

    @Test
    fun searchIsCaseInsensitive() {
        assertEquals(
            listOf("title", "title-later", "excerpt", "split"),
            filterSearchIndex(entries, "KoTlIn   CoMpOsE").map { it.id },
        )
    }

    @Test
    fun blankQueryReturnsEmptyList() {
        assertEquals(emptyList<SearchEntry>(), filterSearchIndex(entries, " \t  "))
    }

    @Test
    fun queryWithNoMatchesReturnsEmptyList() {
        assertEquals(emptyList<SearchEntry>(), filterSearchIndex(entries, "swift"))
    }

    private fun entry(id: String, title: String, excerpt: String?) =
        SearchEntry(id = id, slug = id, title = title, excerpt = excerpt, url = null)
}
