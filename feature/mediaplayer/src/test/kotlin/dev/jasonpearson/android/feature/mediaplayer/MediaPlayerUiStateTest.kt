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
package dev.jasonpearson.android.feature.mediaplayer

import dev.jasonpearson.android.core.model.MediaItem
import dev.jasonpearson.android.subsystem.storage.InMemoryKeyValueStore
import dev.jasonpearson.android.subsystem.storage.SessionRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlayerUiStateTest {

    @Test
    fun `playing a track marks it playing and resets position for a new track`() {
        val playing =
            MediaPlayerReducer.play(initialMediaPlayerState(), "track-1", "Elephant March")
        assertTrue(playing.isPlaying)
        assertEquals("track-1", playing.trackId)
        assertEquals(0, playing.positionSeconds)
    }

    @Test
    fun `advance only accrues while playing`() {
        val paused = initialMediaPlayerState()
        assertEquals(0, MediaPlayerReducer.advance(paused, 10).positionSeconds)

        val playing = MediaPlayerReducer.play(paused, "track-1", "Elephant March")
        assertEquals(10, MediaPlayerReducer.advance(playing, 10).positionSeconds)
    }

    @Test
    fun `resuming the same track keeps its position`() {
        val played = MediaPlayerReducer.play(initialMediaPlayerState(), "track-1", "Elephant March")
        val advanced = MediaPlayerReducer.advance(played, 30)
        val paused = MediaPlayerReducer.pause(advanced)
        val resumed = MediaPlayerReducer.play(paused, "track-1", "Elephant March")
        assertFalse(paused.isPlaying)
        assertEquals(30, resumed.positionSeconds)
    }

    @Test
    fun `recording playback persists the last played id`() = runTest {
        val session = SessionRepository(InMemoryKeyValueStore())
        val item =
            MediaItem(
                id = "track-1",
                title = "Elephant March",
                artist = "The Herd",
                duration = 214.seconds,
                releaseDate = LocalDate(2024, 1, 1),
            )
        session.recordPlayback(item)
        assertEquals("track-1", session.lastPlayedId.first())
    }
}
