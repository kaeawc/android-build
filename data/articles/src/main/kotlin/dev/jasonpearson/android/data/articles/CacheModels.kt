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
import dev.jasonpearson.android.core.model.Author
import dev.jasonpearson.android.core.model.SearchEntry
import dev.jasonpearson.android.core.model.Tag
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class CachedTag(
    val id: String,
    val slug: String,
    val name: String,
    val postCount: Int?,
)

internal fun Tag.toCacheModel() = CachedTag(id, slug, name, postCount)

internal fun CachedTag.toTag() = Tag(id, slug, name, postCount)

@Serializable
internal data class CachedAuthor(
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val url: String?,
)

internal fun Author.toCacheModel() = CachedAuthor(id, name, profileImageUrl, url)

internal fun CachedAuthor.toAuthor() = Author(id, name, profileImageUrl, url)

@Serializable
internal data class CachedArticle(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String?,
    val html: String?,
    val featureImageUrl: String?,
    val publishedAt: String?,
    val readingTimeMinutes: Int?,
    val tags: List<CachedTag>,
    val author: CachedAuthor?,
    val featured: Boolean,
    val primaryTag: CachedTag?,
    val url: String?,
)

internal fun Article.toCacheModel() =
    CachedArticle(
        id = id,
        slug = slug,
        title = title,
        excerpt = excerpt,
        html = html,
        featureImageUrl = featureImageUrl,
        publishedAt = publishedAt?.toString(),
        readingTimeMinutes = readingTimeMinutes,
        tags = tags.map(Tag::toCacheModel),
        author = author?.toCacheModel(),
        featured = featured,
        primaryTag = primaryTag?.toCacheModel(),
        url = url,
    )

internal fun CachedArticle.toArticle() =
    Article(
        id = id,
        slug = slug,
        title = title,
        excerpt = excerpt,
        html = html,
        featureImageUrl = featureImageUrl,
        publishedAt = publishedAt?.let { Instant.parse(it) },
        readingTimeMinutes = readingTimeMinutes,
        tags = tags.map(CachedTag::toTag),
        author = author?.toAuthor(),
        featured = featured,
        primaryTag = primaryTag?.toTag(),
        url = url,
    )

@Serializable
internal data class CachedSearchEntry(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String?,
    val url: String?,
)

internal fun SearchEntry.toCacheModel() = CachedSearchEntry(id, slug, title, excerpt, url)

internal fun CachedSearchEntry.toSearchEntry() = SearchEntry(id, slug, title, excerpt, url)
