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

import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import retrofit2.HttpException
import retrofit2.Response

/** Result of a conditional (`If-None-Match`) GitHub request. */
sealed interface Conditional<out T> {
    /** A fresh `200` body, with the `ETag` to send next time (if GitHub returned one). */
    data class Modified<T>(val value: T, val etag: String?) : Conditional<T>

    /** `304 Not Modified`: the caller's cached copy for the sent ETag is still current. */
    data object NotModified : Conditional<Nothing>
}

/**
 * GitHub refused the request because the unauthenticated rate limit is used up. [resetAt] is when
 * the quota refills, from `X-RateLimit-Reset` (or `Retry-After`), if GitHub said.
 */
class GitHubRateLimitException(
    val resetAt: Instant?,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : IOException(rateLimitMessage(resetAt, timeZone))

internal fun rateLimitMessage(resetAt: Instant?, timeZone: TimeZone): String {
    val base = "GitHub's hourly request limit was reached."
    if (resetAt == null) return "$base Please try again later."
    val time = resetAt.toLocalDateTime(timeZone)
    val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
    val minute = time.minute.toString().padStart(2, '0')
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$base Try again after $hour12:$minute $amPm."
}

/**
 * Maps a raw GitHub [Response] to a [Conditional], throwing [GitHubRateLimitException] when the
 * rate limit is exhausted and [HttpException] for any other error status.
 */
internal fun <T : Any, R> Response<T>.toConditional(
    now: () -> Instant = { Clock.System.now() },
    transform: (T) -> R,
): Conditional<R> {
    val code = code()
    return when {
        code == HTTP_NOT_MODIFIED -> Conditional.NotModified
        isSuccessful -> {
            val body = body() ?: throw IOException("GitHub returned an empty body ($code)")
            Conditional.Modified(transform(body), headers()["ETag"])
        }
        isRateLimited() -> throw GitHubRateLimitException(rateLimitResetAt(now))
        else -> throw HttpException(this)
    }
}

private fun Response<*>.isRateLimited(): Boolean {
    val code = code()
    if (code == HTTP_TOO_MANY_REQUESTS) return true
    // Plain 403s (e.g. a blocked repo) are not rate limits; GitHub marks them with these headers.
    return code == HTTP_FORBIDDEN &&
        (headers()["X-RateLimit-Remaining"] == "0" || headers()["Retry-After"] != null)
}

private fun Response<*>.rateLimitResetAt(now: () -> Instant): Instant? {
    headers()["X-RateLimit-Reset"]?.trim()?.toLongOrNull()?.let {
        return Instant.fromEpochSeconds(it)
    }
    return headers()["Retry-After"]?.trim()?.toLongOrNull()?.let { now() + it.seconds }
}

private const val HTTP_NOT_MODIFIED = 304
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429
