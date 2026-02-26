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

import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface ApplicationModule {

    @Qualifier @Retention(BINARY) annotation class Initializers

    @Qualifier @Retention(BINARY) annotation class AsyncInitializers

    @Qualifier @Retention(BINARY) annotation class LazyDelegate

    @Qualifier @Retention(BINARY) annotation class PresenterScope

    companion object {

        @Provides @SingleIn(AppScope::class) fun provideClock(): Clock = Clock.System

        @PresenterScope
        @Provides
        @SingleIn(AppScope::class)
        fun providePresenterScope(backgroundScope: BackgroundAppCoroutineScope): CoroutineScope =
            backgroundScope
    }
}

/**
 * Module for app initialization hooks. Provides sets of initializer functions that run at app
 * startup.
 */
@ContributesTo(AppScope::class)
interface InitializersModule {

    companion object {
        /**
         * Placeholder for synchronous initializers. In a real app, you would contribute actual
         * initializers here using @IntoSet with @Initializers qualifier.
         */
        @ApplicationModule.Initializers
        @Provides
        fun provideInitializers(): Set<() -> Unit> = emptySet()

        /**
         * Pre-initializes Dispatchers.Main off the main thread to avoid disk I/O on first access.
         * This is contributed to the async initializers set.
         */
        @ApplicationModule.AsyncInitializers
        @IntoSet
        @Provides
        fun mainDispatcherInit(): () -> Unit = {
            // This makes a call to disk, so initialize it off the main thread first... ironically
            Dispatchers.Main
        }
    }
}
