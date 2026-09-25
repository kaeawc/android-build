package dev.jasonpearson.android.subsystem.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsentGatedAnalyticsClientTest {

    @Test
    fun `does not send events when consent is disabled`() {
        val sent = mutableListOf<AnalyticsEvent>()
        val client =
            ConsentGatedAnalyticsClient(AnalyticsSink(sent::add), AnalyticsConsent { false })

        client.track(AnalyticsEvent("tap", mapOf("target" to "project")))

        assertEquals(emptyList<AnalyticsEvent>(), sent)
    }

    @Test
    fun `forwards the exact event when consent is enabled`() {
        val sent = mutableListOf<AnalyticsEvent>()
        val client =
            ConsentGatedAnalyticsClient(AnalyticsSink(sent::add), AnalyticsConsent { true })
        val event = AnalyticsEvent("tap", mapOf("target" to "project"))

        client.track(event)

        assertEquals(listOf(event), sent)
    }
}
