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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jasonpearson.android.foundation.designsystem.util.openUrl

/**
 * Renders post/page HTML (Ghost, GitHub READMEs) as native Compose blocks: styled h1–h6, scrollable
 * monospace code blocks, blockquotes, nested lists, images via [NetworkImage], and links that open
 * in a Custom Tab. Unknown tags are tolerated: block-ish containers are descended into and
 * everything else is passed through as inline text.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    textSizeSp: Float = 16f,
) {
    val blocks = remember(html) { parseHtmlBlocks(html) }
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val styles =
        HtmlStyles(
            body =
                MaterialTheme.typography.bodyLarge.copy(
                    color = color,
                    fontSize = textSizeSp.sp,
                    lineHeight = (textSizeSp * 1.5f).sp,
                ),
            color = color,
            links =
                TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                ),
            onLinkClick = { url -> openUrl(context, url) },
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block -> HtmlBlockContent(block, styles) }
    }
}

private class HtmlStyles(
    val body: TextStyle,
    val color: Color,
    val links: TextLinkStyles,
    val onLinkClick: (String) -> Unit,
)

@Composable
private fun HtmlBlockContent(block: HtmlBlock, styles: HtmlStyles) {
    when (block) {
        is HtmlBlock.Paragraph -> InlineHtml(block.html, styles, styles.body)
        is HtmlBlock.Heading -> {
            val typography = MaterialTheme.typography
            val style =
                when (block.level) {
                    1 -> typography.headlineMedium
                    2 -> typography.headlineSmall
                    3 -> typography.titleLarge
                    else -> typography.titleMedium
                }
            InlineHtml(
                block.html,
                styles,
                style.copy(color = styles.color),
                Modifier.padding(top = 8.dp),
            )
        }
        is HtmlBlock.Code -> CodeBlock(block.code, styles)
        is HtmlBlock.Quote -> Quote(block.children, styles)
        is HtmlBlock.ListBlock -> ListItems(block.items, styles)
        is HtmlBlock.Image -> Figure(block, styles)
        HtmlBlock.Divider -> HorizontalDivider(Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun InlineHtml(
    html: String,
    styles: HtmlStyles,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val text = remember(html, styles.links) { inlineAnnotatedString(html, styles) }
    Text(text = text, style = style, modifier = modifier)
}

private fun inlineAnnotatedString(html: String, styles: HtmlStyles): AnnotatedString =
    AnnotatedString.fromHtml(
        // android.text.Html knows <tt> as monospace but not <code>.
        htmlString = html.replace(codeTag, "<$1tt>"),
        linkStyles = styles.links,
        linkInteractionListener = { link ->
            (link as? LinkAnnotation.Url)?.url?.let { url ->
                runCatching { styles.onLinkClick(url) }
            }
        },
    )

private val codeTag = Regex("""<(/?)code(\s[^>]*)?>""", RegexOption.IGNORE_CASE)

@Composable
private fun CodeBlock(code: String, styles: HtmlStyles) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = code,
            style =
                styles.body.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (styles.body.fontSize.value * 0.85f).sp,
                    lineHeight = (styles.body.fontSize.value * 1.3f).sp,
                ),
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
        )
    }
}

@Composable
private fun Quote(children: List<HtmlBlock>, styles: HtmlStyles) {
    val barColor = MaterialTheme.colorScheme.primary
    val quoteStyles =
        HtmlStyles(
            body = styles.body.copy(fontStyle = FontStyle.Italic),
            color = styles.color,
            links = styles.links,
            onLinkClick = styles.onLinkClick,
        )
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .drawBehind {
                    val width = 4.dp.toPx()
                    drawLine(
                        color = barColor,
                        start = Offset(width / 2, 0f),
                        end = Offset(width / 2, size.height),
                        strokeWidth = width,
                    )
                }
                .padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        children.forEach { child -> HtmlBlockContent(child, quoteStyles) }
    }
}

@Composable
private fun ListItems(items: List<HtmlListItem>, styles: HtmlStyles) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(modifier = Modifier.padding(start = (item.depth * 20).dp)) {
                Text(
                    text = item.marker,
                    style = styles.body,
                    modifier = Modifier.widthIn(min = 24.dp).padding(end = 6.dp),
                )
                InlineHtml(item.html, styles, styles.body)
            }
        }
    }
}

@Composable
private fun Figure(image: HtmlBlock.Image, styles: HtmlStyles) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NetworkImage(
            url = image.src,
            contentDescription = image.alt,
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.FillWidth,
        )
        image.captionHtml?.let { caption ->
            InlineHtml(
                caption,
                styles,
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Block parsing: pure Kotlin, no Android dependencies, so it can be unit tested on the JVM.
// ---------------------------------------------------------------------------------------------

/** A block-level piece of an HTML document; inline markup stays as HTML in the text fields. */
internal sealed interface HtmlBlock {
    data class Paragraph(val html: String) : HtmlBlock

