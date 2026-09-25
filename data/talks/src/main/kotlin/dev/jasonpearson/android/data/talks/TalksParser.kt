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

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Parses the Ghost "talks" page. Each non-blank top-level paragraph starts a talk (an optional
 * `M/D/YYYY` date prefix, then either `Event - "Title"` or a bare title, plus any inline links);
 * the image, embed and bookmark cards that follow it attach to that talk. Anything unrecognised is
 * skipped rather than failing the whole page.
 */
internal fun parseTalks(html: String): List<Talk> {
    val document = Jsoup.parse(html, BASE_URL)
    val body = document.body()
    val topLevelElements =
        if (body.children().isNotEmpty()) body.children() else document.children()
    val talks = mutableListOf<TalkBuilder>()
    var current: TalkBuilder? = null

    for (element in topLevelElements) {
        when (element.normalName()) {
            "p" -> {
                val heading = element.text().trim()
                if (heading.isBlank()) {
                    // An image-only paragraph illustrates the talk above it.
                    element.selectFirst("img")?.let { image: Element ->
                        current?.attachImage(image)
                    }
                    continue
                }
                val (date, headingWithoutDate) = extractDate(heading)
                val (title, event) = splitTitleAndEvent(headingWithoutDate)
                if (title.isBlank()) {
                    // A date with no title can't be shown; don't let its cards attach elsewhere.
                    current = null
                    continue
                }
                val talk = TalkBuilder(title, event, date)
                talk.links += element.select("a[href]").mapNotNull { headingLink(it) }
                current = talk
                talks += talk
            }
            "figure" -> current?.attachFigure(element)
        }
    }

    return talks.map { talk -> talk.build() }
}

private class TalkBuilder(
    private val title: String,
    private val event: String?,
    private val date: String?,
) {
    val links = mutableListOf<TalkLink>()
    private var imageUrl: String? = null

    fun attachFigure(figure: Element) {
        val classes = figure.classNames()
        when {
            "kg-embed-card" in classes ->
                figure.selectFirst("iframe[src]")?.absAttr("src")?.let(::embedLink)?.let(links::add)
            "kg-bookmark-card" in classes ->
                figure.selectFirst("a[href]")?.let { anchor: Element ->
                    val label = figure.selectFirst(".kg-bookmark-title")?.text()?.trim()
                    headingLink(anchor, label)?.let(links::add)
                }
            "kg-image-card" in classes || "kg-gallery-card" in classes ->
                figure.selectFirst("img")?.let(::attachImage)
        }
    }

    fun attachImage(image: Element) {
        if (imageUrl != null) return
        imageUrl =
            listOf("src", "data-src")
                .mapNotNull { attribute -> image.absAttr(attribute) }
                .firstOrNull { it.isHttpUrl() }
    }

    fun build(): Talk =
        Talk(
            title = title,
            event = event,
            date = date,
            imageUrl = imageUrl,
            links = links.distinctBy(TalkLink::url),
        )
}

private fun extractDate(heading: String): Pair<String?, String> {
    val match = DATE_PREFIX.find(heading) ?: return null to heading
    return match.groupValues[1] to heading.substring(match.range.last + 1).trimStart()
}

private fun splitTitleAndEvent(heading: String): Pair<String, String?> {
    val quoted = QUOTED_TITLE.find(heading)
    if (quoted == null) return heading.trim() to null

    val event = heading.substring(0, quoted.range.first).trim().trimEnd('-', '—', '–').trim()
    return quoted.groupValues[1].trim() to event.takeIf(String::isNotBlank)
}

