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
package dev.jasonpearson.android.data.projects

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadmeUrlsTest {

    private fun resolve(html: String) = resolveReadmeUrls(html, "kaeawc", "auto-mobile")

    @Test
    fun `relative image resolves to raw githubusercontent`() {
        assertEquals(
            """<img src="https://raw.githubusercontent.com/kaeawc/auto-mobile/HEAD/docs/logo.png" alt="logo">""",
            resolve("""<img src="docs/logo.png" alt="logo">"""),
        )
    }

    @Test
    fun `relative link resolves to the blob page`() {
        assertEquals(
            """<a href="https://github.com/kaeawc/auto-mobile/blob/HEAD/CONTRIBUTING.md">guide</a>""",
            resolve("""<a href="CONTRIBUTING.md">guide</a>"""),
        )
    }

    @Test
    fun `dot segments and root-relative paths are normalized within the repo`() {
        assertEquals(
            "https://github.com/kaeawc/auto-mobile/blob/HEAD/docs/setup.md",
            resolveReadmeUrl("./docs/../docs/setup.md", "kaeawc", "auto-mobile", isImage = false),
        )
        assertEquals(
            "https://raw.githubusercontent.com/kaeawc/auto-mobile/HEAD/img/a.gif",
            resolveReadmeUrl("/img/a.gif", "kaeawc", "auto-mobile", isImage = true),
        )
        assertEquals(
            "https://github.com/kaeawc/auto-mobile/blob/HEAD/LICENSE",
            resolveReadmeUrl("../../LICENSE", "kaeawc", "auto-mobile", isImage = false),
        )
    }

    @Test
    fun `query and fragment are preserved`() {
        assertEquals(
            "https://github.com/kaeawc/auto-mobile/blob/HEAD/docs/api.md#install",
            resolveReadmeUrl("docs/api.md#install", "kaeawc", "auto-mobile", isImage = false),
        )
        assertEquals(
            "https://raw.githubusercontent.com/kaeawc/auto-mobile/HEAD/a.svg?sanitize=true",
            resolveReadmeUrl("a.svg?sanitize=true", "kaeawc", "auto-mobile", isImage = true),
        )
    }

    @Test
    fun `absolute urls fragments and other schemes are untouched`() {
        val html =
            """<p><a href="https://example.com/x">x</a> <a href="#usage">u</a> """ +
                """<a href="mailto:me@example.com">m</a> """ +
                """<img src="https://camo.githubusercontent.com/abc"></p>"""

        assertEquals(html, resolve(html))
    }

    @Test
    fun `protocol relative urls get https`() {
        assertEquals(
            "https://img.shields.io/badge.svg",
            resolveReadmeUrl("//img.shields.io/badge.svg", "kaeawc", "auto-mobile", isImage = true),
        )
    }

    @Test
    fun `github blob image source points at the raw file`() {
        assertEquals(
            "https://raw.githubusercontent.com/kaeawc/auto-mobile/main/docs/demo.gif",
            resolveReadmeUrl(
                "https://github.com/kaeawc/auto-mobile/blob/main/docs/demo.gif",
                "kaeawc",
                "auto-mobile",
                isImage = true,
            ),
        )
        assertEquals(
            "https://github.com/kaeawc/auto-mobile/blob/main/docs/demo.gif",
            resolveReadmeUrl(
                "https://github.com/kaeawc/auto-mobile/blob/main/docs/demo.gif",
                "kaeawc",
                "auto-mobile",
                isImage = false,
            ),
        )
    }

    @Test
    fun `single quoted attributes and linked images are both resolved`() {
        assertEquals(
            """<a target="_blank" href='https://github.com/kaeawc/auto-mobile/blob/HEAD/shot.png'>""" +
                """<img src='https://raw.githubusercontent.com/kaeawc/auto-mobile/HEAD/shot.png' /></a>""",
            resolve("""<a target="_blank" href='shot.png'><img src='shot.png' /></a>"""),
        )
    }

    @Test
    fun `text outside tags and non url attributes are untouched`() {
        val html = """<p data-src="x.png">src="docs/a.png" is text</p>"""

        assertEquals(html, resolve(html))
    }
}
