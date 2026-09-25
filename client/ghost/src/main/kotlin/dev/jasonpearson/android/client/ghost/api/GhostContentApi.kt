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
package dev.jasonpearson.android.client.ghost.api

import dev.jasonpearson.android.client.ghost.dto.GhostPagesResponse
import dev.jasonpearson.android.client.ghost.dto.GhostPostsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSearchIndexResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSettingsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostTagsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GhostContentApi {

    @GET("posts/")
    suspend fun getPosts(
        @Query("key") key: String,
        @Query("include") include: String = "tags,authors",
        @Query("limit") limit: String = "all",
        @Query("formats") formats: String = "html",
    ): GhostPostsResponse

    @GET("posts/slug/{slug}/")
    suspend fun getPostBySlug(
        @Path("slug") slug: String,
        @Query("key") key: String,
        @Query("include") include: String = "tags,authors",
        @Query("formats") formats: String = "html",
    ): GhostPostsResponse

    @GET("pages/slug/{slug}/")
    suspend fun getPageBySlug(
        @Path("slug") slug: String,
        @Query("key") key: String,
        @Query("formats") formats: String = "html",
    ): GhostPagesResponse

    @GET("posts/")
    suspend fun getPostsPage(
        @Query("key") key: String,
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("include") include: String = "tags,authors",
        @Query("formats") formats: String = "html",
    ): GhostPostsResponse

    @GET("tags/")
    suspend fun getTags(
        @Query("key") key: String,
        @Query("include") include: String = "count.posts",
        @Query("limit") limit: Int = 100,
        @Query("filter") filter: String = "visibility:public",
    ): GhostTagsResponse

    @GET("settings/") suspend fun getSettings(@Query("key") key: String): GhostSettingsResponse

    @GET("search-index/posts/")
    suspend fun getSearchIndexPosts(@Query("key") key: String): GhostSearchIndexResponse
}
