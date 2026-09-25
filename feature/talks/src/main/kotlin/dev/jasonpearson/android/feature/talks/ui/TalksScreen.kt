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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.talks.Talk
import dev.jasonpearson.android.data.talks.TalkLink
import dev.jasonpearson.android.data.talks.TalkLinkKind
import dev.jasonpearson.android.data.talks.TalksRepository
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage
import dev.jasonpearson.android.foundation.designsystem.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalksScreen(repository: TalksRepository, modifier: Modifier = Modifier) {
    val state by
        produceState<TalksUiState>(TalksUiState.Loading, repository) {
            value =
                when (val r = repository.talks()) {
                    is NetworkResult.Success -> TalksUiState.Content(r.data)
                    is NetworkResult.Failure ->
                        TalksUiState.Error(r.error.message ?: "Failed to load")
                }
        }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Talks") }) }) { paddingValues
        ->
        when (val currentState = state) {
            TalksUiState.Loading -> LoadingContent(paddingValues)
            is TalksUiState.Error -> ErrorContent(currentState.message, paddingValues)
            is TalksUiState.Content -> TalksContent(currentState.talks, paddingValues)
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
private fun TalksContent(talks: List<Talk>, paddingValues: PaddingValues) {
    if (talks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text("No talks yet.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(talks) { talk -> TalkCard(talk) }
    }
}

@Composable
private fun TalkCard(talk: Talk) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (talk.imageUrl != null) {
                NetworkImage(
                    url = talk.imageUrl,
                    contentDescription = talk.title,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
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
