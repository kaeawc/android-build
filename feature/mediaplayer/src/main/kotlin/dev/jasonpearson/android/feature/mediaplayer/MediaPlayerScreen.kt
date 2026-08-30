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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.MediaItem
import dev.jasonpearson.android.foundation.designsystem.components.PrimaryButton
import dev.jasonpearson.android.foundation.designsystem.theme.AndroidBuildTheme
import dev.jasonpearson.android.subsystem.storage.SessionRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * The media player screen. Transport state is driven by the pure [MediaPlayerReducer] and each play
 * is recorded through [session] so the last played track survives process death.
 */
@Composable
fun MediaPlayerScreen(
    session: SessionRepository,
    item: MediaItem = SampleTrack,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(initialMediaPlayerState()) }
    val scope = rememberCoroutineScope()

    AndroidBuildTheme {
        Column(modifier = modifier.padding(16.dp)) {
            Text(text = "Now playing")
            Text(text = state.title.ifBlank { item.title })
            Text(
                text =
                    "${state.positionSeconds}s ${if (state.isPlaying) "(playing)" else "(paused)"}"
            )

            PrimaryButton(
                text = if (state.isPlaying) "Pause" else "Play",
                onClick = {
                    state =
                        if (state.isPlaying) {
                            MediaPlayerReducer.pause(state)
                        } else {
                            scope.launch { session.recordPlayback(item) }
                            MediaPlayerReducer.play(state, item.id, item.title)
                        }
                },
            )
        }
    }
}

/** A stand-in track used when the host does not supply one. */
val SampleTrack: MediaItem =
    MediaItem(
        id = "track-1",
        title = "Elephant March",
        artist = "The Herd",
        duration = 214.seconds,
        releaseDate = LocalDate(2024, 1, 1),
    )
