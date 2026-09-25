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
import dev.jasonpearson.android.data.projects.ProjectsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ProjectDetailViewModel(private val repository: ProjectsRepository, private val name: String) :
    ViewModel() {
    private val _project = MutableStateFlow<NetworkResult<Project>?>(null)
    val project: StateFlow<NetworkResult<Project>?> = _project
    private val _readme = MutableStateFlow<NetworkResult<String>?>(null)
    val readme: StateFlow<NetworkResult<String>?> = _readme
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val refreshErrorChannel = Channel<String>(Channel.BUFFERED)
    val refreshErrors = refreshErrorChannel.receiveAsFlow()
    private var projectLoading = false
    private var readmeLoading = false

    init {
        loadProject(false)
        loadReadme(false)
    }

    fun retryProject() {
        if (_readme.value is NetworkResult.Failure) retryReadme()
        loadProject(false)
    }

    fun retryReadme() = loadReadme(false)

    fun refresh() {
        if (projectLoading || readmeLoading) return
        _isRefreshing.value = true
        loadProject(true, preserveContent = true)
        loadReadme(true, preserveContent = true)
    }

    private fun loadProject(forceRefresh: Boolean, preserveContent: Boolean = false) {
        if (projectLoading) return
        projectLoading = true
        if (!preserveContent) _project.value = null
        viewModelScope.launch {
            try {
                when (val result = repository.project(name, forceRefresh)) {
                    is NetworkResult.Failure -> {
                        if (preserveContent && _project.value is NetworkResult.Success) {
                            refreshErrorChannel.send(
                                projectFailureMessage(result.error, "Couldn't load this project")
                            )
                        } else {
                            _project.value = result
                        }
                    }
                    is NetworkResult.Success -> _project.value = result
                }
            } finally {
                projectLoading = false
                if (forceRefresh && !readmeLoading) _isRefreshing.value = false
            }
        }
    }

    private fun loadReadme(forceRefresh: Boolean, preserveContent: Boolean = false) {
        if (readmeLoading) return
        readmeLoading = true
        if (!preserveContent) _readme.value = null
        viewModelScope.launch {
            try {
                when (val result = repository.readme(name, forceRefresh)) {
                    is NetworkResult.Failure -> {
                        if (preserveContent && _readme.value is NetworkResult.Success) {
                            refreshErrorChannel.send(
                                projectFailureMessage(result.error, "Couldn't load the README")
                            )
                        } else {
                            _readme.value = result
                        }
                    }
                    is NetworkResult.Success -> _readme.value = result
                }
            } finally {
                readmeLoading = false
                if (forceRefresh && !projectLoading) _isRefreshing.value = false
            }
        }
    }
}
