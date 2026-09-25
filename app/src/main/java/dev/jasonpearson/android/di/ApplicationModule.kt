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
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.GhostApiUrl
import dev.jasonpearson.android.core.di.GhostContentKey
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.di.StorageDirectory
import dev.jasonpearson.android.core.network.DebugBuild
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import java.io.File
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope

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
