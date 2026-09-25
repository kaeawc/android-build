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

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsentGatedAnalyticsClientTest {

    private val sent = mutableListOf<AnalyticsEvent>()

    private fun client(consent: AnalyticsConsent) =
        ConsentGatedAnalyticsClient(AnalyticsSink(sent::add), consent)

    @Test
    fun `does not send events when consent is disabled`() {
        val client = client(AnalyticsConsent(initial = false))

        client.track(AnalyticsEvent("tap", mapOf("target" to "project")))

        assertEquals(emptyList<AnalyticsEvent>(), sent)
    }

    @Test
    fun `forwards the exact event when consent is enabled`() {
        val client = client(AnalyticsConsent(initial = true))
        val event = AnalyticsEvent("tap", mapOf("target" to "project"))

        client.track(event)

        assertEquals(listOf(event), sent)
    }

    @Test
    fun `holds events until consent loads, then sends them in order when enabled`() {
        val consent = AnalyticsConsent()
        val client = client(consent)
        val first = AnalyticsEvent("screen_view", mapOf("screen" to "Articles"))
        val second = AnalyticsEvent("tap", mapOf("target" to "article"))

        client.track(first)
        client.track(second)
        assertEquals(emptyList<AnalyticsEvent>(), sent)

        consent.update(true)

        assertEquals(listOf(first, second), sent)
    }

    @Test
    fun `discards held events when consent loads as disabled`() {
        val consent = AnalyticsConsent()
        val client = client(consent)

        client.track(AnalyticsEvent("screen_view", mapOf("screen" to "Articles")))
        consent.update(false)
        consent.update(true)

        assertEquals(emptyList<AnalyticsEvent>(), sent)
    }

    @Test
    fun `sends later events directly once consent has loaded`() {
        val consent = AnalyticsConsent()
        val client = client(consent)
        val held = AnalyticsEvent("screen_view", mapOf("screen" to "Articles"))
        val later = AnalyticsEvent("screen_view", mapOf("screen" to "Talks"))

        client.track(held)
        consent.update(true)
        client.track(later)

        assertEquals(listOf(held, later), sent)
    }

    @Test
    fun `stops sending once consent is withdrawn`() {
        val consent = AnalyticsConsent(initial = true)
        val client = client(consent)
        val before = AnalyticsEvent("tap", mapOf("target" to "before"))

        client.track(before)
        consent.update(false)
        client.track(AnalyticsEvent("tap", mapOf("target" to "after")))

        assertEquals(listOf(before), sent)
    }

    @Test
    fun `keeps only the newest events while consent is unknown`() {
        val consent = AnalyticsConsent()
        val client = client(consent)
        val total = ConsentGatedAnalyticsClient.MAX_PENDING_EVENTS + 5

        repeat(total) { client.track(AnalyticsEvent("tap", mapOf("index" to "$it"))) }
        consent.update(true)

        assertEquals((5 until total).map { "$it" }, sent.map { it.params.getValue("index") })
    }
}
