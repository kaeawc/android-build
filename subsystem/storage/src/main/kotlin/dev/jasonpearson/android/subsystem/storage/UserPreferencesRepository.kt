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
package dev.jasonpearson.android.subsystem.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User-facing preferences persisted through the [KeyValueStore]. */
data class UserPreferences(val darkTheme: Boolean = false, val onboardingComplete: Boolean = false)

/** Reads and writes [UserPreferences], mapping the typed model to/from string keys. */
class UserPreferencesRepository(private val store: KeyValueStore) {

    val preferences: Flow<UserPreferences> = store.let { s ->
        s.observe(KEY_DARK_THEME).map { dark ->
            UserPreferences(
                darkTheme = dark.toBoolean(),
                onboardingComplete = s.get(KEY_ONBOARDING_COMPLETE).toBoolean(),
            )
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) = store.put(KEY_DARK_THEME, enabled.toString())

    suspend fun setOnboardingComplete(complete: Boolean) =
        store.put(KEY_ONBOARDING_COMPLETE, complete.toString())

    private companion object {
        const val KEY_DARK_THEME = "pref.dark_theme"
        const val KEY_ONBOARDING_COMPLETE = "pref.onboarding_complete"
    }
}
