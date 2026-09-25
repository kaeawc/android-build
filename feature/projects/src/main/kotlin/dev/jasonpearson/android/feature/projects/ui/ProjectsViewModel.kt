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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.projects.ProjectsRateLimitedException
import dev.jasonpearson.android.data.projects.ProjectsRepository
import dev.jasonpearson.android.feature.projects.ProjectsExperiments
import dev.jasonpearson.android.subsystem.analytics.AnalyticsClient
import dev.jasonpearson.android.subsystem.analytics.AnalyticsEvent
import dev.jasonpearson.android.subsystem.experimentation.ExperimentRepository
import dev.jasonpearson.android.subsystem.experimentation.Treatment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal fun projectFailureMessage(error: Throwable, fallback: String): String =
    if (error is ProjectsRateLimitedException) error.message ?: fallback else fallback

class ProjectsViewModel(
    private val repository: ProjectsRepository,
    experiments: ExperimentRepository? = null,
    private val analytics: AnalyticsClient? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<ProjectsUiState>(ProjectsUiState.Loading)
    val state: StateFlow<ProjectsUiState> = _state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val refreshErrorChannel = Channel<String>(Channel.BUFFERED)
    val refreshErrors = refreshErrorChannel.receiveAsFlow()
    val treatment = experiments?.treatmentFor(ProjectsExperiments.DefaultSort) ?: Treatment.CONTROL
    private var loading = false

    init {
        viewModelScope.launch {
            analytics?.track(
                AnalyticsEvent(
                    "experiment_exposure",
                    mapOf("experiment" to "projects_default_sort", "treatment" to treatment.name),
                )
            )
        }
        load(forceRefresh = false)
    }

    fun retry() {
        if (loading) return
        _state.value = ProjectsUiState.Loading
        load(forceRefresh = false)
    }

    fun refresh() {
        if (loading) return
        _isRefreshing.value = true
        load(forceRefresh = true)
    }

    fun onProjectOpened(project: Project) {
        analytics?.track(AnalyticsEvent("project_open", mapOf("name" to project.name)))
    }

    private fun load(forceRefresh: Boolean) {
        loading = true
        viewModelScope.launch {
            try {
                when (val result = repository.overview(forceRefresh)) {
                    is NetworkResult.Success -> _state.value = ProjectsUiState.Content(result.data)
                    is NetworkResult.Failure -> {
                        val message = projectFailureMessage(result.error, "Failed to load")
                        if (_state.value is ProjectsUiState.Content) {
                            refreshErrorChannel.send(message)
                        } else {
                            _state.value = ProjectsUiState.Error(message)
                        }
                    }
                }
            } finally {
                _isRefreshing.value = false
                loading = false
            }
        }
    }
}
