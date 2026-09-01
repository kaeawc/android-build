/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
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
pluginManagement {
    // Convention plugins (e.g. androidbuild.kotlin-common) live in the build-logic
    // included build.
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        // Uncomment to pin R8 version from the R8 releases repo
        // exclusiveContent {
        //     forRepository {
        //         maven("https://storage.googleapis.com/r8-releases/raw") { name = "R8-releases" }
        //     }
        //     filter { includeModule("com.android.tools", "r8") }
        // }
    }

    plugins {
        id("com.gradle.develocity") version "4.0.2"
        id("com.fueledbycaffeine.spotlight") version "1.7.0"
        id("xyz.block.artifactswap.settings") version "0.1.12"
    }
}

// Spotlight moves the project `include`s into gradle/all-projects.txt and computes the
// dependency graph by parsing build scripts, so the IDE can sync a focused subset of
// projects (gradle/ide-projects.txt). Artifact Swap builds on top of it: for projects
// outside the focused set that are unchanged vs the BOM branch, it swaps the local
// project for a pre-compiled artifact so Gradle never configures it. Artifact Swap
// applies Spotlight itself, so Spotlight is declared here with `apply false`.
plugins {
    // Artifact Swap's settings plugin references the Develocity build-scan API, so the
    // Develocity plugin must be on the settings classpath. Applied without a server, it
    // only provides the classes -- no build scans are published.
    id("com.gradle.develocity")
    id("com.fueledbycaffeine.spotlight") apply false
    id("xyz.block.artifactswap.settings")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Gradle's generated type-safe project accessors (TYPESAFE_PROJECT_ACCESSORS) are
// intentionally NOT enabled: they require every referenced project to exist in the
// build, while Artifact Swap excludes swapped projects during IDE sync. Modules use
// the hand-maintained, swap-aware `projects.*` accessors from
// build-logic (dev.jasonpearson.gradle.SwappableProjectDependencies) instead --
// same call-site syntax, but each accessor resolves to the real project or a
// pre-compiled artifact as appropriate. See docs/artifact-swap.md.
rootProject.name =
    "android-build"

// Project `include`s live in gradle/all-projects.txt, managed by the Spotlight
// plugin applied above. Add or remove projects there (or via ./gradlew
// :fixAllProjectsList), not here. `./gradlew :checkAllProjectsList` verifies this
// file contains no stray `include`s.
