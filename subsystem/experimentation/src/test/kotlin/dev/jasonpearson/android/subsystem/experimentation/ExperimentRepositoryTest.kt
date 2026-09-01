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
import org.junit.Test

class ExperimentRepositoryTest {

    private val partyMode = Experiment(id = "party_mode", default = Treatment.CONTROL)

    @Test
    fun `falls back to the experiment default when unassigned`() {
        val repository = InMemoryExperimentRepository()
        assertEquals(Treatment.CONTROL, repository.treatmentFor(partyMode))
    }

    @Test
    fun `honors an explicit assignment`() {
        val repository = InMemoryExperimentRepository(mapOf("party_mode" to Treatment.VARIANT))
        assertEquals(Treatment.VARIANT, repository.treatmentFor(partyMode))
    }
}
