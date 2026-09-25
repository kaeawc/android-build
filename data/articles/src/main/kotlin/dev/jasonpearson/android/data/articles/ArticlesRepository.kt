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
import dev.jasonpearson.android.core.model.SearchEntry
import dev.jasonpearson.android.core.model.Tag
import dev.jasonpearson.android.core.network.NetworkResult

interface ArticlesRepository {
    suspend fun articles(): NetworkResult<List<Article>>

    suspend fun article(slug: String): NetworkResult<Article>

    suspend fun featured(): NetworkResult<List<Article>>

    suspend fun tags(): NetworkResult<List<Tag>>

    suspend fun articlesByTag(slug: String): NetworkResult<List<Article>>

    suspend fun related(article: Article): NetworkResult<List<Article>>

    suspend fun search(query: String): NetworkResult<List<SearchEntry>>

    suspend fun adjacent(slug: String): NetworkResult<Pair<Article?, Article?>>

    /**
     * One page (1-based) of the newest-first article list, narrowed to [tagSlug] when given. A
     * [refresh] goes straight to the network and reports its failure instead of serving the offline
     * cache.
     *
     * The default serves everything as a single page, for implementations without paging.
     */
    suspend fun articlesPage(
        page: Int,
        tagSlug: String? = null,
        refresh: Boolean = false,
    ): NetworkResult<ArticlesPage> {
        if (page > FIRST_PAGE) return NetworkResult.Success(ArticlesPage(emptyList(), page, null))
        return when (val result = if (tagSlug == null) articles() else articlesByTag(tagSlug)) {
            is NetworkResult.Success -> NetworkResult.Success(ArticlesPage(result.data, page, null))
            is NetworkResult.Failure -> result
        }
    }
}
