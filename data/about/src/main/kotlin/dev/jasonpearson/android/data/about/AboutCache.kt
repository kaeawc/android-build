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

import dev.jasonpearson.android.core.model.ContentPage
import dev.jasonpearson.android.core.model.NavLink
import dev.jasonpearson.android.core.model.SiteSettings
import kotlinx.serialization.Serializable

@Serializable
internal data class ContentPageCache(val slug: String, val title: String, val html: String) {
    fun toDomain() = ContentPage(slug = slug, title = title, html = html)
}

internal fun ContentPage.toCache() = ContentPageCache(slug = slug, title = title, html = html)

@Serializable
internal data class SiteSettingsCache(
    val title: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val coverImageUrl: String? = null,
    val url: String? = null,
    val twitter: String? = null,
    val facebook: String? = null,
    val linkedin: String? = null,
    val navigation: List<NavLinkCache> = emptyList(),
) {
    fun toDomain() =
        SiteSettings(
            title = title,
            description = description,
            iconUrl = iconUrl,
            coverImageUrl = coverImageUrl,
            url = url,
            twitter = twitter,
            facebook = facebook,
            linkedin = linkedin,
            navigation = navigation.map { it.toDomain() },
        )
}

internal fun SiteSettings.toCache() =
    SiteSettingsCache(
        title = title,
        description = description,
        iconUrl = iconUrl,
        coverImageUrl = coverImageUrl,
        url = url,
        twitter = twitter,
        facebook = facebook,
        linkedin = linkedin,
        navigation = navigation.map { it.toCache() },
    )

@Serializable
internal data class NavLinkCache(val label: String, val url: String) {
    fun toDomain() = NavLink(label = label, url = url)
}

internal fun NavLink.toCache() = NavLinkCache(label = label, url = url)