    data class Heading(val level: Int, val html: String) : HtmlBlock

    /** Decoded, whitespace-preserving code; [language] from a `language-*` class if present. */
    data class Code(val code: String, val language: String? = null) : HtmlBlock

    data class Quote(val children: List<HtmlBlock>) : HtmlBlock

    /** A flattened (possibly nested) list; each item carries its own depth and marker. */
    data class ListBlock(val items: List<HtmlListItem>) : HtmlBlock

    data class Image(val src: String, val alt: String?, val captionHtml: String? = null) : HtmlBlock

    data object Divider : HtmlBlock
}

internal data class HtmlListItem(val html: String, val depth: Int, val marker: String)

internal fun parseHtmlBlocks(html: String): List<HtmlBlock> {
    val tokens = tokenizeHtml(html)
    return HtmlBlockParser(tokens).parse(0, tokens.size)
}

internal sealed interface HtmlToken {
    val raw: String

    data class Text(override val raw: String) : HtmlToken

    data class Open(
        val name: String,
        val attributes: Map<String, String>,
        override val raw: String,
        val selfClosing: Boolean,
    ) : HtmlToken

    data class Close(val name: String, override val raw: String) : HtmlToken
}

private val tagOrComment =
    Regex(
        """<!--[\s\S]*?-->|<!\[CDATA\[[\s\S]*?]]>|<![^>]*>|<(/?)([a-zA-Z][a-zA-Z0-9:-]*)((?:[^>"']|"[^"]*"|'[^']*')*)>"""
    )
private val attribute =
    Regex("""([^\s"'<>/=]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?""")

private val voidElements =
    setOf(
        "area",
        "base",
        "br",
        "col",
        "embed",
        "hr",
        "img",
        "input",
        "link",
        "meta",
        "source",
        "track",
        "wbr",
    )

internal fun tokenizeHtml(html: String): List<HtmlToken> {
    val tokens = mutableListOf<HtmlToken>()
    var cursor = 0
    for (match in tagOrComment.findAll(html)) {
        if (match.range.first > cursor) {
            tokens += HtmlToken.Text(html.substring(cursor, match.range.first))
        }
        cursor = match.range.last + 1
        val name = match.groupValues[2].lowercase()
        if (name.isEmpty()) continue // comment, doctype or CDATA
        if (match.groupValues[1] == "/") {
            tokens += HtmlToken.Close(name, match.value)
        } else {
            val attributeText = match.groupValues[3]
            val attributes =
                attribute.findAll(attributeText).associate { attr ->
                    val value =
                        attr.groupValues[2]
                            .ifEmpty { attr.groupValues[3] }
                            .ifEmpty { attr.groupValues[4] }
                    attr.groupValues[1].lowercase() to decodeHtmlEntities(value)
                }
            val selfClosing = name in voidElements || attributeText.trimEnd().endsWith("/")
            tokens += HtmlToken.Open(name, attributes, match.value, selfClosing)
        }
    }
    if (cursor < html.length) tokens += HtmlToken.Text(html.substring(cursor))
    return tokens
}

private val headings = mapOf("h1" to 1, "h2" to 2, "h3" to 3, "h4" to 4, "h5" to 5, "h6" to 6)

/** Structural tags that end the current paragraph; their contents are parsed as more blocks. */
private val blockContainers =
    setOf(
        "p",
        "div",
        "section",
        "article",
        "header",
        "footer",
        "main",
        "aside",
        "nav",
        "figcaption",
        "table",
        "caption",
        "thead",
        "tbody",
        "tfoot",
        "tr",
        "td",
        "th",
        "dl",
        "dt",
        "dd",
        "li",
        "details",
        "summary",
        "center",
        "address",
        "body",
        "html",
    )

/** Elements whose content is never shown. */
private val skippedElements =
    setOf(
        "head",
        "script",
        "style",
        "noscript",
        "template",
        "iframe",
        "svg",
        "video",
        "audio",
        "object",
        "canvas",
        "form",
        "button",
        "select",
        "textarea",
    )

private val bullets = listOf("•", "◦", "▪")

private class HtmlBlockParser(private val tokens: List<HtmlToken>) {

