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
//import com.diffplug.gradle.spotless.KotlinExtension
//import com.diffplug.gradle.spotless.SpotlessExtension
//import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
//import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Buildscript dependencies moved to settings.gradle.kts so they're available
// to settings plugins like artifact-swap

plugins {
    `version-catalog`
//    alias(libs.plugins.spotless)
    //    alias(libs.plugins.doctor)
    alias(libs.plugins.dependencyAnalysis)
    // Kotlin/Android plugins are already loaded in buildscript classpath in settings.gradle.kts
    // for artifact-swap plugin compatibility, so we can't redeclare them here
//    alias(libs.plugins.kotlin.android) apply false
//    alias(libs.plugins.android.library) apply false
//    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.metro) apply false
}

val externalFiles = listOf("MemoizedSequence").map { "src/**/$it.kt" }
val gradleWorkerJvmArgs = providers.gradleProperty("org.gradle.testWorker.jvmargs").get()

allprojects {
//    apply(plugin = "com.diffplug.spotless")
//    val spotlessFormatters: SpotlessExtension.() -> Unit = {
//        lineEndings = LineEnding.PLATFORM_NATIVE
//
//        format("misc") {
//            target("*.md", ".gitignore")
//            trimTrailingWhitespace()
//            endWithNewline()
//        }
//        kotlin {
//            target("src/**/*.kt")
//            targetExclude(externalFiles)
//            trimTrailingWhitespace()
//            endWithNewline()
//            licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
//            targetExclude("**/copyright.kt", *externalFiles.toTypedArray())
//        }
//        format("kotlinExternal", KotlinExtension::class.java) {
//            target(externalFiles)
//            trimTrailingWhitespace()
//            endWithNewline()
//            targetExclude("**/copyright.kt")
//        }
//        kotlinGradle {
//            target("*.kts")
//            trimTrailingWhitespace()
//            endWithNewline()
//            licenseHeaderFile(
//                rootProject.file("spotless/copyright.kt"),
//                "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
//            )
//        }
//    }
//    configure<SpotlessExtension> {
//        spotlessFormatters()
//        if (project.rootProject == project) {
//            predeclareDeps()
//        }
//    }
//    if (project.rootProject == project) {
//        configure<SpotlessExtensionPredeclare> { spotlessFormatters() }
//    }

    tasks.withType<Test>().configureEach { jvmArgs(gradleWorkerJvmArgs) }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            languageVersion.set(
                KotlinVersion.valueOf(
                    "KOTLIN_${libs.versions.build.kotlin.language.get().replace(".", "_")}"
                )
            )
            jvmTarget.set(JvmTarget.valueOf("JVM_${libs.versions.build.java.target.get()}"))
            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=kotlin.time.ExperimentalTime,kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlin.ExperimentalUnsignedTypes",
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview",
                    "-Xcontext-parameters",
                )
            )
        }
    }
}

// doctor {
//    /**
//     * Throw an exception when multiple Gradle Daemons are running.
//     *
//     * Windows is not supported yet, see https://github.com/runningcode/gradle-doctor/issues/84
//     */
//    disallowMultipleDaemons.set(false)
//    /**
//     * Show a message if the download speed is less than this many megabytes / sec.
//     */
//    downloadSpeedWarningThreshold.set(.5f)
//    /**
//     * The level at which to warn when a build spends more than this percent garbage collecting.
//     */
//    GCWarningThreshold.set(0.10f)
//    /**
//     * The level at which to fail when a build spends more than this percent garbage collecting.
//     */
//    GCFailThreshold = 0.9f
//    /**
//     * Print a warning to the console if we spend more than this amount of time with Dagger
// annotation processors.
//     */
//    daggerThreshold.set(5000)
//    /**
//     * By default, Gradle caches test results. This can be dangerous if tests rely on timestamps,
// dates, or other files
//     * which are not declared as inputs. We should therefore write tests with this in mind so that
// we can keep
//     * test caching enabled.
//     */
//    enableTestCaching.set(true)
//    /**
//     * By default, Gradle treats empty directories as inputs to compilation tasks. This can cause
// cache misses.
//     */
//    failOnEmptyDirectories.set(true)
//    /**
//     * Do not allow building all apps simultaneously. This is likely not what the user intended.
//     */
//    allowBuildingAllAndroidAppsSimultaneously.set(false)
//    /**
//     * Warn if using Android Jetifier. It slows down builds.
//     */
//    warnWhenJetifierEnabled.set(true)
//    /**
//     * Negative Avoidance Savings Threshold
//     * By default the Gradle Doctor will print out a warning when a task is slower to pull from
// the cache than to
//     * re-execute. There is some variance in the amount of time a task can take when several tasks
// are running
//     * concurrently. In order to account for this there is a threshold you can set. When the
// difference is above the
//     * threshold, a warning is displayed.
//     */
//    negativeAvoidanceThreshold.set(500)
//    /**
//     * Do not warn when not using ParallelGC, currently evaluating G1Gc on JDK 21.
//     */
//    warnWhenNotUsingParallelGC.set(false)
//    /**
//     * Throws an error when the `Delete` or `clean` task has dependencies.
//     * If a clean task depends on other tasks, clean can be reordered and made to run after the
// tasks that would produce
//     * output. This can lead to build failures or just strangeness with seemingly straightforward
// builds
//     * (e.g., gradle clean build).
//     * http://github.com/gradle/gradle/issues/2488
//     */
//    disallowCleanTaskDependencies.set(true)
//    /**
//     * Warn if using the Kotlin Compiler Daemon Fallback. The fallback is incredibly slow and
// should be avoided.
//     * https://youtrack.jetbrains.com/issue/KT-48843
//     */
//    warnIfKotlinCompileDaemonFallback.set(true)
//
//    /** Configuration properties relating to JAVA_HOME */
//    javaHome {
//        /**
//         * Ensure that we are using JAVA_HOME to build with this Gradle.
//         */
//        ensureJavaHomeMatches.set(true)
//        /**
//         * Ensure we have JAVA_HOME set.
//         */
//        ensureJavaHomeIsSet.set(true)
//        /**
//         * Fail on any `JAVA_HOME` issues.
//         */
//        failOnError.set(true)
//        /**
//         * Extra message text, if any, to show with the Gradle Doctor message. This is useful if
// you have a wiki page or
//         * other instructions that you want to link for developers on your team if they encounter
// an issue.
//         */
//        // extraMessage.set("TODO: Once Java / JVM documentation is written")
//    }
// }
