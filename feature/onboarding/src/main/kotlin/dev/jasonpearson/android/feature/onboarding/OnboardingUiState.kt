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

/** Pure, framework-free UI state for the onboarding flow. */
data class OnboardingUiState(val stepIndex: Int = 0, val steps: List<String> = DefaultSteps) {
    val currentStep: String
        get() = steps[stepIndex]

    val isFirstStep: Boolean
        get() = stepIndex == 0

    val isLastStep: Boolean
        get() = stepIndex == steps.lastIndex

    /** Progress through the flow in the range 0.0..1.0. */
    val progress: Float
        get() = (stepIndex + 1).toFloat() / steps.size
}

/** The default copy shown across the onboarding steps. */
val DefaultSteps: List<String> =
    listOf("Welcome aboard", "Personalize your feed", "Enable notifications", "You're all set")

/** The state a freshly launched onboarding flow starts from. */
fun initialOnboardingState(): OnboardingUiState = OnboardingUiState()

/** Pure reducers that move through the onboarding [OnboardingUiState]. */
object OnboardingReducer {
    fun next(state: OnboardingUiState): OnboardingUiState =
        state.copy(stepIndex = (state.stepIndex + 1).coerceAtMost(state.steps.lastIndex))

    fun back(state: OnboardingUiState): OnboardingUiState =
        state.copy(stepIndex = (state.stepIndex - 1).coerceAtLeast(0))
}
