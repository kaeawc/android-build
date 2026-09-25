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

import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.model.SearchEntry
import dev.jasonpearson.android.core.model.Tag
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.articles.ArticlesPage
import dev.jasonpearson.android.data.articles.ArticlesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listLoadsPagesAndKeepsContentOnRefreshFailure() =
        runTest(dispatcher) {
            val first = article("first")
            val second = article("second")
            val tag = Tag("tag", "news", "News")
            val repository =
                FakeArticlesRepository().apply {
                    pages[1] = NetworkResult.Success(ArticlesPage(listOf(first), 1, 2))
                    pages[2] = NetworkResult.Success(ArticlesPage(listOf(second), 2, null))
                    featuredResult = NetworkResult.Success(listOf(first))
                    tagsResult = NetworkResult.Success(listOf(tag))
                }
            val model = ArticlesListViewModel(repository)
            advanceUntilIdle()
            assertEquals(listOf(first), model.state.value.page.articles)
            assertEquals(listOf(first), model.state.value.featured)
            assertEquals(listOf(tag), model.state.value.tags)
            assertEquals(1, repository.pageCalls.count { it == 1 })

            model.loadMore()
            advanceUntilIdle()
            assertEquals(listOf(first, second), model.state.value.page.articles)

            repository.pages[1] = NetworkResult.Failure(IllegalStateException("offline"))
            model.refresh()
            advanceUntilIdle()
            assertEquals(listOf(first, second), model.state.value.page.articles)
            assertEquals("offline", model.state.value.page.error?.message)
        }

    @Test
    fun detailLoadsArticleBySlug() =
        runTest(dispatcher) {
            val expected = article("requested")
            val repository =
                FakeArticlesRepository().apply { articleResult = NetworkResult.Success(expected) }
            val model = ArticleDetailViewModel(repository, "requested", null)
            advanceUntilIdle()
            assertEquals(listOf("requested"), repository.articleCalls)
            assertEquals(
                expected,
                (model.state.value.article as ArticleDetailUiState.Content).article,
            )
        }

    @Test
    fun detailFailureProducesError() =
        runTest(dispatcher) {
            val repository =
                FakeArticlesRepository().apply {
                    articleResult = NetworkResult.Failure(IllegalStateException("unavailable"))
                }
            val model = ArticleDetailViewModel(repository, "missing", null)
            advanceUntilIdle()
            assertEquals(
                "unavailable",
                (model.state.value.article as ArticleDetailUiState.Error).message,
            )
        }

    private fun article(slug: String) =
        Article(
            id = slug,
            slug = slug,
            title = slug,
            excerpt = null,
            html = null,
            featureImageUrl = null,
            publishedAt = null,
            readingTimeMinutes = null,
            tags = emptyList(),
            author = null,
        )
}

private class FakeArticlesRepository : ArticlesRepository {
    val pages = mutableMapOf<Int, NetworkResult<ArticlesPage>>()
    val pageCalls = mutableListOf<Int>()
    val articleCalls = mutableListOf<String>()
    var featuredResult: NetworkResult<List<Article>> = NetworkResult.Success(emptyList())
    var tagsResult: NetworkResult<List<Tag>> = NetworkResult.Success(emptyList())
    var articleResult: NetworkResult<Article> =
        NetworkResult.Failure(IllegalStateException("unset"))

    override suspend fun articles(): NetworkResult<List<Article>> =
        NetworkResult.Success(emptyList())

    override suspend fun article(slug: String): NetworkResult<Article> {
        articleCalls += slug
        return articleResult
    }

    override suspend fun featured(): NetworkResult<List<Article>> = featuredResult

    override suspend fun tags(): NetworkResult<List<Tag>> = tagsResult

    override suspend fun articlesByTag(slug: String): NetworkResult<List<Article>> =
        NetworkResult.Success(emptyList())

    override suspend fun related(article: Article): NetworkResult<List<Article>> =
        NetworkResult.Success(emptyList())

    override suspend fun search(query: String): NetworkResult<List<SearchEntry>> =
        NetworkResult.Success(emptyList())

    override suspend fun adjacent(slug: String): NetworkResult<Pair<Article?, Article?>> =
        NetworkResult.Success(null to null)

    override suspend fun articlesPage(
        page: Int,
        tagSlug: String?,
        refresh: Boolean,
    ): NetworkResult<ArticlesPage> {
        pageCalls += page
        return pages.getValue(page)
    }
}
