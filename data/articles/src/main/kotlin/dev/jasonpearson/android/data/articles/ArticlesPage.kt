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
import kotlinx.serialization.Serializable

/**
 * One page of the newest-first article list. [nextPage] is null on the last page; [fromCache] is
 * true when the network failed and the page was served from the offline cache.
 */
data class ArticlesPage(
    val articles: List<Article>,
    val page: Int,
    val nextPage: Int?,
    val total: Int? = null,
    val fromCache: Boolean = false,
)

@Serializable
internal data class CachedArticlesPage(
    val articles: List<CachedArticle>,
    val page: Int,
    val nextPage: Int?,
    val total: Int?,
)

internal fun ArticlesPage.toCacheModel() =
    CachedArticlesPage(articles.map(Article::toCacheModel), page, nextPage, total)

internal fun CachedArticlesPage.toArticlesPage() =
    ArticlesPage(articles.map(CachedArticle::toArticle), page, nextPage, total)
