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
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

// Publishes each library module to GitHub Packages so artifact-swap can substitute
// a pre-compiled artifact for a local project. Applied automatically by the
// androidbuild.android-library and androidbuild.kotlin-jvm conventions; the app is
// never published. artifactId is the module's Gradle path flattened with dashes
// (e.g. :core:common -> core-common) so names never collide across layers.
plugins { id("maven-publish") }

// Every green main publishes a new, immutable version: the base VERSION_NAME plus
// the commit short SHA. GitHub Packages forbids overwriting an existing version, so
// a fixed version (e.g. -SNAPSHOT) would 409 on the second publish. The SHA comes
// from GITHUB_SHA on CI, falling back to `git rev-parse HEAD` locally, then "local".
val commitSha =
    providers
        .environmentVariable("GITHUB_SHA")
        .orElse(providers.exec { commandLine("git", "rev-parse", "HEAD") }.standardOutput.asText)
        .map { it.trim().take(7) }
        .orElse("local")

group = providers.gradleProperty("GROUP").get()

version = "${providers.gradleProperty("VERSION_NAME").get()}-${commitSha.get()}"

val moduleArtifactId = path.removePrefix(":").replace(":", "-")

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kaeawc/android-build")
            credentials {
                username =
                    providers
                        .gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orNull
                password =
                    providers
                        .gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orNull
            }
        }
    }
}

// Android library modules: publish the release variant (with sources).
pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        publishing { singleVariant("release") { withSourcesJar() } }
    }
    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])
                    artifactId = moduleArtifactId
                }
            }
        }
    }
}

// Pure-JVM modules: publish the java component (with sources).
pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<JavaPluginExtension> { withSourcesJar() }
    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifactId = moduleArtifactId
                }
            }
        }
    }
}
