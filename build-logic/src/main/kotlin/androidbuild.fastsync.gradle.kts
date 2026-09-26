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

// Fastsync / intransitive sync (Shrinking Elephants, "Fastsync"): during IDE sync,
// stop resolving runtime classpaths transitively. Code completion and highlighting
// only need the *compile* classpath, but the IDE's Gradle model still resolves every
// variant's runtime classpath -- for a large graph that is the bulk of the dependency
// resolution work in a sync. Making those configurations non-transitive keeps their
// direct edges (so the model stays structurally valid) while skipping the transitive
// walk.
//
// Activation is keyed solely off `idea.sync.active`, the system property the IDE sets
// for every sync (and the same key Artifact Swap uses), so CLI builds and CI are
// untouched. Simulate with `./gradlew ... -Didea.sync.active=true`; opt out of the
// behaviour during sync with `-Pfastsync.enabled=false`.
//
// Why a convention plugin and not the upstream `com.fueledbycaffeine.fastsync`
// settings plugin: upstream walks the JVM `SourceSetContainer`, which only covers
// `runtimeClasspath`/`testRuntimeClasspath` on pure-JVM modules. AGP's runtime
// classpaths are variant-named (`debugRuntimeClasspath`, `debugUnitTestRuntimeClasspath`,
// `releaseAndroidTestRuntimeClasspath`, ...) and never appear in that container, so
// matching by name suffix is what actually covers an Android build. Per-project
// (no cross-project wiring) so it stays Isolated-Projects-compatible.
//
// Known trade-off (also called out by Block): a non-transitive runtime classpath can
// affect IDE features that run code, e.g. Compose previews and the debugger's runtime
// classpath, when what they need is only reachable transitively. Flip
// `fastsync.enabled=false` for the session if you hit that.

val RUNTIME_SUFFIX = "RuntimeClasspath"
val COMPILE_SUFFIX = "CompileClasspath"

val ideSyncActive =
    providers.systemProperty("idea.sync.active").map { it.toBoolean() }.getOrElse(false)
val fastsyncEnabled =
    providers.gradleProperty("fastsync.enabled").map { it.toBoolean() }.getOrElse(true)

if (ideSyncActive && fastsyncEnabled) {
    configurations.configureEach {
        if (isCanBeResolved && name.endsWith(RUNTIME_SUFFIX, ignoreCase = true)) {
            isTransitive = false
            // Even without walking the graph, versions still need to agree with the
            // compile classpath: BOM/platform-managed versions live as constraints on
            // the (now unwalked) platform node, so without this every BOM-versioned
            // dependency fails to resolve. Resolving consistently with the matching
            // compile classpath borrows its already-computed versions -- the same
            // trick upstream Fastsync uses. Both the JVM plugin and AGP register the
            // compile classpath before the runtime one, so a direct lookup is safe;
            // if that ever changes, fail loudly rather than silently degrade sync.
            // `runtimeClasspath` (JVM main source set) pairs with `compileClasspath`;
            // prefixed ones (`debugRuntimeClasspath`, `testRuntimeClasspath`) keep the
            // capitalised suffix.
            val prefix = name.dropLast(RUNTIME_SUFFIX.length)
            val compileName =
                if (prefix.isEmpty()) COMPILE_SUFFIX.replaceFirstChar { it.lowercase() }
                else prefix + COMPILE_SUFFIX
            val compile =
                configurations.findByName(compileName)
                    ?: error(
                        "Fastsync: '$name' has no matching '$compileName' to resolve " +
                            "consistently with; adjust androidbuild.fastsync or disable it " +
                            "for this sync with -Pfastsync.enabled=false."
                    )
            shouldResolveConsistentlyWith(compile)
            // AGP later re-points test-component runtime classpaths (e.g.
            // `debugAndroidTestRuntimeClasspath`) at the tested variant's runtime
            // classpath, overriding the call above. That source is itself
            // non-transitive here, so it never saw the test-only BOM-managed
            // dependencies (`ui-test-junit4`), which then resolve FAILED. Re-assert the
            // compile twin at resolution time, after every plugin has configured.
            incoming.beforeResolve { shouldResolveConsistentlyWith(compile) }
        }
    }
}
