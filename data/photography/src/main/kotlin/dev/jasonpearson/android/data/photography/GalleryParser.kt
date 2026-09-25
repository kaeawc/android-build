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
package dev.jasonpearson.android.data.photography

import java.net.URI
import org.jsoup.Jsoup

internal fun parseGallery(html: String): List<GalleryPhoto> {
    val seenSources = mutableSetOf<String>()

    return Jsoup.parse(html)
        .select("figure.kg-gallery-card div.kg-gallery-image > img, figure.kg-image-card img")
        .mapNotNull { image ->
            val source = image.attr("src").trim()
            if (!source.isAbsoluteUrl() || !seenSources.add(source)) return@mapNotNull null

            val candidates = image.attr("srcset").parseCandidates()
            val thumbnail =
                candidates.minByOrNull { kotlin.math.abs(it.width - 600) }?.url ?: source
            val full =
                candidates.filter { it.width <= 2400 }.maxByOrNull { it.width }?.url ?: source

            GalleryPhoto(
                thumbUrl = thumbnail,
                fullUrl = full,
                width = image.attr("width").toIntOrNull(),
                height = image.attr("height").toIntOrNull(),
                takenOn = source.takenOn(),
            )
        }
}

private data class SrcSetCandidate(val url: String, val width: Int)

private val srcSetEntry = Regex("^(.+?)\\s+(\\d+)w$")

private fun String.parseCandidates(): List<SrcSetCandidate> =
    split(',').mapNotNull { entry ->
        val match = srcSetEntry.matchEntire(entry.trim()) ?: return@mapNotNull null
        val url = match.groupValues[1].trim()
        val width = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
        if (width <= 0 || !url.isAbsoluteUrl()) return@mapNotNull null
        SrcSetCandidate(url, width)
    }

private fun String.isAbsoluteUrl(): Boolean =
    runCatching { URI(this).isAbsolute }.getOrDefault(false)

private fun String.takenOn(): String? {
    val filename =
        substringAfterLast('/').substringBefore('?').substringBefore('#').substringBeforeLast('.')
    val date =
        listOf(
                Regex("^PXL_(\\d{4})(\\d{2})(\\d{2})_"),
                Regex("^IMG_(\\d{4})(\\d{2})(\\d{2})_"),
                Regex("^XTR_(\\d{4})(\\d{2})(\\d{2})"),
                Regex("^(\\d{4})(\\d{2})(\\d{2})_"),
            )
            .firstNotNullOfOrNull { pattern -> pattern.find(filename) } ?: return null

    val (year, month, day) = date.groupValues.drop(1).map(String::toInt)
    if (month !in 1..12 || day !in 1..31) return null
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
