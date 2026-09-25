package dev.jasonpearson.android.data.articles

import dev.jasonpearson.android.core.model.SearchEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleSearchTest {
    private val entries =
        listOf(
            entry("excerpt", "Notes", "Kotlin Compose patterns"),
            entry("title", "Kotlin Compose guide", null),
            entry("split", "Kotlin basics", "Compose tips"),
            entry("other", "Android", "Compose only"),
            entry("title-later", "Compose and Kotlin", null),
        )

    @Test
    fun multiTokenQueryMatchesAcrossTitleAndExcerpt() {
        assertEquals(
            listOf("title", "title-later", "excerpt", "split"),
            filterSearchIndex(entries, "Kotlin Compose").map { it.id },
        )
    }

    @Test
    fun titleMatchesRankFirstAndPreserveIndexOrder() {
        assertEquals(
            listOf("title", "title-later", "excerpt", "split"),
            filterSearchIndex(entries, "compose kotlin").map { it.id },
        )
    }

    @Test
    fun searchIsCaseInsensitive() {
        assertEquals(
            listOf("title", "title-later", "excerpt", "split"),
            filterSearchIndex(entries, "KoTlIn   CoMpOsE").map { it.id },
        )
    }

    @Test
    fun blankQueryReturnsEmptyList() {
        assertEquals(emptyList<SearchEntry>(), filterSearchIndex(entries, " \t  "))
    }

    @Test
    fun queryWithNoMatchesReturnsEmptyList() {
        assertEquals(emptyList<SearchEntry>(), filterSearchIndex(entries, "swift"))
    }

    private fun entry(id: String, title: String, excerpt: String?) =
        SearchEntry(id = id, slug = id, title = title, excerpt = excerpt, url = null)
}
