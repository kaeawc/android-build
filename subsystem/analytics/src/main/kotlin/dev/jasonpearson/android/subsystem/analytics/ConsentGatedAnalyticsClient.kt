package dev.jasonpearson.android.subsystem.analytics

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
public class ConsentGatedAnalyticsClient(
    private val sink: AnalyticsSink,
    private val consent: AnalyticsConsent,
) : AnalyticsClient {
    override fun track(event: AnalyticsEvent) {
        if (consent.isEnabled()) sink.send(event)
    }
}
