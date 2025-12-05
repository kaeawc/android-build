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
import android.content.Context
import android.content.ContextWrapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dev.zacsweers.metro.ContributesTo
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
@Module
abstract class ApplicationModule {

    @Qualifier @Retention(BINARY) annotation class Initializers

    @Qualifier @Retention(BINARY) annotation class AsyncInitializers

    @Qualifier @Retention(BINARY) annotation class LazyDelegate

    /** Provides initializers for app startup. */
    @Initializers @Multibinds abstract fun initializers(): Set<() -> Unit>

    /** Provides initializers for app startup that can be initialized async. */
    @AsyncInitializers @Multibinds abstract fun asyncInitializers(): Set<() -> Unit>

    @Binds
    @ApplicationContext
    @SingleIn(AppScope::class)
    abstract fun provideApplicationContext(real: Application): Context

    companion object {

        /**
         * This Context is only available for things that don't care what type of Context they need.
         *
         * Wrapped so no one can try to cast it as an Application.
         */
        @Provides
        @SingleIn(AppScope::class)
        internal fun provideGeneralUseContext(@ApplicationContext appContext: Context): Context =
            ContextWrapper(appContext)

        @AsyncInitializers
        @IntoSet
        @Provides
        fun mainDispatcherInit(): () -> Unit = {
            // This makes a call to disk, so initialize it off the main thread first... ironically
            Dispatchers.Main
        }

        @OptIn(ExperimentalTime::class)
        @Provides
        @SingleIn(AppScope::class)
        fun provideClock(): Clock = Clock.System
    }
}
