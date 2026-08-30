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
package dev.jasonpearson.android.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.foundation.designsystem.components.PrimaryButton
import dev.jasonpearson.android.foundation.designsystem.theme.AndroidBuildTheme
import dev.jasonpearson.android.subsystem.storage.UserPreferencesRepository
import kotlinx.coroutines.launch

/**
 * The onboarding flow. Steps are advanced by the pure [OnboardingReducer]; finishing the last step
 * persists completion through [preferencesRepository].
 */
@Composable
fun OnboardingScreen(
    preferencesRepository: UserPreferencesRepository,
    onFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(initialOnboardingState()) }
    val scope = rememberCoroutineScope()

    AndroidBuildTheme {
        Column(modifier = modifier.padding(16.dp)) {
            Text(text = state.currentStep)
            Text(text = "Step ${state.stepIndex + 1} of ${state.steps.size}")

            PrimaryButton(
                text = if (state.isLastStep) "Get started" else "Next",
                onClick = {
                    if (state.isLastStep) {
                        scope.launch {
                            preferencesRepository.setOnboardingComplete(true)
                            onFinished()
                        }
                    } else {
                        state = OnboardingReducer.next(state)
                    }
                },
            )
        }
    }
}
