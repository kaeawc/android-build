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
package dev.jasonpearson.android.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiStateTest {

    @Test
    fun `starts on the first step`() {
        val state = initialOnboardingState()
        assertTrue(state.isFirstStep)
        assertFalse(state.isLastStep)
        assertEquals(0, state.stepIndex)
    }

    @Test
    fun `next advances one step`() {
        val state = OnboardingReducer.next(initialOnboardingState())
        assertEquals(1, state.stepIndex)
    }

    @Test
    fun `next is clamped at the final step`() {
        var state = initialOnboardingState()
        repeat(state.steps.size + 3) { state = OnboardingReducer.next(state) }
        assertTrue(state.isLastStep)
        assertEquals(state.steps.lastIndex, state.stepIndex)
    }

    @Test
    fun `back is clamped at the first step`() {
        val state = OnboardingReducer.back(initialOnboardingState())
        assertTrue(state.isFirstStep)
    }

    @Test
    fun `progress reaches one on the last step`() {
        var state = initialOnboardingState()
        while (!state.isLastStep) state = OnboardingReducer.next(state)
        assertEquals(1.0f, state.progress, 0.0001f)
    }
}
