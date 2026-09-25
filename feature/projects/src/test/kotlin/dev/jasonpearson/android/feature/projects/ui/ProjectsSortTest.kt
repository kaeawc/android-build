package dev.jasonpearson.android.feature.projects.ui

import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.subsystem.experimentation.Treatment
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectsSortTest {

    @Test
    fun `control keeps the input order`() {
        val projects =
            listOf(
                project(1, "older", "2024-01-01T00:00:00Z"),
                project(2, "newer", "2024-03-01T00:00:00Z"),
            )

        assertEquals(projects, sortOthers(projects, Treatment.CONTROL))
    }

    @Test
    fun `variant sorts by update time descending`() {
        val oldest = project(1, "oldest", "2024-01-01T00:00:00Z")
        val newest = project(2, "newest", "2024-03-01T00:00:00Z")
        val middle = project(3, "middle", "2024-02-01T00:00:00Z")

        assertEquals(
            listOf(newest, middle, oldest),
            sortOthers(listOf(oldest, newest, middle), Treatment.VARIANT),
        )
    }

    @Test
    fun `variant puts null update times last and preserves their relative order`() {
        val nullFirst = project(1, "null-first", null)
        val datedOld = project(2, "dated-old", "2024-01-01T00:00:00Z")
        val nullSecond = project(3, "null-second", null)
        val datedNew = project(4, "dated-new", "2024-02-01T00:00:00Z")

        assertEquals(
            listOf(datedNew, datedOld, nullFirst, nullSecond),
            sortOthers(listOf(nullFirst, datedOld, nullSecond, datedNew), Treatment.VARIANT),
        )
    }

    private fun project(id: Long, name: String, updatedAt: String?): Project =
        Project(
            id = id,
            name = name,
            description = null,
            url = "https://example.com/$name",
            stars = id.toInt(),
            language = null,
            topics = emptyList(),
            updatedAt = updatedAt?.let(Instant::parse),
        )
}
