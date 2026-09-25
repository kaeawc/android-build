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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** GitHub linguist colors (github/linguist `languages.yml`) for the languages likely to show up. */
private val LINGUIST_COLORS: Map<String, Long> =
    mapOf(
        "c" to 0xFF555555,
        "c#" to 0xFF178600,
        "c++" to 0xFFF34B7D,
        "css" to 0xFF663399,
        "dart" to 0xFF00B4AB,
        "dockerfile" to 0xFF384D54,
        "elixir" to 0xFF6E4A7E,
        "go" to 0xFF00ADD8,
        "groovy" to 0xFF4298B8,
        "hcl" to 0xFF844FBA,
        "html" to 0xFFE34C26,
        "java" to 0xFFB07219,
        "javascript" to 0xFFF1E05A,
        "jupyter notebook" to 0xFFDA5B0B,
        "kotlin" to 0xFFA97BFF,
        "lua" to 0xFF000080,
        "makefile" to 0xFF427819,
        "nix" to 0xFF7E7EFF,
        "objective-c" to 0xFF438EFF,
        "php" to 0xFF4F5D95,
        "python" to 0xFF3572A5,
        "ruby" to 0xFF701516,
        "rust" to 0xFFDEA584,
        "scala" to 0xFFC22D40,
        "shell" to 0xFF89E051,
        "starlark" to 0xFF76D275,
        "svelte" to 0xFFFF3E00,
        "swift" to 0xFFF05138,
        "typescript" to 0xFF3178C6,
        "vue" to 0xFF41B883,
        "zig" to 0xFFEC915C,
    )

/** ARGB linguist color for [language], or null when it isn't in the table. */
internal fun linguistColor(language: String): Long? = LINGUIST_COLORS[language.lowercase()]

/** Small filled circle in the language's GitHub color; neutral for unknown languages. */
@Composable
internal fun LanguageDot(language: String, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val color = linguistColor(language)?.let { Color(it) } ?: MaterialTheme.colorScheme.outline
    Box(modifier = modifier.size(size).background(color, CircleShape))
}
