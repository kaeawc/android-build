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
package dev.jasonpearson.android.feature.settings

/** Pure, framework-free UI state for the settings screen. */
data class SettingsUiState(
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
) {
    val summary: String
        get() {
            val theme = if (darkTheme) "Dark" else "Light"
            val notifications = if (notificationsEnabled) "on" else "off"
            return "$theme theme, notifications $notifications"
        }
}

/** The state a settings screen starts from before preferences are loaded. */
fun initialSettingsState(): SettingsUiState = SettingsUiState()

/** Pure reducers that transform [SettingsUiState] in response to user intent. */
object SettingsReducer {
    fun toggleDarkTheme(state: SettingsUiState): SettingsUiState =
        state.copy(darkTheme = !state.darkTheme)

    fun toggleNotifications(state: SettingsUiState): SettingsUiState =
        state.copy(notificationsEnabled = !state.notificationsEnabled)
}
