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
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticlesPaginatorTest {

    /** 3 pages of 2 articles over ids a1..a6, with optional scripted failures and overrides. */
    private class FakeSource {
        val calls = mutableListOf<Pair<Int, Boolean>>()
        val failures = mutableSetOf<Int>()
        val overrides = mutableMapOf<Int, ArticlesPage>()
        var gate: CompletableDeferred<Unit>? = null

        suspend fun load(page: Int, refresh: Boolean): NetworkResult<ArticlesPage> {
            calls += page to refresh
            gate?.await()
            if (page in failures) return NetworkResult.Failure(IOException("page $page failed"))
            overrides[page]?.let {
                return NetworkResult.Success(it)
            }
            val articles = listOf(article("a${page * 2 - 1}"), article("a${page * 2}"))
            return NetworkResult.Success(
                ArticlesPage(articles, page, nextPage = (page + 1).takeIf { it <= 3 }, total = 6)
            )
        }
    }

    private fun TestScope.paginator(source: FakeSource) =
        ArticlesPaginator(backgroundScope, source::load)

    @Test
    fun startLoadsTheFirstPageOnce() = runTest {
        val source = FakeSource()
        val paginator = paginator(source)

        paginator.start()
        paginator.start()
        runCurrent()
        paginator.start()
        runCurrent()

        val state = paginator.state.value
        assertTrue(state.loaded)
        assertEquals(listOf("a1", "a2"), state.articles.ids())
        assertEquals(2, state.nextPage)
        assertEquals(listOf(1 to false), source.calls)
    }

    @Test
    fun loadMoreAppendsPagesUntilTheEnd() = runTest {
        val source = FakeSource()
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        paginator.loadMore()
        runCurrent()
        paginator.loadMore()
        runCurrent()

        val state = paginator.state.value
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), state.articles.ids())
        assertTrue(state.endReached)
        assertFalse(state.canLoadMore)

        paginator.loadMore()
        runCurrent()
        assertEquals(listOf(1 to false, 2 to false, 3 to false), source.calls)
    }

    @Test
    fun loadMoreIsIgnoredWhileAPageIsInFlight() = runTest {
        val source = FakeSource()
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        source.gate = CompletableDeferred()
        paginator.loadMore()
        runCurrent()
        assertTrue(paginator.state.value.isLoadingMore)
        paginator.loadMore()
        paginator.loadMore()
        source.gate!!.complete(Unit)
        runCurrent()

        assertEquals(listOf(1 to false, 2 to false), source.calls)
        assertFalse(paginator.state.value.isLoadingMore)
    }

    @Test
    fun pagesAreMergedWithoutDuplicates() = runTest {
        val source = FakeSource()
        // A post published between loads shifts a2 onto page 2.
        source.overrides[2] = ArticlesPage(listOf(article("a2"), article("a3")), 2, nextPage = null)
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        paginator.loadMore()
        runCurrent()

        assertEquals(listOf("a1", "a2", "a3"), paginator.state.value.articles.ids())
    }

    @Test
    fun aNextPageThatDoesNotAdvanceEndsPagination() = runTest {
        val source = FakeSource()
        source.overrides[2] = ArticlesPage(listOf(article("a3")), 2, nextPage = 2)
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        paginator.loadMore()
        runCurrent()

        assertTrue(paginator.state.value.endReached)
    }

    @Test
    fun loadMoreFailureBlocksAutoLoadingUntilRetry() = runTest {
        val source = FakeSource()
        source.failures += 2
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        paginator.loadMore()
        runCurrent()
        val failed = paginator.state.value
        assertNotNull(failed.loadMoreError)
        assertEquals(listOf("a1", "a2"), failed.articles.ids())
        assertFalse(failed.canLoadMore)

        // Scrolling near the end again must not hammer a failing endpoint.
        paginator.loadMore()
        runCurrent()
        assertEquals(2, source.calls.size)

        source.failures.clear()
        paginator.retry()
        runCurrent()
        val recovered = paginator.state.value
        assertNull(recovered.loadMoreError)
        assertEquals(listOf("a1", "a2", "a3", "a4"), recovered.articles.ids())
    }

    @Test
    fun initialFailureIsRetried() = runTest {
        val source = FakeSource()
        source.failures += 1
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        val failed = paginator.state.value
        assertFalse(failed.loaded)
        assertEquals("page 1 failed", failed.error?.message)

        source.failures.clear()
        paginator.retry()
        runCurrent()

        val recovered = paginator.state.value
        assertTrue(recovered.loaded)
        assertNull(recovered.error)
        assertEquals(listOf(1 to false, 1 to false), source.calls)
    }

    @Test
    fun refreshRequestsTheNetworkAndReplacesAccumulatedPages() = runTest {
        val source = FakeSource()
        val paginator = paginator(source)
        paginator.start()
        runCurrent()
        paginator.loadMore()
        runCurrent()

        source.overrides[1] = ArticlesPage(listOf(article("new"), article("a1")), 1, nextPage = 2)
        source.gate = CompletableDeferred()
        paginator.refresh()
        runCurrent()
        // Existing content stays on screen while the refresh is in flight.
        assertTrue(paginator.state.value.isRefreshing)
        assertEquals(4, paginator.state.value.articles.size)

        source.gate!!.complete(Unit)
        runCurrent()

        val state = paginator.state.value
        assertFalse(state.isRefreshing)
        assertEquals(listOf("new", "a1"), state.articles.ids())
        assertEquals(2, state.nextPage)
        assertEquals(1 to true, source.calls.last())
    }

    @Test
    fun refreshFailureKeepsContentAndReportsTheError() = runTest {
        val source = FakeSource()
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        source.failures += 1
        paginator.refresh()
        runCurrent()

        val state = paginator.state.value
        assertTrue(state.loaded)
        assertFalse(state.isRefreshing)
        assertEquals(listOf("a1", "a2"), state.articles.ids())
        assertNotNull(state.error)
    }

    @Test
    fun refreshCancelsAnInFlightLoadMore() = runTest {
        val source = FakeSource()
        val paginator = paginator(source)
        paginator.start()
        runCurrent()

        source.gate = CompletableDeferred()
        paginator.loadMore()
        runCurrent()
        paginator.refresh()
        source.gate!!.complete(Unit)
        runCurrent()

        val state = paginator.state.value
        assertFalse(state.isLoadingMore)
        assertEquals(listOf("a1", "a2"), state.articles.ids())
        assertEquals(2, state.nextPage)
    }

    private fun List<Article>.ids() = map(Article::id)

    private companion object {
        fun article(id: String) =
            Article(
                id = id,
                slug = id,
                title = id,
                excerpt = null,
                html = null,
                featureImageUrl = null,
                publishedAt = null,
                readingTimeMinutes = null,
                tags = emptyList(),
                author = null,
            )
    }
}
