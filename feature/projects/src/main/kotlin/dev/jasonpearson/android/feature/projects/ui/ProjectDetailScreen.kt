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
package dev.jasonpearson.android.feature.projects.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.projects.ProjectsRepository
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.HtmlText
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent
import dev.jasonpearson.android.foundation.designsystem.util.openUrl
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    repository: ProjectsRepository,
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel = viewModel(key = name) { ProjectDetailViewModel(repository, name) }
    val projectResult by viewModel.project.collectAsState()
    val readmeResult by viewModel.readme.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.refreshErrors.collect { snackbarHostState.showSnackbar(it) }
    }
    val project =
        when (val result = projectResult) {
            is NetworkResult.Success -> result.data
            else -> null
        }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (project != null) {
                        TextButton(onClick = { openUrl(context, project.url) }) {
                            Text("Open on GitHub")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val currentProject = projectResult) {
            null -> LoadingContent(Modifier.padding(innerPadding))
            is NetworkResult.Failure ->
                ErrorContent(
                    message =
                        projectFailureMessage(currentProject.error, "Couldn't load this project"),
                    modifier = Modifier.padding(innerPadding),
                    onRetry = viewModel::retryProject,
                )
            is NetworkResult.Success ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val loadedProject = currentProject.data
                        ProjectHeader(loadedProject)
                        Text("README", style = MaterialTheme.typography.titleLarge)
                        when (val readme = readmeResult) {
                            null -> CircularProgressIndicator()
                            is NetworkResult.Success ->
                                HtmlText(html = readme.data, modifier = Modifier.fillMaxWidth())
                            is NetworkResult.Failure -> {
                                Text(
                                    projectFailureMessage(readme.error, "Couldn't load the README")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { openUrl(context, loadedProject.url) }) {
                                        Text("Open on GitHub")
                                    }
                                    TextButton(onClick = viewModel::retryReadme) { Text("Retry") }
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun ProjectHeader(project: Project) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            project.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            ProjectMetadata(project, topicLimit = 0, style = MaterialTheme.typography.bodyMedium)
            project.updatedAt?.let {
                Text(
                    "Updated ${it.toString().take(10)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (project.topics.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    project.topics.forEach { topic ->
                        AssistChip(onClick = {}, label = { Text(topic) })
                    }
                }
            }
        }
    }
}
