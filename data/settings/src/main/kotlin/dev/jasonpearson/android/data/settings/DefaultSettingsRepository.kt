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

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.subsystem.storage.KeyValueStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
public class DefaultSettingsRepository @Inject constructor(private val store: KeyValueStore) :
    SettingsRepository {
    override val settings: Flow<AppSettings> =
        combine(
            store.observe(THEME_MODE_KEY),
            store.observe(DYNAMIC_COLOR_KEY),
            store.observe(ANALYTICS_ENABLED_KEY),
        ) { themeMode, dynamicColor, analyticsEnabled ->
            AppSettings(
                themeMode = parseThemeMode(themeMode),
                dynamicColor = parseBoolean(dynamicColor, AppSettings().dynamicColor),
                analyticsEnabled = parseBoolean(analyticsEnabled, AppSettings().analyticsEnabled),
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.put(THEME_MODE_KEY, mode.name)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        store.put(DYNAMIC_COLOR_KEY, enabled.toString())
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        store.put(ANALYTICS_ENABLED_KEY, enabled.toString())
    }

    private fun parseThemeMode(raw: String?): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == raw } ?: AppSettings().themeMode

    private fun parseBoolean(raw: String?, default: Boolean): Boolean =
        raw?.toBooleanStrictOrNull() ?: default

    private companion object {
        const val THEME_MODE_KEY = "settings.theme_mode"
        const val DYNAMIC_COLOR_KEY = "settings.dynamic_color"
        const val ANALYTICS_ENABLED_KEY = "settings.analytics_enabled"
    }
}
