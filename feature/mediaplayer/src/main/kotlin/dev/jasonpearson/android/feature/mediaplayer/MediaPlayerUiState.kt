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
package dev.jasonpearson.android.feature.mediaplayer

/** Pure, framework-free UI state for the media player screen. */
data class MediaPlayerUiState(
    val trackId: String? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val positionSeconds: Int = 0,
)

/** The state a media player starts from with nothing loaded. */
fun initialMediaPlayerState(): MediaPlayerUiState = MediaPlayerUiState()

/** Pure reducers that transform [MediaPlayerUiState] in response to transport controls. */
object MediaPlayerReducer {
    fun play(state: MediaPlayerUiState, trackId: String, title: String): MediaPlayerUiState {
        val sameTrack = state.trackId == trackId
        return state.copy(
            trackId = trackId,
            title = title,
            isPlaying = true,
            positionSeconds = if (sameTrack) state.positionSeconds else 0,
        )
    }

    fun pause(state: MediaPlayerUiState): MediaPlayerUiState = state.copy(isPlaying = false)

    fun advance(state: MediaPlayerUiState, seconds: Int): MediaPlayerUiState =
        if (state.isPlaying) {
            state.copy(positionSeconds = (state.positionSeconds + seconds).coerceAtLeast(0))
        } else {
            state
        }
}
