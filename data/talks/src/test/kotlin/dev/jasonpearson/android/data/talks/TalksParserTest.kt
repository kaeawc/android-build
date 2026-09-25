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
import org.junit.Test

class TalksParserTest {
    @Test
    fun `parses talk cards from fixture`() {
        val stream = javaClass.classLoader?.getResourceAsStream("talks.html")
        assertNotNull("talks.html fixture should be on the classpath", stream)
        val html = stream!!.bufferedReader().use { it.readText() }

        val talks = parseTalks(html)

        assertEquals(7, talks.size)

        assertEquals("AutoMobile", talks[0].title)
        assertEquals("DroidCon NYC 2025", talks[0].event)
        assertEquals(null, talks[0].date)
        assertEquals(emptyList<TalkLink>(), talks[0].links)
        assertEquals(null, talks[0].imageUrl)

        assertEquals("Performant Android CI", talks[1].title)
        assertEquals("Minneapolis Platform Engineering Meetup", talks[1].event)
        assertEquals(null, talks[1].date)
        assertEquals(
            listOf(
                TalkLink(
                    label = "Meetup",
                    url = "https://www.meetup.com/platform-engineering-minneapolis/",
                    kind = TalkLinkKind.EVENT,
                )
            ),
            talks[1].links,
        )

        assertEquals("Performant Android CI", talks[2].title)
        assertEquals("DevCommunity 2025", talks[2].event)
        assertEquals(null, talks[2].date)
        assertEquals(
            listOf(
                TalkLink(
                    label = "Slides",
                    url = "https://speakerdeck.com/kaeawc/performant-android-ci",
                    kind = TalkLinkKind.SLIDES,
                )
            ),
            talks[2].links,
        )

        assertEquals("From Laptop Builds to Advanced CI", talks[3].title)
        assertEquals("Droidcon London 2023", talks[3].event)
        assertEquals(null, talks[3].date)
        assertEquals(
            listOf(
                TalkLink(
                    label = "Slides",
                    url = "https://speakerdeck.com/kaeawc/from-laptop-builds-to-advanced-ci",
                    kind = TalkLinkKind.SLIDES,
                )
            ),
            talks[3].links,
        )
        assertEquals(
            "https://storage.ghost.io/c/jp/content/images/2023/10/droidcon-london.jpg",
            talks[3].imageUrl,
        )

        assertEquals("MotionLayout & RecyclerView", talks[4].title)
        assertEquals("Droidcon Italy 2021", talks[4].event)
        assertEquals(null, talks[4].date)
        assertEquals(
            listOf(
                TalkLink(
                    label = "Video",
                    url =
                        "https://www.droidcon.com/2020/11/17/motionlayout-recyclerview/?video=481206547",
                    kind = TalkLinkKind.WATCH,
                )
            ),
            talks[4].links,
        )
        assertEquals("https://pbs.twimg.com/media/example.jpg", talks[4].imageUrl)

        assertEquals("NYC Android Meetup", talks[5].title)
        assertEquals(null, talks[5].event)
        assertEquals("1/15/2020", talks[5].date)
        assertEquals(null, talks[5].imageUrl)
        assertEquals(
            listOf(
                TalkLink(
                    label = "Slides",
                    url = "https://speakerdeck.com/player/2468107af1a6418e92f063963ec3f0d6",
                    kind = TalkLinkKind.SLIDES,
                )
            ),
            talks[5].links,
        )

        assertEquals("DroidCon SF", talks[6].title)
        assertEquals(null, talks[6].event)
        assertEquals("11/16/2019", talks[6].date)
        assertEquals(null, talks[6].imageUrl)
        assertEquals(
            listOf(TalkLink("Watch", "https://vimeo.com/380845332", TalkLinkKind.WATCH)),
            talks[6].links,
        )
    }
}
