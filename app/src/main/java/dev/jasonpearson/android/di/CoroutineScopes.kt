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

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

/**
 * Application-scoped CoroutineScope tied to the Main dispatcher.
 *
 * Use this for coroutines that need to run on the main thread throughout
 * the app lifecycle. This scope is automatically cancelled when the app
 * process is terminated.
 *
 * Useful for UI-related operations that span beyond a single screen.
 */
@SingleIn(AppScope::class)
class MainAppCoroutineScope @Inject constructor() : CoroutineScope by MainScope()

/**
 * Application-scoped CoroutineScope for background work.
 *
 * Use this for long-running background operations throughout the app lifecycle.
 * Uses Dispatchers.Default for CPU-intensive work and includes a SupervisorJob
 * to prevent failures in one coroutine from cancelling others.
 *
 * This scope is automatically cancelled when the app process is terminated.
 */
@SingleIn(AppScope::class)
class BackgroundAppCoroutineScope @Inject constructor() :
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
