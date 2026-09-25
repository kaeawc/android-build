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

internal val PINNED =
    listOf("auto-mobile", "krit", "spectra", "golang-build", "android-build", "auto-worktree")

data class ProjectsOverview(
    val pinned: List<Project>,
    val others: List<Project>,
    val languages: List<String>,
)

internal fun buildOverview(
    all: List<Project>,
    extraPinned: List<Project>,
    pinned: List<String>,
): ProjectsOverview {
    val allByName = all.associateBy(Project::name)
    val extraByName = extraPinned.associateBy(Project::name)
    val pinnedProjects = pinned.mapNotNull { name -> allByName[name] ?: extraByName[name] }
    val pinnedNames = pinned.toSet()
    val others = all.filterNot { it.name in pinnedNames }.sortedByDescending(Project::stars)
    val languages =
        (pinnedProjects + others)
            .mapNotNull(Project::language)
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
    return ProjectsOverview(pinnedProjects, others, languages)
}
