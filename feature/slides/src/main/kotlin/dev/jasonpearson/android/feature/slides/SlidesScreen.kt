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
package dev.jasonpearson.android.feature.slides

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.foundation.designsystem.components.PrimaryButton
import dev.jasonpearson.android.foundation.designsystem.theme.AndroidBuildTheme

/** A simple wrap-around slide carousel driven by the pure [SlidesReducer]. */
@Composable
fun SlidesScreen(modifier: Modifier = Modifier, slides: List<String> = DefaultSlides) {
    var state by remember { mutableStateOf(slidesStateFor(slides)) }

    AndroidBuildTheme {
        Column(modifier = modifier.padding(16.dp)) {
            Text(text = state.current)
            Text(text = state.position)
            PrimaryButton(text = "Next", onClick = { state = SlidesReducer.next(state) })
        }
    }
}

/** The slides shown when the host supplies none. */
val DefaultSlides: List<String> =
    listOf("Fast builds", "Modular by design", "Tested end to end", "Ship with confidence")
