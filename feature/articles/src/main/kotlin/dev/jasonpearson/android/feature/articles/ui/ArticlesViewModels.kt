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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.model.SearchEntry
import dev.jasonpearson.android.core.model.Tag
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.articles.ArticlesPaginator
import dev.jasonpearson.android.data.articles.ArticlesRepository
import dev.jasonpearson.android.data.articles.PagedArticlesState
import dev.jasonpearson.android.data.bookmarks.BookmarksRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ArticlesListUiState(
    val page: PagedArticlesState = PagedArticlesState(),
    val featured: List<Article> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

internal class ArticlesListViewModel(private val repository: ArticlesRepository) : ViewModel() {
    val paginator =
        ArticlesPaginator(viewModelScope) { page, refresh ->
            repository.articlesPage(page, refresh = refresh)
        }
    private val mutableState = MutableStateFlow(ArticlesListUiState())
    val state: StateFlow<ArticlesListUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            paginator.state.collect { page -> mutableState.update { it.copy(page = page) } }
        }
        paginator.start()
        loadHeaders()
    }

    fun refresh() {
        paginator.refresh()
        loadHeaders()
    }

    fun retry() {
        paginator.retry()
        loadHeaders()
    }

    fun loadMore() = paginator.loadMore()

    private fun loadHeaders() {
        viewModelScope.launch {
            (repository.featured() as? NetworkResult.Success)?.let { result ->
                mutableState.update { it.copy(featured = result.data) }
            }
        }
        viewModelScope.launch {
            (repository.tags() as? NetworkResult.Success)?.let { result ->
                mutableState.update { it.copy(tags = result.data) }
            }
        }
    }
}

internal data class TagArticlesUiState(val page: PagedArticlesState = PagedArticlesState())

internal class TagArticlesViewModel(repository: ArticlesRepository, val tagSlug: String) :
    ViewModel() {
    val paginator =
        ArticlesPaginator(viewModelScope) { page, refresh ->
            repository.articlesPage(page, tagSlug = tagSlug, refresh = refresh)
        }
    private val mutableState = MutableStateFlow(TagArticlesUiState())
    val state: StateFlow<TagArticlesUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            paginator.state.collect { page -> mutableState.value = TagArticlesUiState(page) }
        }
        paginator.start()
    }

    fun refresh() = paginator.refresh()

    fun retry() = paginator.retry()

    fun loadMore() = paginator.loadMore()
}

internal sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Results(val entries: List<SearchEntry>) : SearchUiState

    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
internal class ArticleSearchViewModel(private val repository: ArticlesRepository) : ViewModel() {
    private val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery.asStateFlow()
    private val attempt = MutableStateFlow(0)
    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(mutableQuery, attempt) { query, retry -> query to retry }
                .distinctUntilChanged()
                .debounce(250)
                .collectLatest { (currentQuery, _) ->
                    if (currentQuery.isBlank()) {
                        mutableState.value = SearchUiState.Idle
                    } else {
                        mutableState.value = SearchUiState.Loading
                        mutableState.value =
                            when (val result = repository.search(currentQuery)) {
                                is NetworkResult.Success -> SearchUiState.Results(result.data)
                                is NetworkResult.Failure ->
                                    SearchUiState.Error(result.error.message ?: "Search failed")
                            }
                    }
                }
        }
    }

    fun setQuery(value: String) {
        mutableQuery.value = value
    }

    fun retry() {
        attempt.value++
    }
}

internal data class ArticleDetailState(
    val article: ArticleDetailUiState = ArticleDetailUiState.Loading,
    val adjacent: Pair<Article?, Article?>? = null,
    val related: List<Article> = emptyList(),
    val isBookmarked: Boolean = false,
)

internal class ArticleDetailViewModel(
    private val repository: ArticlesRepository,
    private val slug: String,
    private val bookmarks: BookmarksRepository?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ArticleDetailState())
    private var loadJob: Job? = null
    val state: StateFlow<ArticleDetailState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun toggleBookmark() {
        val article = (state.value.article as? ArticleDetailUiState.Content)?.article ?: return
        val repository = bookmarks ?: return
        viewModelScope.launch { repository.toggle(article) }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = ArticleDetailState()
            when (val result = repository.article(slug)) {
                is NetworkResult.Failure ->
                    mutableState.update {
                        it.copy(
                            article =
                                ArticleDetailUiState.Error(result.error.message ?: "Failed to load")
                        )
                    }
                is NetworkResult.Success -> {
                    val article = result.data
                    mutableState.update { it.copy(article = ArticleDetailUiState.Content(article)) }
                    bookmarks?.let { source ->
                        launch {
                            source.isBookmarked(article.slug).collect { saved ->
                                mutableState.update { it.copy(isBookmarked = saved) }
                            }
                        }
                    }
                    launch {
                        (repository.adjacent(article.slug) as? NetworkResult.Success)?.let {
                            adjacent ->
                            mutableState.update { it.copy(adjacent = adjacent.data) }
                        }
                    }
                    launch {
                        (repository.related(article) as? NetworkResult.Success)?.let { related ->
                            mutableState.update { it.copy(related = related.data) }
                        }
                    }
                }
            }
        }
    }
}