    fun parse(from: Int, to: Int): List<HtmlBlock> {
        val blocks = mutableListOf<HtmlBlock>()
        val inline = StringBuilder()
        fun flush() {
            val text = inline.toString().trim()
            if (hasVisibleText(text)) blocks += HtmlBlock.Paragraph(text)
            inline.clear()
        }

        var i = from
        while (i < to) {
            when (val token = tokens[i]) {
                is HtmlToken.Text -> {
                    inline.append(token.raw)
                    i++
                }
                is HtmlToken.Close -> {
                    if (token.name in blockContainers) flush()
                    else if (token.name !in skippedElements) inline.append(token.raw)
                    i++
                }
                is HtmlToken.Open -> {
                    val name = token.name
                    val end = if (token.selfClosing) i else findClose(i, to)
                    when {
                        name in headings -> {
                            flush()
                            val text = inlineHtml(i + 1, end).trim()
                            if (hasVisibleText(text)) {
                                blocks += HtmlBlock.Heading(headings.getValue(name), text)
                            }
                            i = end + 1
                        }
                        name == "pre" -> {
                            flush()
                            codeBlock(i + 1, end)?.let(blocks::add)
                            i = end + 1
                        }
                        name == "blockquote" -> {
                            flush()
                            val children = parse(i + 1, end)
                            if (children.isNotEmpty()) blocks += HtmlBlock.Quote(children)
                            i = end + 1
                        }
                        name == "ul" || name == "ol" -> {
                            flush()
                            val items =
                                listItems(i + 1, end, ordered = name == "ol", depth = 0, token)
                            if (items.isNotEmpty()) blocks += HtmlBlock.ListBlock(items)
                            i = end + 1
                        }
                        name == "img" -> {
                            flush()
                            image(token)?.let(blocks::add)
                            i++
                        }
                        name == "figure" -> {
                            flush()
                            blocks += figure(i + 1, end)
                            i = end + 1
                        }
                        name == "hr" -> {
                            flush()
                            blocks += HtmlBlock.Divider
                            i++
                        }
                        name in skippedElements -> i = end + 1
                        name in blockContainers -> {
                            flush()
                            i++
                        }
                        else -> {
                            inline.append(token.raw)
                            i++
                        }
                    }
                }
            }
        }
        flush()
        return blocks
    }

    /**
     * Index of the close tag matching the open tag at [openIndex], or [to] if it's never closed.
     */
    private fun findClose(openIndex: Int, to: Int): Int {
        val name = (tokens[openIndex] as HtmlToken.Open).name
        var depth = 0
        for (i in openIndex + 1 until to) {
            val token = tokens[i]
            if (token is HtmlToken.Open && token.name == name && !token.selfClosing) depth++
            if (token is HtmlToken.Close && token.name == name) {
                if (depth == 0) return i
                depth--
            }
        }
        return to
    }

    private fun inlineHtml(from: Int, to: Int): String = buildString {
        var i = from
        while (i < to) {
            val token = tokens[i]
            if (token is HtmlToken.Open && token.name in skippedElements && !token.selfClosing) {
                i = findClose(i, to) + 1
                continue
            }
            if (token !is HtmlToken.Open || token.name != "img") append(token.raw)
            i++
        }
    }

    private fun plainText(from: Int, to: Int): String = buildString {
        for (i in from until minOf(to, tokens.size)) {
            when (val token = tokens[i]) {
                is HtmlToken.Text -> append(token.raw)
                is HtmlToken.Open -> if (token.name == "br") append('\n')
                is HtmlToken.Close -> Unit
            }
        }
    }

    private fun codeBlock(from: Int, to: Int): HtmlBlock.Code? {
        val code = decodeHtmlEntities(plainText(from, to)).trim('\n').trimEnd()
        if (code.isBlank()) return null
        val language =
            (from until minOf(to, tokens.size))
                .asSequence()
                .mapNotNull { (tokens[it] as? HtmlToken.Open)?.attributes?.get("class") }
                .flatMap { it.split(' ') }
                .firstOrNull { it.startsWith("language-") || it.startsWith("lang-") }
                ?.substringAfter('-')
        return HtmlBlock.Code(code, language)
    }

    private fun listItems(
        from: Int,
        to: Int,
        ordered: Boolean,
        depth: Int,
        listToken: HtmlToken.Open,
    ): List<HtmlListItem> {
        val items = mutableListOf<HtmlListItem>()
        var number = listToken.attributes["start"]?.toIntOrNull() ?: 1
        var i = from
        while (i < to) {
            val token = tokens[i]
            if (token !is HtmlToken.Open || token.name != "li") {
                i++
                continue
            }
            val end = minOf(findClose(i, to), nextSiblingItem(i, to))
            val text = StringBuilder()
            val nested = mutableListOf<HtmlListItem>()
            var j = i + 1
            while (j < end) {
                val child = tokens[j]
                if (child is HtmlToken.Open && (child.name == "ul" || child.name == "ol")) {
                    val childEnd = findClose(j, end)
                    nested += listItems(j + 1, childEnd, child.name == "ol", depth + 1, child)
                    j = childEnd + 1
                    continue
                }
                if (
                    child is HtmlToken.Open && child.name in skippedElements && !child.selfClosing
                ) {
                    j = findClose(j, end) + 1
                    continue
                }
                val isParagraphTag =
                    (child is HtmlToken.Open && child.name == "p") ||
                        (child is HtmlToken.Close && child.name == "p")
                if (isParagraphTag) {
                    if (child is HtmlToken.Open && hasVisibleText(text.toString())) {
                        text.append("<br>")
                    }
                } else if (child !is HtmlToken.Open || child.name != "img") {
                    text.append(child.raw)
                }
                j++
            }
            val marker = if (ordered) "${number++}." else bullets[depth % bullets.size]
            val itemHtml = text.toString().trim()
            if (hasVisibleText(itemHtml)) items += HtmlListItem(itemHtml, depth, marker)
            items += nested
            // An implicitly closed item ends *at* the next <li>, which must not be skipped.
            i = if (end < to && tokens[end] is HtmlToken.Close) end + 1 else end
        }
        return items
    }

