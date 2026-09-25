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
