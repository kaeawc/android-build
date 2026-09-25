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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android.data.settings

import dev.jasonpearson.android.subsystem.storage.InMemoryKeyValueStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultSettingsRepositoryTest {
    @Test
    fun `empty store emits defaults`() = runTest {
        val repository = DefaultSettingsRepository(InMemoryKeyValueStore())

        assertEquals(AppSettings(), repository.settings.first())
    }

    @Test
    fun `setters round trip`() = runTest {
        val repository = DefaultSettingsRepository(InMemoryKeyValueStore())

        repository.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.settings.first().themeMode)

        repository.setDynamicColor(true)
        assertEquals(true, repository.settings.first().dynamicColor)

        repository.setAnalyticsEnabled(false)
        assertEquals(false, repository.settings.first().analyticsEnabled)
    }

    @Test
    fun `invalid values fall back to defaults`() = runTest {
        val store =
            InMemoryKeyValueStore(
                mapOf(
                    "settings.theme_mode" to "not-a-real-mode",
                    "settings.dynamic_color" to "not-a-bool",
                    "settings.analytics_enabled" to "not-a-bool",
                )
            )
        val repository = DefaultSettingsRepository(store)

        assertEquals(AppSettings(), repository.settings.first())
    }
}
