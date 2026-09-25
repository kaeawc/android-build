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
import org.junit.Assert.assertNull
import org.junit.Test

class TalkGroupingTest {

    private fun talk(title: String, event: String? = null, date: String? = null) =
        Talk(title = title, event = event, date = date, imageUrl = null, links = emptyList())

    @Test
    fun `year comes from the date, then the event, then the title`() {
        assertEquals(2019, talk("DroidCon SF", event = "Droidcon 2024", date = "11/16/2019").year)
        assertEquals(2023, talk("CI", event = "Droidcon London 2023").year)
        assertEquals(2018, talk("Year in Review 2018").year)
        assertNull(talk("Performant Android CI", event = "Platform Meetup").year)
        assertNull(talk("Build 12345 speedups").year)
    }

    @Test
    fun `groups newest year first, keeping page order, with unknown years joining the talk above`() {
        val top = talk("Top", event = "Meetup")
        val newA = talk("New A", event = "DroidCon NYC 2025")
        val unknown = talk("Unknown", event = "Meetup")
        val newB = talk("New B", event = "DevCommunity 2025")
        val old = talk("Old", date = "1/15/2020")
        val middle = talk("Middle", event = "Droidcon London 2023")
        val afterMiddle = talk("After Middle")

        val groups = groupTalksByYear(listOf(top, newA, unknown, newB, old, middle, afterMiddle))

        assertEquals(
            listOf(
                TalkYearGroup(2025, listOf(top, newA, unknown, newB)),
                TalkYearGroup(2023, listOf(middle, afterMiddle)),
                TalkYearGroup(2020, listOf(old)),
            ),
            groups,
        )
    }

    @Test
    fun `talks with no years at all stay in one group in page order`() {
        val talks = listOf(talk("B"), talk("A"))
        assertEquals(listOf(TalkYearGroup(null, talks)), groupTalksByYear(talks))
    }

    @Test
    fun `fixture groups by year`() {
        val html = javaClass.classLoader!!.getResourceAsStream("talks.html")!!.reader().readText()

        val groups = groupTalksByYear(parseTalks(html))

        assertEquals(listOf(2025, 2023, 2021, 2020, 2019), groups.map(TalkYearGroup::year))
        assertEquals(
            listOf(
                "DroidCon NYC 2025",
                "Minneapolis Platform Engineering Meetup",
                "DevCommunity 2025",
            ),
            groups.first().talks.map(Talk::event),
        )
    }

    @Test
    fun `empty input yields no groups`() {
        assertEquals(emptyList<TalkYearGroup>(), groupTalksByYear(emptyList()))
    }
}
