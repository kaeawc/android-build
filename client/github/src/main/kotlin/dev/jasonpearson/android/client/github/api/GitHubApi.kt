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
package dev.jasonpearson.android.client.github.api

import dev.jasonpearson.android.client.github.dto.GitHubRepoDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Every call returns the raw [Response] so callers can read `ETag` and rate-limit headers, and so a
 * `304 Not Modified` (which doesn't count against the unauthenticated 60/hour limit) isn't thrown
 * as an error. Pass the cached ETag as `ifNoneMatch`; `null` omits the header.
 */
interface GitHubApi {

    @Headers(value = ["User-Agent: kaeawc-portfolio-app", "Accept: application/vnd.github+json"])
    @GET("users/{user}/repos")
    suspend fun getRepos(
        @Path("user") user: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("type") type: String = "owner",
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<List<GitHubRepoDto>>

    @Headers(value = ["User-Agent: kaeawc-portfolio-app", "Accept: application/vnd.github+json"])
    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<GitHubRepoDto>

    @Headers(
        value = ["User-Agent: kaeawc-portfolio-app", "Accept: application/vnd.github.html+json"]
    )
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadmeHtml(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<ResponseBody>
}
