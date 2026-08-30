/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
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
package dev.jasonpearson.android.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.jasonpearson.android.feature.demos.DemosScreen
import dev.jasonpearson.android.feature.discover.DiscoverScreen
import dev.jasonpearson.android.feature.home.HomeScreen
import dev.jasonpearson.android.feature.login.LoginScreen
import dev.jasonpearson.android.feature.mediaplayer.MediaPlayerScreen
import dev.jasonpearson.android.feature.onboarding.OnboardingScreen
import dev.jasonpearson.android.feature.settings.SettingsScreen
import dev.jasonpearson.android.feature.slides.SlidesScreen
import dev.jasonpearson.android.foundation.navigation.Destination
import dev.jasonpearson.android.subsystem.analytics.AnalyticsClient
import dev.jasonpearson.android.subsystem.experimentation.ExperimentRepository
import dev.jasonpearson.android.subsystem.storage.SessionRepository
import dev.jasonpearson.android.subsystem.storage.UserPreferencesRepository

/** Extra route names for feature screens not part of the top-level [Destination] contract. */
private const val ROUTE_SLIDES = "slides"
private const val ROUTE_DEMOS = "demos"

/**
 * Registers every `:feature` screen against the shared [Destination] route contract. The subsystem
 * collaborators are constructed once by the app and passed in, keeping this graph free of any DI
 * wiring while still exercising the full `:app -> :feature -> :subsystem` dependency chain.
 */
fun NavGraphBuilder.featureGraph(
    navController: NavController,
    analytics: AnalyticsClient,
    preferences: UserPreferencesRepository,
    session: SessionRepository,
    experiments: ExperimentRepository,
) {
    composable(Destination.Onboarding.route) {
        OnboardingScreen(
            preferencesRepository = preferences,
            onFinished = { navController.navigate(Destination.Home.route) },
        )
    }
    composable(Destination.Login.route) {
        LoginScreen(
            preferencesRepository = preferences,
            onLoggedIn = { navController.navigate(Destination.Home.route) },
        )
    }
    composable(Destination.Home.route) {
        HomeScreen(
            analytics = analytics,
            experiments = experiments,
            onOpenDiscover = { navController.navigate(Destination.Discover.route) },
        )
    }
    composable(Destination.Discover.route) { DiscoverScreen(analytics = analytics) }
    composable(Destination.MediaPlayer.route) { MediaPlayerScreen(session = session) }
    composable(Destination.Settings.route) { SettingsScreen(preferencesRepository = preferences) }
    composable(ROUTE_SLIDES) { SlidesScreen() }
    composable(ROUTE_DEMOS) { DemosScreen() }
}
