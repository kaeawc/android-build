/*
 * MIT License
 *
 * Copyright (c) 2023 Jason Pearson
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
package dev.jasonpearson.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jasonpearson.android.di.appGraph
import dev.jasonpearson.android.navigation.featureGraph
import dev.jasonpearson.android.resume.ResumePresenter
import dev.jasonpearson.android.resume.ui.LinkedInQRScreen
import dev.jasonpearson.android.resume.ui.ResumeApp
import dev.jasonpearson.android.subsystem.analytics.RecordingAnalyticsClient
import dev.jasonpearson.android.subsystem.experimentation.InMemoryExperimentRepository
import dev.jasonpearson.android.subsystem.storage.InMemoryKeyValueStore
import dev.jasonpearson.android.subsystem.storage.SessionRepository
import dev.jasonpearson.android.subsystem.storage.UserPreferencesRepository
import dev.jasonpearson.android.ui.theme.AndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val presenter = appGraph.resumePresenter
        setContent { AndroidTheme { ResumeNavigation(presenter = presenter) } }
    }
}

@Composable
fun ResumeNavigation(presenter: ResumePresenter) {
    val navController = rememberNavController()

    // Lightweight in-memory subsystem instances shared by the feature screens. These are
    // constructed here (rather than through the Metro graph) to keep this wiring self-contained;
    // the app still exercises the full :app -> :feature -> :subsystem dependency chain.
    val store = remember { InMemoryKeyValueStore() }
    val analytics = remember { RecordingAnalyticsClient() }
    val preferences = remember { UserPreferencesRepository(store) }
    val session = remember { SessionRepository(store) }
    val experiments = remember { InMemoryExperimentRepository() }

    // Launch destination stays "resume" so the existing resume UI and its UI tests are unaffected;
    // the feature screens are additional destinations reachable by navigation.
    NavHost(navController = navController, startDestination = "resume") {
        composable("resume") {
            ResumeApp(
                presenter = presenter,
                onShareLinkedIn = { navController.navigate("linkedin_qr") },
            )
        }
        composable("linkedin_qr") { LinkedInQRScreen(onBack = { navController.popBackStack() }) }
        featureGraph(navController, analytics, preferences, session, experiments)
    }
}
