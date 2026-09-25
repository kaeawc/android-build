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
package dev.jasonpearson.android.client.github

import dev.jasonpearson.android.client.github.api.GitHubApi
import dev.jasonpearson.android.client.github.dto.GitHubRepoDto
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okhttp3.Headers.Companion.headersOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class GitHubDataSourceTest {

    private class FakeApi : GitHubApi {
        var reposResponse: Response<List<GitHubRepoDto>> = Response.success(emptyList())
        var readmeResponse: Response<ResponseBody> = Response.success("".toResponseBody())
        val sentEtags = mutableListOf<String?>()

        override suspend fun getRepos(
            user: String,
            sort: String,
            perPage: Int,
            type: String,
            ifNoneMatch: String?,
        ): Response<List<GitHubRepoDto>> {
            sentEtags += ifNoneMatch
            return reposResponse
        }

        override suspend fun getRepo(
            owner: String,
            repo: String,
            ifNoneMatch: String?,
        ): Response<GitHubRepoDto> {
            sentEtags += ifNoneMatch
            return Response.success(repo(1, repo))
        }

        override suspend fun getReadmeHtml(
            owner: String,
            repo: String,
            ifNoneMatch: String?,
        ): Response<ResponseBody> {
            sentEtags += ifNoneMatch
            return readmeResponse
        }
    }

    private val api = FakeApi()
    private val dataSource = GitHubDataSource(api)

    @Test
    fun `200 returns the mapped body with its etag`() = runTest {
        api.reposResponse =
            Response.success(
                listOf(repo(1, "low", stars = 1), repo(2, "fork", fork = true), repo(3, "high", 9)),
                headersOf("ETag", "W/\"abc\""),
            )

        val result = dataSource.getProjects()

        result as Conditional.Modified
        assertEquals(listOf("high", "low"), result.value.map { it.name })
        assertEquals("W/\"abc\"", result.etag)
    }

    @Test
    fun `cached etag is sent as If-None-Match`() = runTest {
        dataSource.getProjects(etag = "\"v1\"")
        dataSource.getReadmeHtml("repo", etag = "\"v2\"")
        dataSource.getProject("repo")

        assertEquals(listOf("\"v1\"", "\"v2\"", null), api.sentEtags)
    }

    @Test
    fun `304 is NotModified rather than an error`() = runTest {
        api.readmeResponse = errorResponse(304, headersOf("ETag", "\"v2\""))

        assertEquals(Conditional.NotModified, dataSource.getReadmeHtml("repo", etag = "\"v2\""))
    }

    @Test
    fun `403 with no remaining quota is a rate limit carrying the reset time`() = runTest {
        api.reposResponse =
            errorResponse(
                403,
                headersOf("X-RateLimit-Remaining", "0", "X-RateLimit-Reset", "1700000000"),
            )

        val error = runCatching { dataSource.getProjects() }.exceptionOrNull()

        assertTrue(error is GitHubRateLimitException)
        assertEquals(
            Instant.fromEpochSeconds(1_700_000_000),
            (error as GitHubRateLimitException).resetAt,
        )
    }

    @Test
    fun `429 is a rate limit even without quota headers`() = runTest {
        api.reposResponse = errorResponse(429, headersOf())

        val error = runCatching { dataSource.getProjects() }.exceptionOrNull()

        assertTrue(error is GitHubRateLimitException)
        assertNull((error as GitHubRateLimitException).resetAt)
    }

    @Test
    fun `403 without rate limit headers is a plain http error`() = runTest {
        api.reposResponse = errorResponse(403, headersOf("X-RateLimit-Remaining", "12"))

        try {
            dataSource.getProjects()
            fail("expected HttpException")
        } catch (e: HttpException) {
            assertEquals(403, e.code())
        }
    }

    @Test
    fun `retry-after is used when the reset header is missing`() {
        val response: Response<String> = errorResponse(403, headersOf("Retry-After", "60"))
        val now = Instant.fromEpochSeconds(1_000)

        val error = runCatching { response.toConditional(now = { now }) { it } }.exceptionOrNull()

        assertEquals(Instant.fromEpochSeconds(1_060), (error as GitHubRateLimitException).resetAt)
    }

    @Test
    fun `rate limit message names the reset time`() {
        // 2023-11-14T22:13:20Z
        val message = rateLimitMessage(Instant.fromEpochSeconds(1_700_000_000), TimeZone.UTC)

        assertEquals(
            "GitHub's hourly request limit was reached. Try again after 10:13 PM.",
            message,
        )
        assertEquals(
            "GitHub's hourly request limit was reached. Please try again later.",
            rateLimitMessage(null, TimeZone.UTC),
        )
    }

    private fun <T> errorResponse(code: Int, headers: okhttp3.Headers): Response<T> =
        Response.error(
            "".toResponseBody(),
            okhttp3.Response.Builder()
                .code(code)
                .message("HTTP $code")
                .protocol(Protocol.HTTP_1_1)
                .headers(headers)
                .request(Request.Builder().url("https://api.github.com/").build())
                .build(),
        )

    private companion object {
        fun repo(id: Long, name: String, stars: Int = 0, fork: Boolean = false) =
            GitHubRepoDto(
                id = id,
                name = name,
                htmlUrl = "https://github.com/kaeawc/$name",
                stargazersCount = stars,
                fork = fork,
            )
    }
}
