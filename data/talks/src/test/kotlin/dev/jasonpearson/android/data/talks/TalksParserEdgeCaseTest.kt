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

package dev.jasonpearson.android.data.talks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TalksParserEdgeCaseTest {

    private fun fixture(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
        assertNotNull("$name fixture should be on the classpath", stream)
        return stream!!.bufferedReader().use { it.readText() }
    }

    private val edgeCases by lazy { parseTalks(fixture("talks-edge-cases.html")) }

    private fun talk(title: String): Talk =
        edgeCases.firstOrNull { it.title == title } ?: error("No talk titled \"$title\"")

    @Test
    fun `empty and blank pages parse to no talks`() {
        assertEquals(emptyList<Talk>(), parseTalks(""))
        assertEquals(emptyList<Talk>(), parseTalks("<p></p><p>   </p>"))
    }

    @Test
    fun `leading cards with no talk above them are ignored`() {
        assertEquals("Card Without Links", edgeCases.first().title)
    }

    @Test
    fun `card without links keeps its fields and has no links`() {
        val talk = talk("Card Without Links")
        assertEquals("KotlinConf 2024", talk.event)
        assertNull(talk.date)
        assertNull(talk.imageUrl)
        assertEquals(emptyList<TalkLink>(), talk.links)
    }

    @Test
    fun `image-only paragraph illustrates the talk above it`() {
        val talk = talk("Image Only Talk")
        assertEquals("https://jasonpearson.dev/content/images/2022/05/stage.jpg", talk.imageUrl)
        assertEquals(emptyList<TalkLink>(), talk.links)
    }

    @Test
    fun `first image wins and lazy data-src images are used`() {
        assertEquals("https://example.com/lazy.jpg", talk("Lazy Image Talk").imageUrl)
    }

    @Test
    fun `unknown embeds and embeds without a source are dropped`() {
        val talk = talk("Unknown Embed Talk")
        assertEquals(emptyList<TalkLink>(), talk.links)
        assertNull(talk.imageUrl)
    }

    @Test
    fun `youtube embeds become watch links`() {
        assertEquals(
            listOf(
                TalkLink("Watch", "https://www.youtube.com/watch?v=abc123XYZ", TalkLinkKind.WATCH)
            ),
            talk("YouTube Embed Talk").links,
        )
    }

    @Test
    fun `non-web links are dropped and relative links resolve against the site`() {
        val talk = talk("Odd Links Talk")
        assertEquals(
            listOf(
                TalkLink("Recap", "https://jasonpearson.dev/blog/recap/", TalkLinkKind.OTHER),
                TalkLink("example.com", "https://example.com/talk", TalkLinkKind.OTHER),
            ),
            talk.links,
        )
        assertTrue(talk.links.none { it.url.startsWith("mailto:") })
    }

    @Test
    fun `bookmark cards become links labelled with their title`() {
        assertEquals(
            listOf(
                TalkLink(
                    "Recording on YouTube",
                    "https://www.youtube.com/watch?v=zzz",
                    TalkLinkKind.WATCH,
                )
            ),
            talk("Bookmark Talk").links,
        )
    }

    @Test
    fun `date-only paragraph is skipped and its cards do not attach to the previous talk`() {
        assertTrue(edgeCases.none { it.title.isBlank() || it.date != null })
        assertTrue(talk("Bookmark Talk").links.none { it.kind == TalkLinkKind.SLIDES })
    }

    @Test
    fun `duplicate links collapse to one`() {
        assertEquals(1, talk("Duplicate Links Talk").links.size)
    }

    @Test
    fun `lists every talk in page order`() {
        assertEquals(
            listOf(
                "Card Without Links",
                "Image Only Talk",
                "Lazy Image Talk",
                "Unknown Embed Talk",
                "YouTube Embed Talk",
                "Odd Links Talk",
                "Bookmark Talk",
                "Duplicate Links Talk",
            ),
            edgeCases.map(Talk::title),
        )
    }
}
