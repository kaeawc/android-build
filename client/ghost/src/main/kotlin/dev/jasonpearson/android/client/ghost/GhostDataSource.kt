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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android.client.ghost

import dev.jasonpearson.android.client.ghost.api.GhostContentApi
import dev.jasonpearson.android.client.ghost.mapper.extractPhotos
import dev.jasonpearson.android.client.ghost.mapper.toArticle
import dev.jasonpearson.android.client.ghost.mapper.toContentPage
import dev.jasonpearson.android.client.ghost.mapper.toSearchEntry
import dev.jasonpearson.android.client.ghost.mapper.toSiteSettings
import dev.jasonpearson.android.client.ghost.mapper.toTag
import dev.jasonpearson.android.core.di.GhostContentKey
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.model.ContentPage
import dev.jasonpearson.android.core.model.Photo
import dev.jasonpearson.android.core.model.SearchEntry
import dev.jasonpearson.android.core.model.SiteSettings
import dev.jasonpearson.android.core.model.Tag
import dev.zacsweers.metro.Inject

@Inject
class GhostDataSource(
    private val api: GhostContentApi,
    @GhostContentKey private val contentKey: String,
) {
    suspend fun getArticles(): List<Article> =
        api.getPosts(key = contentKey).posts.map { it.toArticle() }

    suspend fun getArticle(slug: String): Article =
        api.getPostBySlug(slug, contentKey).posts.first().toArticle()

    suspend fun getPage(slug: String): ContentPage =
        api.getPageBySlug(slug, contentKey).pages.first().toContentPage()

    suspend fun getPhotos(pageSlug: String = "photography"): List<Photo> =
        extractPhotos(getPage(pageSlug).html)

    suspend fun getAllArticles(): List<Article> {
        val articles = mutableListOf<Article>()
        var page = 1
        do {
            val response = api.getPostsPage(key = contentKey, page = page)
            articles += response.posts.map { it.toArticle() }
            page = response.meta?.pagination?.next ?: break
        } while (true)
        return articles
    }

    suspend fun getFeaturedArticles(): List<Article> =
        api.getPostsPage(key = contentKey, filter = "featured:true").posts.map { it.toArticle() }

    suspend fun getArticlesByTag(tagSlug: String): List<Article> =
        api.getPostsPage(key = contentKey, filter = "tag:$tagSlug").posts.map { it.toArticle() }

    suspend fun getRelatedArticles(article: Article, limit: Int = 3): List<Article> {
        val tag = article.primaryTag ?: article.tags.firstOrNull() ?: return emptyList()
        return api.getPostsPage(
                key = contentKey,
                filter = buildRelatedFilter(tag.slug, article.id),
                limit = limit,
            )
            .posts
            .map { it.toArticle() }
    }

    suspend fun getTags(): List<Tag> = api.getTags(contentKey).tags.map { it.toTag() }

    suspend fun getSettings(): SiteSettings = api.getSettings(contentKey).settings.toSiteSettings()

    suspend fun getSearchIndex(): List<SearchEntry> =
        api.getSearchIndexPosts(contentKey).posts.map { it.toSearchEntry() }
}

internal fun buildRelatedFilter(tagSlug: String, excludeId: String): String =
    "tag:$tagSlug+id:-$excludeId"
