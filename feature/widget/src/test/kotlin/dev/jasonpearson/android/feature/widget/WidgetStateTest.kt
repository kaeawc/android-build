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
package dev.jasonpearson.android.feature.widget

import java.io.File
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WidgetStateTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun freshValueReplacesLastGood() {
        assertEquals(
            WidgetState.Content("fresh"),
            nextWidgetState(Result.success("fresh"), lastGood = "old"),
        )
    }

    @Test
    fun failureKeepsLastGoodContent() {
        assertEquals(
            WidgetState.Content("old"),
            nextWidgetState(Result.failure<String?>(IOException()), lastGood = "old"),
        )
    }

    @Test
    fun failureWithoutLastGoodIsError() {
        assertEquals(
            WidgetState.Error,
            nextWidgetState(Result.failure<String?>(IOException()), lastGood = null),
        )
    }

    @Test
    fun successfulEmptyFeedIsEmpty() {
        assertEquals(WidgetState.Empty, nextWidgetState(Result.success<String?>(null), "old"))
    }

    @Test
    fun snapshotRoundTripsLabelAndBytes() {
        val snapshots = WidgetSnapshotFile(File(temporaryFolder.root, "widgets/photo.snapshot"))
        val bytes = byteArrayOf(1, 2, 3, 4)

        snapshots.write("2024-09-16", bytes)

        val read = snapshots.read()!!
        assertEquals("2024-09-16", read.label)
        assertArrayEquals(bytes, read.bytes)
    }

    @Test
    fun snapshotWithoutLabelReadsNullLabel() {
        val snapshots = WidgetSnapshotFile(File(temporaryFolder.root, "photo.snapshot"))

        snapshots.write(null, byteArrayOf(9))

        assertNull(snapshots.read()!!.label)
    }

    @Test
    fun missingOrTruncatedSnapshotReadsNull() {
        val file = File(temporaryFolder.root, "photo.snapshot")
        val snapshots = WidgetSnapshotFile(file)
        assertNull(snapshots.read())

        file.writeBytes(byteArrayOf(0))

        assertNull(snapshots.read())
    }
}
