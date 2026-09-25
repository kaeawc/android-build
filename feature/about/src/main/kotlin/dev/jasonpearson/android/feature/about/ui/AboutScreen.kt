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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.about.AboutProfile
import dev.jasonpearson.android.data.about.AboutRepository
import dev.jasonpearson.android.data.about.ExperienceEntry
import dev.jasonpearson.android.data.about.SocialLinks
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.HtmlText
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage
import dev.jasonpearson.android.foundation.designsystem.util.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    repository: AboutRepository,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var state by remember(repository) { mutableStateOf<AboutUiState>(AboutUiState.Loading) }
    var loadRequest by remember(repository) { mutableIntStateOf(0) }
    var isRefreshing by remember(repository) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repository, loadRequest) {
        val loaded = loadAbout(repository)
        isRefreshing = false
        // A failed refresh keeps the page already on screen.
        if (loaded is AboutUiState.Error && state is AboutUiState.Content) {
            snackbarHostState.showSnackbar(loaded.message)
        } else {
            state = loaded
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("About") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val currentState = state) {
            AboutUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            is AboutUiState.Error ->
                ErrorContent(
                    message = currentState.message,
                    modifier = Modifier.padding(innerPadding),
                    onRetry = {
                        state = AboutUiState.Loading
                        loadRequest++
                    },
                )
            is AboutUiState.Content ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        loadRequest++
                    },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    AboutContent(currentState, onOpenUrl = { openUrl(context, it) })
                }
        }
    }
}

/** Profile and page load in parallel; only the page is required, the profile has a fallback. */
private suspend fun loadAbout(repository: AboutRepository): AboutUiState = coroutineScope {
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

@Composable
private fun AboutContent(content: AboutUiState.Content, onOpenUrl: (String) -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileHeader(content.profile)
        HtmlText(html = content.page.html)
        ExperienceSection(content.experience)
        ConnectSection(content.profile, onOpenUrl = onOpenUrl)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileHeader(profile: AboutProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkImage(
            url = profile.iconUrl,
            contentDescription = "${profile.title} profile image",
            modifier = Modifier.size(64.dp).clip(CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(profile.title, style = MaterialTheme.typography.headlineSmall)
            profile.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ExperienceSection(entries: List<ExperienceEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Experience", style = MaterialTheme.typography.titleMedium)
        entries.forEachIndexed { index, entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("●", color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.role, style = MaterialTheme.typography.titleSmall)
                        Text(entry.company, style = MaterialTheme.typography.bodyMedium)
                        Text(entry.period, style = MaterialTheme.typography.bodySmall)
                        entry.highlights.forEach { highlight ->
                            Text("• $highlight", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (index < entries.lastIndex) Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ConnectSection(profile: AboutProfile, onOpenUrl: (String) -> Unit) {
    var showQrDialog by remember { mutableStateOf(false) }
    var useWebsite by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Connect", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onOpenUrl(profile.socials.github) }) { Text("GitHub") }
            TextButton(onClick = { onOpenUrl(profile.socials.linkedin) }) { Text("LinkedIn") }
            profile.socials.x?.let { url -> TextButton(onClick = { onOpenUrl(url) }) { Text("X") } }
        }
        TextButton(onClick = { showQrDialog = true }) { Text("Show QR code") }
    }

    if (showQrDialog) {
        val qrText =
            if (useWebsite) profile.socials.website ?: profile.socials.linkedin
            else profile.socials.linkedin
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Connect with me") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (profile.socials.website != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !useWebsite,
                                onClick = { useWebsite = false },
                                label = { Text("LinkedIn") },
                            )
                            FilterChip(
                                selected = useWebsite,
                                onClick = { useWebsite = true },
                                label = { Text("Website") },
                            )
                        }
                    }
                    QrCodeImage(text = qrText)
                }
            },
            confirmButton = { TextButton(onClick = { showQrDialog = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun QrCodeImage(text: String) {
    val sizePx = 512
    val image by
        produceState<ImageBitmap?>(initialValue = null, key1 = text) {
            value = withContext(Dispatchers.Default) { generateQrCodeBitmap(text, sizePx) }
        }
    Surface(color = Color.White, modifier = Modifier.padding(8.dp)) {
        Box(modifier = Modifier.size(240.dp).padding(8.dp), contentAlignment = Alignment.Center) {
            val bitmap = image
            if (bitmap == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = bitmap,
                    contentDescription = "QR code for $text",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
