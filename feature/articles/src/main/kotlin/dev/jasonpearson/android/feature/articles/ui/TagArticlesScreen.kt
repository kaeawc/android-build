package dev.jasonpearson.android.feature.articles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.Article
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.articles.ArticlesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagArticlesScreen(
    repository: ArticlesRepository,
    tagSlug: String,
    onArticleClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by
        produceState<ArticlesUiState>(ArticlesUiState.Loading, repository, tagSlug) {
            value =
                when (val result = repository.articlesByTag(tagSlug)) {
                    is NetworkResult.Success -> ArticlesUiState.Content(result.data)
                    is NetworkResult.Failure ->
                        ArticlesUiState.Error(result.error.message ?: "Failed to load")
                }
        }
    val title =
        (state as? ArticlesUiState.Content)
            ?.articles
            ?.firstOrNull()
            ?.tags
            ?.firstOrNull { it.slug == tagSlug }
            ?.name ?: tagSlug

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val currentState = state) {
            ArticlesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is ArticlesUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(currentState.message)
                }
            }
            is ArticlesUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(currentState.articles, key = Article::id) { article ->
                        ArticleCard(article = article, onClick = { onArticleClick(article.slug) })
                    }
                }
            }
        }
    }
}
