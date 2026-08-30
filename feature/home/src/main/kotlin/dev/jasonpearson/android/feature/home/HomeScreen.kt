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
package dev.jasonpearson.android.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.foundation.designsystem.components.PrimaryButton
import dev.jasonpearson.android.foundation.designsystem.theme.AndroidBuildTheme
import dev.jasonpearson.android.subsystem.analytics.AnalyticsClient
import dev.jasonpearson.android.subsystem.analytics.AnalyticsEvent
import dev.jasonpearson.android.subsystem.experimentation.ExperimentRepository
import dev.jasonpearson.android.subsystem.experimentation.InMemoryExperimentRepository

/**
 * The home screen. The greeting copy is chosen by an [experiments] assignment and the view is
 * reported to [analytics] once per composition.
 */
@Composable
fun HomeScreen(
    analytics: AnalyticsClient,
    experiments: ExperimentRepository = InMemoryExperimentRepository(),
    onOpenDiscover: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val treatment = experiments.treatmentFor(HomeExperiments.Greeting)
    val state = remember(treatment) { homeStateFor(treatment) }

    LaunchedEffect(state.greeting) {
        analytics.track(
            AnalyticsEvent(name = "home_viewed", params = mapOf("greeting" to state.greeting))
        )
    }

    AndroidBuildTheme {
        Column(modifier = modifier.padding(16.dp)) {
            Text(text = state.greeting)
            Text(text = state.subtitle)
            PrimaryButton(text = "Discover", onClick = onOpenDiscover)
        }
    }
}
