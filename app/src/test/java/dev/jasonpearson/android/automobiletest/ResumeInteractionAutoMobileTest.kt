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
package dev.jasonpearson.android.automobiletest

import dev.jasonpearson.automobile.junit.AutoMobilePlan
import dev.jasonpearson.automobile.junit.AutoMobileRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AutoMobile JUnit tests for verifying interactive swipe and tap gestures on the resume app UI.
 * Covers scrolling through the resume list, tapping skill chips, and navigating to the LinkedIn QR
 * screen and back.
 *
 * These tests run on the host JVM and communicate with the device via ADB using the AutoMobile
 * runner. Requires an ADB-connected device and the AutoMobile control proxy APK path set via
 * AUTOMOBILE_CTRL_PROXY_APK_PATH environment variable.
 *
 * Note: swipeOn uses autoTarget: false with direction: up to perform raw screen swipes on the
 * resume LazyColumn, since accessibility-based scroll actions are not exposed by this layout.
 */
@RunWith(AutoMobileRunner::class)
class ResumeInteractionAutoMobileTest {

    @Test
    fun `scroll to skills section and tap skill chips`() {
        val result = AutoMobilePlan(planPath = "test-plans/scroll-and-tap-skill.yaml").execute()

        assertTrue(result.success)
    }

    @Test
    fun `tap share button opens linkedin qr screen and back navigates home`() {
        val result = AutoMobilePlan(planPath = "test-plans/linkedin-navigation.yaml").execute()

        assertTrue(result.success)
    }

    @Test
    fun `scroll to education and talks sections verifies resume content`() {
        val result = AutoMobilePlan(planPath = "test-plans/scroll-to-education.yaml").execute()

        assertTrue(result.success)
    }
}
