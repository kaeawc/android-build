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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.jasonpearson.android.core.network.NetworkResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PhotoSnapshot(val bitmap: Bitmap, val takenOn: String?)

@OptIn(ExperimentalGlanceApi::class)
class PhotoOfTheDayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Shared by every instance: they all show the same daily photo.
        val snapshotFile =
            WidgetSnapshotFile(File(context.cacheDir, "widgets/photo_of_the_day.snapshot"))
        val lastGood = withContext(Dispatchers.IO) { snapshotFile.read()?.toPhotoSnapshot() }
        val photographyIntent = deepLinkIntent(context, "photography")

        provideContent {
            // Show the last good photo immediately; only a first-ever load shows a spinner.
            var state by remember {
                mutableStateOf(lastGood?.let { WidgetState.Content(it) } ?: WidgetState.Loading)
            }
            LaunchedEffect(Unit) {
                state = nextWidgetState(fetchPhoto(context, snapshotFile), lastGood)
            }
            GlanceTheme { PhotoOfTheDayContent(state, photographyIntent) }
        }
    }

    private suspend fun fetchPhoto(
        context: Context,
        snapshotFile: WidgetSnapshotFile,
    ): Result<PhotoSnapshot?> =
        try {
            val graph =
                (context.applicationContext as? WidgetGraphProvider)?.widgetGraph
                    ?: throw IOException("Widget graph unavailable")
            when (val result = graph.photographyRepository.photos()) {
                is NetworkResult.Success -> {
                    val photos = result.data
                    if (photos.isEmpty()) {
                        Result.success(null)
                    } else {
                        val photo = photos[photoIndexFor(LocalDate.now().dayOfYear, photos.size)]
                        val bitmap = loadBitmap(photo.thumbUrl)
                        saveSnapshot(snapshotFile, bitmap, photo.takenOn)
                        Result.success(PhotoSnapshot(bitmap, photo.takenOn))
                    }
                }
                is NetworkResult.Failure -> Result.failure(IOException("Photos unavailable"))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun saveSnapshot(file: WidgetSnapshotFile, bitmap: Bitmap, takenOn: String?) {
        try {
            withContext(Dispatchers.IO) {
                val bytes = ByteArrayOutputStream()
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)) {
                    file.write(takenOn, bytes.toByteArray())
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Best-effort: failing to persist must not hide the fresh photo.
        }
    }

    private suspend fun loadBitmap(url: String): Bitmap =
        withContext(Dispatchers.IO) {
            // Bounded timeouts: provideGlance runs inside the receiver's goAsync window, and a
            // stalled CDN must not hang it (HttpURLConnection defaults to no timeout).
            val connection =
                URL(url).openConnection().apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
            val bytes = connection.getInputStream().use { it.readBytes() }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("Invalid photo dimensions")
            }
            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = inSampleSizeFor(bounds.outWidth, bounds.outHeight, 720)
                }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: throw IOException("Unable to decode photo")
        }
}

@Composable
private fun PhotoOfTheDayContent(state: WidgetState<PhotoSnapshot>, intent: Intent) {
    val modifier =
        GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity(intent))
    val textStyle = TextStyle(color = GlanceTheme.colors.onSurface)
    when (state) {
        is WidgetState.Content ->
            Box(modifier = modifier, contentAlignment = Alignment.BottomStart) {
                Image(
                    provider = ImageProvider(state.value.bitmap),
                    contentDescription = "Photo by Jason Pearson",
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize(),
                )
                state.value.takenOn?.let { date ->
                    Text(date, modifier = GlanceModifier.padding(8.dp), maxLines = 1)
                }
            }
        WidgetState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        WidgetState.Empty ->
            Box(modifier = modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                Text("No photos yet", style = textStyle)
            }
        WidgetState.Error ->
            Box(modifier = modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                Text("Photo unavailable", style = textStyle)
            }
    }
}

private fun WidgetSnapshotFile.Snapshot.toPhotoSnapshot(): PhotoSnapshot? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { PhotoSnapshot(it, label) }
