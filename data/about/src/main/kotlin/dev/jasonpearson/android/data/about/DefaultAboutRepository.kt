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
package dev.jasonpearson.android.data.about

import dev.jasonpearson.android.client.ghost.GhostDataSource
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.model.ContentPage
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.fetchWithFallback
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DefaultAboutRepository(
    private val ghost: GhostDataSource,
    private val contentCache: ContentCache,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AboutRepository {

    override suspend fun aboutPage(): NetworkResult<ContentPage> {
        val result =
            contentCache.fetchWithFallback<ContentPageCache>(
                key = "about:page",
                encode = { json.encodeToString(it) },
                decode = { json.decodeFromString<ContentPageCache>(it) },
                fetch = { ghost.getPage("about-me").toCache() },
            )
        return result.fold(
            onSuccess = { NetworkResult.Success(it.value.toDomain()) },
            onFailure = { NetworkResult.Failure(it) },
        )
    }

    override suspend fun profile(): NetworkResult<AboutProfile> {
        val result =
            contentCache.fetchWithFallback<SiteSettingsCache>(
                key = "about:settings",
                encode = { json.encodeToString(it) },
                decode = { json.decodeFromString<SiteSettingsCache>(it) },
                fetch = { ghost.getSettings().toCache() },
            )
        val profile =
            result.fold(
                onSuccess = { cached ->
                    val settings = cached.value.toDomain()
                    AboutProfile(
                        title = settings.title,
                        description = settings.description,
                        iconUrl = settings.iconUrl,
                        socials = mergeSocials(settings),
                    )
                },
                onFailure = {
                    AboutProfile(
                        title = "Jason Pearson",
                        description = null,
                        iconUrl = null,
                        socials = mergeSocials(null),
                    )
                },
            )
        return NetworkResult.Success(profile)
    }

    override fun experience(): List<ExperienceEntry> = experience

    override fun education(): List<EducationEntry> = education

    override fun talks(): List<TalkEntry> = talks
}
