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
package dev.jasonpearson.android.di

import android.app.Application
import android.util.Log
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.GhostApiUrl
import dev.jasonpearson.android.core.di.GhostContentKey
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.di.StorageDirectory
import dev.jasonpearson.android.core.network.DebugBuild
import dev.jasonpearson.android.data.settings.SettingsRepository
import dev.jasonpearson.android.subsystem.analytics.AnalyticsConsent
import dev.jasonpearson.android.subsystem.analytics.AnalyticsSink
import dev.jasonpearson.android.subsystem.experimentation.InstallId
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import java.io.File
import java.util.UUID
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@ContributesTo(AppScope::class)
interface ApplicationModule {

    @Qualifier @Retention(BINARY) annotation class LazyDelegate

    @Qualifier @Retention(BINARY) annotation class PresenterScope

    companion object {

        @Provides @SingleIn(AppScope::class) fun provideClock(): Clock = Clock.System

        @Provides
        @GhostApiUrl
        fun provideGhostApiUrl(): String = dev.jasonpearson.android.BuildConfig.GHOST_API_URL

        @Provides
        @GhostContentKey
        fun provideGhostContentKey(): String =
            dev.jasonpearson.android.BuildConfig.GHOST_CONTENT_API_KEY

        @Provides
        @StorageDirectory
        fun provideStorageDirectory(application: Application): File = application.filesDir

        /**
         * Backs analytics consent with the persisted opt-out. Consent stays unknown until the first
         * persisted value loads, so cold-start events wait for it instead of being dropped.
         */
        @Provides
        @SingleIn(AppScope::class)
        fun provideAnalyticsConsent(
            settings: SettingsRepository,
            scope: BackgroundAppCoroutineScope,
        ): AnalyticsConsent =
            AnalyticsConsent().also { consent ->
                settings.settings
                    .map { it.analyticsEnabled }
                    .distinctUntilChanged()
                    .onEach(consent::update)
                    .launchIn(scope)
            }

        @Provides
        fun provideAnalyticsSink(): AnalyticsSink = AnalyticsSink { event ->
            Log.d("Analytics", "${event.name} ${event.params}")
        }

        /**
         * A random per-install id (never a device identifier) used only for experiment bucketing.
         */
        @Provides
        @SingleIn(AppScope::class)
        @InstallId
        fun provideInstallId(@StorageDirectory directory: File): String {
            val file = File(directory, "install_id")
            return file.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
                ?: UUID.randomUUID().toString().also { file.writeText(it) }
        }

        @Provides
        @DebugBuild
        fun provideDebugBuild(): Boolean = dev.jasonpearson.android.BuildConfig.DEBUG

        @PresenterScope
        @Provides
        @SingleIn(AppScope::class)
        fun providePresenterScope(backgroundScope: BackgroundAppCoroutineScope): CoroutineScope =
            backgroundScope
    }
}
