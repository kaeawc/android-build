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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.photography.GalleryPhoto
import dev.jasonpearson.android.data.photography.PhotographyRepository
import dev.jasonpearson.android.foundation.designsystem.components.NetworkImage

@Composable
fun PhotographyScreen(repository: PhotographyRepository, modifier: Modifier = Modifier) {
    val state by
        produceState<PhotographyUiState>(PhotographyUiState.Loading, repository) {
            value =
                when (val result = repository.photos()) {
                    is NetworkResult.Success -> PhotographyUiState.Content(result.data)
                    is NetworkResult.Failure ->
                        PhotographyUiState.Error(result.error.message ?: "Failed to load")
                }
        }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }

    when (val currentState = state) {
        PhotographyUiState.Loading -> LoadingContent(modifier)
        is PhotographyUiState.Error -> ErrorContent(currentState.message, modifier)
        is PhotographyUiState.Content -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(currentState.photos.size) { index ->
                    val photo = currentState.photos[index]
                    NetworkImage(
                        url = photo.thumbUrl,
                        contentDescription = photo.takenOn,
                        modifier = Modifier.aspectRatio(1f).clickable { selectedPhotoIndex = index },
                    )
                }
            }
            selectedPhotoIndex?.let { index ->
                GalleryViewer(
                    photos = currentState.photos,
                    initialPage = index,
                    onDismiss = { selectedPhotoIndex = null },
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
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val multiTouch = event.changes.count { it.pressed } > 1
                                    if (multiTouch || scale > 1f) {
                                        val newScale =
                                            (scale * event.calculateZoom()).coerceIn(1f, 5f)
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
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        translation = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    NetworkImage(
                        url = photo.fullUrl,
                        contentDescription = photo.takenOn,
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

            Row(
                modifier =
                    Modifier.align(Alignment.TopCenter)
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
                val activePhoto = photos[pagerState.currentPage]
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

private fun clampOffset(offset: Offset, scale: Float, containerSize: IntSize): Offset {
    val maxX = containerSize.width * (scale - 1) / 2
    val maxY = containerSize.height * (scale - 1) / 2
    return Offset(x = offset.x.coerceIn(-maxX, maxX), y = offset.y.coerceIn(-maxY, maxY))
}
