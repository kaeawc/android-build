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

import dev.jasonpearson.android.client.github.GitHubDataSource
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.Fetched
import dev.jasonpearson.android.subsystem.storage.fetchWithFallback
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DefaultProjectsRepository(
    private val github: GitHubDataSource,
    private val contentCache: ContentCache,
    private val json: Json,
) : ProjectsRepository {

    private val projectListSerializer = ListSerializer(CachedProject.serializer())

    override suspend fun projects(): NetworkResult<List<Project>> =
        contentCache
            .fetchWithFallback(
                key = "projects:list",
                encode = { projects: List<Project> ->
                    json.encodeToString(
                        projectListSerializer,
                        projects.map(Project::toCachedProject),
                    )
                },
                decode = { cached ->
                    json
                        .decodeFromString(projectListSerializer, cached)
                        .map(CachedProject::toProject)
                },
                fetch = { github.getProjects() },
            )
            .toNetworkResult()

    override suspend fun overview(): NetworkResult<ProjectsOverview> =
        when (val all = projects()) {
            is NetworkResult.Failure -> all
            is NetworkResult.Success -> {
                val namesInList = all.data.map(Project::name).toSet()
                val extraPinned =
                    PINNED.filterNot { it in namesInList }
                        .mapNotNull { name ->
                            when (val result = project(name)) {
                                is NetworkResult.Success -> result.data
                                is NetworkResult.Failure -> null
                            }
                        }
                NetworkResult.Success(buildOverview(all.data, extraPinned, PINNED))
            }
        }

    override suspend fun project(name: String): NetworkResult<Project> =
        contentCache
            .fetchWithFallback(
                key = "projects:repo:$name",
                encode = { project: Project ->
                    json.encodeToString(CachedProject.serializer(), project.toCachedProject())
                },
                decode = { cached ->
                    json.decodeFromString(CachedProject.serializer(), cached).toProject()
                },
                fetch = { github.getProject(name) },
            )
            .toNetworkResult()

    override suspend fun readme(name: String): NetworkResult<String> =
        contentCache
            .fetchWithFallback(
                key = "projects:readme:$name",
                // The HTML is a plain JSON string so quotes and line breaks round-trip safely.
                encode = { html: String -> json.encodeToString(String.serializer(), html) },
                decode = { cached -> json.decodeFromString(String.serializer(), cached) },
                fetch = { github.getReadmeHtml(name) },
            )
            .toNetworkResult()
}

private fun <T> Result<Fetched<T>>.toNetworkResult(): NetworkResult<T> =
    fold(onSuccess = { NetworkResult.Success(it.value) }, onFailure = { NetworkResult.Failure(it) })
