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
import androidx.compose.foundation.layout.Box
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
import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.projects.ProjectsRepository
import dev.jasonpearson.android.foundation.designsystem.components.HtmlText
import dev.jasonpearson.android.foundation.designsystem.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    repository: ProjectsRepository,
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val projectResult by
        produceState<NetworkResult<Project>?>(null, repository, name) {
            value = repository.project(name)
        }
    val readmeResult by
        produceState<NetworkResult<String>?>(null, repository, name) {
            value = repository.readme(name)
        }
    val project =
        when (val result = projectResult) {
            is NetworkResult.Success -> result.data
            else -> null
        }

    Scaffold(
        modifier = modifier,
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
            null ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            is NetworkResult.Failure ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Couldn't load this project")
                }
            is NetworkResult.Success ->
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(innerPadding)
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
                            Text("Couldn't load the README")
                            Button(onClick = { openUrl(context, loadedProject.url) }) {
                                Text("Open on GitHub")
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
            val metadata = buildList {
                add("★ ${project.stars}")
                project.language?.let(::add)
                project.updatedAt?.let { add("Updated ${it.toString().take(10)}") }
            }
            Text(metadata.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
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
