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
package dev.jasonpearson.android.subsystem.storage

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.jasonpearson.android.core.di.StorageDirectory
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
public class FileContentCache(@StorageDirectory directory: File, private val clock: Clock) :
    ContentCache {
    private val cacheDirectory = File(directory, "cache")

    override suspend fun get(key: String): CachedEntry? =
        withContext(Dispatchers.IO) {
            val file = fileFor(key)
            if (!file.isFile) return@withContext null
            try {
                val content = file.readText()
                val newline = content.indexOf('\n')
                if (newline < 0) return@withContext null
                val savedAt =
                    content.substring(0, newline).toLongOrNull() ?: return@withContext null
                CachedEntry(content.substring(newline + 1), Instant.fromEpochMilliseconds(savedAt))
            } catch (_: IOException) {
                null
            }
        }

    override suspend fun put(key: String, value: String): Unit =
        withContext(Dispatchers.IO) {
            // Idempotent and race-safe: concurrent first-launch puts may both create the directory.
            Files.createDirectories(cacheDirectory.toPath())
            val temporary = Files.createTempFile(cacheDirectory.toPath(), "entry-", ".tmp")
            try {
                Files.writeString(temporary, "${clock.now().toEpochMilliseconds()}\n$value")
                Files.move(
                    temporary,
                    fileFor(key).toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }

    override suspend fun clear(): Unit =
        withContext(Dispatchers.IO) {
            // deleteIfExists: a concurrent put may move or remove its temp file mid-iteration.
            cacheDirectory.listFiles()?.forEach { Files.deleteIfExists(it.toPath()) }
            Unit
        }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        val hexDigits = "0123456789abcdef"
        val name =
            buildString(digest.size * 2 + 5) {
                for (byte in digest) {
                    val value = byte.toInt() and 0xff
                    append(hexDigits[value ushr 4])
                    append(hexDigits[value and 0x0f])
                }
                append(".json")
            }
        return File(cacheDirectory, name)
    }
}
