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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.model.Tag
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.articles.ArticlesRepository
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesListScreen(
    repository: ArticlesRepository,
    onArticleClick: (slug: String) -> Unit,
    onTagClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by
        produceState<ArticlesUiState>(ArticlesUiState.Loading, repository) {
            value =
                when (val r = repository.articles()) {
                    is NetworkResult.Success -> ArticlesUiState.Content(r.data)
                    is NetworkResult.Failure ->
                        ArticlesUiState.Error(r.error.message ?: "Failed to load")
                }
        }
    val featured by
        produceState<List<Article>>(emptyList(), repository) {
            value =
                when (val result = repository.featured()) {
                    is NetworkResult.Success -> result.data
                    is NetworkResult.Failure -> emptyList()
                }
        }
    val tags by
        produceState<List<Tag>>(emptyList(), repository) {
            value =
                when (val result = repository.tags()) {
                    is NetworkResult.Success -> result.data
                    is NetworkResult.Failure -> emptyList()
                }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Articles") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val currentState = state) {
            ArticlesUiState.Loading -> LoadingContent(Modifier.padding(paddingValues))
            is ArticlesUiState.Error ->
                ErrorContent(currentState.message, Modifier.padding(paddingValues))
            is ArticlesUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (featured.isNotEmpty()) {
                        item(key = "featured") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(featured, key = Article::id) { article ->
                                    Card(
                                        modifier =
                                            Modifier.fillParentMaxWidth().clickable {
                                                onArticleClick(article.slug)
                                            }
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            NetworkImage(
                                                url = article.featureImageUrl,
                                                contentDescription = article.title,
                                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                            )
                                            Text(
                                                text = article.title,
                                                style = MaterialTheme.typography.titleLarge,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (tags.isNotEmpty()) {
                        item(key = "tags") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(tags, key = Tag::id) { tag ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { onTagClick(tag.slug) },
                                        label = { Text("${tag.name} ${tag.postCount}") },
                                    )
                                }
                            }
                        }
                    }
                    items(currentState.articles, key = Article::id) { article ->
                        ArticleCard(article = article, onClick = { onArticleClick(article.slug) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message)
    }
}

@Composable
internal fun ArticleCard(article: Article, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!article.featureImageUrl.isNullOrBlank()) {
                NetworkImage(
                    url = article.featureImageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
            Text(text = article.title, style = MaterialTheme.typography.titleLarge)
            article.excerpt?.let { excerpt ->
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val metadata = buildList {
                article.tags
                    .takeIf { it.isNotEmpty() }
                    ?.let { tags -> add(tags.joinToString { it.name }) }
                article.readingTimeMinutes?.let { minutes -> add("$minutes min read") }
            }
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
