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
package dev.jasonpearson.android.foundation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkRouterTest {
    @Test
    fun webRoutes() {
        assertEquals(
            listOf(AppDestination.Articles),
            DeepLinkRouter.route("https://www.jasonpearson.dev/"),
        )
        assertEquals(
            listOf(AppDestination.Talks),
            DeepLinkRouter.route("https://jasonpearson.dev/talks/"),
        )
        assertEquals(
            listOf(AppDestination.Articles, AppDestination.ArticleDetail("automobile-mobile-use")),
            DeepLinkRouter.route("https://www.jasonpearson.dev/automobile-mobile-use/?ref=x#y"),
        )
        assertEquals(
            listOf(AppDestination.Articles, AppDestination.Tag("android")),
            DeepLinkRouter.route("https://www.jasonpearson.dev/tag/android/"),
        )
        assertEquals(
            listOf(AppDestination.About),
            DeepLinkRouter.route("https://WWW.JASONPEARSON.DEV/about-me"),
        )
        assertEquals(
            listOf(AppDestination.About),
            DeepLinkRouter.route("https://jasonpearson.dev/about"),
        )
        assertEquals(
            listOf(AppDestination.Projects),
            DeepLinkRouter.route("https://jasonpearson.dev/oss"),
        )
        assertEquals(
            listOf(AppDestination.Photography),
            DeepLinkRouter.route("https://jasonpearson.dev/photography"),
        )
        assertEquals(
            listOf(AppDestination.Articles, AppDestination.ArticleDetail("hello world")),
            DeepLinkRouter.route("https://www.jasonpearson.dev/hello%20world"),
        )
        assertNull(DeepLinkRouter.route("https://www.jasonpearson.dev/ghost/api/content/"))
        assertNull(DeepLinkRouter.route("https://www.jasonpearson.dev/tag"))
        assertNull(DeepLinkRouter.route("https://example.com/foo"))
    }

    @Test
    fun customSchemeRoutes() {
        assertEquals(
            listOf(
                AppDestination.Articles,
                AppDestination.ArticleDetail("metaspace-in-jvm-builds"),
            ),
            DeepLinkRouter.route("jasonpearsondev://article/metaspace-in-jvm-builds"),
        )
        assertEquals(
            listOf(AppDestination.Projects, AppDestination.ProjectDetail("auto-mobile")),
            DeepLinkRouter.route("jasonpearsondev://project/auto-mobile"),
        )
        assertEquals(
            listOf(AppDestination.About, AppDestination.Settings),
            DeepLinkRouter.route("jasonpearsondev://settings"),
        )
        assertEquals(
            listOf(AppDestination.Articles, AppDestination.Saved),
            DeepLinkRouter.route("jasonpearsondev://saved"),
        )
        assertEquals(
            listOf(AppDestination.Articles, AppDestination.Tag("android")),
            DeepLinkRouter.route("jasonpearsondev://tag/android"),
        )
        assertEquals(
            listOf(AppDestination.Articles, AppDestination.Search),
            DeepLinkRouter.route("jasonpearsondev://search"),
        )
        assertEquals(
            listOf(AppDestination.Articles),
            DeepLinkRouter.route("jasonpearsondev://articles"),
        )
        assertEquals(listOf(AppDestination.Talks), DeepLinkRouter.route("jasonpearsondev://talks"))
        assertEquals(
            listOf(AppDestination.Projects),
            DeepLinkRouter.route("jasonpearsondev://projects"),
        )
        assertEquals(
            listOf(AppDestination.Photography),
            DeepLinkRouter.route("jasonpearsondev://photography"),
        )
        assertEquals(listOf(AppDestination.About), DeepLinkRouter.route("jasonpearsondev://about"))
        assertNull(DeepLinkRouter.route("jasonpearsondev://article"))
        assertNull(DeepLinkRouter.route("jasonpearsondev://article/a/b"))
        assertNull(DeepLinkRouter.route("jasonpearsondev://project"))
        assertNull(DeepLinkRouter.route("jasonpearsondev://tag"))
        assertNull(DeepLinkRouter.route("jasonpearsondev://unknown"))
    }

    @Test
    fun rejectsMalformedAndUnsupportedUris() {
        assertNull(DeepLinkRouter.route("not a uri %%%"))
        assertNull(DeepLinkRouter.route("mailto:foo@bar.com"))
    }

    @Test
    fun mapsEveryDestinationToItsTopLevelEntry() {
        assertEquals(AppDestination.Articles, DeepLinkRouter.topLevelFor(AppDestination.Articles))
        assertEquals(
            AppDestination.Articles,
            DeepLinkRouter.topLevelFor(AppDestination.ArticleDetail("x")),
        )
        assertEquals(AppDestination.Articles, DeepLinkRouter.topLevelFor(AppDestination.Tag("x")))
        assertEquals(AppDestination.Articles, DeepLinkRouter.topLevelFor(AppDestination.Search))
        assertEquals(AppDestination.Talks, DeepLinkRouter.topLevelFor(AppDestination.Talks))
        assertEquals(AppDestination.Projects, DeepLinkRouter.topLevelFor(AppDestination.Projects))
        assertEquals(
            AppDestination.Projects,
            DeepLinkRouter.topLevelFor(AppDestination.ProjectDetail("x")),
        )
        assertEquals(
            AppDestination.Photography,
            DeepLinkRouter.topLevelFor(AppDestination.Photography),
        )
        assertEquals(AppDestination.About, DeepLinkRouter.topLevelFor(AppDestination.About))
        assertEquals(AppDestination.About, DeepLinkRouter.topLevelFor(AppDestination.Settings))
        assertEquals(AppDestination.Articles, DeepLinkRouter.topLevelFor(AppDestination.Saved))
    }
}
