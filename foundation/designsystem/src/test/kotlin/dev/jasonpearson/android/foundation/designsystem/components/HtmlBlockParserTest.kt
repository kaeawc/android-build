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

import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.Code
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.Divider
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.Heading
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.Image
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.InlineImageGroup
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.ListBlock
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.Paragraph
import dev.jasonpearson.android.foundation.designsystem.components.HtmlBlock.Quote
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlBlockParserTest {

    @Test
    fun paragraphsKeepInlineMarkup() {
        assertEquals(
            listOf(
                Paragraph("Hello <strong>world</strong>, see <a href=\"https://x.dev\">this</a>."),
                Paragraph("Second"),
            ),
            parseHtmlBlocks(
                "<p>Hello <strong>world</strong>, see <a href=\"https://x.dev\">this</a>.</p>" +
                    "\n<p>Second</p>"
            ),
        )
    }

    @Test
    fun bareTextAndEmptyParagraphs() {
        assertEquals(listOf(Paragraph("just text")), parseHtmlBlocks("just text"))
        assertEquals(emptyList<HtmlBlock>(), parseHtmlBlocks("<p> &nbsp; </p><p><br></p>"))
        assertEquals(emptyList<HtmlBlock>(), parseHtmlBlocks(""))
    }

    @Test
    fun headingsCarryTheirLevel() {
        assertEquals(
            listOf(
                Heading(1, "One"),
                Heading(2, "Two <em>x</em>"),
                Heading(4, "Four"),
                Heading(6, "Six"),
            ),
            parseHtmlBlocks(
                "<h1>One</h1><h2 id=\"two\">Two <em>x</em></h2><h4>Four</h4><h6>Six</h6><h3> </h3>"
            ),
        )
    }

    @Test
    fun preCodeIsDecodedAndKeepsWhitespace() {
        val html =
            "<pre><code class=\"language-kotlin\">fun main() {\n" +
                "    println(&quot;a &lt; b &amp;&amp; c&quot;)\n}\n</code></pre>"
        assertEquals(
            listOf(Code("fun main() {\n    println(\"a < b && c\")\n}", "kotlin")),
            parseHtmlBlocks(html),
        )
    }

    @Test
    fun preWithoutCodeOrLanguage() {
        assertEquals(listOf(Code("line1\nline2")), parseHtmlBlocks("<pre>line1<br>line2</pre>"))
    }

    @Test
    fun blockquotesNestBlocks() {
        assertEquals(
            listOf(Quote(listOf(Paragraph("Quoted"), Paragraph("Also")))),
            parseHtmlBlocks("<blockquote><p>Quoted</p><p>Also</p></blockquote>"),
        )
        assertEquals(
            listOf(Quote(listOf(Paragraph("Bare quote")))),
            parseHtmlBlocks("<blockquote>Bare quote</blockquote>"),
        )
    }

    @Test
    fun listsFlattenWithDepthAndMarkers() {
        val html =
            "<ul><li>One</li><li>Two<ul><li>Two A</li><li><p>Two B</p></li></ul></li>" +
                "<li><code>three()</code></li></ul>" +
                "<ol start=\"3\"><li>Third</li><li>Fourth</li></ol>"
        assertEquals(
            listOf(
                ListBlock(
                    listOf(
                        HtmlListItem("One", 0, "•"),
                        HtmlListItem("Two", 0, "•"),
                        HtmlListItem("Two A", 1, "◦"),
                        HtmlListItem("Two B", 1, "◦"),
                        HtmlListItem("<code>three()</code>", 0, "•"),
                    )
                ),
                ListBlock(listOf(HtmlListItem("Third", 0, "3."), HtmlListItem("Fourth", 0, "4."))),
            ),
            parseHtmlBlocks(html),
        )
    }

    @Test
    fun unclosedListItemsEndAtTheNextItem() {
        assertEquals(
            listOf(ListBlock(listOf(HtmlListItem("a", 0, "1."), HtmlListItem("b", 0, "2.")))),
            parseHtmlBlocks("<ol><li>a<li>b</ol>"),
        )
    }

    @Test
    fun inlineImagesBecomeImageBlocks() {
        assertEquals(
            listOf(
                Paragraph("Before"),
                InlineImageGroup(listOf(InlineImageRef("https://x.dev/a.png", "A &amp; B"))),
                Paragraph("after"),
            ),
            parseHtmlBlocks(
                "<p>Before<img src=\"https://x.dev/a.png\" alt=\"A &amp;amp; B\">after</p>"
            ),
        )
        assertEquals(emptyList<HtmlBlock>(), parseHtmlBlocks("<img alt=\"no src\">"))
    }

    @Test
    fun badgeRowBecomesOneInlineImageGroup() {
        val html =
            "<p><a href=\"https://x.dev/a\"><img src=\"a.svg\" alt=\"A\"></a> " +
                "<a href=\"https://x.dev/b\"><img src=\"b.svg\" alt=\"B\" width=\"20\" height=\"20\"></a></p>"
        assertEquals(
            listOf(
                InlineImageGroup(
                    listOf(
                        InlineImageRef("a.svg", "A", href = "https://x.dev/a"),
                        InlineImageRef(
                            "b.svg",
                            "B",
                            href = "https://x.dev/b",
                            width = 20,
                            height = 20,
                        ),
                    )
                )
            ),
            parseHtmlBlocks(html),
        )
    }

    @Test
    fun figuresCarryCaptions() {
        val html =
            "<figure class=\"kg-card kg-image-card\"><img src=\"https://x.dev/a.png\" " +
                "class=\"kg-image\" alt=\"\" loading=\"lazy\">" +
                "<figcaption><span>Shot on <a href=\"https://x.dev\">film</a></span></figcaption>" +
                "</figure>"
        assertEquals(
            listOf(
                Image(
                    "https://x.dev/a.png",
                    null,
                    "<span>Shot on <a href=\"https://x.dev\">film</a></span>",
                )
            ),
            parseHtmlBlocks(html),
        )
    }

    @Test
    fun galleryFiguresYieldEveryImage() {
        val html =
            "<figure class=\"kg-gallery-card\"><div class=\"kg-gallery-row\">" +
                "<div><img src=\"1.jpg\"></div><div><img src=\"2.jpg\"></div></div></figure>"
        assertEquals(listOf(Image("1.jpg", null), Image("2.jpg", null)), parseHtmlBlocks(html))
    }

    @Test
    fun bookmarkCardsBecomeLinks() {
        val html =
            "<figure class=\"kg-card kg-bookmark-card\"><a class=\"kg-bookmark-container\" " +
                "href=\"https://x.dev/post?a=1&amp;b=2\"><div class=\"kg-bookmark-content\">" +
                "<div class=\"kg-bookmark-title\">Tips &amp; tricks</div></div>" +
                "<div class=\"kg-bookmark-thumbnail\"><img src=\"thumb.png\"></div></a></figure>"
        assertEquals(
            listOf(Paragraph("<a href=\"https://x.dev/post?a=1&amp;b=2\">Tips &amp; tricks</a>")),
            parseHtmlBlocks(html),
        )
    }

    @Test
    fun unknownAndHiddenTagsAreTolerated() {
        val html =
            "<!-- kg-card-begin: html --><custom-el data-x=\"1\">Custom <b>bold</b></custom-el>" +
                "<script>alert('x')</script><style>p{}</style><hr/>" +
                "<div><section>Nested <span>text</span></section></div>" +
                "<iframe src=\"https://youtube.com/embed/x\"></iframe>unclosed <em>tail"
        assertEquals(
            listOf(
                Paragraph("<custom-el data-x=\"1\">Custom <b>bold</b></custom-el>"),
                Divider,
                Paragraph("Nested <span>text</span>"),
                Paragraph("unclosed <em>tail"),
            ),
            parseHtmlBlocks(html),
        )
    }

    @Test
    fun malformedMarkupNeverThrows() {
        listOf(
                "<",
                "<p",
                "</p></div>",
                "<ul><li>",
                "<pre><code>",
                "<blockquote>",
                "<figure>",
                "a < b > c",
                "<a href='x>y'>",
            )
            .forEach { parseHtmlBlocks(it) }
    }

    @Test
    fun entitiesDecode() {
        assertEquals(
            "a < b & \"c\" ' é 😀 &bogus;",
            decodeHtmlEntities("a &lt; b &amp; &quot;c&quot; &#39; &#xE9; &#128512; &bogus;"),
        )
    }
}
