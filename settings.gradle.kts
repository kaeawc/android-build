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
    }
}

// The Gradle build exists so Android Studio can sync and run the project; Bazel is the build
// used on the command line and in CI (see docs/bazel.md). Keep both in step: a module added
// here also needs a BUILD.bazel.
plugins {
    // Lets Gradle download the JDK toolchain (build-java-toolchain) and the daemon JVM
    // (gradle/gradle-daemon-jvm.properties) when no matching local JDK is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // Krit (krit.yml) on every module. CI runs its kritExportModel task: each module's source
    // sets, compile classpath and Kotlin compiler plugins, which Krit's FIR checks compile
    // against. Keep the version in step with kaeawc/krit in .github/workflows/commit.yml.
    id("dev.jasonpearson.krit.settings") version "0.5.0"
}

krit {
    config = file("krit.yml")
    baseline = file("ci/krit-baseline.xml")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "android-build"

include(
    ":app",
    ":client:ghost",
    ":client:github",
    ":core:common",
    ":core:di",
    ":core:model",
    ":core:network",
    ":data:about",
    ":data:articles",
    ":data:bookmarks",
    ":data:photography",
    ":data:projects",
    ":data:settings",
    ":data:talks",
    ":feature:about",
    ":feature:articles",
    ":feature:photography",
    ":feature:projects",
    ":feature:saved",
    ":feature:settings",
    ":feature:talks",
    ":feature:widget",
    ":foundation:designassets",
    ":foundation:designsystem",
    ":foundation:navigation",
    ":subsystem:analytics",
    ":subsystem:experimentation",
    ":subsystem:storage",
)
