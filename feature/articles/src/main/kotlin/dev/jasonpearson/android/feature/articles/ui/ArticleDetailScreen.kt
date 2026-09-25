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
package dev.jasonpearson.android.feature.articles.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.articles.ArticlesRepository
import dev.jasonpearson.android.data.bookmarks.BookmarksRepository
import dev.jasonpearson.android.foundation.designsystem.components.HtmlText
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage
import dev.jasonpearson.android.foundation.designsystem.util.openUrl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    repository: ArticlesRepository,
    slug: String,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    bookmarks: BookmarksRepository? = null,
) {
    val state by
        produceState<ArticleDetailUiState>(ArticleDetailUiState.Loading, repository, slug) {
            value =
                when (val r = repository.article(slug)) {
                    is NetworkResult.Success -> ArticleDetailUiState.Content(r.data)
                    is NetworkResult.Failure ->
                        ArticleDetailUiState.Error(r.error.message ?: "Failed to load")
                }
        }
    val title = (state as? ArticleDetailUiState.Content)?.article?.title ?: slug
    val loadedArticle = (state as? ArticleDetailUiState.Content)?.article
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bookmarkFlow =
        remember(bookmarks, loadedArticle?.slug) {
            val articleSlug = loadedArticle?.slug
            if (bookmarks == null || articleSlug == null) flowOf(false)
            else bookmarks.isBookmarked(articleSlug)
        }
    val isBookmarked by bookmarkFlow.collectAsState(initial = false)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    loadedArticle?.let { article ->
                        if (bookmarks != null) {
                            IconButton(
                                onClick = { coroutineScope.launch { bookmarks.toggle(article) } }
                            ) {
                                Icon(
                                    imageVector =
                                        if (isBookmarked) Icons.Filled.Bookmark
                                        else Icons.Filled.BookmarkBorder,
                                    contentDescription =
                                        if (isBookmarked) "Remove from saved" else "Save article",
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val intent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${article.title}\n${article.url}",
                                        )
                                    }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        article.url?.let { url ->
                            IconButton(onClick = { openUrl(context, url) }) {
                                Icon(Icons.Filled.Language, contentDescription = "Open in browser")
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val currentState = state) {
            ArticleDetailUiState.Loading -> LoadingContent(paddingValues)
            is ArticleDetailUiState.Error -> ErrorContent(currentState.message, paddingValues)
            is ArticleDetailUiState.Content ->
                ArticleContent(
                    article = currentState.article,
                    repository = repository,
                    onArticleClick = onArticleClick,
                    paddingValues = paddingValues,
                )
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message)
    }
}

@Composable
private fun ArticleContent(
    article: Article,
    repository: ArticlesRepository,
    onArticleClick: (String) -> Unit,
    paddingValues: PaddingValues,
) {
    val adjacent by
        produceState<Pair<Article?, Article?>?>(null, repository, article.slug) {
            value =
                when (val result = repository.adjacent(article.slug)) {
                    is NetworkResult.Success -> result.data
                    is NetworkResult.Failure -> null
                }
        }
    val related by
        produceState<List<Article>>(emptyList(), repository, article.id) {
            value =
                when (val result = repository.related(article)) {
                    is NetworkResult.Success -> result.data
                    is NetworkResult.Failure -> emptyList()
                }
        }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!article.featureImageUrl.isNullOrBlank()) {
            NetworkImage(
                url = article.featureImageUrl,
                contentDescription = article.title,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
        }
        Text(text = article.title, style = MaterialTheme.typography.headlineMedium)
        articleMetadata(article)
            .takeIf { it.isNotEmpty() }
            ?.let { metadata -> Text(text = metadata, style = MaterialTheme.typography.bodyMedium) }
        HtmlText(html = article.html.orEmpty(), modifier = Modifier.fillMaxWidth())
        adjacent?.let { (previous, next) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    enabled = previous != null,
                    onClick = { previous?.let { onArticleClick(it.slug) } },
                ) {
                    Text("Previous")
                }
                TextButton(
                    enabled = next != null,
                    onClick = { next?.let { onArticleClick(it.slug) } },
                ) {
                    Text("Next")
                }
            }
        }
        if (related.isNotEmpty()) {
            Text("Related", style = MaterialTheme.typography.titleLarge)
            related.forEach { relatedArticle ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth().clickable { onArticleClick(relatedArticle.slug) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(relatedArticle.title, style = MaterialTheme.typography.titleMedium)
                        relatedArticle.excerpt?.let { excerpt ->
                            Text(
                                text = excerpt,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun articleMetadata(article: Article): String {
    val parts = buildList {
        article.author?.name?.let(::add)
        article.publishedAt?.let { publishedAt -> add(publishedAt.toString()) }
        article.readingTimeMinutes?.let { minutes -> add("$minutes min read") }
    }
    return parts.joinToString(" · ")
}
