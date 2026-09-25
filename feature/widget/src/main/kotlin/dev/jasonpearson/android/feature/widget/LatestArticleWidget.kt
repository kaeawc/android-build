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
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.network.NetworkResult
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalGlanceApi::class)
class LatestArticleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val article =
            try {
                val graph = (context.applicationContext as? WidgetGraphProvider)?.widgetGraph
                when (val result = graph?.articlesRepository?.articles()) {
                    is NetworkResult.Success ->
                        result.data
                            .sortedWith(compareByDescending<Article> { it.publishedAt })
                            .firstOrNull()
                    else -> null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        val metadata = article?.let {
            listOfNotNull(
                    it.publishedAt
                        ?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ?.date
                        ?.toString(),
                    it.readingTimeMinutes?.let { minutes -> "$minutes min read" },
                )
                .joinToString(" · ")
        }
        val articleIntent = article?.let {
            Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("jasonpearsondev://article/${Uri.encode(it.slug)}"),
                )
                .setPackage(context.packageName)
        }
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        provideContent {
            GlanceTheme {
                if (article != null && articleIntent != null) {
                    Column(
                        modifier =
                            GlanceModifier.fillMaxSize()
                                .clickable(actionStartActivity(articleIntent))
                                .padding(12.dp)
                    ) {
                        Text("Jason Pearson · Latest")
                        Spacer(GlanceModifier.height(8.dp))
                        Text(
                            article.title,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            maxLines = 3,
                        )
                        if (!metadata.isNullOrEmpty()) {
                            Spacer(GlanceModifier.height(4.dp))
                            Text(metadata, maxLines = 1)
                        }
                        article.excerpt?.let { excerpt ->
                            Spacer(GlanceModifier.height(4.dp))
                            Text(excerpt, maxLines = 2)
                        }
                    }
                } else {
                    val modifier =
                        if (launcherIntent != null) {
                            GlanceModifier.fillMaxSize()
                                .clickable(actionStartActivity(launcherIntent))
                                .padding(12.dp)
                        } else {
                            GlanceModifier.fillMaxSize().padding(12.dp)
                        }
                    Column(modifier = modifier) { Text("Open the app to load articles") }
                }
            }
        }
    }
}
