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

import dev.jasonpearson.android.client.ghost.GhostDataSource
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.core.network.networkResult
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.fetchFresh
import dev.jasonpearson.android.subsystem.storage.fetchWithFallback
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DefaultPhotographyRepository(
    private val ghost: GhostDataSource,
    private val contentCache: ContentCache,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : PhotographyRepository {

    private val serializer = ListSerializer(GalleryPhoto.serializer())

    override suspend fun photos(forceRefresh: Boolean): NetworkResult<List<GalleryPhoto>> =
        networkResult {
            if (forceRefresh) {
                contentCache
                    .fetchFresh(
                        key = CACHE_KEY,
                        encode = { json.encodeToString(serializer, it) },
                        fetch = ::fetchPhotos,
                    )
                    .getOrThrow()
                    .value
            } else {
                contentCache
                    .fetchWithFallback(
                        key = CACHE_KEY,
                        encode = { json.encodeToString(serializer, it) },
                        decode = { json.decodeFromString(serializer, it) },
                        fetch = ::fetchPhotos,
                    )
                    .getOrThrow()
                    .value
            }
        }

    private suspend fun fetchPhotos(): List<GalleryPhoto> =
        parseGallery(ghost.getPage("photography").html)

    private companion object {
        const val CACHE_KEY = "photography:gallery"
    }
}
