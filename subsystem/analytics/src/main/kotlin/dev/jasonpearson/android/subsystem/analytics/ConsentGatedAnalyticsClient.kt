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

    init {
        consent.addListener { enabled -> synchronized(lock) { drainPending(enabled) } }
    }

    override fun track(event: AnalyticsEvent) {
        synchronized(lock) {
            when (val enabled = consent.current) {
                null -> {
                    if (pending.size == MAX_PENDING_EVENTS) pending.removeFirst()
                    pending.addLast(event)
                }
                else -> {
                    drainPending(enabled)
                    if (enabled) sink.send(event)
                }
            }
        }
    }

    private fun drainPending(enabled: Boolean) {
        while (pending.isNotEmpty()) {
            val event = pending.removeFirst()
            if (enabled) sink.send(event)
        }
    }

    internal companion object {
        /** Bounds the cold-start buffer; the oldest events are dropped beyond this. */
        const val MAX_PENDING_EVENTS: Int = 32
    }
}
