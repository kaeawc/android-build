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
import dev.jasonpearson.android.App
import dev.jasonpearson.android.resume.ResumePresenter
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/**
 * Application-level dependency graph using Metro DI.
 *
 * This graph is the root of the dependency tree and lives for the entire app lifecycle. All
 * dependencies contributed with `@ContributesTo(AppScope::class)` will be included here.
 *
 * Access the graph from any Context using:
 * ```
 * context.appGraph
 * ```
 */
@DependencyGraph(scope = AppScope::class)
@SingleIn(AppScope::class)
internal interface AppGraph {

    /** Injects dependencies into the Application class. Called during app initialization. */
    fun inject(application: App)

    /** Factory for creating the AppGraph. Metro generates the implementation of this interface. */
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }

    // Exposed dependencies for convenient access
    // Note: In a larger app, consider using subcomponents instead of exposing everything
    val resumePresenter: ResumePresenter
}
