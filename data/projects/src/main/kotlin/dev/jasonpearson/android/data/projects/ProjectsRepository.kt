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
import dev.jasonpearson.android.core.network.NetworkResult

/**
 * GitHub-backed projects. Reads revalidate the offline cache with ETags; [forceRefresh] (e.g.
 * pull-to-refresh) skips revalidation and always downloads a fresh copy.
 */
interface ProjectsRepository {
    suspend fun projects(forceRefresh: Boolean = false): NetworkResult<List<Project>>

    suspend fun overview(forceRefresh: Boolean = false): NetworkResult<ProjectsOverview>

    suspend fun project(name: String, forceRefresh: Boolean = false): NetworkResult<Project>

    /** README HTML with relative image and link URLs resolved against the repo. */
    suspend fun readme(name: String, forceRefresh: Boolean = false): NetworkResult<String>
}
