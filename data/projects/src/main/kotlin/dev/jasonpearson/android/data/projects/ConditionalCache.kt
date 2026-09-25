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
package dev.jasonpearson.android.data.projects

import dev.jasonpearson.android.client.github.Conditional
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.Fetched
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What a conditional cache key holds: the encoded body plus the `ETag` GitHub served it with. */
@Serializable internal data class EtagEnvelope(val etag: String? = null, val body: String)

/**
 * Network-first read that revalidates with `If-None-Match` so unchanged GitHub resources answer
 * `304` (free against the rate limit) and are served from the cache.
 *
 * - `200`: the new body and ETag are cached best-effort and returned.
 * - `304`: the cached body is returned (and re-saved so its timestamp marks the revalidation).
 * - Any failure, including [dev.jasonpearson.android.client.github.GitHubRateLimitException]: the
 *   cached copy is returned flagged `fromCache`, or the original error when nothing is cached.
 *
 * [forceRefresh] skips `If-None-Match` so GitHub always returns a full body; the cache is still the
 * fallback if that request fails. Entries written before ETags were stored (a bare body) still
 * decode and are simply fetched unconditionally.
 */
internal suspend fun <T : Any> ContentCache.fetchConditional(
    key: String,
    json: Json,
    forceRefresh: Boolean = false,
    encode: (T) -> String,
    decode: (String) -> T,
    fetch: suspend (etag: String?) -> Conditional<T>,
): Result<Fetched<T>> {
    val entry =
        try {
            get(key)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    val envelope = entry?.let { readEnvelope(json, it.value) }
    val cachedValue = envelope?.let { runCatching { decode(it.body) }.getOrNull() }
    val etag = envelope?.etag?.takeIf { !forceRefresh && cachedValue != null }

    return try {
        when (val response = fetch(etag)) {
            is Conditional.Modified -> {
                putQuietly(
                    key,
                    writeEnvelope(json, EtagEnvelope(response.etag, encode(response.value))),
                )
                Result.success(Fetched(response.value, fromCache = false))
            }
            Conditional.NotModified -> {
                // Only reachable when an ETag was sent, which requires a decodable cached value.
                val value = checkNotNull(cachedValue) { "304 without a cached body for $key" }
                entry?.let { putQuietly(key, it.value) }
                Result.success(Fetched(value, fromCache = false))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (cachedValue != null) {
            Result.success(Fetched(cachedValue, fromCache = true, savedAt = entry?.savedAt))
        } else {
            Result.failure(e)
        }
    }
}

/** A bare body (cached before ETags were stored) reads as an envelope without an ETag. */
private fun readEnvelope(json: Json, raw: String): EtagEnvelope {
    val envelope = runCatching { json.decodeFromString(EtagEnvelope.serializer(), raw) }
    return envelope.getOrElse { EtagEnvelope(etag = null, body = raw) }
}

private fun writeEnvelope(json: Json, envelope: EtagEnvelope): String =
    json.encodeToString(EtagEnvelope.serializer(), envelope)

private suspend fun ContentCache.putQuietly(key: String, value: String) {
    try {
        put(key, value)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Best-effort: a failed cache write must not fail a successful fetch.
    }
}
