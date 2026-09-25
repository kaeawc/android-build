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

import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Snapshot of an [ArticlesPaginator]. */
data class PagedArticlesState(
    val articles: List<Article> = emptyList(),
    val nextPage: Int? = FIRST_PAGE,
    /** True once the first page has arrived; until then [error] is a full-screen failure. */
    val loaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Failure of the initial load or of the latest refresh. */
    val error: Throwable? = null,
    /**
     * Failure of the latest next-page load; blocks further loads until [ArticlesPaginator.retry].
     */
    val loadMoreError: Throwable? = null,
) {
    val endReached: Boolean
        get() = loaded && nextPage == null

    val canLoadMore: Boolean
        get() =
            loaded && nextPage != null && !isLoadingMore && !isRefreshing && loadMoreError == null
}

/**
 * Accumulates [ArticlesPage]s from [loadPage] into one de-duplicated, newest-first list. Offset
 * pagination can repeat an article when a post is published between page loads, so appended pages
 * are merged by id. All loads run in [scope]; a refresh cancels any in-flight load.
 */
class ArticlesPaginator(
    private val scope: CoroutineScope,
    private val loadPage: suspend (page: Int, refresh: Boolean) -> NetworkResult<ArticlesPage>,
) {
    private val mutableState = MutableStateFlow(PagedArticlesState())
    val state: StateFlow<PagedArticlesState> = mutableState.asStateFlow()

    private var job: Job? = null

    /** Loads the first page unless it's already loaded or loading. */
    fun start() {
        if (mutableState.value.loaded || job?.isActive == true) return
        loadFirstPage(refresh = false)
    }

    /** Reloads from the first page straight from the network, keeping current content meanwhile. */
    fun refresh() = loadFirstPage(refresh = true)

    /** Loads the next page if there is one and nothing else is loading or blocked by an error. */
    fun loadMore() {
        val current = mutableState.value
        val page = current.nextPage
        if (page == null || !current.canLoadMore || job?.isActive == true) return
        mutableState.update { it.copy(isLoadingMore = true) }
        job = scope.launch {
            when (val result = loadPage(page, false)) {
                is NetworkResult.Success ->
                    mutableState.update { state ->
                        val merged = (state.articles + result.data.articles).distinctBy(Article::id)
                        state.copy(
                            articles = merged,
                            // Guard against a page that doesn't advance, which would loop forever.
                            nextPage =
                                result.data.nextPage?.takeIf {
                                    it > page && result.data.articles.isNotEmpty()
                                },
                            isLoadingMore = false,
                        )
                    }
                is NetworkResult.Failure ->
                    mutableState.update {
                        it.copy(isLoadingMore = false, loadMoreError = result.error)
                    }
            }
        }
    }

    /** Re-runs whatever last failed: the first page, or the next page. */
    fun retry() {
        val current = mutableState.value
        when {
            !current.loaded -> loadFirstPage(refresh = false)
            current.loadMoreError != null -> {
                mutableState.update { it.copy(loadMoreError = null) }
                loadMore()
            }
            current.error != null -> refresh()
        }
    }

    private fun loadFirstPage(refresh: Boolean) {
        job?.cancel()
        mutableState.update {
            it.copy(
                isRefreshing = refresh,
                isLoadingMore = false,
                error = null,
                loadMoreError = null,
            )
        }
        job = scope.launch {
            when (val result = loadPage(FIRST_PAGE, refresh)) {
                is NetworkResult.Success ->
                    mutableState.value =
                        PagedArticlesState(
                            articles = result.data.articles.distinctBy(Article::id),
                            nextPage = result.data.nextPage?.takeIf { it > FIRST_PAGE },
                            loaded = true,
                        )
                is NetworkResult.Failure ->
                    mutableState.update { it.copy(isRefreshing = false, error = result.error) }
            }
        }
    }
}

internal const val FIRST_PAGE = 1
