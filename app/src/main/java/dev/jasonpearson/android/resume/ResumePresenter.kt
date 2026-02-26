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
package dev.jasonpearson.android.resume

import dev.jasonpearson.android.di.ApplicationModule
import dev.jasonpearson.android.di.AppScope
import dev.jasonpearson.android.di.SingleIn
import dev.jasonpearson.android.timer.TimerProvider
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds UI state for the resume screen. Scoped to the application lifetime so navigation
 * back to the resume screen sees the already-loaded items.
 *
 * In tests, construct directly: `ResumePresenter(FakeTimer(), testScope)`.
 */
@SingleIn(AppScope::class)
class ResumePresenter @Inject constructor(
    private val timerProvider: TimerProvider,
    @ApplicationModule.PresenterScope private val scope: CoroutineScope,
) {
    private val _items = MutableStateFlow<List<ResumeItem>>(emptyList())

    /** Resume items to display. Empty until after [INITIAL_DELAY_MS] has elapsed. */
    val items: StateFlow<List<ResumeItem>> = _items.asStateFlow()

    init {
        scope.launch {
            timerProvider.delay(INITIAL_DELAY_MS)
            _items.value = resumeItems
        }
    }

    companion object {
        /** Delay before resume items appear, matching the original animation timing. */
        const val INITIAL_DELAY_MS = 200L
    }
}
