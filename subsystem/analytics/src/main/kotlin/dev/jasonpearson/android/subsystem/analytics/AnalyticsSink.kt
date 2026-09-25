package dev.jasonpearson.android.subsystem.analytics

/** The app provides a Logcat-backed implementation of this event sink. */
fun interface AnalyticsSink {
    fun send(event: AnalyticsEvent)
}
