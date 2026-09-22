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
package dev.jasonpearson.android.client.ghost.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class GhostPostsResponse(val posts: List<GhostPostDto>)

@Serializable data class GhostPagesResponse(val pages: List<GhostPostDto>)

@Serializable
data class GhostPostDto(
    val id: String,
    val uuid: String,
    val slug: String,
    val title: String,
    val html: String? = null,
    @SerialName("feature_image") val featureImage: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("reading_time") val readingTime: Int? = null,
    @SerialName("custom_excerpt") val customExcerpt: String? = null,
    val excerpt: String? = null,
    val tags: List<GhostTagDto>? = null,
    @SerialName("primary_author") val primaryAuthor: GhostAuthorDto? = null,
    val authors: List<GhostAuthorDto>? = null,
)

@Serializable data class GhostTagDto(val id: String, val slug: String, val name: String)

@Serializable
data class GhostAuthorDto(
    val id: String,
    val name: String,
    val slug: String,
    @SerialName("profile_image") val profileImage: String? = null,
    val url: String? = null,
)
