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

internal const val WORDS_PER_MINUTE = 265

private val nonTextElements =
    Regex("""<(script|style)\b[^>]*>[\s\S]*?</\1\s*>""", RegexOption.IGNORE_CASE)
private val tags = Regex("""<[^>]*>""")
private val entities = Regex("""&(#\d+|#x[0-9a-fA-F]+|[a-zA-Z]+);""")
private val whitespace = Regex("""\s+""")

/**
 * Estimated minutes to read [html] at [WORDS_PER_MINUTE], rounded up; null when it has no words.
 * Markup, entities and script/style bodies don't count as words.
 */
internal fun estimateReadingTimeMinutes(html: String?): Int? {
    if (html.isNullOrBlank()) return null
    val text = html.replace(nonTextElements, " ").replace(tags, " ").replace(entities, " ")
    val words = text.split(whitespace).count { word -> word.any(Char::isLetterOrDigit) }
    if (words == 0) return null
    return (words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE
}

/**
 * Keeps Ghost's `reading_time` when it's a real value; otherwise (absent, or 0 for very short
 * posts) falls back to an estimate from the HTML body.
 */
internal fun Article.withReadingTime(): Article {
    val ghostMinutes = readingTimeMinutes
    if (ghostMinutes != null && ghostMinutes > 0) return this
    val estimate = estimateReadingTimeMinutes(html) ?: return this
    return copy(readingTimeMinutes = estimate)
}
