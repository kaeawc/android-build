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
package dev.jasonpearson.android.feature.saved

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.data.bookmarks.Bookmark
import dev.jasonpearson.android.data.bookmarks.BookmarksRepository
import dev.jasonpearson.android.foundation.designsystem.components.EmptyContent
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private sealed interface SavedUiState {
    data object Loading : SavedUiState

    data class Loaded(val bookmarks: List<Bookmark>) : SavedUiState

    data object Error : SavedUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    repository: BookmarksRepository,
    onArticleClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reloadKey by remember { mutableIntStateOf(0) }
    // Starts in Loading (not an empty list) so the empty state never flashes before the first read.
    val state by
        produceState<SavedUiState>(SavedUiState.Loading, repository, reloadKey) {
            value = SavedUiState.Loading
            repository.bookmarks
                .catch { value = SavedUiState.Error }
                .collect { bookmarks -> value = SavedUiState.Loaded(bookmarks) }
        }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val removeWithUndo: (Bookmark) -> Unit = { bookmark ->
        coroutineScope.launch {
            repository.remove(bookmark.slug)
            // A newer removal replaces the pending Undo; the replaced one resolves as Dismissed.
            snackbarHostState.currentSnackbarData?.dismiss()
            val result =
                snackbarHostState.showSnackbar(
                    message = "Removed from saved",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) repository.restore(bookmark)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Saved") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        when (val current = state) {
            SavedUiState.Loading -> LoadingContent(Modifier.padding(contentPadding))
            SavedUiState.Error ->
                ErrorContent(
                    message = "Couldn't load saved articles",
                    modifier = Modifier.padding(contentPadding),
                    onRetry = { reloadKey++ },
                )
            is SavedUiState.Loaded ->
                if (current.bookmarks.isEmpty()) {
                    // The saved AutoMobile plan asserts this exact text.
                    EmptyContent("No saved articles yet", Modifier.padding(contentPadding))
                } else {
                    SavedList(
                        bookmarks = current.bookmarks,
                        contentPadding = contentPadding,
                        onArticleClick = onArticleClick,
                        onRemove = removeWithUndo,
                    )
                }
        }
    }
}

@Composable
private fun SavedList(
    bookmarks: List<Bookmark>,
    contentPadding: PaddingValues,
    onArticleClick: (String) -> Unit,
    onRemove: (Bookmark) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The repository emits newest-saved first.
        items(bookmarks, key = Bookmark::slug) { bookmark ->
            // Keyed by slug, so an undone item comes back with a fresh, un-swiped state.
            val dismissState = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = { DismissBackground(dismissState) },
                modifier = Modifier.animateItem(),
                onDismiss = { onRemove(bookmark) },
            ) {
                SavedBookmarkCard(
                    bookmark = bookmark,
                    onClick = { onArticleClick(bookmark.slug) },
                    onRemove = { onRemove(bookmark) },
                )
            }
        }
    }
}

@Composable
private fun DismissBackground(state: SwipeToDismissBoxState) {
    val alignment =
        when (state.dismissDirection) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            else -> Alignment.CenterEnd
        }
    Box(
        modifier =
            Modifier.fillMaxSize()
                .clip(CardDefaults.shape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SavedBookmarkCard(bookmark: Bookmark, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bookmark.featureImageUrl?.let { imageUrl ->
                    NetworkImage(
                        url = imageUrl,
                        contentDescription = bookmark.title,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                }
                Text(bookmark.title, style = MaterialTheme.typography.titleMedium)
                bookmark.excerpt?.let { excerpt ->
                    Text(
                        excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Saved ${savedDate(bookmark.savedAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove from saved")
            }
        }
    }
}

private fun savedDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
