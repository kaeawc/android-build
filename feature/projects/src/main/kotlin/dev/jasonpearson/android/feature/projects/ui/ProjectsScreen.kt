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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.projects.ProjectsRepository
import dev.jasonpearson.android.feature.projects.ProjectsExperiments
import dev.jasonpearson.android.foundation.designsystem.components.EmptyContent
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent
import dev.jasonpearson.android.foundation.designsystem.util.openUrl
import dev.jasonpearson.android.subsystem.analytics.AnalyticsClient
import dev.jasonpearson.android.subsystem.analytics.AnalyticsEvent
import dev.jasonpearson.android.subsystem.experimentation.ExperimentRepository
import dev.jasonpearson.android.subsystem.experimentation.Treatment

/** Each new value re-runs the load; [forceRefresh] bypasses the ETag-revalidated cache. */
private data class LoadRequest(val id: Int = 0, val forceRefresh: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    repository: ProjectsRepository,
    modifier: Modifier = Modifier,
    onProjectClick: ((String) -> Unit)? = null,
    experiments: ExperimentRepository? = null,
    analytics: AnalyticsClient? = null,
) {
    val context = LocalContext.current
    val treatment =
        remember(experiments) {
            experiments?.treatmentFor(ProjectsExperiments.DefaultSort) ?: Treatment.CONTROL
        }
    LaunchedEffect(treatment) {
        analytics?.track(
            AnalyticsEvent(
                "experiment_exposure",
                mapOf("experiment" to "projects_default_sort", "treatment" to treatment.name),
            )
        )
    }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    var state by remember(repository) { mutableStateOf<ProjectsUiState>(ProjectsUiState.Loading) }
    var request by remember(repository) { mutableStateOf(LoadRequest()) }
    var isRefreshing by remember(repository) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repository, request) {
        val result = repository.overview(forceRefresh = request.forceRefresh)
        isRefreshing = false
        when (result) {
            is NetworkResult.Success -> state = ProjectsUiState.Content(result.data)
            is NetworkResult.Failure -> {
                val message = result.error.message ?: "Failed to load"
                // A failed refresh keeps the projects already on screen.
                if (state is ProjectsUiState.Content) snackbarHostState.showSnackbar(message)
                else state = ProjectsUiState.Error(message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Projects") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val currentState = state) {
            ProjectsUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            is ProjectsUiState.Error ->
                ErrorContent(
                    message = currentState.message,
                    modifier = Modifier.padding(innerPadding),
                    onRetry = {
                        state = ProjectsUiState.Loading
                        request = LoadRequest(request.id + 1)
                    },
                )
            is ProjectsUiState.Content ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        request = LoadRequest(request.id + 1, forceRefresh = true)
                    },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    ProjectsContent(
                        content = currentState,
                        selectedLanguage = selectedLanguage,
                        onSelectLanguage = { selectedLanguage = it },
                        treatment = treatment,
                        onOpen = { project ->
                            analytics?.track(
                                AnalyticsEvent("project_open", mapOf("name" to project.name))
                            )
                            if (onProjectClick != null) onProjectClick(project.name)
                            else openUrl(context, project.url)
                        },
                    )
                }
        }
    }
}

@Composable
private fun ProjectsContent(
    content: ProjectsUiState.Content,
    selectedLanguage: String?,
    onSelectLanguage: (String?) -> Unit,
    treatment: Treatment,
    onOpen: (Project) -> Unit,
) {
    val data = content.overview
    if (data.pinned.isEmpty() && data.others.isEmpty()) {
        // Scrollable so the pull-to-refresh gesture still works on an empty list.
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "empty") {
                EmptyContent("No projects to show yet", Modifier.fillParentMaxSize())
            }
        }
        return
    }
    val pinned = data.pinned.filter { selectedLanguage == null || it.language == selectedLanguage }
    val filteredOthers =
        data.others.filter { selectedLanguage == null || it.language == selectedLanguage }
    val others = sortOthers(filteredOthers, treatment)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "filters") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedLanguage == null,
                    onClick = { onSelectLanguage(null) },
                    label = { Text("All") },
                )
                data.languages.forEach { language ->
                    FilterChip(
                        selected = selectedLanguage == language,
                        onClick = { onSelectLanguage(language) },
                        label = { Text(language) },
                        leadingIcon = { LanguageDot(language) },
                    )
                }
            }
        }
        item(key = "pinned_header") { SectionHeader("Pinned") }
        items(pinned, key = { "pinned_${it.id}" }) { project ->
            ProjectCard(project, pinned = true) { onOpen(project) }
        }
        item(key = "all_header") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionHeader("All projects")
                Text(
                    text =
                        if (treatment == Treatment.CONTROL) "Sorted by stars"
                        else "Sorted by recently updated",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        items(others, key = { "other_${it.id}" }) { project ->
            ProjectCard(project, pinned = false) { onOpen(project) }
        }
    }
}

internal fun sortOthers(others: List<Project>, treatment: Treatment): List<Project> =
    when (treatment) {
        Treatment.CONTROL -> others
        Treatment.VARIANT -> {
            val recentlyUpdated =
                others.filter { it.updatedAt != null }.sortedByDescending { it.updatedAt }
            val withoutUpdateTime = others.filter { it.updatedAt == null }
            recentlyUpdated + withoutUpdateTime
        }
    }

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProjectCard(project: Project, pinned: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pinned) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            project.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ProjectMetadata(project)
        }
    }
}

/** `★ stars · ● Language · topics`, with the dot in the language's GitHub color. */
@Composable
internal fun ProjectMetadata(
    project: Project,
    topicLimit: Int = 3,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "★ ${project.stars}", style = style)
        project.language?.let { language ->
            Text(text = "·", style = style)
            LanguageDot(language)
            Text(text = language, style = style)
        }
        project.topics
            .take(topicLimit)
            .takeIf { it.isNotEmpty() }
            ?.let { topics ->
                Text(text = "·", style = style)
                Text(
                    text = topics.joinToString(),
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}