/** Maps a known player embed to a link people can open; unknown embeds are dropped. */
private fun embedLink(src: String): TalkLink? {
    if (!src.isHttpUrl()) return null
    val uri = runCatching { URI(src) }.getOrNull() ?: return null
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
    val path = uri.rawPath.orEmpty()
    fun idAfter(prefix: String): String? =
        path.removePrefix(prefix).substringBefore('/').takeIf(String::isNotBlank)
    return when {
        host == "speakerdeck.com" && path.startsWith("/player/") ->
            TalkLink("Slides", cleanUrl(src), TalkLinkKind.SLIDES)
        host == "player.vimeo.com" && path.startsWith("/video/") ->
            idAfter("/video/")?.let { id ->
                TalkLink("Watch", "https://vimeo.com/$id", TalkLinkKind.WATCH)
            }
        (host == "youtube.com" || host == "youtube-nocookie.com") && path.startsWith("/embed/") ->
            idAfter("/embed/")?.let { id ->
                TalkLink("Watch", "https://www.youtube.com/watch?v=$id", TalkLinkKind.WATCH)
            }
        else -> null
    }
}

private fun headingLink(anchor: Element, labelOverride: String? = null): TalkLink? {
    val rawUrl = anchor.absAttr("href")?.takeIf { it.isHttpUrl() } ?: return null
    val url = cleanUrl(rawUrl)
    val host = runCatching { URI(url).host?.lowercase()?.removePrefix("www.") }.getOrNull() ?: ""
    val query = runCatching { URI(url).rawQuery.orEmpty() }.getOrDefault("")
    val kind =
        when {
            host == "speakerdeck.com" || host.endsWith(".speakerdeck.com") -> TalkLinkKind.SLIDES
            host == "vimeo.com" ||
                host.endsWith(".vimeo.com") ||
                host == "youtube.com" ||
                host.endsWith(".youtube.com") ||
                host == "youtu.be" ||
                host.endsWith(".youtu.be") ||
                (host.endsWith("droidcon.com") && queryHasVideo(query)) -> TalkLinkKind.WATCH
            host.endsWith("meetup.com") || host.endsWith("droidcon.com") || isEventSite(host) ->
                TalkLinkKind.EVENT
            else -> TalkLinkKind.OTHER
        }
    val label =
        labelOverride?.takeIf(String::isNotBlank)
            ?: anchor.text().trim().ifBlank { host.ifBlank { "Link" } }
    return TalkLink(label = label, url = url, kind = kind)
}

private fun queryHasVideo(query: String): Boolean =
    query.split('&').any { it.substringBefore('=') == "video" }

private fun isEventSite(host: String): Boolean =
    listOf("eventbrite.com", "sessionize.com", "lu.ma", "eventful.com", "conferenceindex.org").any {
        host == it || host.endsWith(".$it")
    }

/**
 * The attribute resolved to an absolute URL, or null when it's missing or blank (jsoup would
 * otherwise resolve an empty value to the page's own URL).
 */
private fun Element.absAttr(key: String): String? =
    attr(key).takeIf(String::isNotBlank)?.let { absUrl(key).trim() }?.takeIf(String::isNotBlank)

private fun String.isHttpUrl(): Boolean {
    val scheme = runCatching { URI(this).scheme?.lowercase() }.getOrNull()
    return scheme == "http" || scheme == "https"
}

private fun cleanUrl(url: String): String {
    val uri = runCatching { URI(url) }.getOrNull() ?: return url
    val query =
        uri.rawQuery
            ?.split('&')
            ?.filterNot { it.substringBefore('=') == "ref" }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("&")
    return buildString {
        uri.scheme?.let { append(it).append(':') }
        uri.rawAuthority?.let { append("//").append(it) }
        append(uri.rawPath.orEmpty())
        query?.let { append('?').append(it) }
        uri.rawFragment?.let { append('#').append(it) }
    }
}

/** Resolves relative hrefs/srcs (e.g. `/content/images/...` or `//host/...`) against the site. */
private const val BASE_URL = "https://jasonpearson.dev/talks/"
private val DATE_PREFIX = Regex("^(\\d{1,2}/\\d{1,2}/\\d{4})(?:\\s+|$)")
private val QUOTED_TITLE = Regex("[\\\"“”]([^\\\"“”]+)[\\\"“”]")
