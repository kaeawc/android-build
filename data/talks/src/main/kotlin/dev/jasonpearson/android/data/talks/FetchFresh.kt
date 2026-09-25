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

package dev.jasonpearson.android.data.talks

import dev.jasonpearson.android.subsystem.storage.ContentCache
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cache-bypassing read for an explicit refresh: always fetches, never falls back to the cached copy
 * (so a failure surfaces), and stores a successful result best-effort, like `fetchWithFallback`.
 */
internal suspend fun <T : Any> ContentCache.fetchFresh(
    key: String,
    encode: (T) -> String,
    fetch: suspend () -> T,
): T {
    val fresh = fetch()
    try {
        put(key, encode(fresh))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Best-effort: a failed cache write must not fail a successful refresh.
    }
    return fresh
}
