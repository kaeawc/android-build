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
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Shared Kotlin compiler configuration for every Kotlin module (JVM and Android):
// the language version, JVM target, opt-in list, and Java toolchain, all sourced
// from the single version catalog. Centralizing this here avoids re-declaring the
// same block in every module build file and keeps the build compatible with
// Isolated Projects -- there is no cross-project `subprojects {}`/`allprojects {}`
// wiring, which that feature forbids.

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val kotlinLanguageVersion = libs.findVersion("build-kotlin-language").get().requiredVersion
val javaTarget = libs.findVersion("build-java-target").get().requiredVersion

// Pin compilation to the Java toolchain so `./gradlew` behaves identically
// regardless of the developer's default JDK. AGP registers JavaPluginExtension for
// Android modules too, so this single hook covers both JVM and Android modules;
// KGP picks up the same Java toolchain.
plugins.withType<JavaBasePlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaTarget.toInt())) }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.fromVersion(kotlinLanguageVersion))
        jvmTarget.set(JvmTarget.fromTarget(javaTarget))
        // addAll (not assignment) so plugin-contributed args -- Compose, Metro, etc.
        // -- are preserved alongside this project-wide opt-in list.
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-Xcontext-parameters",
        )
    }
}

// Carries this project into the dev.jasonpearson.gradle.project(String) dependency
// override (see SwappableProjectDependencies.kt) so it can consult Artifact Swap
// state when rewriting project dependencies during IDE sync.
dependencies.extensions.add(
    dev.jasonpearson.gradle.SWAP_CONTEXT_EXTENSION,
    dev.jasonpearson.gradle.SwapContext(project),
)
