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

/** Talks given in one [year]; a null year collects talks whose year couldn't be determined. */
data class TalkYearGroup(val year: Int?, val talks: List<Talk>)

/**
 * The year a talk was given: from its date when present, otherwise the first four-digit year in its
 * event or title (e.g. "Droidcon London 2023").
 */
val Talk.year: Int?
    get() =
        listOfNotNull(date, event, title).firstNotNullOfOrNull { text ->
            YEAR.find(text)?.value?.toIntOrNull()
        }

/**
 * Groups talks by year, newest year first, keeping page order within a year.
 *
 * The talks page is listed chronologically, so a talk without its own [year] is grouped with the
 * talk above it (or, at the top of the page, the first talk below it that has one). Only when no
 * talk has a year does everything land in a single null-year group.
 */
fun groupTalksByYear(talks: List<Talk>): List<TalkYearGroup> {
    val ownYears = talks.map(Talk::year)
    val firstKnownYear = ownYears.firstOrNull { it != null }
    var previousYear: Int? = null
    val inferredYears = ownYears.map { year ->
        (year ?: previousYear ?: firstKnownYear).also { previousYear = it }
    }

    return talks
        .zip(inferredYears)
        .groupBy(keySelector = { it.second }, valueTransform = { it.first })
        .entries
        .sortedWith(compareBy(nullsLast(reverseOrder())) { it.key })
        .map { (year, talksInYear) -> TalkYearGroup(year, talksInYear) }
}

private val YEAR = Regex("(?<!\\d)(19|20)\\d{2}(?!\\d)")
