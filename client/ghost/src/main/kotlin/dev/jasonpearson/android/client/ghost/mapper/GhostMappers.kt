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
package dev.jasonpearson.android.client.ghost.mapper

import dev.jasonpearson.android.client.ghost.dto.GhostPostDto
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.model.Author
import dev.jasonpearson.android.core.model.ContentPage
import dev.jasonpearson.android.core.model.Photo
import dev.jasonpearson.android.core.model.Tag
import kotlinx.datetime.Instant

fun GhostPostDto.toArticle(): Article =
    Article(
        id = id,
        slug = slug,
        title = title,
        excerpt = customExcerpt ?: excerpt,
        html = html,
        featureImageUrl = featureImage,
        publishedAt = publishedAt?.let { Instant.parse(it) },
        readingTimeMinutes = readingTime,
        tags = tags?.map { Tag(it.id, it.slug, it.name) } ?: emptyList(),
        author = primaryAuthor?.let { Author(it.id, it.name, it.profileImage, it.url) },
    )

fun GhostPostDto.toContentPage(): ContentPage = ContentPage(slug, title, html ?: "")

fun extractPhotos(html: String): List<Photo> =
    Regex("""<img[^>]+src=[\"']([^\"']+)[\"']""")
        .findAll(html)
        .map { Photo(it.groupValues[1]) }
        .toList()
