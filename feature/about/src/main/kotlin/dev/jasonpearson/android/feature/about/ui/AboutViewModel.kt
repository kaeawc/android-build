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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.about.AboutProfile
import dev.jasonpearson.android.data.about.AboutRepository
import dev.jasonpearson.android.data.about.SocialLinks
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AboutViewModel(private val repository: AboutRepository) : ViewModel() {
    private val _state = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val state: StateFlow<AboutUiState> = _state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val refreshErrorChannel = Channel<String>(Channel.BUFFERED)
    val refreshErrors = refreshErrorChannel.receiveAsFlow()
    private var loading = false

    init {
        load()
    }

    fun retry() {
        if (loading) return
        _state.value = AboutUiState.Loading
        load()
    }

    fun refresh() {
        if (loading) return
        _isRefreshing.value = true
        load()
    }

    private fun load() {
        loading = true
        viewModelScope.launch {
            try {
                val loaded = loadAbout()
                if (loaded is AboutUiState.Error && _state.value is AboutUiState.Content) {
                    refreshErrorChannel.send(loaded.message)
                } else {
                    _state.value = loaded
                }
            } finally {
                _isRefreshing.value = false
                loading = false
            }
        }
    }

    /** Profile and page load in parallel; only the page is required, the profile has a fallback. */
    private suspend fun loadAbout(): AboutUiState = coroutineScope {
        val profileResult = async { repository.profile() }
        val pageResult = async { repository.aboutPage() }
        when (val page = pageResult.await()) {
            is NetworkResult.Success -> {
                val profile =
                    when (val result = profileResult.await()) {
                        is NetworkResult.Success -> result.data
                        is NetworkResult.Failure -> fallbackProfile()
                    }
                AboutUiState.Content(
                    profile = profile,
                    page = page.data,
                    experience = repository.experience(),
                )
            }
            is NetworkResult.Failure -> AboutUiState.Error(page.error.message ?: "Failed to load")
        }
    }
}

private fun fallbackProfile() =
    AboutProfile(
        title = "Jason Pearson",
        description = null,
        iconUrl = null,
        socials =
            SocialLinks(
                github = "https://github.com/kaeawc",
                linkedin = "https://www.linkedin.com/in/jasondpearson/",
                x = "https://x.com/kaeawc",
                website = null,
            ),
    )
