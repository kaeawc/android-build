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
package dev.jasonpearson.android.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {

    @Test
    fun `initial state is light theme with notifications on`() {
        val state = initialSettingsState()
        assertFalse(state.darkTheme)
        assertTrue(state.notificationsEnabled)
    }

    @Test
    fun `toggling dark theme flips only the theme`() {
        val toggled = SettingsReducer.toggleDarkTheme(initialSettingsState())
        assertTrue(toggled.darkTheme)
        assertTrue(toggled.notificationsEnabled)
    }

    @Test
    fun `toggling dark theme twice is a no-op`() {
        val original = initialSettingsState()
        val roundTripped =
            SettingsReducer.toggleDarkTheme(SettingsReducer.toggleDarkTheme(original))
        assertEquals(original, roundTripped)
    }

    @Test
    fun `summary reflects the current state`() {
        val state = SettingsUiState(darkTheme = true, notificationsEnabled = false)
        assertEquals("Dark theme, notifications off", state.summary)
    }
}
