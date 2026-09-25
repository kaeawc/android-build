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
package dev.jasonpearson.android.client.ghost.mapper

import dev.jasonpearson.android.client.ghost.dto.GhostPostsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSearchIndexResponse
import dev.jasonpearson.android.client.ghost.dto.GhostSettingsResponse
import dev.jasonpearson.android.client.ghost.dto.GhostTagsResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GhostMappersTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun mapsSettingsAndNavigation() {
        val response =
            json.decodeFromString<GhostSettingsResponse>(
                """{"settings":{"title":"Jason Pearson","description":"Engineering Journeys & Insights","logo":null,"icon":"https://example.com/icon.png","cover_image":"https://example.com/cover.jpg","accent_color":"#123456","url":"https://www.jasonpearson.dev/","twitter":"@kaeawc","facebook":null,"linkedin":null,"navigation":[{"label":"Home","url":"/"},{"label":"Github","url":"https://github.com/kaeawc/"}],"secondary_navigation":[]}}"""
            )

        val settings = response.settings.toSiteSettings()

        assertEquals("Jason Pearson", settings.title)
        assertEquals(null, settings.linkedin)
        assertEquals(2, settings.navigation.size)
        assertEquals("Home", settings.navigation[0].label)
    }

    @Test
    fun mapsTagPostCount() {
        val response =
            json.decodeFromString<GhostTagsResponse>(
                """{"tags":[{"id":"tag-1","name":"Kotlin","slug":"kotlin","description":"Kotlin posts","feature_image":null,"visibility":"public","url":"https://example.com/tag/kotlin/","count":{"posts":13}}],"meta":{"pagination":{"page":1,"limit":100,"pages":1,"total":1,"next":null,"prev":null}}}"""
            )

        val tag = response.tags.single().toTag()

        assertEquals(13, tag.postCount)
    }

    @Test
    fun mapsSearchIndexEntry() {
        val response =
            json.decodeFromString<GhostSearchIndexResponse>(
                """{"posts":[{"id":"post-1","slug":"hello-world","title":"Hello World","excerpt":"A short excerpt","url":"https://example.com/hello-world/","updated_at":"2026-09-24T10:00:00Z","visibility":"public"}]}"""
            )

        val entry = response.posts.single().toSearchEntry()

        assertEquals("post-1", entry.id)
        assertEquals("hello-world", entry.slug)
        assertEquals("Hello World", entry.title)
        assertEquals("A short excerpt", entry.excerpt)
        assertEquals("https://example.com/hello-world/", entry.url)
    }

    @Test
    fun mapsFeaturedPostAndPrimaryTag() {
        val response =
            json.decodeFromString<GhostPostsResponse>(
                """{"posts":[{"id":"post-1","uuid":"uuid-1","slug":"hello-world","title":"Hello World","featured":true,"primary_tag":{"id":"tag-1","slug":"kotlin","name":"Kotlin","count":{"posts":13}}}]}"""
            )

        val article = response.posts.single().toArticle()

        assertEquals(true, article.featured)
        assertEquals("kotlin", article.primaryTag?.slug)
        assertEquals(13, article.primaryTag?.postCount)
    }
}
