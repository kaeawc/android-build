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
package dev.jasonpearson.android.feature.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.ContentPage
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.about.AboutRepository
import dev.jasonpearson.android.foundation.designsystem.components.HtmlText
import dev.jasonpearson.android.foundation.designsystem.util.openUrl

private const val GITHUB_URL = "https://github.com/kaeawc"
private const val LINKEDIN_URL = "https://www.linkedin.com/in/kaeawc"
private const val X_URL = "https://x.com/kaeawc"

@Composable
fun AboutScreen(repository: AboutRepository, modifier: Modifier = Modifier) {
    val state by
        produceState<AboutUiState>(AboutUiState.Loading, repository) {
            value =
                when (val r = repository.aboutPage()) {
                    is NetworkResult.Success -> AboutUiState.Content(r.data)
                    is NetworkResult.Failure ->
                        AboutUiState.Error(r.error.message ?: "Failed to load")
                }
        }

    when (val currentState = state) {
        AboutUiState.Loading -> LoadingContent(modifier)
        is AboutUiState.Error -> ErrorContent(currentState.message, modifier)
        is AboutUiState.Content -> AboutContent(currentState.page, modifier)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutContent(page: ContentPage, modifier: Modifier) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("About") }) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HtmlText(html = page.html)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Connect", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { openUrl(context, GITHUB_URL) }) { Text("GitHub") }
                TextButton(onClick = { openUrl(context, LINKEDIN_URL) }) { Text("LinkedIn") }
                TextButton(onClick = { openUrl(context, X_URL) }) { Text("X") }
            }
        }
    }
}
