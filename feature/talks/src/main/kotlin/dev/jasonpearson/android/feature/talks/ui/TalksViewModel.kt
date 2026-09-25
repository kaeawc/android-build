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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.talks.TalksRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TalksViewModel(private val repository: TalksRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<TalksUiState>(TalksUiState.Loading)
    val uiState: StateFlow<TalksUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    init {
        loadInitial()
    }

    fun retry() {
        _uiState.value = TalksUiState.Loading
        loadInitial()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (val result = repository.talks(forceRefresh = true)) {
                    is NetworkResult.Success -> _uiState.value = TalksUiState.Content(result.data)
                    is NetworkResult.Failure -> {
                        if (_uiState.value is TalksUiState.Content) {
                            messages.send("Couldn't refresh talks")
                        } else {
                            _uiState.value =
                                TalksUiState.Error(result.error.message ?: "Failed to load")
                        }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            when (val result = repository.talks(forceRefresh = false)) {
                is NetworkResult.Success -> _uiState.value = TalksUiState.Content(result.data)
                is NetworkResult.Failure ->
                    _uiState.value = TalksUiState.Error(result.error.message ?: "Failed to load")
            }
        }
    }
}
