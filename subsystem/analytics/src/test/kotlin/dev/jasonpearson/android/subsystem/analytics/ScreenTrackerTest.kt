package dev.jasonpearson.android.subsystem.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTrackerTest {

    @Test
    fun `skips consecutive duplicate screens`() {
        val client = RecordingAnalyticsClient()
        val tracker = ScreenTracker(client)

        tracker.trackScreen("home")
        tracker.trackScreen("home")

        assertEquals(
            listOf(AnalyticsEvent("screen_view", mapOf("screen" to "home"))),
            client.events,
        )
    }

    @Test
    fun `tracks a screen again after an intervening screen`() {
        val client = RecordingAnalyticsClient()
        val tracker = ScreenTracker(client)

        tracker.trackScreen("home")
        tracker.trackScreen("detail")
        tracker.trackScreen("home")

        assertEquals(
            listOf("home", "detail", "home"),
            client.events.map { it.params.getValue("screen") },
        )
    }
}
