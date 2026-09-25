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

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** What a widget renders: loading, fresh or last-good content, an empty feed, or an error. */
internal sealed interface WidgetState<out T> {
    data object Loading : WidgetState<Nothing>

    data class Content<T>(val value: T) : WidgetState<T>

    data object Empty : WidgetState<Nothing>

    data object Error : WidgetState<Nothing>
}

/**
 * Resolves a refresh: a fresh value wins, a successful empty feed is [WidgetState.Empty], and a
 * failure keeps [lastGood] on screen rather than wiping it, falling back to [WidgetState.Error].
 */
internal fun <T : Any> nextWidgetState(fetched: Result<T?>, lastGood: T?): WidgetState<T> =
    fetched.fold(
        onSuccess = { value ->
            if (value != null) WidgetState.Content(value) else WidgetState.Empty
        },
        onFailure = { lastGood?.let { WidgetState.Content(it) } ?: WidgetState.Error },
    )

/** An explicit in-app deep link (`jasonpearsondev://<path>`), scoped to this package. */
internal fun deepLinkIntent(context: Context, path: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("jasonpearsondev://$path")).setPackage(context.packageName)

/**
 * A single-file snapshot of a widget's last good image plus a short label, written atomically so
 * concurrent widget sessions never observe a half-written file.
 */
internal class WidgetSnapshotFile(private val file: File) {
    class Snapshot(val label: String?, val bytes: ByteArray)

    fun read(): Snapshot? =
        try {
            if (!file.isFile) null
            else
                DataInputStream(file.inputStream().buffered()).use { input ->
                    val label = input.readUTF().takeIf { it.isNotEmpty() }
                    val bytes = input.readBytes()
                    if (bytes.isEmpty()) null else Snapshot(label, bytes)
                }
        } catch (_: IOException) {
            null
        }

    fun write(label: String?, bytes: ByteArray) {
        val directory = file.absoluteFile.parentFile ?: throw IOException("No parent for $file")
        Files.createDirectories(directory.toPath())
        val encoded =
            ByteArrayOutputStream(bytes.size + 64).also { buffer ->
                DataOutputStream(buffer).use { output ->
                    output.writeUTF(label.orEmpty())
                    output.write(bytes)
                }
            }
        val temporary = Files.createTempFile(directory.toPath(), "snapshot-", ".tmp")
        try {
            Files.write(temporary, encoded.toByteArray())
            Files.move(
                temporary,
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
