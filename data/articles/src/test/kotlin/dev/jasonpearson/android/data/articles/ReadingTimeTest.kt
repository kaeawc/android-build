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

import dev.jasonpearson.android.core.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingTimeTest {

    @Test
    fun missingOrEmptyHtmlHasNoEstimate() {
        assertNull(estimateReadingTimeMinutes(null))
        assertNull(estimateReadingTimeMinutes("   "))
        assertNull(estimateReadingTimeMinutes("<p> </p><img src=\"a.png\">&nbsp;"))
    }

    @Test
    fun shortPostRoundsUpToOneMinute() {
        assertEquals(1, estimateReadingTimeMinutes("<p>Hello world</p>"))
    }

    @Test
    fun roundsUpAtTheWordsPerMinuteBoundary() {
        assertEquals(1, estimateReadingTimeMinutes(words(WORDS_PER_MINUTE)))
        assertEquals(2, estimateReadingTimeMinutes(words(WORDS_PER_MINUTE + 1)))
        assertEquals(4, estimateReadingTimeMinutes(words(WORDS_PER_MINUTE * 4)))
    }

    @Test
    fun markupEntitiesAndScriptsAreNotWords() {
        val html =
            "<h2 class=\"title\">One two</h2>" +
                "<p>three&nbsp;&amp;&nbsp;<a href=\"https://x.dev\">four</a> — </p>" +
                "<script>var a = 1; var b = 2;</script><style>p { color: red; }</style>"
        // Only the four real words count, so the whole thing is well under a minute.
        assertEquals(1, estimateReadingTimeMinutes(html))
        assertEquals(1, estimateReadingTimeMinutes(html + words(WORDS_PER_MINUTE - 4)))
        assertEquals(2, estimateReadingTimeMinutes(html + words(WORDS_PER_MINUTE - 3)))
    }

    @Test
    fun ghostReadingTimeWins() {
        val article = article(readingTime = 7, html = words(WORDS_PER_MINUTE * 2))
        assertEquals(7, article.withReadingTime().readingTimeMinutes)
    }

    @Test
    fun missingOrZeroGhostReadingTimeFallsBackToEstimate() {
        val html = words(WORDS_PER_MINUTE * 3)
        assertEquals(
            3,
            article(readingTime = null, html = html).withReadingTime().readingTimeMinutes,
        )
        assertEquals(3, article(readingTime = 0, html = html).withReadingTime().readingTimeMinutes)
    }

    @Test
    fun noHtmlLeavesReadingTimeUnset() {
        assertNull(article(readingTime = null, html = null).withReadingTime().readingTimeMinutes)
    }

    private fun words(count: Int) = (1..count).joinToString(" ", "<p>", "</p>") { "word$it" }

    private fun article(readingTime: Int?, html: String?) =
        Article(
            id = "id",
            slug = "slug",
            title = "Title",
            excerpt = null,
            html = html,
            featureImageUrl = null,
            publishedAt = null,
            readingTimeMinutes = readingTime,
            tags = emptyList(),
            author = null,
        )
}