    /** Where an unclosed `<li>` implicitly ends: at the next `<li>` of the same list. */
    private fun nextSiblingItem(liIndex: Int, to: Int): Int {
        var depth = 0
        for (i in liIndex + 1 until to) {
            val token = tokens[i]
            if (token is HtmlToken.Open && (token.name == "ul" || token.name == "ol")) depth++
            if (token is HtmlToken.Close && (token.name == "ul" || token.name == "ol")) depth--
            if (depth == 0 && token is HtmlToken.Open && token.name == "li") return i
        }
        return to
    }

    private fun image(token: HtmlToken.Open, captionHtml: String? = null): HtmlBlock.Image? {
        val src = token.attributes["src"]?.takeIf(String::isNotBlank) ?: return null
        return HtmlBlock.Image(
            src,
            token.attributes["alt"]?.takeIf(String::isNotBlank),
            captionHtml,
        )
    }

    /**
     * Ghost cards: a bookmark card becomes a link paragraph, image and gallery cards become one
     * image per `<img>` (captioned on the last), anything else is parsed as ordinary content.
     */
    private fun figure(from: Int, to: Int): List<HtmlBlock> {
        val opens =
            (from until to).mapNotNull { index ->
                (tokens[index] as? HtmlToken.Open)?.let { index to it }
            }
        fun classOf(token: HtmlToken.Open) = token.attributes["class"].orEmpty()

        val bookmark = opens.firstOrNull { (_, t) ->
            t.name == "a" && "kg-bookmark-container" in classOf(t)
        }
        if (bookmark != null) {
            val href = bookmark.second.attributes["href"].orEmpty()
            val title =
                opens
                    .firstOrNull { (_, t) -> "kg-bookmark-title" in classOf(t) }
                    ?.let { (index, _) ->
                        decodeHtmlEntities(plainText(index + 1, findClose(index, to))).trim()
                    }
                    ?.takeIf(String::isNotBlank)
            if (href.isNotBlank()) {
                val label = title ?: href
                return listOf(
                    HtmlBlock.Paragraph(
                        "<a href=\"${escapeAttribute(href)}\">${escapeText(label)}</a>"
                    )
                )
            }
        }

        val caption =
            opens
                .firstOrNull { (_, t) -> t.name == "figcaption" }
                ?.let { (index, _) -> inlineHtml(index + 1, findClose(index, to)).trim() }
                ?.takeIf(::hasVisibleText)
        val images = opens.filter { (_, t) -> t.name == "img" }.mapNotNull { (_, t) -> image(t) }
        if (images.isEmpty()) return parse(from, to)
        return images.dropLast(1) + images.last().copy(captionHtml = caption)
    }
}

private val entity = Regex("""&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);""")
private val namedEntities =
    mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "ndash" to "–",
        "mdash" to "—",
        "hellip" to "…",
        "lsquo" to "‘",
        "rsquo" to "’",
        "ldquo" to "“",
        "rdquo" to "”",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
    )

/** Decodes numeric and common named entities; unknown names are left untouched. */
internal fun decodeHtmlEntities(text: String): String =
    if ('&' !in text) text
    else
        entity.replace(text) { match ->
            val name = match.groupValues[1]
            val codePoint =
                when {
                    name.startsWith("#x") || name.startsWith("#X") ->
                        name.substring(2).toIntOrNull(16)
                    name.startsWith("#") -> name.substring(1).toIntOrNull()
                    else -> null
                }
            when {
                codePoint != null && Character.isValidCodePoint(codePoint) ->
                    String(Character.toChars(codePoint))
                codePoint != null -> match.value
                else -> namedEntities[name] ?: match.value
            }
        }

private val anyTag = Regex("""<[^>]*>""")

/** True when [html] renders at least one non-whitespace character. */
private fun hasVisibleText(html: String): Boolean =
    decodeHtmlEntities(html.replace(anyTag, "")).any { !it.isWhitespace() }

private fun escapeText(text: String) =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun escapeAttribute(text: String) = escapeText(text).replace("\"", "&quot;")
