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
package dev.jasonpearson.android.debug

import android.os.Build
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.UntaggedSocketViolation
import android.util.Log
import dev.jasonpearson.android.di.AppScope
import dev.jasonpearson.android.di.ApplicationModule
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import java.util.concurrent.Executors

private const val TAG = "StrictModeModule"

/**
 * Contributes StrictMode setup to the async initializers set.
 *
 * StrictMode is configured to detect all violations and log them. Known false positives from
 * Flipper and Chrome Custom Tabs are suppressed.
 */
@ContributesTo(AppScope::class)
interface StrictModeModule {

    companion object {

        @ApplicationModule.AsyncInitializers
        @IntoSet
        @Provides
        fun strictModeInit(): () -> Unit = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            when {
                                violation is UntaggedSocketViolation -> {
                                    // Known issue with Flipper - ignore
                                }

                                violation is DiskReadViolation &&
                                    violation.stackTraceToString()
                                        .contains("CustomTabsConnection") -> {
                                    // Known issue with Chrome Custom Tabs - ignore
                                }

                                else -> {
                                    Log.e(TAG, "StrictMode violation detected", violation)
                                }
                            }
                        }
                        .build()
                )
            }
        }
    }
}
