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
package dev.jasonpearson.android

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.UntaggedSocketViolation
import android.util.Log
import dev.jasonpearson.android.di.ApplicationComponent
import dev.jasonpearson.android.di.ApplicationModule
import dev.jasonpearson.android.di.BackgroundAppCoroutineScope
import dev.jasonpearson.android.di.DaggerApplicationComponent
import dev.jasonpearson.android.di.DaggerSet
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.launch

private typealias InitializerFunction = () -> @JvmSuppressWildcards Unit

class App : Application() {

    companion object {
        internal val TAG = App::class.simpleName!!
    }

    lateinit var appComponent: ApplicationComponent

    @Inject
    fun asyncInits(
        scope: BackgroundAppCoroutineScope,
        @ApplicationModule.AsyncInitializers asyncInitializers: DaggerSet<InitializerFunction>,
    ) {
        scope.launch {
            // TODO - run these in parallel?
            asyncInitializers.forEach { it() }
        }
    }

    @Inject
    fun inits(@ApplicationModule.Initializers initializers: DaggerSet<InitializerFunction>) {
        initializers.forEach { it() }
    }

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerApplicationComponent.factory().create(this).apply { inject(this@App) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                        if (violation is UntaggedSocketViolation) {
                            // This is a known issue with Flipper
                        } else if (
                            violation is DiskReadViolation &&
                                violation.stackTraceToString().contains("CustomTabsConnection")
                        ) {
                            // This is a known issue with Chrome Custom Tabs
                        } else {
                            Log.e(TAG, violation.toString())
                        }
                    }
                    .build()
            )
        }
    }
}
