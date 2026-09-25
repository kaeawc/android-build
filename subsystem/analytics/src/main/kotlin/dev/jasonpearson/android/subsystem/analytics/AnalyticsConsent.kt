package dev.jasonpearson.android.subsystem.analytics

/** The app backs this consent check with the persisted analytics opt-out setting. */
fun interface AnalyticsConsent {
    fun isEnabled(): Boolean
}
