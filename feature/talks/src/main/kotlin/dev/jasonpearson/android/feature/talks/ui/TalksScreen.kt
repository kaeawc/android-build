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

package dev.jasonpearson.android.feature.talks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jasonpearson.android.data.talks.Talk
import dev.jasonpearson.android.data.talks.TalkLink
import dev.jasonpearson.android.data.talks.TalkLinkKind
import dev.jasonpearson.android.data.talks.TalkYearGroup
import dev.jasonpearson.android.data.talks.TalksRepository
import dev.jasonpearson.android.data.talks.groupTalksByYear
import dev.jasonpearson.android.foundation.designsystem.components.EmptyContent
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage
import dev.jasonpearson.android.foundation.designsystem.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalksScreen(repository: TalksRepository, modifier: Modifier = Modifier) {
    val viewModel: TalksViewModel = viewModel { TalksViewModel(repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messageFlow.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Talks") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (val currentState = state) {
            TalksUiState.Loading -> LoadingContent(Modifier.padding(paddingValues))
            is TalksUiState.Error,
            is TalksUiState.Content ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                ) {
                    if (currentState is TalksUiState.Content) {
                        TalksContent(currentState.talks)
                    } else if (currentState is TalksUiState.Error) {
                        // Scrollable so the pull gesture works on the error state too.
                        LazyColumn(Modifier.fillMaxSize()) {
                            item {
                                ErrorContent(
                                    message = currentState.message,
                                    modifier = Modifier.fillParentMaxSize(),
                                    onRetry = viewModel::retry,
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun TalksContent(talks: List<Talk>) {
    val groups = remember(talks) { groupTalksByYear(talks) }
    // A single group of undated talks gets no header; there's nothing to distinguish.
    val showHeaders = groups.size > 1 || groups.singleOrNull()?.year != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (talks.isEmpty()) {
            item { EmptyContent("No talks yet.", Modifier.fillParentMaxSize()) }
        }
        groups.forEach { group ->
            if (showHeaders) {
                item(contentType = "header") { YearHeader(group) }
            }
            items(group.talks, contentType = { "talk" }) { talk -> TalkCard(talk) }
        }
    }
}

@Composable
private fun YearHeader(group: TalkYearGroup) {
    Text(
        text = group.year?.toString() ?: "Other",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun TalkCard(talk: Talk) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (talk.imageUrl != null) {
                // The tinted box is the placeholder while the image loads (or if it fails).
                Box(
                    Modifier.fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    NetworkImage(
                        url = talk.imageUrl,
                        contentDescription = talk.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(talk.title, style = MaterialTheme.typography.titleMedium)
                val metadata = listOfNotNull(talk.event, talk.date).joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Text(metadata, style = MaterialTheme.typography.bodySmall)
                }
                if (talk.links.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        talk.links.forEach { link ->
                            TalkLinkChip(link = link) { openUrl(context, link.url) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TalkLinkChip(link: TalkLink, onClick: () -> Unit) {
    val icon =
        when (link.kind) {
            TalkLinkKind.WATCH -> Icons.Default.PlayArrow
            TalkLinkKind.SLIDES -> Icons.Default.Slideshow
            TalkLinkKind.EVENT -> Icons.Default.Event
            TalkLinkKind.OTHER -> Icons.Default.Link
        }
    AssistChip(
        onClick = onClick,
        label = { Text(link.label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
    )
}
