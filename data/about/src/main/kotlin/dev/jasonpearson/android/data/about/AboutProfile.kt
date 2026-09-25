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
package dev.jasonpearson.android.data.about

import dev.jasonpearson.android.core.model.SiteSettings

data class SocialLinks(
    val github: String,
    val linkedin: String,
    val x: String?,
    val website: String?,
)

data class AboutProfile(
    val title: String,
    val description: String?,
    val iconUrl: String?,
    val socials: SocialLinks,
)

internal fun mergeSocials(settings: SiteSettings?): SocialLinks {
    val github =
        settings
            ?.navigation
            ?.firstOrNull { it.url.contains("github.com") }
            ?.url
            ?.let { url -> if (url.endsWith('/')) url.dropLast(1) else url }
            ?: "https://github.com/kaeawc"

    val linkedin =
        settings
            ?.linkedin
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                val trimmed = value.trim()
                val handle =
                    if (trimmed.contains("/in/")) {
                        trimmed
                            .substringAfter("/in/")
                            .substringBefore('?')
                            .substringBefore('#')
                            .split('/')
                            .lastOrNull { it.isNotBlank() }
                            .orEmpty()
                    } else {
                        trimmed.trimStart('@', '/').trimEnd('/')
                    }
                if (handle.isBlank()) "jasondpearson" else handle
            }
            ?.let { "https://www.linkedin.com/in/$it/" }
            ?: "https://www.linkedin.com/in/jasondpearson/"

    val x =
        settings
            ?.twitter
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?.trimStart('@', '/')
            ?.takeIf { it.isNotBlank() }
            ?.let { "https://x.com/$it" } ?: "https://x.com/kaeawc"

    return SocialLinks(github = github, linkedin = linkedin, x = x, website = settings?.url)
}
