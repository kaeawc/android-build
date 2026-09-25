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
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The few strings the widget renders, persisted in Glance state as the last good content. */
internal data class LatestArticleSnapshot(
    val slug: String,
    val title: String,
    val metadata: String?,
    val excerpt: String?,
)

@OptIn(ExperimentalGlanceApi::class)
class LatestArticleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lastGood =
            try {
                getAppWidgetState<Preferences>(context, id).toLatestArticleSnapshot()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

        provideContent {
            // Show the last good article immediately; only a first-ever load shows a spinner.
            var state by remember {
                mutableStateOf(lastGood?.let { WidgetState.Content(it) } ?: WidgetState.Loading)
            }
            LaunchedEffect(Unit) {
                val fetched = fetchLatest(context)
                fetched.getOrNull()?.let { fresh ->
                    try {
                        updateAppWidgetState(context, id) { it.putLatestArticle(fresh) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Best-effort: failing to persist must not hide fresh content.
                    }
                }
                state = nextWidgetState(fetched, lastGood)
            }
            GlanceTheme { LatestArticleContent(context, state) }
        }
    }

    private suspend fun fetchLatest(context: Context): Result<LatestArticleSnapshot?> =
        try {
            val graph =
                (context.applicationContext as? WidgetGraphProvider)?.widgetGraph
                    ?: throw IOException("Widget graph unavailable")
            when (val result = graph.articlesRepository.articles()) {
                is NetworkResult.Success ->
                    Result.success(
                        result.data
                            .sortedWith(compareByDescending<Article> { it.publishedAt })
                            .firstOrNull()
                            ?.toSnapshot()
                    )
                is NetworkResult.Failure -> Result.failure(IOException("Articles unavailable"))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
}

@Composable
private fun LatestArticleContent(context: Context, state: WidgetState<LatestArticleSnapshot>) {
    val path =
        if (state is WidgetState.Content) "article/${Uri.encode(state.value.slug)}" else "articles"
    val modifier =
        GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity(deepLinkIntent(context, path)))
            .padding(12.dp)
    val textStyle = TextStyle(color = GlanceTheme.colors.onSurface)
    when (state) {
        is WidgetState.Content -> {
            val article = state.value
            Column(modifier = modifier) {
                Text("Jason Pearson · Latest", style = textStyle)
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    article.title,
                    style = textStyle.copy(fontWeight = FontWeight.Bold),
                    maxLines = 3,
                )
                if (!article.metadata.isNullOrEmpty()) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(article.metadata, style = textStyle, maxLines = 1)
                }
                article.excerpt?.let { excerpt ->
                    Spacer(GlanceModifier.height(4.dp))
                    Text(excerpt, style = textStyle, maxLines = 2)
                }
            }
        }
        WidgetState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        WidgetState.Empty ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text("No articles yet", style = textStyle)
            }
        WidgetState.Error ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text("Couldn't load articles. Tap to open the app.", style = textStyle)
            }
    }
}

private fun Article.toSnapshot(): LatestArticleSnapshot =
    LatestArticleSnapshot(
        slug = slug,
        title = title,
        metadata =
            listOfNotNull(
                    publishedAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.toString(),
                    readingTimeMinutes?.let { minutes -> "$minutes min read" },
                )
                .joinToString(" · ")
                .ifEmpty { null },
        excerpt = excerpt,
    )

private val SlugKey = stringPreferencesKey("latest_article.slug")
private val TitleKey = stringPreferencesKey("latest_article.title")
private val MetadataKey = stringPreferencesKey("latest_article.metadata")
private val ExcerptKey = stringPreferencesKey("latest_article.excerpt")

private fun Preferences.toLatestArticleSnapshot(): LatestArticleSnapshot? {
    val slug = this[SlugKey] ?: return null
    val title = this[TitleKey] ?: return null
    return LatestArticleSnapshot(slug, title, this[MetadataKey], this[ExcerptKey])
}

private fun MutablePreferences.putLatestArticle(snapshot: LatestArticleSnapshot) {
    this[SlugKey] = snapshot.slug
    this[TitleKey] = snapshot.title
    if (snapshot.metadata != null) this[MetadataKey] = snapshot.metadata else remove(MetadataKey)
    if (snapshot.excerpt != null) this[ExcerptKey] = snapshot.excerpt else remove(ExcerptKey)
}
