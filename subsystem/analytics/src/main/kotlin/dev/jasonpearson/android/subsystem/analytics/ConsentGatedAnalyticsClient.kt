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
package dev.jasonpearson.android.subsystem.analytics

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Sends events only with the user's consent. Until the persisted choice has loaded, up to
 * [MAX_PENDING_EVENTS] events (e.g. the cold-start screen view) wait in a buffer; they are sent in
 * order once consent resolves to enabled and discarded if it resolves to disabled.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
public class ConsentGatedAnalyticsClient(
    private val sink: AnalyticsSink,
    private val consent: AnalyticsConsent,
) : AnalyticsClient {
    private val lock = Any()
    private val pending = ArrayDeque<AnalyticsEvent>()
    private var flushing = false

    init {
        consent.addListener { enabled ->
            val shouldFlush =
                synchronized(lock) {
                    if (!enabled) pending.clear()
                    startFlushingIfNeeded(enabled)
                }
            if (shouldFlush) flushPending()
        }
    }

    override fun track(event: AnalyticsEvent) {
        val shouldFlush =
            synchronized(lock) {
                when (val enabled = consent.current) {
                    null -> {
                        if (pending.size == MAX_PENDING_EVENTS) pending.removeFirst()
                        pending.addLast(event)
                        false
                    }
                    false -> {
                        pending.clear()
                        false
                    }
                    true -> {
                        pending.addLast(event)
                        startFlushingIfNeeded(enabled)
                    }
                }
            }
        if (shouldFlush) flushPending()
    }

    /** Must be called under [lock]. */
    private fun startFlushingIfNeeded(enabled: Boolean): Boolean {
        if (!enabled || flushing || pending.isEmpty()) return false
        flushing = true
        return true
    }

    private fun flushPending() {
        while (true) {
            val event =
                synchronized(lock) {
                    if (pending.isEmpty()) {
                        flushing = false
                        return
                    }
                    pending.removeFirst()
                }
            try {
                sink.send(event)
            } catch (failure: Throwable) {
                synchronized(lock) { flushing = false }
                throw failure
            }
        }
    }

    internal companion object {
        /** Bounds the cold-start buffer; the oldest events are dropped beyond this. */
        const val MAX_PENDING_EVENTS: Int = 32
    }
}
