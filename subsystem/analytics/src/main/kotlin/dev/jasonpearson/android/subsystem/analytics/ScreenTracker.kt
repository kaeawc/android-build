package dev.jasonpearson.android.subsystem.analytics

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.zacsweers.metro.Inject
import java.util.concurrent.atomic.AtomicReference

@SingleIn(AppScope::class)
@Inject
public class ScreenTracker(private val client: AnalyticsClient) {
    private val previousScreen = AtomicReference<String?>(null)

    public fun trackScreen(screen: String) {
        val previous = previousScreen.getAndSet(screen)
        if (previous != screen) {
            client.track(AnalyticsEvent("screen_view", mapOf("screen" to screen)))
        }
    }
}
