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
import dev.jasonpearson.gradle.PrefetchDependenciesTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

// Dependency pre-fetching (Shrinking Elephants): a `prefetchDependencies` task on every
// module that resolves the external half of each compile and runtime classpath and
// nothing else. Running it warms ~/.gradle/caches/modules-2 (and the transforms cache
// for AARs) so a later IDE sync or build on the same branch never waits on downloads.
// It is wired into .githooks/post-checkout as an opt-in background job on branch
// switches; see scripts/prefetch-dependencies.sh and the README.
//
// Per-project and dependency-free across projects, so it is Isolated-Projects-safe:
// `./gradlew prefetchDependencies` simply runs the task in every project that has it.

val RESOLVABLE_SUFFIXES = listOf("CompileClasspath", "RuntimeClasspath")

val prefetch =
    tasks.register<PrefetchDependenciesTask>("prefetchDependencies") {
        group = "help"
        description =
            "Downloads every external dependency on this project's compile and runtime " +
                "classpaths (warms the Gradle dependency cache); no compilation."
    }

configurations.configureEach {
    if (isCanBeResolved && RESOLVABLE_SUFFIXES.any { name.endsWith(it, ignoreCase = true) }) {
        val external = incoming.artifactView {
            // Skip project dependencies: they would drag in compile tasks. Lenient so
            // one unresolvable artifact does not abort the rest of the warm-up.
            componentFilter { it is ModuleComponentIdentifier }
            isLenient = true
        }
        prefetch.configure { artifacts.from(external.files) }
    }
}
