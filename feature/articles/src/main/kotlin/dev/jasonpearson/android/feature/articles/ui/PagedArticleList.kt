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
package dev.jasonpearson.android.feature.articles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.data.articles.ArticlesPaginator
import dev.jasonpearson.android.data.articles.PagedArticlesState
import dev.jasonpearson.android.foundation.designsystem.components.EmptyContent
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent

/** Start fetching the next page once the last visible item is this close to the end. */
private const val LOAD_MORE_THRESHOLD = 4

/**
 * Pull-to-refresh, paged article list with shared loading/error/empty states. [header] items sit
 * above the articles and scroll with them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PagedArticles(
    paginator: ArticlesPaginator,
    onArticleClick: (slug: String) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = paginator::retry,
    onLoadMore: () -> Unit = paginator::loadMore,
    header: LazyListScope.() -> Unit = {},
) {
    val state by paginator.state.collectAsState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onRefresh() },
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.loaded ->
                ArticleList(
                    state = state,
                    onArticleClick = onArticleClick,
                    onLoadMore = onLoadMore,
                    onRetry = onRetry,
                    emptyMessage = emptyMessage,
                    header = header,
                )
            state.error != null ->
                ErrorContent(
                    message = state.error?.message ?: "Failed to load",
                    onRetry = { onRetry() },
                )
            else -> LoadingContent()
        }
    }
}

@Composable
private fun ArticleList(
    state: PagedArticlesState,
    onArticleClick: (slug: String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    emptyMessage: String,
    header: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by
        remember(listState) {
            derivedStateOf {
                val layout = listState.layoutInfo
                val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index
                lastVisible != null &&
                    lastVisible >= layout.totalItemsCount - 1 - LOAD_MORE_THRESHOLD
            }
        }
    // Re-keyed on the article count so a short page that still fits on screen keeps loading.
    LaunchedEffect(nearEnd, state.canLoadMore, state.articles.size) {
        if (nearEnd && state.canLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.error != null) {
            // A failed pull-to-refresh keeps the list; say so rather than failing silently.
            item(key = "refresh-error") {
                InlineRetry(message = "Couldn't refresh articles", onRetry = onRetry)
            }
        }
        header()
        if (state.articles.isEmpty()) {
            item(key = "empty") { EmptyContent(emptyMessage, Modifier.fillParentMaxSize()) }
        }
        items(state.articles, key = Article::id) { article ->
            ArticleCard(article = article, onClick = { onArticleClick(article.slug) })
        }
        when {
            state.isLoadingMore ->
                item(key = "loading-more") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            state.loadMoreError != null ->
                item(key = "load-more-error") {
                    InlineRetry(message = "Couldn't load more articles", onRetry = onRetry)
                }
        }
    }
}

@Composable
private fun InlineRetry(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
