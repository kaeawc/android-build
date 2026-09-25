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

import java.util.concurrent.CopyOnWriteArrayList

/**
 * The user's analytics choice. It is unknown until the app publishes the persisted opt-out with
 * [update], so events tracked at cold start can wait for the real value instead of being dropped
 * (or sent without consent).
 */
public class AnalyticsConsent(initial: Boolean? = null) {
    private val listeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    @Volatile private var value: Boolean? = initial

    /** The user's latest choice, or null while it has not loaded yet. */
    public val current: Boolean?
        get() = value

    /** Publishes the user's latest choice and notifies every listener. */
    public fun update(enabled: Boolean) {
        value = enabled
        listeners.forEach { it(enabled) }
    }

    /** Calls [listener] with every later [update]. */
    public fun addListener(listener: (Boolean) -> Unit) {
        listeners += listener
    }
}
