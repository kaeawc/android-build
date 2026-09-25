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
package dev.jasonpearson.android.subsystem.storage

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.Instant

/** A value from [fetchWithFallback], flagged when it came from the offline cache. */
public data class Fetched<T>(val value: T, val fromCache: Boolean, val savedAt: Instant? = null)

/**
 * Network-first read with an offline fallback. On success the fresh value is cached best-effort
 * (cache failures never fail the fetch); on failure the last cached copy is returned if it still
 * decodes, otherwise the original fetch error.
 */
public suspend fun <T : Any> ContentCache.fetchWithFallback(
    key: String,
    encode: (T) -> String,
    decode: (String) -> T,
    fetch: suspend () -> T,
): Result<Fetched<T>> =
    try {
        val fresh = fetch()
        try {
            put(key, encode(fresh))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort: a failed cache write must not fail a successful fetch.
        }
        Result.success<Fetched<T>>(Fetched(fresh, fromCache = false))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val cached =
            try {
                get(key)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                null
            }
        val decoded = cached?.let { entry ->
            runCatching { decode(entry.value) }
                .getOrNull()
                ?.let { Fetched(it, fromCache = true, savedAt = entry.savedAt) }
        }
        if (decoded != null) Result.success<Fetched<T>>(decoded) else Result.failure<Fetched<T>>(e)
    }

/**
 * Network-only refresh. This stays separate from [fetchWithFallback] so existing callers retain
 * their offline fallback behavior; refresh failures never read or modify the cache.
 */
public suspend fun <T : Any> ContentCache.fetchFresh(
    key: String,
    encode: (T) -> String,
    fetch: suspend () -> T,
): Result<Fetched<T>> =
    try {
        val fresh = fetch()
        try {
            put(key, encode(fresh))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort: a failed cache write must not fail a successful refresh.
        }
        Result.success(Fetched(fresh, fromCache = false))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
