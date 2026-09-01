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
package dev.jasonpearson.android.feature.slides

/** Pure, framework-free UI state for a wrap-around slide carousel. */
data class SlidesUiState(val index: Int = 0, val slides: List<String> = emptyList()) {
    val current: String
        get() = slides[index]

    val position: String
        get() = "${index + 1} / ${slides.size}"
}

/** Builds the initial carousel state, guarding against an empty [slides] list. */
fun slidesStateFor(slides: List<String>): SlidesUiState {
    require(slides.isNotEmpty()) { "A slide carousel needs at least one slide." }
    return SlidesUiState(index = 0, slides = slides)
}

/** Pure reducers that page through the carousel, wrapping at either end. */
object SlidesReducer {
    fun next(state: SlidesUiState): SlidesUiState =
        state.copy(index = (state.index + 1) % state.slides.size)

    fun previous(state: SlidesUiState): SlidesUiState =
        state.copy(index = (state.index - 1 + state.slides.size) % state.slides.size)
}
