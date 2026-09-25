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
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import dev.jasonpearson.android.core.network.NetworkResult
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalGlanceApi::class)
class PhotoOfTheDayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val photo =
            try {
                val graph = (context.applicationContext as? WidgetGraphProvider)?.widgetGraph
                when (val result = graph?.photographyRepository?.photos()) {
                    is NetworkResult.Success ->
                        result.data
                            .takeIf { it.isNotEmpty() }
                            ?.let { photos ->
                                photos[photoIndexFor(LocalDate.now().dayOfYear, photos.size)]
                            }
                    else -> null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        val bitmap = photo?.let {
            try {
                loadBitmap(it.thumbUrl)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
        val photographyIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("jasonpearsondev://photography"))
                .setPackage(context.packageName)

        provideContent {
            GlanceTheme {
                if (bitmap != null) {
                    Box(
                        modifier =
                            GlanceModifier.fillMaxSize()
                                .clickable(actionStartActivity(photographyIntent)),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = "Photo by Jason Pearson",
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxSize(),
                        )
                        photo?.takenOn?.let { date ->
                            Text(date, modifier = GlanceModifier.padding(8.dp), maxLines = 1)
                        }
                    }
                } else {
                    val modifier =
                        if (photo != null) {
                            GlanceModifier.fillMaxSize()
                                .clickable(actionStartActivity(photographyIntent))
                                .padding(12.dp)
                        } else {
                            GlanceModifier.fillMaxSize().padding(12.dp)
                        }
                    Box(modifier = modifier, contentAlignment = Alignment.Center) {
                        Text("Photo unavailable")
                    }
                }
            }
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
