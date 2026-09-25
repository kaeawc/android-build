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

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.photography.GalleryPhoto
import dev.jasonpearson.android.data.photography.PhotographyRepository
import dev.jasonpearson.android.foundation.designsystem.components.EmptyContent
import dev.jasonpearson.android.foundation.designsystem.components.ErrorContent
import dev.jasonpearson.android.foundation.designsystem.components.LoadingContent
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotographyScreen(repository: PhotographyRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var state by
        remember(repository) { mutableStateOf<PhotographyUiState>(PhotographyUiState.Loading) }
    var isRefreshing by remember(repository) { mutableStateOf(false) }
    // Bumped by Retry to re-run the initial (cache-backed) load.
    var loadAttempt by remember(repository) { mutableIntStateOf(0) }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(repository, loadAttempt) { state = repository.load(forceRefresh = false) }

    val onRetry: () -> Unit = {
        state = PhotographyUiState.Loading
        loadAttempt++
    }
    val onRefresh: () -> Unit = {
        if (!isRefreshing) {
            scope.launch {
                isRefreshing = true
                val refreshed = repository.load(forceRefresh = true)
                isRefreshing = false
                if (refreshed is PhotographyUiState.Error && state is PhotographyUiState.Content) {
                    // Keep what's on screen; just say the refresh didn't work.
                    snackbarHostState.showSnackbar("Couldn't refresh photos")
                } else {
                    state = refreshed
                }
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        when (val currentState = state) {
            PhotographyUiState.Loading -> LoadingContent()
            is PhotographyUiState.Error,
            is PhotographyUiState.Content ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        currentState is PhotographyUiState.Content &&
                            currentState.photos.isNotEmpty() ->
                            PhotoGrid(
                                photos = currentState.photos,
                                onPhotoClick = { index -> selectedPhotoIndex = index },
                            )
                        // Scrollable so the pull gesture works on these states too.
                        currentState is PhotographyUiState.Error ->
                            LazyColumn(Modifier.fillMaxSize()) {
                                item {
                                    ErrorContent(
                                        message = currentState.message,
                                        modifier = Modifier.fillParentMaxSize(),
                                        onRetry = onRetry,
                                    )
                                }
                            }
                        else ->
                            LazyColumn(Modifier.fillMaxSize()) {
                                item {
                                    EmptyContent("No photos yet.", Modifier.fillParentMaxSize())
                                }
                            }
                    }
                }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    val photos = (state as? PhotographyUiState.Content)?.photos.orEmpty()
    selectedPhotoIndex
        ?.takeIf { it in photos.indices }
        ?.let { index ->
            GalleryViewer(
                photos = photos,
                initialPage = index,
                onDismiss = { selectedPhotoIndex = null },
            )
        }
}

private suspend fun PhotographyRepository.load(forceRefresh: Boolean): PhotographyUiState =
    when (val result = photos(forceRefresh)) {
        is NetworkResult.Success -> PhotographyUiState.Content(result.data)
        is NetworkResult.Failure ->
            PhotographyUiState.Error(result.error.message ?: "Failed to load")
    }

private val GalleryPhoto.description: String?
    get() = altText ?: caption ?: takenOn

@Composable
private fun PhotoGrid(photos: List<GalleryPhoto>, onPhotoClick: (Int) -> Unit) {
    // Adaptive columns: ~3 on a phone in portrait, more in landscape and on tablets.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(photos.size) { index ->
            val photo = photos[index]
            // The tinted box is the placeholder while the thumbnail loads (or if it fails).
            Box(
                Modifier.aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClickLabel = "View photo") { onPhotoClick(index) }
            ) {
                NetworkImage(
                    url = photo.thumbUrl,
                    contentDescription = photo.description,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun GalleryViewer(photos: List<GalleryPhoto>, initialPage: Int, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { photos.size })
    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var chromeVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        translation = Offset.Zero
    }
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale == 1f,
            ) { page ->
                val photo = photos[page]
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black)
                        .onSizeChanged { containerSize = it }
                        // Only consume pointer movement for pinches or while zoomed, so a
                        // one-finger swipe at 1x still pages.
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val multiTouch = event.changes.count { it.pressed } > 1
                                    if (multiTouch || scale > 1f) {
                                        val newScale =
                                            (scale * event.calculateZoom()).coerceIn(1f, MAX_SCALE)
                                        scale = newScale
                                        translation =
                                            if (newScale > 1f) {
                                                clampOffset(
                                                    translation + event.calculatePan(),
                                                    newScale,
                                                    containerSize,
                                                )
                                            } else {
                                                Offset.Zero
                                            }
                                        event.changes.forEach {
                                            if (it.positionChanged()) it.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { chromeVisible = !chromeVisible },
                                onDoubleTap = { tap ->
                                    if (scale > 1f) {
                                        scale = 1f
                                        translation = Offset.Zero
                                    } else {
                                        // Zoom toward the tapped point so it stays under the
                                        // finger (graphicsLayer scales around the center).
                                        val center =
                                            Offset(
                                                containerSize.width / 2f,
                                                containerSize.height / 2f,
                                            )
                                        scale = DOUBLE_TAP_SCALE
                                        translation =
                                            clampOffset(
                                                (center - tap) * (DOUBLE_TAP_SCALE - 1f),
                                                DOUBLE_TAP_SCALE,
                                                containerSize,
                                            )
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    NetworkImage(
                        url = photo.fullUrl,
                        contentDescription = photo.description,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = translation.x
                                translationY = translation.y
                            },
                    )
                }
            }

            val activePhoto = photos[pagerState.currentPage]
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close viewer",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text =
                            buildString {
                                append("${pagerState.currentPage + 1} / ${photos.size}")
                                activePhoto.takenOn?.let { append("  ·  $it") }
                            },
                        color = Color.White,
                    )
                    IconButton(
                        onClick = {
                            val sendIntent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, activePhoto.fullUrl)
                                }
                            context.startActivity(Intent.createChooser(sendIntent, "Share photo"))
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share photo",
                            tint = Color.White,
                        )
                    }
                }
            }

            val caption = activePhoto.caption ?: activePhoto.altText
            AnimatedVisibility(
                visible = chromeVisible && caption != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Text(
                    text = caption.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

private fun clampOffset(offset: Offset, scale: Float, containerSize: IntSize): Offset {
    val maxX = containerSize.width * (scale - 1) / 2
    val maxY = containerSize.height * (scale - 1) / 2
    return Offset(x = offset.x.coerceIn(-maxX, maxX), y = offset.y.coerceIn(-maxY, maxY))
}

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
