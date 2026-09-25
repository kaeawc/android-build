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

internal fun parseTalks(html: String): List<Talk> {
    val document = Jsoup.parse(html)
    val body = document.body()
    val topLevelElements =
        if (body.children().isNotEmpty()) body.children() else document.children()
    val talks = mutableListOf<TalkBuilder>()
    var current: TalkBuilder? = null

    for (element in topLevelElements) {
        when (element.normalName()) {
            "p" -> {
                val heading = element.text().trim()
                if (heading.isNotBlank()) {
                    val (date, headingWithoutDate) = extractDate(heading)
                    val (title, event) = splitTitleAndEvent(headingWithoutDate)
                    val talk = TalkBuilder(title, event, date)
                    talk.links += element.select("a[href]").mapNotNull(::headingLink)
                    current = talk
                    talks += talk
                }
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
        if ("kg-embed-card" in classes) {
            figure.selectFirst("iframe[src]")?.attr("src")?.takeIf(String::isNotBlank)?.let { src ->
                val absoluteSrc = if (src.startsWith("//")) "https:$src" else src
                when {
                    absoluteSrc.startsWith("https://speakerdeck.com/player/") ->
                        links += TalkLink("Slides", cleanUrl(absoluteSrc), TalkLinkKind.SLIDES)
                    absoluteSrc.startsWith("https://player.vimeo.com/video/") -> {
                        val id =
                            absoluteSrc
                                .substringAfter("/video/")
                                .substringBefore('?')
                                .substringBefore('/')
                        if (id.isNotBlank()) {
                            links += TalkLink("Watch", "https://vimeo.com/$id", TalkLinkKind.WATCH)
                        }
                    }
                }
            }
        }
        if ("kg-image-card" in classes && imageUrl == null) {
            imageUrl = figure.selectFirst("img[src]")?.attr("src")?.takeIf(String::isNotBlank)
        }
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

private fun headingLink(anchor: Element): TalkLink? {
    val rawUrl = anchor.attr("href").trim().takeIf(String::isNotBlank) ?: return null
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
    val label = anchor.text().trim().ifBlank { host }
    return TalkLink(label = label, url = url, kind = kind)
}

private fun queryHasVideo(query: String): Boolean =
    query.split('&').any { it.substringBefore('=') == "video" }

private fun isEventSite(host: String): Boolean =
    listOf("eventbrite.com", "sessionize.com", "lu.ma", "eventful.com", "conferenceindex.org").any {
        host == it || host.endsWith(".$it")
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

private val DATE_PREFIX = Regex("^(\\d{1,2}/\\d{1,2}/\\d{4})(?:\\s+|$)")
private val QUOTED_TITLE = Regex("[\\\"“”]([^\\\"“”]+)[\\\"“”]")
