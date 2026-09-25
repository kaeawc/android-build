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
package dev.jasonpearson.android.data.projects

private val TAG = Regex("<([a-zA-Z][a-zA-Z0-9-]*)(\\s[^>]*)?>")
private val URL_ATTRIBUTE =
    Regex("""(\s)(src|href)(\s*=\s*)(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val GITHUB_BLOB = Regex("^https://github\\.com/([^/]+)/([^/]+)/(?:blob|raw)/(.+)$")

/**
 * GitHub's rendered README HTML keeps the author's relative URLs, which only work on github.com.
 * Rewrites every `src` to the raw file on `raw.githubusercontent.com` (so images load) and every
 * relative `href` to the file's page on github.com. Absolute URLs, fragments (`#section`) and other
 * schemes (`mailto:`) are left alone, except that `github.com/.../blob/...` image sources are
 * pointed at the raw file since the blob URL is an HTML page, not an image.
 */
internal fun resolveReadmeUrls(html: String, owner: String, repo: String): String =
    TAG.replace(html) { tag ->
        val attributes = tag.groups[2]?.value ?: return@replace tag.value
        val resolved =
            URL_ATTRIBUTE.replace(attributes) { attribute ->
                val (space, name, equals) = attribute.destructured
                val doubleQuoted = attribute.groups[4] != null
                val url = attribute.groups[4]?.value ?: attribute.groupValues[5]
                val isImage = name.equals("src", ignoreCase = true)
                val quote = if (doubleQuoted) "\"" else "'"
                "$space$name$equals$quote${resolveReadmeUrl(url, owner, repo, isImage)}$quote"
            }
        "<${tag.groupValues[1]}$resolved>"
    }

internal fun resolveReadmeUrl(url: String, owner: String, repo: String, isImage: Boolean): String {
    val trimmed = url.trim()
    return when {
        trimmed.isEmpty() || trimmed.startsWith("#") -> url
        trimmed.startsWith("//") -> "https:$trimmed"
        SCHEME.containsMatchIn(trimmed) -> if (isImage) blobToRaw(trimmed) ?: trimmed else trimmed
        else -> {
            val suffixStart = trimmed.indexOfFirst { it == '?' || it == '#' }
            val path = if (suffixStart < 0) trimmed else trimmed.substring(0, suffixStart)
            val suffix = if (suffixStart < 0) "" else trimmed.substring(suffixStart)
            val normalized = normalizeRepoPath(path)
            when {
                isImage -> "https://raw.githubusercontent.com/$owner/$repo/HEAD/$normalized$suffix"
                normalized.isEmpty() -> "https://github.com/$owner/$repo$suffix"
                else -> "https://github.com/$owner/$repo/blob/HEAD/$normalized$suffix"
            }
        }
    }
}

/** README paths are relative to the repo root; `/x` means root too, and `..` can't escape it. */
private fun normalizeRepoPath(path: String): String {
    val segments = ArrayDeque<String>()
    path.split('/').forEach { segment ->
        when (segment) {
            "",
            "." -> Unit
            ".." -> segments.removeLastOrNull()
            else -> segments.addLast(segment)
        }
    }
    val joined = segments.joinToString("/")
    return if (path.endsWith("/") && joined.isNotEmpty()) "$joined/" else joined
}

private fun blobToRaw(url: String): String? =
    GITHUB_BLOB.matchEntire(url)?.destructured?.let { (owner, repo, refAndPath) ->
        "https://raw.githubusercontent.com/$owner/$repo/$refAndPath"
    }
