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
package dev.jasonpearson.android.resume

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeDataTest {

    @Test
    fun `resumeItems is not empty`() {
        assertFalse(resumeItems.isEmpty())
    }

    @Test
    fun `resumeItems contains at least one Profile`() {
        assertTrue(resumeItems.any { it is ResumeItem.Profile })
    }

    @Test
    fun `resumeItems contains at least one Experience`() {
        assertTrue(resumeItems.any { it is ResumeItem.Experience })
    }

    @Test
    fun `resumeItems contains at least one Skills`() {
        assertTrue(resumeItems.any { it is ResumeItem.Skills })
    }

    @Test
    fun `resumeItems contains at least one Education`() {
        assertTrue(resumeItems.any { it is ResumeItem.Education })
    }

    @Test
    fun `resumeItems contains at least one Talks`() {
        assertTrue(resumeItems.any { it is ResumeItem.Talks })
    }

    @Test
    fun `all Experience items have non-empty responsibilities`() {
        val experiences = resumeItems.filterIsInstance<ResumeItem.Experience>()
        assertTrue(experiences.all { it.responsibilities.isNotEmpty() })
    }

    @Test
    fun `all Talks items have non-empty talks list`() {
        val talksItems = resumeItems.filterIsInstance<ResumeItem.Talks>()
        assertTrue(talksItems.all { it.talks.isNotEmpty() })
    }
}
