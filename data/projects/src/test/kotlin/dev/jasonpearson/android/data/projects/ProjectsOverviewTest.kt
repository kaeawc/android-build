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

import dev.jasonpearson.android.core.model.Project
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectsOverviewTest {

    @Test
    fun `pinned order follows allowlist even when input order differs`() {
        val all = listOf(project("third"), project("first"), project("second"))

        val overview = buildOverview(all, emptyList(), listOf("first", "second", "third"))

        assertEquals(listOf("first", "second", "third"), overview.pinned.map(Project::name))
    }

    @Test
    fun `extra pinned fills a missing allowlist position`() {
        val overview =
            buildOverview(
                all = listOf(project("first"), project("third")),
                extraPinned = listOf(project("second")),
                pinned = listOf("first", "second", "third"),
            )

        assertEquals(listOf("first", "second", "third"), overview.pinned.map(Project::name))
        assertEquals(emptyList<Project>(), overview.others)
    }

    @Test
    fun `missing pinned project is skipped`() {
        val overview =
            buildOverview(
                all = listOf(project("first")),
                extraPinned = emptyList(),
                pinned = listOf("first", "missing"),
            )

        assertEquals(listOf("first"), overview.pinned.map(Project::name))
    }

    @Test
    fun `others excludes pinned names and sorts by stars`() {
        val overview =
            buildOverview(
                all =
                    listOf(
                        project("low", stars = 1),
                        project("pinned", stars = 4),
                        project("high", stars = 9),
                        project("middle", stars = 5),
                    ),
                extraPinned = emptyList(),
                pinned = listOf("pinned"),
            )

        assertEquals(listOf("high", "middle", "low"), overview.others.map(Project::name))
    }

    @Test
    fun `languages sort by frequency then alphabetically on a tie`() {
        val overview =
            buildOverview(
                all =
                    listOf(
                        project("pinned", language = "Kotlin"),
                        project("rust", language = "Rust"),
                        project("swift", language = "Swift"),
                        project("kotlin2", language = "Kotlin"),
                        project("swift2", language = "Swift"),
                        project("rust2", language = "Rust"),
                        project("none"),
                    ),
                extraPinned = emptyList(),
                pinned = listOf("pinned"),
            )

        assertEquals(listOf("Kotlin", "Rust", "Swift"), overview.languages)
    }

    private fun project(name: String, stars: Int = 0, language: String? = null): Project =
        Project(
            id = name.hashCode().toLong(),
            name = name,
            description = null,
            url = "https://github.com/kaeawc/$name",
            stars = stars,
            language = language,
            topics = emptyList(),
            updatedAt = null,
        )
}
