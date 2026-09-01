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
package dev.jasonpearson.android.feature.discover

/** Pure, framework-free UI state for the discover screen. */
data class DiscoverUiState(val query: String = "", val results: List<String> = emptyList()) {
    val isEmpty: Boolean
        get() = results.isEmpty()
}

/** The catalog the discover screen searches when no other source is supplied. */
val DefaultCatalog: List<String> =
    listOf("Ambient", "Blues", "Classical", "Dance", "Electronic", "Folk", "Jazz", "Rock")

/**
 * Filters [catalog] by a case-insensitive substring match against [query]. A blank query returns
 * the full catalog.
 */
fun discoverStateFor(query: String, catalog: List<String> = DefaultCatalog): DiscoverUiState {
    val trimmed = query.trim()
    val results =
        if (trimmed.isEmpty()) catalog
        else catalog.filter { it.contains(trimmed, ignoreCase = true) }
    return DiscoverUiState(query = query, results = results)
}
