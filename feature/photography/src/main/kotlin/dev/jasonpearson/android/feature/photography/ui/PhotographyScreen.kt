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
package dev.jasonpearson.android.feature.photography.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.feature.photography.data.PhotographyRepository
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage

@Composable
fun PhotographyScreen(repository: PhotographyRepository, modifier: Modifier = Modifier) {
    val state by
        produceState<PhotographyUiState>(PhotographyUiState.Loading, repository) {
            value =
                when (val r = repository.photos()) {
                    is NetworkResult.Success -> PhotographyUiState.Content(r.data)
                    is NetworkResult.Failure ->
                        PhotographyUiState.Error(r.error.message ?: "Failed to load")
                }
        }

    when (val currentState = state) {
        PhotographyUiState.Loading -> LoadingContent(modifier)
        is PhotographyUiState.Error -> ErrorContent(currentState.message, modifier)
        is PhotographyUiState.Content -> {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = modifier.fillMaxSize()) {
                items(currentState.photos) { photo ->
                    NetworkImage(
                        url = photo.url,
                        contentDescription = photo.caption,
                        modifier = Modifier.aspectRatio(1f).padding(2.dp),
                    )
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
