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
package dev.jasonpearson.android.data.bookmarks

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.subsystem.storage.KeyValueStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
public class DefaultBookmarksRepository
@Inject
constructor(private val store: KeyValueStore, private val clock: Clock) : BookmarksRepository {
    override val bookmarks: Flow<List<Bookmark>> =
        store.observe(BOOKMARKS_KEY).map { raw ->
            decode(raw).sortedByDescending(Bookmark::savedAtEpochMillis)
        }

    private val mutex = Mutex()

    override fun isBookmarked(slug: String): Flow<Boolean> = bookmarks.map { saved ->
        saved.any { it.slug == slug }
    }

    override suspend fun toggle(article: Article) {
        mutex.withLock {
            val current = decode(store.get(BOOKMARKS_KEY))
            val updated =
                if (current.any { it.slug == article.slug }) {
                    current.filterNot { it.slug == article.slug }
                } else {
                    current +
                        Bookmark(
                            slug = article.slug,
                            title = article.title,
                            excerpt = article.excerpt,
                            featureImageUrl = article.featureImageUrl,
                            url = article.url,
                            savedAtEpochMillis = clock.now().toEpochMilliseconds(),
                        )
                }
            store.put(
                BOOKMARKS_KEY,
                json.encodeToString(ListSerializer(Bookmark.serializer()), updated),
            )
        }
    }

    override suspend fun remove(slug: String) {
        mutex.withLock {
            val updated = decode(store.get(BOOKMARKS_KEY)).filterNot { it.slug == slug }
            store.put(
                BOOKMARKS_KEY,
                json.encodeToString(ListSerializer(Bookmark.serializer()), updated),
            )
        }
    }

    private fun decode(raw: String?): List<Bookmark> =
        if (raw == null) emptyList()
        else
            runCatching { json.decodeFromString(ListSerializer(Bookmark.serializer()), raw) }
                .getOrDefault(emptyList())

    private companion object {
        const val BOOKMARKS_KEY = "bookmarks.v1"
        val json = Json { ignoreUnknownKeys = true }
    }
}
