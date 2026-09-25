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
package dev.jasonpearson.android.data.articles

import dev.jasonpearson.android.client.ghost.GhostDataSource
import dev.jasonpearson.android.client.ghost.api.GhostContentApi
import dev.jasonpearson.android.client.ghost.mapper.toArticle
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.GhostContentKey
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.model.SearchEntry
import dev.jasonpearson.android.core.model.Tag
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.Fetched
import dev.jasonpearson.android.subsystem.storage.fetchFresh
import dev.jasonpearson.android.subsystem.storage.fetchWithFallback
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DefaultArticlesRepository(
    private val ghost: GhostDataSource,
    private val api: GhostContentApi,
    @GhostContentKey private val contentKey: String,
    private val contentCache: ContentCache,
    private val json: Json,
) : ArticlesRepository {
    private val searchIndexMutex = Mutex()
    private var searchIndex: List<SearchEntry>? = null

    override suspend fun articles(): NetworkResult<List<Article>> =
        cached("articles:all", ::encodeArticles, ::decodeArticles) {
            ghost.getAllArticles().map(Article::withReadingTime)
        }

    override suspend fun article(slug: String): NetworkResult<Article> =
        cached(
            "articles:detail:$slug",
            { json.encodeToString(it.toCacheModel()) },
            { json.decodeFromString<CachedArticle>(it).toArticle().withReadingTime() },
        ) {
            ghost.getArticle(slug).withReadingTime()
        }

    override suspend fun featured(): NetworkResult<List<Article>> =
        cached("articles:featured", ::encodeArticles, ::decodeArticles) {
            ghost.getFeaturedArticles().map(Article::withReadingTime)
        }

    override suspend fun articlesPage(
        page: Int,
        tagSlug: String?,
        refresh: Boolean,
    ): NetworkResult<ArticlesPage> {
        require(page >= FIRST_PAGE) { "Pages are 1-based: $page" }
        // New posts should become searchable after a pull-to-refresh, not only after a restart.
        if (refresh) searchIndexMutex.withLock { searchIndex = null }
        return fetched(
                key = "articles:page:${tagSlug ?: "all"}:$PAGE_SIZE:$page",
                encode = { json.encodeToString(it.toCacheModel()) },
                decode = { value ->
                    val cachedPage =
                        json.decodeFromString<CachedArticlesPage>(value).toArticlesPage()
                    cachedPage.copy(articles = cachedPage.articles.map(Article::withReadingTime))
                },
                refresh = refresh,
            ) {
                val response =
                    api.getPostsPage(
                        key = contentKey,
                        filter = tagSlug?.let { "tag:$it" },
                        page = page,
                        limit = PAGE_SIZE,
                    )
                val pagination = response.meta?.pagination
                ArticlesPage(
                    articles = response.posts.map { it.toArticle().withReadingTime() },
                    page = page,
                    nextPage = pagination?.next,
                    total = pagination?.total,
                )
            }
            .fold(
                onSuccess = { NetworkResult.Success(it.value.copy(fromCache = it.fromCache)) },
                onFailure = { NetworkResult.Failure(it) },
            )
    }

    override suspend fun tags(): NetworkResult<List<Tag>> =
        when (
            val result =
                cached(
                    "articles:tags",
                    { json.encodeToString(it.map(Tag::toCacheModel)) },
                    { json.decodeFromString<List<CachedTag>>(it).map(CachedTag::toTag) },
                ) {
                    ghost.getTags()
                }
        ) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    result.data
                        .filter { (it.postCount ?: 0) > 0 }
                        .sortedByDescending { it.postCount ?: 0 }
                )
            is NetworkResult.Failure -> result
        }

    override suspend fun articlesByTag(slug: String): NetworkResult<List<Article>> =
        cached("articles:tag:$slug", ::encodeArticles, ::decodeArticles) {
            ghost.getArticlesByTag(slug).map(Article::withReadingTime)
        }

    override suspend fun related(article: Article): NetworkResult<List<Article>> =
        cached("articles:related:${article.id}", ::encodeArticles, ::decodeArticles) {
            ghost.getRelatedArticles(article).map(Article::withReadingTime)
        }

    override suspend fun search(query: String): NetworkResult<List<SearchEntry>> {
        if (query.isBlank()) return NetworkResult.Success(emptyList())

        val index = searchIndexMutex.withLock {
            searchIndex?.let {
                return@withLock NetworkResult.Success(it)
            }
            val result =
                cached(
                    "articles:search-index",
                    { json.encodeToString(it.map(SearchEntry::toCacheModel)) },
                    {
                        json
                            .decodeFromString<List<CachedSearchEntry>>(it)
                            .map(CachedSearchEntry::toSearchEntry)
                    },
                ) {
                    ghost.getSearchIndex()
                }
            if (result is NetworkResult.Success) searchIndex = result.data
            result
        }
        return when (index) {
            is NetworkResult.Success -> NetworkResult.Success(filterSearchIndex(index.data, query))
            is NetworkResult.Failure -> index
        }
    }

    override suspend fun adjacent(slug: String): NetworkResult<Pair<Article?, Article?>> =
        when (val result = articles()) {
            is NetworkResult.Success ->
                if (result.data.none { it.slug == slug }) {
                    NetworkResult.Failure(NoSuchElementException("Article slug not found: $slug"))
                } else {
                    NetworkResult.Success(adjacentArticles(result.data, slug))
                }
            is NetworkResult.Failure -> result
        }

    private fun encodeArticles(articles: List<Article>): String =
        json.encodeToString(articles.map(Article::toCacheModel))

    private fun decodeArticles(value: String): List<Article> =
        json.decodeFromString<List<CachedArticle>>(value).map { it.toArticle().withReadingTime() }

    private suspend fun <T : Any> cached(
        key: String,
        encode: (T) -> String,
        decode: (String) -> T,
        fetch: suspend () -> T,
    ): NetworkResult<T> =
        fetched(key, encode, decode, refresh = false, fetch)
            .fold(
                onSuccess = { NetworkResult.Success(it.value) },
                onFailure = { NetworkResult.Failure(it) },
            )

    /**
     * Network-first with offline fallback; a [refresh] is network-only, so a failed pull-to-refresh
     * surfaces its error rather than quietly re-serving the cached copy. Either way a fresh value
     * replaces the cached one.
     */
    private suspend fun <T : Any> fetched(
        key: String,
        encode: (T) -> String,
        decode: (String) -> T,
        refresh: Boolean,
        fetch: suspend () -> T,
    ): Result<Fetched<T>> {
        return if (refresh) {
            contentCache.fetchFresh(key, encode, decode, fetch)
        } else {
            contentCache.fetchWithFallback(key, encode, decode, fetch)
        }
    }

    companion object {
        /** Posts per page; small enough that the first page renders quickly on a slow network. */
        const val PAGE_SIZE = 15
    }
}
