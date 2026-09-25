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
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal const val GITHUB_OWNER = "kaeawc"

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DefaultProjectsRepository(
    private val github: GitHubDataSource,
    private val contentCache: ContentCache,
    private val json: Json,
) : ProjectsRepository {

    private val projectListSerializer = ListSerializer(CachedProject.serializer())

    override suspend fun projects(forceRefresh: Boolean): NetworkResult<List<Project>> =
        contentCache
            .fetchConditional(
                key = "projects:list",
                json = json,
                forceRefresh = forceRefresh,
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
                fetch = { etag -> github.getProjects(etag = etag, user = GITHUB_OWNER) },
            )
            .toNetworkResult()

    override suspend fun overview(forceRefresh: Boolean): NetworkResult<ProjectsOverview> =
        when (val all = projects(forceRefresh)) {
            is NetworkResult.Failure -> all
            is NetworkResult.Success -> {
                val namesInList = all.data.map(Project::name).toSet()
                val extraPinned =
                    PINNED.filterNot { it in namesInList }
                        .mapNotNull { name ->
                            when (val result = project(name, forceRefresh)) {
                                is NetworkResult.Success -> result.data
                                is NetworkResult.Failure -> null
                            }
                        }
                NetworkResult.Success(buildOverview(all.data, extraPinned, PINNED))
            }
        }

    override suspend fun project(name: String, forceRefresh: Boolean): NetworkResult<Project> =
        contentCache
            .fetchConditional(
                key = "projects:repo:$name",
                json = json,
                forceRefresh = forceRefresh,
                encode = { project: Project ->
                    json.encodeToString(CachedProject.serializer(), project.toCachedProject())
                },
                decode = { cached ->
                    json.decodeFromString(CachedProject.serializer(), cached).toProject()
                },
                fetch = { etag -> github.getProject(name, etag = etag, owner = GITHUB_OWNER) },
            )
            .toNetworkResult()

    override suspend fun readme(name: String, forceRefresh: Boolean): NetworkResult<String> =
        contentCache
            .fetchConditional(
                key = "projects:readme:$name",
                json = json,
                forceRefresh = forceRefresh,
                // The HTML is a plain JSON string so quotes and line breaks round-trip safely.
                encode = { html: String -> json.encodeToString(String.serializer(), html) },
                decode = { cached -> json.decodeFromString(String.serializer(), cached) },
                fetch = { etag -> github.getReadmeHtml(name, etag = etag, owner = GITHUB_OWNER) },
            )
            // Resolved on read (not before caching) so copies cached by older builds are fixed too.
            .map { fetched ->
                fetched.copy(value = resolveReadmeUrls(fetched.value, GITHUB_OWNER, name))
            }
            .toNetworkResult()
}

private fun <T> Result<Fetched<T>>.toNetworkResult(): NetworkResult<T> =
    fold(onSuccess = { NetworkResult.Success(it.value) }, onFailure = { NetworkResult.Failure(it) })
