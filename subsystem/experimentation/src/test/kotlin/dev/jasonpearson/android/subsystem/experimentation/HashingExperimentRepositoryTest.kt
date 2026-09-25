package dev.jasonpearson.android.subsystem.experimentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashingExperimentRepositoryTest {

    @Test
    fun `same install and experiment always receive the same treatment`() {
        val experiment = Experiment(id = "projects_default_sort")
        val first = HashingExperimentRepository("stable-install-id")
        val second = HashingExperimentRepository("stable-install-id")
        val expected = first.treatmentFor(experiment)

        repeat(10) { assertEquals(expected, first.treatmentFor(experiment)) }
        assertEquals(expected, second.treatmentFor(experiment))
    }

    @Test
    fun `two treatments split install ids roughly evenly`() {
        val experiment = Experiment(id = "projects_default_sort", treatments = Treatment.entries)
        val counts = Treatment.entries.associateWith { 0 }.toMutableMap()
        repeat(1_000) { index ->
            val treatment = HashingExperimentRepository("install-$index").treatmentFor(experiment)
            counts[treatment] = counts.getValue(treatment) + 1
        }

        Treatment.entries.forEach { treatment ->
            assertTrue(
                "${counts.getValue(treatment)} assignments for $treatment",
                counts.getValue(treatment) in 400..600,
            )
        }
    }

    @Test
    fun `empty treatment list returns the experiment default`() {
        val experiment =
            Experiment(id = "empty", default = Treatment.VARIANT, treatments = emptyList())

        assertEquals(
            Treatment.VARIANT,
            HashingExperimentRepository("install-a").treatmentFor(experiment),
        )
        assertEquals(
            Treatment.VARIANT,
            HashingExperimentRepository("install-b").treatmentFor(experiment),
        )
    }
}
