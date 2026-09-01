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
package dev.jasonpearson.android.feature.home

import dev.jasonpearson.android.subsystem.experimentation.Experiment
import dev.jasonpearson.android.subsystem.experimentation.Treatment

/** Pure, framework-free UI state for the home screen. */
data class HomeUiState(val greeting: String, val subtitle: String)

/** The experiments the home screen resolves against. */
object HomeExperiments {
    val Greeting: Experiment = Experiment(id = "home_greeting")
}

/** Maps an assigned [treatment] to the copy the home screen should render. */
fun homeStateFor(treatment: Treatment): HomeUiState =
    when (treatment) {
        Treatment.CONTROL ->
            HomeUiState(greeting = "Welcome back", subtitle = "Here is what's new today.")
        Treatment.VARIANT ->
            HomeUiState(greeting = "Hey there!", subtitle = "Jump back into the action.")
    }
