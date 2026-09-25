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

import dev.jasonpearson.android.core.model.NavLink
import dev.jasonpearson.android.core.model.SiteSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {

    @Test
    fun `null settings use all fallback socials`() {
        assertEquals(
            SocialLinks(
                github = "https://github.com/kaeawc",
                linkedin = "https://www.linkedin.com/in/jasondpearson/",
                x = "https://x.com/kaeawc",
                website = null,
            ),
            mergeSocials(null),
        )
    }

    @Test
    fun `missing linkedin uses fallback`() {
        assertEquals(
            "https://www.linkedin.com/in/jasondpearson/",
            mergeSocials(settings()).linkedin,
        )
    }

    @Test
    fun `bare linkedin handle is normalized`() {
        assertEquals(
            "https://www.linkedin.com/in/jasondpearson/",
            mergeSocials(settings(linkedin = "jasondpearson")).linkedin,
        )
    }

    @Test
    fun `twitter handle without at prefix is normalized`() {
        assertEquals("https://x.com/kaeawc", mergeSocials(settings(twitter = "@kaeawc")).x)
    }

    @Test
    fun `github navigation url has one trailing slash trimmed`() {
        val result =
            mergeSocials(
                settings(
                    navigation =
                        listOf(NavLink(label = "GitHub", url = "https://github.com/kaeawc/"))
                )
            )
        assertEquals("https://github.com/kaeawc", result.github)
    }

    private fun settings(
        linkedin: String? = null,
        twitter: String? = null,
        navigation: List<NavLink> = emptyList(),
    ) =
        SiteSettings(
            title = "Jason Pearson",
            description = null,
            iconUrl = null,
            coverImageUrl = null,
            url = null,
            twitter = twitter,
            facebook = null,
            linkedin = linkedin,
            navigation = navigation,
        )
}
